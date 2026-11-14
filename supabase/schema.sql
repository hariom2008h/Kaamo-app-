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
  user_id         uuid unique references auth.users (id) on delete cascade, -- null only for demo rows
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
  -- Coarse work location (rounded to 2 decimals, about 1 km). Never returned by the API, only used for distance.
  latitude        double precision check (latitude between -90 and 90),
  longitude       double precision check (longitude between -180 and 180),
  is_demo         boolean not null default false,
  created_at      timestamptz not null default now(),
  updated_at      timestamptz not null default now(),
  check (is_demo or user_id is not null)
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

-- Great-circle distance in whole km (haversine).
create or replace function public.distance_km(lat1 double precision, lon1 double precision,
                                              lat2 double precision, lon2 double precision) returns integer
language sql immutable set search_path = public as $$
  select round(6371 * 2 * asin(least(1, sqrt(
           power(sin(radians(lat2 - lat1) / 2), 2) +
           cos(radians(lat1)) * cos(radians(lat2)) * power(sin(radians(lon2 - lon1) / 2), 2)))))::integer;
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
  p_cev_use         text default null,
  p_latitude        double precision default null,
  p_longitude       double precision default null
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
    indicative_rate, rc_number, insurance_until, fitness_until, phone, phone_public, notice_language,
    latitude, longitude
  ) values (
    auth.uid(), trim(p_full_name), trim(p_village), trim(p_district), p_category,
    coalesce(p_sub_categories, '{}'), coalesce(p_service_areas, '{}'), p_cev_use,
    nullif(trim(coalesce(p_indicative_rate, '')), ''), nullif(trim(coalesce(p_rc_number, '')), ''),
    p_insurance_until, p_fitness_until, v_mobile, coalesce(p_phone_public, false),
    coalesce(p_notice_language, 'en'),
    case when p_latitude between -90 and 90 and p_longitude between -180 and 180
         then round(p_latitude::numeric, 2)::double precision end,
    case when p_latitude between -90 and 90 and p_longitude between -180 and 180
         then round(p_longitude::numeric, 2)::double precision end
  )
  on conflict (user_id) do update set
    full_name = excluded.full_name, village = excluded.village, district = excluded.district,
    category = excluded.category, sub_categories = excluded.sub_categories,
    service_areas = excluded.service_areas, cev_use = excluded.cev_use,
    indicative_rate = excluded.indicative_rate, rc_number = excluded.rc_number,
    insurance_until = excluded.insurance_until, fitness_until = excluded.fitness_until,
    phone_public = excluded.phone_public, notice_language = excluded.notice_language,
    latitude = excluded.latitude, longitude = excluded.longitude,
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
-- p_lat / p_lng are the caller's live GPS position. They are used only to compute distance_km in this
-- call and are never stored. Without them distance_km is null and the list is ordered by name.
create or replace function public.list_partners(p_lat double precision default null,
                                                p_lng double precision default null)
returns table (
  id uuid, full_name text, service_category text, sub_categories text[],
  village text, district text, masked_phone text, phone_verified boolean, indicative_rate text,
  distance_km integer
)
language plpgsql stable security definer set search_path = public as $$
declare
  v_lat double precision := case when p_lat between -90 and 90 then p_lat end;
  v_lng double precision := case when p_lng between -180 and 180 then p_lng end;
begin
  if auth.uid() is null then return; end if;
  return query
    select p.id, p.full_name, p.category, p.sub_categories, p.village, p.district,
           '+91 ' || left(p.phone, 2) || 'XXX XX' || right(p.phone, 3),
           true, p.indicative_rate,
           case when v_lat is not null and v_lng is not null and p.latitude is not null and p.longitude is not null
                then public.distance_km(v_lat, v_lng, p.latitude, p.longitude) end
    from public.partners p
    where p.status = 'active' and p.phone_public and public.listing_is_current(p)
    order by 10, p.full_name
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
  public.distance_km(double precision, double precision, double precision, double precision),
  public.register_customer(text, text),
  public.register_partner(text, text, text, text, text[], text[], boolean, text, text, text, date, date, text, double precision, double precision),
  public.set_phone_visibility(boolean), public.remove_listing(), public.list_partners(double precision, double precision),
  public.reveal_contact(uuid), public.record_consent(text, text, text, text),
  public.report_listing(uuid, text, text)
