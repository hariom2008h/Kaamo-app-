import { createClient } from "npm:@supabase/supabase-js@2";

// Turns a position into state / district / tehsil / village hints using OpenStreetMap Nominatim.
// Signed-in users only, positions rounded to ~110 m, results cached, 1 request per second at most,
// and a daily cap per user, in line with the Nominatim usage policy.

const CORS = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, apikey, content-type, x-client-info",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};
const json = (b: unknown, status = 200) =>
  new Response(JSON.stringify(b), { status, headers: { ...CORS, "Content-Type": "application/json" } });

const DAILY_CAP = 40;
let lastCall = 0;

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") return new Response(null, { headers: CORS });
  if (req.method !== "POST") return json({ error: "method" }, 405);

  const admin = createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!, {
    auth: { persistSession: false },
  });
  const token = (req.headers.get("Authorization") || "").replace(/^Bearer\s+/i, "");
  const { data: u } = await admin.auth.getUser(token);
  const uid = u?.user?.id;
  if (!uid) return json({ error: "not_signed_in" }, 401);

  let body: { lat?: number; lng?: number } = {};
  try { body = await req.json(); } catch { /* ignore */ }
  const lat = Number(body.lat), lng = Number(body.lng);
  if (!Number.isFinite(lat) || !Number.isFinite(lng) || lat < 6 || lat > 38 || lng < 68 || lng > 98) {
    return json({ error: "bad_position" }, 400);
  }
  const rl = lat.toFixed(3), rn = lng.toFixed(3);
  const key = `${rl},${rn}`;

  const { data: hit } = await admin.from("geo_cache").select("result").eq("key", key).maybeSingle();
  if (hit) return json({ ...hit.result, cached: true });

  const since = new Date(Date.now() - 24 * 3600 * 1000).toISOString();
  const { count } = await admin.from("geo_calls").select("*", { count: "exact", head: true }).eq("user_id", uid).gte("at", since);
  if ((count ?? 0) >= DAILY_CAP) return json({ error: "limit" }, 429);

  const wait = 1100 - (Date.now() - lastCall);
  if (wait > 0) await new Promise((r) => setTimeout(r, wait));
  lastCall = Date.now();

  let r: Response;
  try {
    r = await fetch(
      `https://nominatim.openstreetmap.org/reverse?format=jsonv2&addressdetails=1&zoom=14&accept-language=en&lat=${rl}&lon=${rn}`,
      { headers: { "User-Agent": "Kaamo-directory/1.0 (+https://kaamo-web.vercel.app)", Referer: "https://kaamo-web.vercel.app/" } },
    );
  } catch {
    return json({ error: "upstream" }, 502);
  }
  if (!r.ok) return json({ error: "upstream" }, 502);
  const j = await r.json();
  const a = j.address || {};
  const result = {
    state: a.state || null,
    district: a.state_district || null,
    county: a.county || null,
    village: a.village || a.hamlet || null,
    town: a.town || a.city || null,
    suburb: a.suburb || a.neighbourhood || null,
    postcode: a.postcode || null,
    attribution: "© OpenStreetMap contributors",
  };
  await admin.from("geo_calls").insert({ user_id: uid });
  await admin.from("geo_cache").upsert({ key, result });
  return json({ ...result, cached: false });
});
