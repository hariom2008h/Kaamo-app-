-- Kaamo backend for Supabase (Postgres). Run once in the Supabase SQL editor.
--
-- Design rules (from the legal / DPDP review):
--  * The app never reads the partners table. It only calls the functions below.
--  * Phone numbers are never returned by list_partners (masked only). The full number comes only from
--    reveal_contact(), which needs a signed-in user, is rate limited (10/hour, 30/day) and is logged.
--  * A partner is listed only if phone_public = true (separate, withdrawable consent, default false),
--    status = 'active' and required documents are not expired.
--  * consent_audit_logs is append-only. Withdrawing consent is a new row, never an update.
--  * Replace the placeholder wording in the app with counsel-approved text before launch.
--
-- Enable phone auth in Supabase (Authentication > Providers > Phone) with an SMS provider that
-- supports India (DLT registration is required for SMS in India).

create extension if not exists pgcrypto;

-- ---------------------------------------------------------------- tables

create table if not exists public.profiles (
  id          uuid primary key references auth.users (id) on delete cascade,
  role        text not null check (role in ('customer', 'partner')),
  full_name   text not null,
  location    text,
  created_at  timestamptz not null default now()
);

create table if not exists public.partners (
  id              uuid primary key default gen_random_uuid(),
  user_id         uuid not null unique references auth.users (id) on delete cascade,
  full_name       text not null,
  village         text not null,
  district        text not null,
  category        text not null check (category in
                    ('plumber', 'electrician', 'construction', 'borewell', 'jcb', 'tractor', 'transport')),
  sub_categories  text[] not null default '{}',
  service_areas   text[] not null default '{}',
  cev_use         text check (cev_use in ('on_site', 'low_bed_transport')),
  indicative_rate text,
  rc_number       text,
  insurance_until date,
  fitness_until   date,
  phone           text not null check (phone ~ '^[0-9]{10}$'),
  phone_public    boolean not null default false,
  notice_language text not null default 'en',
  status          text not null default 'active' check (status in ('active', 'removed', 'suspended')),
  created_at      timestamptz not null default now(),
  updated_at      timestamptz not null default now()
);
create index if not exists partners_listing_idx on public.partners (status, phone_public, category);

create table if not exists public.consent_audit_logs (
  id            bigint generated always as identity primary key,
  user_id       uuid not null references auth.users (id) on delete cascade,
  consent_type  text not null check (consent_type in ('TermsAndPrivacy', 'PartnerDeclaration', 'PublicDirectoryPhone')),
  action        text not null check (action in ('Granted', 'Withdrawn')),
  version       text not null,
  language      text not null,
  created_at    timestamptz not null default now()
);

create table if not exists public.contact_reveal_logs (
  id          bigint generated always as identity primary key,
  user_id     uuid not null references auth.users (id) on delete cascade,
  partner_id  uuid not null references public.partners (id) on delete cascade,
  created_at  timestamptz not null default now()
);
create index if not exists reveal_user_time_idx on public.contact_reveal_logs (user_id, created_at desc);

create table if not exists public.listing_reports (
  id           bigint generated always as identity primary key,
  reporter_id  uuid not null references auth.users (id) on delete cascade,
  partner_id   uuid not null references public.partners (id) on delete cascade,
  reason       text not null check (reason in ('stolen_or_illegal', 'fake', 'unsafe_use', 'wrong_info', 'other')),
  details      text,
  status       text not null default 'open' check (status in ('open', 'acknowledged', 'resolved')),
  created_at   timestamptz not null default now()
);

-- Audit tables are append-only for everyone, including privileged roles.
create or replace function public.block_mutation() returns trigger
language plpgsql as $$
begin
  raise exception 'this table is append-only';
end;
$$;

drop trigger if exists consent_no_update on public.consent_audit_logs;
create trigger consent_no_update before update or delete on public.consent_audit_logs
  for each row execute function public.block_mutation();

drop trigger if exists reveal_no_update on public.contact_reveal_logs;
create trigger reveal_no_update before update on public.contact_reveal_logs
  for each row execute function public.block_mutation();