from public, anon, authenticated;

grant execute on function
  public.register_customer(text, text),
  public.register_partner(text, text, text, text, text[], text[], boolean, text, text, text, date, date, text, double precision, double precision),
  public.set_phone_visibility(boolean), public.remove_listing(), public.list_partners(double precision, double precision),
  public.reveal_contact(uuid), public.record_consent(text, text, text, text),
  public.report_listing(uuid, text, text)
to authenticated;

-- ================================================================ 003 (admin form, invites, policy)
-- 003: admin-configurable registration form, admin invites, policy re-acceptance.
-- Run after schema.sql / 002. Safe to re-run.

alter table public.partners alter column village  drop not null;
alter table public.partners alter column district drop not null;
alter table public.partners drop constraint if exists partners_category_check;
alter table public.partners add column if not exists extra jsonb not null default '{}'::jsonb;

create table if not exists public.admin_mobiles (
  mobile   text primary key check (mobile ~ '^[0-9]{10}$'),
  note     text,
  added_at timestamptz not null default now()
);

create table if not exists public.form_config (
  id         int primary key default 1 check (id = 1),
  version    int not null default 1,
  config     jsonb not null,
  updated_at timestamptz not null default now(),
  updated_by uuid
);

create table if not exists public.form_config_history (
  id       bigint generated always as identity primary key,
  version  int not null,
  config   jsonb not null,
  saved_by uuid,
  saved_at timestamptz not null default now()
);
drop trigger if exists form_history_no_change on public.form_config_history;
create trigger form_history_no_change before update or delete on public.form_config_history
  for each row execute function public.block_mutation();

create table if not exists public.invites (
  id         uuid primary key default gen_random_uuid(),
  code       text not null unique,
  name       text,
  mobile     text check (mobile is null or mobile ~ '^[0-9]{10}$'),
  category   text,
  status     text not null default 'active' check (status in ('active', 'used', 'revoked')),
  expires_at timestamptz not null,
  created_by uuid not null,
  created_at timestamptz not null default now(),
  used_by    uuid,
  used_at    timestamptz
);

alter table public.admin_mobiles       enable row level security;
alter table public.form_config         enable row level security;
alter table public.form_config_history enable row level security;
alter table public.invites             enable row level security;
revoke all on public.admin_mobiles, public.form_config, public.form_config_history, public.invites from anon, authenticated;

