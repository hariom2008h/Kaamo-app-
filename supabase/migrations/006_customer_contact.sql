-- Customers can sign in with an email code; their mobile number is collected without OTP (for now).
alter table public.profiles add column if not exists contact_mobile text
  check (contact_mobile is null or contact_mobile ~ '^[0-9]{10}$');

drop function if exists public.register_customer(text, text);
create or replace function public.register_customer(p_full_name text, p_location text, p_mobile text default null)
returns void language plpgsql security definer set search_path = public as $$
declare v_mobile text := nullif(regexp_replace(coalesce(p_mobile, ''), '\D', '', 'g'), '');
begin
  if auth.uid() is null then raise exception 'not_signed_in'; end if;
  if length(trim(coalesce(p_full_name, ''))) = 0 then raise exception 'name_required'; end if;
  if v_mobile is not null and v_mobile !~ '^[6-9][0-9]{9}$' then raise exception 'mobile_invalid'; end if;
  insert into public.profiles (id, role, full_name, location, contact_mobile)
  values (auth.uid(), 'customer', trim(p_full_name), nullif(trim(coalesce(p_location, '')), ''), v_mobile)
  on conflict (id) do update set full_name = excluded.full_name, location = excluded.location,
    contact_mobile = coalesce(excluded.contact_mobile, public.profiles.contact_mobile);
end;
$$;
revoke all on function public.register_customer(text, text, text) from public, anon;
grant execute on function public.register_customer(text, text, text) to authenticated;
