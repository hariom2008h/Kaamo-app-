-- Location autofill: official state > district > tehsil > village lists (India Local Government Directory)
-- plus anonymous "which village did people pick here" feedback, and register_partner storing the result.
-- Data is loaded separately: supabase/seed/lgd/load.sql

create table if not exists public.lgd_units (
  kind   char(1) not null check (kind in ('S','D','T')),   -- S state, D district, T sub-district (tehsil)
  code   integer not null,
  parent integer not null default 0,
  name   text not null,
  primary key (kind, code)
);
create index if not exists lgd_units_parent on public.lgd_units (kind, parent);

create table if not exists public.lgd_villages (
  code             integer primary key,
  subdistrict_code integer not null,
  name             text not null,
  block            text
);
create index if not exists lgd_villages_sub on public.lgd_villages (subdistrict_code);

-- Anonymous feedback: rounded (~1 km) position and the village a person picked. No user id on purpose.
create table if not exists public.geo_feedback (
  id           bigserial primary key,
  lat          numeric(6,2) not null,
  lng          numeric(6,2) not null,
  village_code integer references public.lgd_villages(code),
  source       text not null check (source in ('gps','search','manual')),
  created_at   timestamptz not null default now()
);

alter table public.lgd_units     enable row level security;
alter table public.lgd_villages  enable row level security;
alter table public.geo_feedback  enable row level security;
revoke all on public.lgd_units, public.lgd_villages, public.geo_feedback from anon, authenticated;
revoke all on sequence public.geo_feedback_id_seq from anon, authenticated;

alter table public.partners
  add column if not exists tehsil text,
  add column if not exists state text,
  add column if not exists village_code integer;

create or replace function public.geo_units(p_kind text, p_parent integer default 0)
returns table (code integer, name text)
language sql stable security definer set search_path = public as $$
  select u.code, u.name from public.lgd_units u
  where auth.uid() is not null and p_kind in ('S','D','T') and u.kind = p_kind
    and u.parent = case when p_kind = 'S' then 0 else coalesce(p_parent, -1) end
  order by u.name;
$$;

create or replace function public.geo_villages(p_subdistrict integer)
returns table (code integer, name text, block text)
language sql stable security definer set search_path = public as $$
  select v.code, v.name, v.block from public.lgd_villages v
  where auth.uid() is not null and v.subdistrict_code = p_subdistrict
  order by v.name limit 5000;
$$;

create or replace function public.geo_feedback(p_lat double precision, p_lng double precision, p_village integer, p_source text)
returns void
language plpgsql security definer set search_path = public as $$
begin
  if auth.uid() is null then raise exception 'not_signed_in'; end if;
  if p_source not in ('gps','search','manual') then raise exception 'bad_source'; end if;
  if p_lat not between 6 and 38 or p_lng not between 68 and 98 then return; end if;          -- outside India: ignore
  if not exists (select 1 from public.lgd_villages where code = p_village) then return; end if;
  if (select count(*) from public.geo_feedback where created_at > now() - interval '1 minute') >= 120 then return; end if;  -- flood guard
  insert into public.geo_feedback (lat, lng, village_code, source)
  values (round(p_lat::numeric, 2), round(p_lng::numeric, 2), p_village, p_source);
end $$;

revoke all on function public.geo_units(text, integer), public.geo_villages(integer),
  public.geo_feedback(double precision, double precision, integer, text) from public, anon;
grant execute on function public.geo_units(text, integer), public.geo_villages(integer),
  public.geo_feedback(double precision, double precision, integer, text) to authenticated;

-- register_partner: same as 003 plus tehsil / state / village_code. When a village code is sent, the village,
-- district, tehsil and state names are taken from the official list on the server, not from the client.
drop function if exists public.register_partner(text, text, text, text, text[], text[], boolean, text, text, text, date, date, text, double precision, double precision, jsonb, text);

create or replace function public.register_partner(
  p_full_name text, p_village text, p_district text, p_category text, p_sub_categories text[], p_service_areas text[],
  p_phone_public boolean, p_notice_language text, p_indicative_rate text default null, p_rc_number text default null,
  p_insurance_until date default null, p_fitness_until date default null, p_cev_use text default null,
  p_latitude double precision default null, p_longitude double precision default null, p_extra jsonb default null,
  p_invite_code text default null, p_tehsil text default null, p_state text default null, p_village_code integer default null)