insert into public.form_config (id, version, config) values (1, 1, $cfg$
{
 "categories": [
  {"code":"plumber","label_en":"Plumber","label_hi":"प्लंबर","enabled":true,"vehicle":false,"fitness":false,"cev":false,
   "subs":[{"en":"Pipe fitting","hi":"पाइप फिटिंग"},{"en":"Leak repair","hi":"लीकेज मरम्मत"},{"en":"Bathroom fitting","hi":"बाथरूम फिटिंग"},{"en":"Water tank","hi":"पानी की टंकी"}]},
  {"code":"electrician","label_en":"Electrician","label_hi":"इलेक्ट्रीशियन","enabled":true,"vehicle":false,"fitness":false,"cev":false,
   "subs":[{"en":"House wiring","hi":"घर की वायरिंग"},{"en":"Fan / light repair","hi":"पंखा / लाइट मरम्मत"},{"en":"Inverter / UPS","hi":"इन्वर्टर / UPS"}]},
  {"code":"construction","label_en":"Construction workers","label_hi":"निर्माण मज़दूर","enabled":true,"vehicle":false,"fitness":false,"cev":false,
   "subs":[{"en":"Mason (mistri)","hi":"राजमिस्त्री"},{"en":"Labour","hi":"मज़दूर"},{"en":"Painter","hi":"पेंटर"},{"en":"Carpenter","hi":"बढ़ई"},{"en":"Tiles","hi":"टाइल्स"}]},
  {"code":"borewell","label_en":"Borewell","label_hi":"बोरवेल","enabled":true,"vehicle":true,"fitness":true,"cev":false,
   "subs":[{"en":"Borewell drilling","hi":"बोरवेल ड्रिलिंग"},{"en":"Motor / pump repair","hi":"मोटर / पंप मरम्मत"},{"en":"Flushing","hi":"फ्लशिंग"}]},
  {"code":"jcb","label_en":"JCB / Earthmover","label_hi":"JCB / अर्थमूवर","enabled":true,"vehicle":true,"fitness":true,"cev":true,
   "subs":[{"en":"Excavation","hi":"खुदाई"},{"en":"Loading","hi":"लोडिंग"},{"en":"Demolition","hi":"तोड़फोड़"},{"en":"Leveling","hi":"समतलीकरण"}]},
  {"code":"tractor","label_en":"Tractor","label_hi":"ट्रैक्टर","enabled":true,"vehicle":true,"fitness":false,"cev":false,
   "subs":[{"en":"Trolley","hi":"ट्रॉली"},{"en":"Cultivator","hi":"कल्टीवेटर"},{"en":"Water Tanker","hi":"पानी का टैंकर"},{"en":"Thresher","hi":"थ्रेशर"}]},
  {"code":"transport","label_en":"Transport / Trailer","label_hi":"ट्रांसपोर्ट / ट्रेलर","enabled":true,"vehicle":true,"fitness":true,"cev":false,"subs":[]}
 ],
 "fields": {
  "village":  {"enabled":true,"required":true, "label_en":"","label_hi":""},
  "district": {"enabled":true,"required":true, "label_en":"","label_hi":""},
  "areas":    {"enabled":true,"required":true, "label_en":"","label_hi":""},
  "rate":     {"enabled":true,"required":false,"label_en":"","label_hi":""},
  "gps":      {"enabled":true,"required":false,"label_en":"","label_hi":""},
  "rc":       {"enabled":true,"required":true, "label_en":"","label_hi":""},
  "insurance":{"enabled":true,"required":true, "label_en":"","label_hi":""},
  "fitness":  {"enabled":true,"required":true, "label_en":"","label_hi":""}
 },
 "custom": [],
 "policy": {"version":"v1.0_2026","summary_en":"","summary_hi":""},
 "registration": {"invite_only": false}
}
$cfg$::jsonb) on conflict (id) do nothing;

-- ---------------------------------------------------------------- helpers

create or replace function public.is_admin() returns boolean
language sql stable security definer set search_path = public, auth as $$
  select exists (select 1 from public.admin_mobiles a where a.mobile = public.current_mobile());
$$;

create or replace function public.config_category(p_code text) returns jsonb
language sql stable security definer set search_path = public as $$
  select c from public.form_config f, jsonb_array_elements(f.config -> 'categories') c
  where f.id = 1 and c ->> 'code' = p_code limit 1;
$$;

-- 'off' | 'optional' | 'required'
create or replace function public.field_state(p_key text) returns text
language sql stable security definer set search_path = public as $$
  select case
    when coalesce((f.config -> 'fields' -> p_key ->> 'enabled')::boolean, true) = false then 'off'
    when coalesce((f.config -> 'fields' -> p_key ->> 'required')::boolean, false) then 'required'
    else 'optional' end
  from public.form_config f where f.id = 1;
$$;

create or replace function public.listing_is_current(p public.partners) returns boolean
language plpgsql stable security definer set search_path = public as $$
declare
  c jsonb := public.config_category(p.category);
  v boolean; f boolean;
begin
  if c is null or coalesce((c ->> 'enabled')::boolean, true) = false then return false; end if;
  v := coalesce((c ->> 'vehicle')::boolean, false);
  f := coalesce((c ->> 'fitness')::boolean, false);
  return (not v or (p.insurance_until is not null and p.insurance_until >= current_date))
     and (not f or (p.fitness_until is not null and p.fitness_until >= current_date));
end;
$$;

-- ---------------------------------------------------------------- config read / admin save

create or replace function public.get_form_config() returns jsonb
language sql stable security definer set search_path = public as $$
  select jsonb_build_object('version', version, 'config', config) from public.form_config where id = 1;
$$;

create or replace function public.admin_save_form_config(p_config jsonb) returns int
language plpgsql security definer set search_path = public as $$
declare
  c jsonb; s jsonb; cf jsonb; o jsonb; f jsonb; k text;
  v_codes text[] := '{}'; v_ids text[] := '{}'; v_text text := '';
  v_cats jsonb := '[]'; v_fields jsonb := '{}'; v_custom jsonb := '[]'; v_subs jsonb; v_opts jsonb; v_cc jsonb;
  v_code text; v_enabled int := 0; v_veh boolean; v_fit boolean; v_ver int; v_pol jsonb; v_cfg jsonb;
