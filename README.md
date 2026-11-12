# Kaamo (basic version)

Android app (Kotlin, Jetpack Compose) for a local-services directory: plumbers, electricians, construction workers, borewell, JCB, tractor and transport. A customer opens the app, is asked **"What work do you need? / आपको क्या काम है?"**, filters or searches, and calls the provider directly. Kaamo is only a directory: no booking, no payments, no ranking, no "verified" claims.

"Kaamo" is a working name pending the trademark check. The name lives in `AppConfig.NAME` and `res/values/strings.xml`; `scripts/rename_app.sh NewName` renames it in code, legal copy, SQL and docs.

## What is in this version

- Splash, role choice, mobile OTP login with unticked Terms/Privacy consent
- Customer registration (name, location)
- Partner registration (category and sub-categories, area, optional rate, documents only for vehicles, declaration, separate "show my number" consent, default OFF)
- Directory with search and category filter pills; masked numbers; a pre-call acknowledgement before every call; call opens the dialer
- One-tap report of a listing; account screen with phone-visibility toggle and listing removal
- Supabase backend (`supabase/schema.sql`): tables, RLS, rate-limited contact reveal, append-only consent log
- Demo mode with sample data when no Supabase keys are set

Not in this version: listing detail page, location-based distance, push notifications, admin panel for reports, English/Hindi UI strings (the app is English with Hindi on legal notices; see `res` for where to add translations).

## Run it

1. Open the folder in Android Studio (Koala or newer, JDK 17). It creates `local.properties` with `sdk.dir`.
2. The app is already pointed at the Supabase project "kaamo" (`SUPABASE_URL` and the public anon key in `gradle.properties`; `supabase/schema.sql` is already applied there). Put different values in `local.properties` to override them, or blank values to run in demo mode (sample data, any 6-digit OTP).
3. **Phone login must be switched on in the Supabase dashboard** (Authentication > Providers > Phone) with an SMS provider that works in India (DLT registration is required). For testing without SMS, add test phone numbers with fixed OTPs in the same place.
4. GitHub Actions builds a debug APK on every push (`.github/workflows/android.yml`).

## How the backend protects phone numbers

- The app never reads the partners table. It calls `list_partners()` (masked numbers only) and `reveal_contact(id)`.
- `reveal_contact` needs a signed-in user, allows 10 per hour and 30 per day, logs every reveal, and only returns numbers of listings that are active, opted in and (for vehicles) have unexpired insurance/fitness.
- `consent_audit_logs` and `contact_reveal_logs` cannot be edited or deleted. Withdrawing consent adds a row.
- `register_partner` rejects listings that mention sand, bricks, earth, cement or mining (the server twin of the app's keyword guard).

## Before you release

- The app was written without an Android SDK in the build environment, so it has **not been compiled or run on a device**. Expect to fix small compile errors on the first build. The SQL was tested on a local Postgres 16 with a stub of Supabase's auth schema.
- Legal text (`legal/LegalCopy.kt`) is draft wording. Appoint a grievance officer, fill the placeholders in `LegalConfig`, host the Terms and Privacy pages, and have Indian counsel approve everything.
- DPDP Act duties (notice, consent, withdrawal) are built in; confirm retention periods and the grievance process with counsel.
- Logo: the mark in `res/drawable/logo_kaamo.xml` is the earlier "K" mark, and the wordmark is plain text. Replace both once the Kaamo logo exists.
- Add an app icon (`mipmap`), a real splash theme, Inter font, and Hindi/regional UI strings.
- Move the session token to EncryptedSharedPreferences, add ProGuard rules, and set up Play Console data-safety answers.