returns void
language plpgsql security definer set search_path = public as $$
declare
  v_mobile text := public.current_mobile();
  v_cfg    jsonb := (select config from public.form_config where id = 1);
  c        jsonb := public.config_category(p_category);
  cf       jsonb; v_val text; v_extra jsonb := '{}'; v_text text;
  v_village text; v_district text; v_areas text[]; v_rate text; v_subs text[]; v_cev text;
  v_tehsil text; v_state text; v_vcode integer; r record;
  v_lat double precision; v_lng double precision;
  v_inv public.invites; v_have_listing boolean;
begin
  if auth.uid() is null then raise exception 'not_signed_in'; end if;
  if v_mobile is null or v_mobile !~ '^[0-9]{10}$' then raise exception 'mobile_not_verified'; end if;
  if length(trim(coalesce(p_full_name, ''))) = 0 then raise exception 'name_required'; end if;
  if c is null or coalesce((c ->> 'enabled')::boolean, true) = false then raise exception 'bad_category'; end if;

  if public.field_state('village') <> 'off' then v_village := nullif(trim(coalesce(p_village, '')), ''); end if;
  if public.field_state('district') <> 'off' then v_district := nullif(trim(coalesce(p_district, '')), ''); end if;
  v_tehsil := left(nullif(trim(coalesce(p_tehsil, '')), ''), 80);
  v_state  := left(nullif(trim(coalesce(p_state, '')), ''), 80);

  if p_village_code is not null then
    select v.code as vcode, v.name as vname, t.name as tname, d.name as dname, s.name as sname into r
    from public.lgd_villages v
    join public.lgd_units t on t.kind = 'T' and t.code = v.subdistrict_code
    join public.lgd_units d on d.kind = 'D' and d.code = t.parent
    join public.lgd_units s on s.kind = 'S' and s.code = d.parent
    where v.code = p_village_code;
    if not found then raise exception 'bad_location'; end if;
    v_vcode := r.vcode; v_tehsil := r.tname; v_state := r.sname;
    if public.field_state('village') <> 'off' then v_village := r.vname; end if;
    if public.field_state('district') <> 'off' then v_district := r.dname; end if;
  end if;

  if public.field_state('village') = 'required' and v_village is null then raise exception 'field_required:village'; end if;
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

  v_subs := coalesce(p_sub_categories, '{}');
  if exists (select 1 from unnest(v_subs) u
             where not exists (select 1 from jsonb_array_elements(c -> 'subs') s where s ->> 'en' = u)) then
    raise exception 'bad_sub';
  end if;
  v_cev := case when coalesce((c ->> 'cev')::boolean, false) and p_cev_use in ('on_site', 'low_bed_transport')
                then p_cev_use end;

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
    user_id, full_name, village, district, tehsil, state, village_code, category, sub_categories, service_areas, cev_use,
    indicative_rate, rc_number, insurance_until, fitness_until, phone, phone_public, notice_language,
    latitude, longitude, extra
  ) values (
    auth.uid(), trim(p_full_name), v_village, v_district, v_tehsil, v_state, v_vcode, p_category, v_subs, v_areas, v_cev,
    v_rate, nullif(trim(coalesce(p_rc_number, '')), ''), p_insurance_until, p_fitness_until, v_mobile,
    coalesce(p_phone_public, false), coalesce(p_notice_language, 'en'), v_lat, v_lng, v_extra
  )
  on conflict (user_id) do update set
    full_name = excluded.full_name, village = excluded.village, district = excluded.district,
    tehsil = excluded.tehsil, state = excluded.state, village_code = excluded.village_code,
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
end $$;

revoke all on function public.register_partner(text, text, text, text, text[], text[], boolean, text, text, text, date, date, text,
  double precision, double precision, jsonb, text, text, text, integer) from public, anon;
grant execute on function public.register_partner(text, text, text, text, text[], text[], boolean, text, text, text, date, date, text,
  double precision, double precision, jsonb, text, text, text, integer) to authenticated;