begin
  if not public.is_admin() then raise exception 'not_admin'; end if;
  if p_config is null or length(p_config::text) > 100000 then raise exception 'bad_config'; end if;
  if jsonb_typeof(p_config -> 'categories') <> 'array' or jsonb_array_length(p_config -> 'categories') not between 1 and 40 then
    raise exception 'bad_categories';
  end if;

  for c in select * from jsonb_array_elements(p_config -> 'categories') loop
    v_code := c ->> 'code';
    if coalesce(v_code, '') !~ '^[a-z][a-z0-9_]{1,29}$' then raise exception 'bad_code'; end if;
    if v_code = any(v_codes) then raise exception 'duplicate_code'; end if;
    v_codes := v_codes || v_code;
    if length(trim(coalesce(c ->> 'label_en', ''))) not between 1 and 60 then raise exception 'bad_label'; end if;
    v_subs := '[]';
    if jsonb_typeof(c -> 'subs') = 'array' then
      if jsonb_array_length(c -> 'subs') > 30 then raise exception 'too_many_subs'; end if;
      for s in select * from jsonb_array_elements(c -> 'subs') loop
        if length(trim(coalesce(s ->> 'en', ''))) not between 1 and 60 then raise exception 'bad_sub'; end if;
        v_subs := v_subs || jsonb_build_object('en', trim(s ->> 'en'),
                    'hi', left(coalesce(nullif(trim(s ->> 'hi'), ''), trim(s ->> 'en')), 60));
        v_text := v_text || ' ' || (s ->> 'en') || ' ' || coalesce(s ->> 'hi', '');
      end loop;
    end if;
    v_fit := v_code in ('borewell', 'jcb', 'transport') or coalesce((c ->> 'fitness')::boolean, false);
    v_veh := v_fit or v_code in ('tractor') or coalesce((c ->> 'vehicle')::boolean, false);
    v_cats := v_cats || jsonb_build_object(
      'code', v_code, 'label_en', trim(c ->> 'label_en'),
      'label_hi', left(coalesce(nullif(trim(c ->> 'label_hi'), ''), trim(c ->> 'label_en')), 60),
      'enabled', coalesce((c ->> 'enabled')::boolean, true),
      'vehicle', v_veh, 'fitness', v_fit,
      'cev', (v_code = 'jcb'),
      'subs', v_subs);
    if coalesce((c ->> 'enabled')::boolean, true) then v_enabled := v_enabled + 1; end if;
    v_text := v_text || ' ' || (c ->> 'label_en') || ' ' || coalesce(c ->> 'label_hi', '');
  end loop;
  if v_enabled = 0 then raise exception 'no_enabled_category'; end if;

  foreach k in array array['village', 'district', 'areas', 'rate', 'gps', 'rc', 'insurance', 'fitness'] loop
    f := coalesce(p_config -> 'fields' -> k, '{}'::jsonb);
    v_fields := v_fields || jsonb_build_object(k, jsonb_build_object(
      'enabled',  k in ('rc', 'insurance', 'fitness') or coalesce((f ->> 'enabled')::boolean, true),
      'required', k in ('rc', 'insurance', 'fitness')
                  or (coalesce((f ->> 'enabled')::boolean, true) and coalesce((f ->> 'required')::boolean, false)),
      'label_en', left(coalesce(f ->> 'label_en', ''), 80),
      'label_hi', left(coalesce(f ->> 'label_hi', ''), 80)));
    v_text := v_text || ' ' || coalesce(f ->> 'label_en', '') || ' ' || coalesce(f ->> 'label_hi', '');
  end loop;

  if jsonb_typeof(p_config -> 'custom') = 'array' then
    if jsonb_array_length(p_config -> 'custom') > 20 then raise exception 'too_many_custom'; end if;
    for cf in select * from jsonb_array_elements(p_config -> 'custom') loop
      if coalesce(cf ->> 'id', '') !~ '^[a-z][a-z0-9_]{1,29}$' then raise exception 'bad_field_id'; end if;
      if (cf ->> 'id') = any(v_ids) then raise exception 'duplicate_field_id'; end if;
      v_ids := v_ids || (cf ->> 'id');
      if coalesce(cf ->> 'type', '') not in ('text', 'number', 'select', 'yesno') then raise exception 'bad_field_type'; end if;
      if length(trim(coalesce(cf ->> 'label_en', ''))) not between 1 and 80 then raise exception 'bad_label'; end if;
      v_opts := '[]';
      if cf ->> 'type' = 'select' then
        if jsonb_typeof(cf -> 'options') <> 'array' or jsonb_array_length(cf -> 'options') not between 2 and 20 then
          raise exception 'bad_options';
        end if;
        for o in select * from jsonb_array_elements(cf -> 'options') loop
          if length(trim(coalesce(o ->> 'en', ''))) not between 1 and 60 then raise exception 'bad_options'; end if;
          v_opts := v_opts || jsonb_build_object('en', trim(o ->> 'en'),
                      'hi', left(coalesce(nullif(trim(o ->> 'hi'), ''), trim(o ->> 'en')), 60));
          v_text := v_text || ' ' || (o ->> 'en') || ' ' || coalesce(o ->> 'hi', '');
        end loop;
      end if;
      v_cc := '["*"]';
      if jsonb_typeof(cf -> 'categories') = 'array' and jsonb_array_length(cf -> 'categories') > 0 then
        v_cc := cf -> 'categories';
        for k in select jsonb_array_elements_text(v_cc) loop
          if k <> '*' and not (k = any(v_codes)) then raise exception 'bad_field_category'; end if;
        end loop;
      end if;
      v_custom := v_custom || jsonb_build_object(
        'id', cf ->> 'id', 'type', cf ->> 'type', 'label_en', trim(cf ->> 'label_en'),
        'label_hi', left(coalesce(nullif(trim(cf ->> 'label_hi'), ''), trim(cf ->> 'label_en')), 80),
        'required', coalesce((cf ->> 'required')::boolean, false),
        'enabled', coalesce((cf ->> 'enabled')::boolean, true),
        'show_public', coalesce((cf ->> 'show_public')::boolean, false),
        'options', v_opts, 'categories', v_cc);
      v_text := v_text || ' ' || (cf ->> 'label_en') || ' ' || coalesce(cf ->> 'label_hi', '');
    end loop;
  end if;

  v_pol := coalesce(p_config -> 'policy', '{}'::jsonb);
  if length(trim(coalesce(v_pol ->> 'version', ''))) not between 1 and 30 then raise exception 'bad_policy_version'; end if;
  v_pol := jsonb_build_object('version', trim(v_pol ->> 'version'),
             'summary_en', left(coalesce(v_pol ->> 'summary_en', ''), 2000),
             'summary_hi', left(coalesce(v_pol ->> 'summary_hi', ''), 2000));

  if public.has_prohibited_term(v_text) then raise exception 'prohibited_term'; end if;

  v_cfg := jsonb_build_object('categories', v_cats, 'fields', v_fields, 'custom', v_custom, 'policy', v_pol,
             'registration', jsonb_build_object('invite_only', coalesce((p_config -> 'registration' ->> 'invite_only')::boolean, false)));

  update public.form_config set version = version + 1, config = v_cfg, updated_at = now(), updated_by = auth.uid()
  where id = 1 returning version into v_ver;
  insert into public.form_config_history (version, config, saved_by) values (v_ver, v_cfg, auth.uid());
  return v_ver;