-- ---------------------------------------------------------------- row level security
-- RLS on, and no policies for the app roles on the audit tables: the app cannot read or write
-- them directly. A partner may read only their own row (never other people's phone numbers).

alter table public.profiles            enable row level security;
alter table public.partners            enable row level security;
alter table public.consent_audit_logs  enable row level security;
alter table public.contact_reveal_logs enable row level security;
alter table public.listing_reports     enable row level security;

drop policy if exists profiles_select_own on public.profiles;
create policy profiles_select_own on public.profiles for select to authenticated using (id = auth.uid());

drop policy if exists partners_select_own on public.partners;
create policy partners_select_own on public.partners for select to authenticated using (user_id = auth.uid());

revoke all on public.profiles, public.partners, public.consent_audit_logs,
              public.contact_reveal_logs, public.listing_reports from anon, authenticated;
grant select on public.profiles, public.partners to authenticated;

-- ---------------------------------------------------------------- helpers

-- The signed-in user's verified mobile (last 10 digits). Phone auth stores it as 91XXXXXXXXXX.
create or replace function public.current_mobile() returns text
language sql stable security definer set search_path = public, auth as $$
  select right(regexp_replace(coalesce(u.phone, ''), '[^0-9]', '', 'g'), 10)
  from auth.users u where u.id = auth.uid();
$$;

-- Server-side twin of the app's keyword guard: no construction-material haulage (MVA s.66 / s.192A risk).
-- Have counsel review this list. Matching is on whole words, case-insensitive.
create or replace function public.has_prohibited_term(p_text text) returns boolean
language sql immutable as $$
  select exists (
    select 1
    from regexp_split_to_table(lower(coalesce(p_text, '')), '[^[:alnum:]ऀ-ॿ]+') as w
    where w in ('sand', 'bajri', 'bajari', 'mitti', 'eent', 'int', 'brick', 'bricks', 'cement', 'mining',
                'रेत', 'बजरी', 'मिट्टी', 'ईंट')
  );
$$;

-- Vehicle categories must hold unexpired documents to stay listed.
create or replace function public.listing_is_current(p public.partners) returns boolean
language sql immutable as $$
  select (p.category not in ('borewell', 'jcb', 'tractor', 'transport') or
          (p.insurance_until is not null and p.insurance_until >= current_date))
     and (p.category not in ('borewell', 'jcb', 'transport') or
          (p.fitness_until is not null and p.fitness_until >= current_date));
$$;

-- ---------------------------------------------------------------- app functions

create or replace function public.register_customer(p_full_name text, p_location text) returns void
language plpgsql security definer set search_path = public as $$
begin
  if auth.uid() is null then raise exception 'not_signed_in'; end if;
  if length(trim(coalesce(p_full_name, ''))) = 0 then raise exception 'name_required'; end if;
  insert into public.profiles (id, role, full_name, location)
  values (auth.uid(), 'customer', trim(p_full_name), nullif(trim(coalesce(p_location, '')), ''))
  on conflict (id) do update set full_name = excluded.full_name, location = excluded.location;
end;
$$;

create or replace function public.register_partner(
  p_full_name       text,
  p_village         text,
  p_district        text,
  p_category        text,
  p_sub_categories  text[],
  p_service_areas   text[],
  p_phone_public    boolean,
  p_notice_language text,
  p_indicative_rate text default null,
  p_rc_number       text default null,
  p_insurance_until date default null,
  p_fitness_until   date default null,
  p_cev_use         text default null
) returns void
language plpgsql security definer set search_path = public as $$
declare
  v_mobile text := public.current_mobile();
begin
  if auth.uid() is null then raise exception 'not_signed_in'; end if;
  if v_mobile is null or v_mobile !~ '^[0-9]{10}$' then raise exception 'mobile_not_verified'; end if;
  if length(trim(coalesce(p_full_name, ''))) = 0 then raise exception 'name_required'; end if;

  if public.has_prohibited_term(array_to_string(coalesce(p_service_areas, '{}') || coalesce(p_sub_categories, '{}'), ' ')
                                || ' ' || coalesce(p_indicative_rate, '')) then
    raise exception 'prohibited_term';
  end if;

  insert into public.profiles (id, role, full_name, location)
  values (auth.uid(), 'partner', trim(p_full_name), trim(p_village))
  on conflict (id) do update set role = 'partner', full_name = excluded.full_name, location = excluded.location;

  insert into public.partners (
    user_id, full_name, village, district, category, sub_categories, service_areas, cev_use,
    indicative_rate, rc_number, insurance_until, fitness_until, phone, phone_public, notice_language
  ) values (
    auth.uid(), trim(p_full_name), trim(p_village), trim(p_district), p_category,
    coalesce(p_sub_categories, '{}'), coalesce(p_service_areas, '{}'), p_cev_use,
    nullif(trim(coalesce(p_indicative_rate, '')), ''), nullif(trim(coalesce(p_rc_number, '')), ''),
    p_insurance_until, p_fitness_until, v_mobile, coalesce(p_phone_public, false),
    coalesce(p_notice_language, 'en')
  )
  on conflict (user_id) do update set
    full_name = excluded.full_name, village = excluded.village, district = excluded.district,
    category = excluded.category, sub_categories = excluded.sub_categories,
    service_areas = excluded.service_areas, cev_use = excluded.cev_use,
    indicative_rate = excluded.indicative_rate, rc_number = excluded.rc_number,
    insurance_until = excluded.insurance_until, fitness_until = excluded.fitness_until,
    phone_public = excluded.phone_public, notice_language = excluded.notice_language,
    status = 'active', updated_at = now();
end;
$$;

create or replace function public.set_phone_visibility(p_visible boolean) returns void
language plpgsql security definer set search_path = public as $$
begin
  if auth.uid() is null then raise exception 'not_signed_in'; end if;
  update public.partners set phone_public = coalesce(p_visible, false), updated_at = now()
  where user_id = auth.uid();
end;
$$;

-- Hides the listing now. Account and consent records are kept as the law requires.
create or replace function public.remove_listing() returns void
language plpgsql security definer set search_path = public as $$
begin
  if auth.uid() is null then raise exception 'not_signed_in'; end if;
  update public.partners set status = 'removed', phone_public = false, updated_at = now()
  where user_id = auth.uid();
end;
$$;

-- Directory list. No phone number, no ranking: ordered by name only.
create or replace function public.list_partners()
returns table (
  id uuid, full_name text, service_category text, sub_categories text[],
  village text, district text, masked_phone text, phone_verified boolean, indicative_rate text
)
language plpgsql stable security definer set search_path = public as $$
begin
  if auth.uid() is null then return; end if;
  return query
    select p.id, p.full_name, p.category, p.sub_categories, p.village, p.district,
           '+91 ' || left(p.phone, 2) || 'XXX XX' || right(p.phone, 3),
           true, p.indicative_rate
    from public.partners p
    where p.status = 'active' and p.phone_public and public.listing_is_current(p)
    order by p.full_name
    limit 200;
end;
$$;

-- The only way to get a full number. 10 reveals per hour and 30 per day per user.
create or replace function public.reveal_contact(p_partner_id uuid) returns jsonb
language plpgsql security definer set search_path = public as $$
declare
  v_user  uuid := auth.uid();
  v_phone text;
begin
  if v_user is null then return jsonb_build_object('status', 'not_signed_in'); end if;

  if (select count(*) from public.contact_reveal_logs
        where user_id = v_user and created_at > now() - interval '1 hour') >= 10
     or (select count(*) from public.contact_reveal_logs
        where user_id = v_user and created_at > now() - interval '1 day') >= 30 then
    return jsonb_build_object('status', 'rate_limited');
  end if;

  select p.phone into v_phone
  from public.partners p
  where p.id = p_partner_id and p.status = 'active' and p.phone_public and public.listing_is_current(p);

  if v_phone is null then return jsonb_build_object('status', 'unavailable'); end if;

  insert into public.contact_reveal_logs (user_id, partner_id) values (v_user, p_partner_id);
  return jsonb_build_object('status', 'ok', 'phone', '+91' || v_phone);
end;
$$;

create or replace function public.record_consent(p_type text, p_action text, p_version text, p_language text)
returns void
language plpgsql security definer set search_path = public as $$
begin
  if auth.uid() is null then raise exception 'not_signed_in'; end if;
  insert into public.consent_audit_logs (user_id, consent_type, action, version, language)
  values (auth.uid(), p_type, p_action, p_version, coalesce(p_language, 'en'));
end;
$$;

create or replace function public.report_listing(p_partner_id uuid, p_reason text, p_details text default null)
returns void
language plpgsql security definer set search_path = public as $$
begin
  if auth.uid() is null then raise exception 'not_signed_in'; end if;
  if (select count(*) from public.listing_reports
        where reporter_id = auth.uid() and created_at > now() - interval '1 day') >= 20 then
    raise exception 'too_many_reports';
  end if;
  insert into public.listing_reports (reporter_id, partner_id, reason, details)
  values (auth.uid(), p_partner_id, p_reason, left(p_details, 1000));
end;
$$;

-- ---------------------------------------------------------------- permissions
-- Functions are executable only by signed-in users.

revoke all on function
  public.current_mobile(), public.has_prohibited_term(text), public.listing_is_current(public.partners),
  public.register_customer(text, text),
  public.register_partner(text, text, text, text, text[], text[], boolean, text, text, text, date, date, text),
  public.set_phone_visibility(boolean), public.remove_listing(), public.list_partners(),
  public.reveal_contact(uuid), public.record_consent(text, text, text, text),
  public.report_listing(uuid, text, text)
from public, anon;

grant execute on function
  public.register_customer(text, text),
  public.register_partner(text, text, text, text, text[], text[], boolean, text, text, text, date, date, text),
  public.set_phone_visibility(boolean), public.remove_listing(), public.list_partners(),
  public.reveal_contact(uuid), public.record_consent(text, text, text, text),
  public.report_listing(uuid, text, text)
to authenticated;
