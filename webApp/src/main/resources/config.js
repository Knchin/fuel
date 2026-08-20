// Local development config - overridden by CI in production from GitHub secret SUPABASE_ANON_KEY
// Replace with your local Supabase anon key for development
window.FUEL_CONFIG = window.FUEL_CONFIG || {
  SUPABASE_URL: "https://uyeipadtsvbtrvtimotz.supabase.co",
  SUPABASE_ANON_KEY: "YOUR_SUPABASE_ANON_KEY_HERE"
};