end;
$$;

-- ---------------------------------------------------------------- registration

drop function if exists public.register_partner(text, text, text, text, text[], text[], boolean, text, text, text, date, date, text, double precision, double precision);

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
  p_cev_use         text default null,
  p_latitude        double precision default null,
  p_longitude       double precision default null,
  p_extra           jsonb default null,
  p_invite_code     text default null
) returns void
language plpgsql security definer set search_path = public as $$
declare
  v_mobile text := public.current_mobile();
  v_cfg    jsonb := (select config from public.form_config where id = 1);
  c        jsonb := public.config_category(p_category);
  cf       jsonb; v_val text; v_extra jsonb := '{}'; v_text text;
  v_village text; v_district text; v_areas text[]; v_rate text; v_subs text[]; v_cev text;
  v_lat double precision; v_lng double precision;
  v_inv public.invites; v_have_listing boolean;
begin
  if auth.uid() is null then raise exception 'not_signed_in'; end if;
  if v_mobile is null or v_mobile !~ '^[0-9]{10}$' then raise exception 'mobile_not_verified'; end if;
  if length(trim(coalesce(p_full_name, ''))) = 0 then raise exception 'name_required'; end if;
  if c is null or coalesce((c ->> 'enabled')::boolean, true) = false then raise exception 'bad_category'; end if;

  -- built-in fields, as configured by the admin
  if public.field_state('village') <> 'off' then v_village := nullif(trim(coalesce(p_village, '')), ''); end if;
  if public.field_state('village') = 'required' and v_village is null then raise exception 'field_required:village'; end if;
  if public.field_state('district') <> 'off' then v_district := nullif(trim(coalesce(p_district, '')), ''); end if;
  if public.field_state('district') = 'required' and v_district is null then raise exception 'field_required:district'; end if;
  if public.field_state('areas') <> 'off' then v_areas := coalesce(p_service_areas, '{}'); else v_areas := '{}'; end if;
  if public.field_state('areas') = 'required' and coalesce(array_length(v_areas, 1), 0) = 0 then raise exception 'field_required:areas'; end if;
  if public.field_state('rate') <> 'off' then v_rate := nullif(trim(coalesce(p_indicative_rate, '')), ''); end if;
  if public.field_state('rate') = 'required' and v_rate is null then raise exception 'field_required:rate'; end if;
  if public.field_state('gps') <> 'off' and p_latitude between -90 and 90 and p_longitude between -180 and 180 then
    v_lat := round(p_latitude::numeric, 2)::double precision;
    v_lng := round(p_longitude::numeric, 2)::double precision;
  end if;
  if public.field_state('gps') = 'required' and v_lat is null then raise exception 'field_required:gps'; end if;

  -- sub-categories must be ones configured for this category
  v_subs := coalesce(p_sub_categories, '{}');
  if exists (select 1 from unnest(v_subs) u
             where not exists (select 1 from jsonb_array_elements(c -> 'subs') s where s ->> 'en' = u)) then
    raise exception 'bad_sub';
  end if;
  v_cev := case when coalesce((c ->> 'cev')::boolean, false) and p_cev_use in ('on_site', 'low_bed_transport')
                then p_cev_use end;

  -- custom fields
  for cf in select * from jsonb_array_elements(v_cfg -> 'custom') loop
    continue when not coalesce((cf ->> 'enabled')::boolean, true);
    continue when not (cf -> 'categories' ? '*' or cf -> 'categories' ? p_category);
    v_val := nullif(trim(coalesce(p_extra ->> (cf ->> 'id'), '')), '');
    if v_val is null then
      if coalesce((cf ->> 'required')::boolean, false) then raise exception 'field_required:%', cf ->> 'id'; end if;
      continue;
    end if;
    case cf ->> 'type'
      when 'text'   then if length(v_val) > 200 then raise exception 'bad_value:%', cf ->> 'id'; end if;
      when 'number' then if v_val !~ '^-?[0-9]{1,12}(\.[0-9]{1,4})?$' then raise exception 'bad_value:%', cf ->> 'id'; end if;
      when 'yesno'  then if v_val not in ('true', 'false') then raise exception 'bad_value:%', cf ->> 'id'; end if;
      when 'select' then
        if not exists (select 1 from jsonb_array_elements(cf -> 'options') o where o ->> 'en' = v_val) then
          raise exception 'bad_value:%', cf ->> 'id';
        end if;
    end case;
    v_extra := v_extra || jsonb_build_object(cf ->> 'id', v_val);
  end loop;

  v_text := array_to_string(v_areas || v_subs, ' ') || ' ' || coalesce(v_rate, '') || ' ' || (select coalesce(string_agg(value, ' '), '') from jsonb_each_text(v_extra));
  if public.has_prohibited_term(v_text) then raise exception 'prohibited_term'; end if;

  -- invite
  v_have_listing := exists (select 1 from public.partners where user_id = auth.uid());
  if coalesce(trim(p_invite_code), '') <> '' then
    select * into v_inv from public.invites where code = upper(trim(p_invite_code)) for update;
    if not found or v_inv.status <> 'active' or v_inv.expires_at <= now() then raise exception 'invite_invalid'; end if;
    if v_inv.mobile is not null and v_inv.mobile <> v_mobile then raise exception 'invite_mobile_mismatch'; end if;
  elsif coalesce((v_cfg -> 'registration' ->> 'invite_only')::boolean, false) and not v_have_listing then
    raise exception 'invite_required';
  end if;

  insert into public.profiles (id, role, full_name, location)
  values (auth.uid(), 'partner', trim(p_full_name), v_village)
  on conflict (id) do update set role = 'partner', full_name = excluded.full_name, location = excluded.location;

  insert into public.partners (
    user_id, full_name, village, district, category, sub_categories, service_areas, cev_use,
    indicative_rate, rc_number, insurance_until, fitness_until, phone, phone_public, notice_language,
    latitude, longitude, extra
  ) values (
    auth.uid(), trim(p_full_name), v_village, v_district, p_category, v_subs, v_areas, v_cev,
    v_rate, nullif(trim(coalesce(p_rc_number, '')), ''), p_insurance_until, p_fitness_until, v_mobile,
    coalesce(p_phone_public, false), coalesce(p_notice_language, 'en'), v_lat, v_lng, v_extra
  )
  on conflict (user_id) do update set
    full_name = excluded.full_name, village = excluded.village, district = excluded.district,
    category = excluded.category, sub_categories = excluded.sub_categories,
    service_areas = excluded.service_areas, cev_use = excluded.cev_use,
    indicative_rate = excluded.indicative_rate, rc_number = excluded.rc_number,
    insurance_until = excluded.insurance_until, fitness_until = excluded.fitness_until,
    phone_public = excluded.phone_public, notice_language = excluded.notice_language,
    latitude = excluded.latitude, longitude = excluded.longitude, extra = excluded.extra,
    status = 'active', updated_at = now();

  if v_inv.id is not null then
    update public.invites set status = 'used', used_by = auth.uid(), used_at = now() where id = v_inv.id;
  end if;
