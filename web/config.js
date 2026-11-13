// Public client config. The anon key is meant to be public; access is protected by RLS and the
// SECURITY DEFINER functions in supabase/schema.sql. Never put the service_role key here.
// Leave both blank (or open the page with ?demo) to run with sample data and any 6-digit OTP.
window.KAAMO_CONFIG = {
  url: "https://tqrghcrfwzvwamaxabyx.supabase.co",
  anonKey: "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InRxcmdoY3Jmd3p2d2FtYXhhYnl4Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTQ0NzczMDUsImV4cCI6MjExMDA1MzMwNX0.9xxTWves0_gMT4VQoz8c2d_WHYaxIlO8YVjGTLhop5U",
};
