# LGD seed data

Administrative units (state, district, sub-district/tehsil, village) from the India Local Government Directory,
via https://github.com/planemad/india-local-government-directory (retrieved March 2022 by that project).
Licence: Government Open Data License - India. Columns are reduced to what the app needs.

- `units.tsv`: type (S state, D district, T sub-district), code, parent code, name
- `villages_<state code>.tsv`: village code, sub-district code, name, block name

Loaded into the `lgd_*` tables by `supabase/migrations/004_location_autofill.sql` + the loader snippet in that file's header.
Refresh with a newer LGD dump when available.