end;
$$;

-- ---------------------------------------------------------------- directory

drop function if exists public.list_partners(double precision, double precision);

create or replace function public.list_partners(p_lat double precision default null,
                                                p_lng double precision default null)
returns table (
  id uuid, full_name text, service_category text, sub_categories text[],
  village text, district text, masked_phone text, phone_verified boolean, indicative_rate text,
  distance_km integer, extra jsonb
)
language plpgsql stable security definer set search_path = public as $$
declare
  v_lat double precision := case when p_lat between -90 and 90 then p_lat end;
  v_lng double precision := case when p_lng between -180 and 180 then p_lng end;
  v_public text[] := coalesce((select array_agg(cf ->> 'id')
                               from public.form_config f, jsonb_array_elements(f.config -> 'custom') cf
                               where f.id = 1 and coalesce((cf ->> 'show_public')::boolean, false)
                                 and coalesce((cf ->> 'enabled')::boolean, true)), '{}');
begin
  if auth.uid() is null then return; end if;
  return query
    select p.id, p.full_name, p.category, p.sub_categories, p.village, p.district,
           '+91 ' || left(p.phone, 2) || 'XXX XX' || right(p.phone, 3),
           true, p.indicative_rate,
           case when v_lat is not null and v_lng is not null and p.latitude is not null and p.longitude is not null
                then public.distance_km(v_lat, v_lng, p.latitude, p.longitude) end,
           coalesce((select jsonb_object_agg(e.key, e.value) from jsonb_each(p.extra) e where e.key = any(v_public)), '{}'::jsonb)
    from public.partners p
    where p.status = 'active' and p.phone_public and public.listing_is_current(p)
    order by 10, p.full_name
    limit 200;
