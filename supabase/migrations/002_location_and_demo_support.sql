-- Incremental migration for databases created from the first version of schema.sql.
-- (Fresh installs: schema.sql already contains all of this.)
alter table public.partners alter column user_id drop not null;
alter table public.partners add column if not exists latitude double precision check (latitude between -90 and 90);
alter table public.partners add column if not exists longitude double precision check (longitude between -180 and 180);
alter table public.partners add column if not exists is_demo boolean not null default false;
alter table public.partners drop constraint if exists partners_demo_or_user;
alter table public.partners add constraint partners_demo_or_user check (is_demo or user_id is not null);

drop function if exists public.list_partners();
drop function if exists public.register_partner(text, text, text, text, text[], text[], boolean, text, text, text, date, date, text);
-- Then run the distance_km, register_partner and list_partners definitions and the grants from schema.sql.
