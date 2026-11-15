-- Cache of reverse-geocode results (written only by the reverse-geocode Edge Function via the service role).
create table if not exists public.geo_cache (
  key text primary key,
  result jsonb not null,
  created_at timestamptz not null default now()
);
alter table public.geo_cache enable row level security;
revoke all on public.geo_cache from anon, authenticated;