end;
$$;

-- ---------------------------------------------------------------- invites

create or replace function public.admin_create_invite(p_name text, p_mobile text default null,
                                                      p_category text default null, p_days int default 14)
returns jsonb
language plpgsql security definer set search_path = public, extensions as $$
declare
  v_mobile text := nullif(right(regexp_replace(coalesce(p_mobile, ''), '[^0-9]', '', 'g'), 10), '');
  v_code text; v_exp timestamptz; v_id uuid;
begin
  if not public.is_admin() then raise exception 'not_admin'; end if;
  if v_mobile is not null and v_mobile !~ '^[6-9][0-9]{9}$' then raise exception 'bad_mobile'; end if;
  if p_category is not null and public.config_category(p_category) is null then raise exception 'bad_category'; end if;
  if p_days not between 1 and 90 then raise exception 'bad_days'; end if;
  v_code := upper(encode(gen_random_bytes(6), 'hex'));
  v_exp := now() + make_interval(days => p_days);
  insert into public.invites (code, name, mobile, category, expires_at, created_by)
  values (v_code, left(nullif(trim(coalesce(p_name, '')), ''), 80), v_mobile, p_category, v_exp, auth.uid())
  returning id into v_id;
  return jsonb_build_object('id', v_id, 'code', v_code, 'expires_at', v_exp);
end;
$$;

create or replace function public.admin_list_invites()
returns table (id uuid, code text, name text, mobile text, category text, status text,
               expires_at timestamptz, created_at timestamptz, used_at timestamptz)
language plpgsql stable security definer set search_path = public as $$
begin
  if not public.is_admin() then raise exception 'not_admin'; end if;
  return query
    select i.id, i.code, i.name, i.mobile, i.category,
           case when i.status = 'active' and i.expires_at <= now() then 'expired' else i.status end,
           i.expires_at, i.created_at, i.used_at
    from public.invites i order by i.created_at desc limit 200;
end;
$$;

create or replace function public.admin_revoke_invite(p_id uuid) returns void
language plpgsql security definer set search_path = public as $$
begin
  if not public.is_admin() then raise exception 'not_admin'; end if;
  update public.invites set status = 'revoked' where id = p_id and status = 'active';
end;
$$;

-- Public (works before sign-in): lets the invite link pre-fill the form. Never returns the mobile number.
create or replace function public.get_invite(p_code text) returns jsonb
language plpgsql stable security definer set search_path = public as $$
declare v public.invites;
begin
  select * into v from public.invites where code = upper(trim(coalesce(p_code, '')));
  if not found or v.status <> 'active' or v.expires_at <= now() then
    return jsonb_build_object('status', 'invalid');
  end if;
  return jsonb_build_object('status', 'ok', 'name', v.name, 'category', v.category,
                            'mobile_locked', v.mobile is not null);
end;
$$;

-- ---------------------------------------------------------------- policy re-acceptance

create or replace function public.get_policy_status() returns jsonb
language plpgsql stable security definer set search_path = public as $$
declare
  v_pol jsonb := (select config -> 'policy' from public.form_config where id = 1);
  v_ver text; v_at timestamptz; v_reason text;
begin
  if auth.uid() is null then return null; end if;
  select version, created_at into v_ver, v_at
  from public.consent_audit_logs
  where user_id = auth.uid() and consent_type = 'TermsAndPrivacy' and action = 'Granted'
  order by created_at desc limit 1;
  v_reason := case when v_ver is null then 'none'
                   when v_ver <> v_pol ->> 'version' then 'new_version'
                   when v_at < now() - interval '365 days' then 'annual' end;
  return jsonb_build_object('current_version', v_pol ->> 'version', 'summary_en', v_pol ->> 'summary_en',
    'summary_hi', v_pol ->> 'summary_hi', 'accepted_version', v_ver, 'accepted_at', v_at,
    'needs_accept', v_reason is not null, 'reason', v_reason);
end;
$$;

-- ---------------------------------------------------------------- permissions

revoke all on function
  public.is_admin(), public.config_category(text), public.field_state(text), public.listing_is_current(public.partners),
  public.get_form_config(), public.admin_save_form_config(jsonb),
  public.register_partner(text, text, text, text, text[], text[], boolean, text, text, text, date, date, text, double precision, double precision, jsonb, text),
  public.list_partners(double precision, double precision),
  public.admin_create_invite(text, text, text, int), public.admin_list_invites(), public.admin_revoke_invite(uuid),
  public.get_invite(text), public.get_policy_status()
  from public, anon, authenticated;

grant execute on function
  public.is_admin(), public.get_form_config(), public.admin_save_form_config(jsonb),
  public.register_partner(text, text, text, text, text[], text[], boolean, text, text, text, date, date, text, double precision, double precision, jsonb, text),
  public.list_partners(double precision, double precision),
  public.admin_create_invite(text, text, text, int), public.admin_list_invites(), public.admin_revoke_invite(uuid),
  public.get_policy_status()
  to authenticated;
grant execute on function public.get_invite(text) to anon, authenticated;
