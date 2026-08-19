import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

interface RawCsvRow {
  id: string;
  departement: string;
  nom: string;
  adresse: string;
  cp: string;
  complement: string;
  latitude: string;
  longitude: string;
  carburant: string;
  prix: string;
  maj: string;
  horaires: string;
  geom: string;
  service: string;
  automate_24h: string;
  pid: string;
  pop: string;
  commune: string;
  is_occase: string;
  is_routier: string;
  is_station: string;
  is_lave: string;
}

interface ParsedStation {
  sourceId: string;
  name: string;
  address: string;
  postalCode: string;
  city: string;
  department: string;
  latitude: number;
  longitude: number;
  services: string;
  openingHours: string;
  isOccase: boolean;
  isRoutier: boolean;
  fuels: ParsedFuel[];
}

interface ParsedFuel {
  fuelType: string;
  pricePerLiter: number;
  reportedAt: string;
  availability: string;
}

// ---------------------------------------------------------------------------
// Fuel type mapping: government codes -> internal codes
// ---------------------------------------------------------------------------

const FUEL_TYPE_MAP: Record<string, string> = {
  gazole: "gazole",
  sp95: "sp95",
  e10: "e10",
  sp98: "sp98",
  gnv: "gnv",
  e85: "e85",
  "ec80": "gplc",
  "e80": "gplc",
};

const VALID_FUEL_TYPES = new Set(["gazole", "sp95", "e10", "sp98", "gnv", "e85", "gplc"]);

// ---------------------------------------------------------------------------
// Observability
// ---------------------------------------------------------------------------

function logObservability(
  event: string,
  data: Record<string, unknown>,
): void {
  console.log(
    JSON.stringify({
      ts: new Date().toISOString(),
      event,
      ...data,
    }),
  );
}

// ---------------------------------------------------------------------------
// Error response helper
// ---------------------------------------------------------------------------

function handleError(
  _req: Request,
  message: string,
  step: string,
): Response {
  logObservability("ingestion_failure", { step, error: message });
  return new Response(
    JSON.stringify({
      status: "error",
      step,
      message: "Ingestion pipeline step failed - previous dataset preserved",
    }),
    {
      headers: { "Content-Type": "application/json" },
      status: 500,
    },
  );
}

// ---------------------------------------------------------------------------
// Supabase client (service-role, server-side only)
// ---------------------------------------------------------------------------

function getSupabase() {
  const url = Deno.env.get("SUPABASE_URL")!;
  const key = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
  return createClient(url, key);
}

// ---------------------------------------------------------------------------
// Step 1-2: Download & validate HTTP
// ---------------------------------------------------------------------------

const PRIMARY_FEED_URL =
  "https://donnees.roulez-eco.fr/api/explore/v2.0/catalog/datasets/prix-des-carburants-en-france-flux-instantane/exports/csv?delimiter=%3B";
const FALLBACK_FEED_URL =
  "https://data.economie.gouv.fr/api/explore/v2.1/catalog/datasets/prix-des-carburants-en-france-flux-instantane/exports/csv?delimiter=%3B";

async function fetchFeed(): Promise<{ text: string; url: string }> {
  let response: Response;
  try {
    response = await fetch(PRIMARY_FEED_URL, { redirect: "follow" });
  } catch (_e) {
    logObservability("feed_primary_failed", { url: PRIMARY_FEED_URL });
    try {
      response = await fetch(FALLBACK_FEED_URL, { redirect: "follow" });
    } catch (e2) {
      throw new Error(`Both feed URLs failed: ${e2}`);
    }
  }

  if (!response.ok) {
    const body = await response.text().catch(() => "");
    throw new Error(
      `HTTP ${response.status} from ${response.url}: ${body.slice(0, 200)}`,
    );
  }

  const text = await response.text();
  return { text, url: response.url };
}

// ---------------------------------------------------------------------------
// Step 3: Parse CSV (semicolon-delimited)
// ---------------------------------------------------------------------------

function parseCsvLines(raw: string): RawCsvRow[] {
  const lines = raw.split("\n").filter((l) => l.trim().length > 0);
  if (lines.length < 2) throw new Error("CSV has no data rows");

  const headerLine = lines[0];
  const headers = headerLine.split(";").map((h) => h.trim().replace(/^\uFEFF/, ""));

  const rows: RawCsvRow[] = [];
  for (let i = 1; i < lines.length; i++) {
    const values = lines[i].split(";");
    if (values.length < headers.length) continue;

    const row: Record<string, string> = {};
    for (let j = 0; j < headers.length; j++) {
      row[headers[j]] = (values[j] ?? "").trim();
    }
    rows.push(row as unknown as RawCsvRow);
  }
  return rows;
}

// ---------------------------------------------------------------------------
// Step 4-5: Schema validation
// ---------------------------------------------------------------------------

function schemaValidate(rows: RawCsvRow[]): RawCsvRow[] {
  const requiredFields = [
    "id", "nom", "adresse", "cp", "latitude", "longitude",
    "carburant", "prix", "maj",
  ];
  return rows.filter((row) => {
    for (const field of requiredFields) {
      const val = row[field as keyof RawCsvRow];
      if (val == null || val === "") return false;
    }
    return true;
  });
}

// ---------------------------------------------------------------------------
// Step 6-7: Safe parse & business validation
// ---------------------------------------------------------------------------

function safeParse(rows: RawCsvRow[]): ParsedStation[] {
  const stationMap = new Map<string, ParsedStation>();

  for (const row of rows) {
    const lat = parseFloat(row.latitude);
    const lng = parseFloat(row.longitude);
    const price = parseFloat(row.prix);

    if (isNaN(lat) || isNaN(lng) || isNaN(price)) continue;
    if (lat < -90 || lat > 90 || lng < -180 || lng > 180) continue;

    const fuelCode = FUEL_TYPE_MAP[row.carburant.toLowerCase()];
    if (!fuelCode || !VALID_FUEL_TYPES.has(fuelCode)) continue;

    const reportedAt = row.maj;
    if (!reportedAt) continue;

    const sourceId = row.pid || row.id;
    const existing = stationMap.get(sourceId);

    const fuel: ParsedFuel = {
      fuelType: fuelCode,
      pricePerLiter: price,
      reportedAt,
      availability: "available",
    };

    if (existing) {
      existing.fuels.push(fuel);
    } else {
      stationMap.set(sourceId, {
        sourceId,
        name: row.nom,
        address: row.adresse,
        postalCode: row.cp,
        city: row.commune || "",
        department: row.departement || "",
        latitude: lat,
        longitude: lng,
        services: row.service || "",
        openingHours: row.horaires || "",
        isOccase: row.is_occase === "1" || row.is_occase?.toLowerCase() === "oui",
        isRoutier: row.is_routier === "1" || row.is_routier?.toLowerCase() === "oui",
        fuels: [fuel],
      });
    }
  }

  return Array.from(stationMap.values());
}

function businessValidate(stations: ParsedStation[]): ParsedStation[] {
  return stations.filter((s) => {
    if (s.latitude < 42 || s.latitude > 52) return false;
    if (s.longitude < -6 || s.longitude > 10) return false;
    if (s.fuels.length === 0) return false;
    return true;
  });
}

// ---------------------------------------------------------------------------
// Step 8-9: Stage into DB
// ---------------------------------------------------------------------------

async function stageRecords(
  supabase: ReturnType<typeof getSupabase>,
  stations: ParsedStation[],
  stagingId: string,
): Promise<{ pricesCount: number; spatialCount: number }> {
  let spatialCount = 0;
  const spatialRows: Record<string, unknown>[] = [];

  const priceRows: Record<string, unknown>[] = [];

  for (const station of stations) {
    spatialRows.push({
      staging_id: stagingId,
      source_id: station.sourceId,
      address: station.address,
      postal_code: station.postalCode,
      city: station.city,
      latitude: station.latitude,
      longitude: station.longitude,
    });

    for (const fuel of station.fuels) {
      priceRows.push({
        staging_id: stagingId,
        station_source_id: station.sourceId,
        fuel_type: fuel.fuelType,
        price_per_liter: fuel.pricePerLiter,
        reported_at: fuel.reportedAt,
        availability: fuel.availability,
      });
    }
  }

  // Upsert stations into the live stations table so we can link foreign keys
  const stationUpserts = stations.map((s) => ({
    source_id: s.sourceId,
    address: s.address,
    postal_code: s.postalCode,
    city: s.city,
    latitude: s.latitude,
    longitude: s.longitude,
    services: s.services,
    opening_hours: s.openingHours,
    source: "gouvernement_francais",
    last_seen_at: new Date().toISOString(),
    data_synchronized_at: new Date().toISOString(),
    active: true,
  }));

  // Batch upsert stations
  const BATCH = 500;
  for (let i = 0; i < stationUpserts.length; i += BATCH) {
    const batch = stationUpserts.slice(i, i + BATCH);
    const { error } = await supabase
      .from("stations")
      .upsert(batch, { onConflict: "source_id, source" });
    if (error) {
      throw new Error(`Station upsert error: ${error.message}`);
    }
  }

  // Fetch back the UUIDs for our source_ids
  const sourceIds = stations.map((s) => s.sourceId);
  const { data: stationRows, error: fetchErr } = await supabase
    .from("stations")
    .select("id, source_id")
    .in("source_id", sourceIds);

  if (fetchErr) throw new Error(`Station fetch error: ${fetchErr.message}`);

  const idMap = new Map<string, string>();
  for (const row of stationRows ?? []) {
    idMap.set(row.source_id, row.id);
  }

  // Build staging price rows with station_id FK
  const stagedPrices: Record<string, unknown>[] = [];
  for (const pr of priceRows) {
    const sid = idMap.get(pr.station_source_id as string);
    if (!sid) continue;
    stagedPrices.push({ ...pr, station_id: sid });
  }

  // Clear previous staging for this ID
  await supabase
    .from("staging_fuel_prices")
    .delete()
    .eq("staging_id", stagingId);

  // Insert staging prices
  for (let i = 0; i < stagedPrices.length; i += BATCH) {
    const batch = stagedPrices.slice(i, i + BATCH);
    const { error } = await supabase
      .from("staging_fuel_prices")
      .upsert(batch, {
        onConflict: "staging_id,station_source_id,fuel_type",
      });
    if (error) throw new Error(`Staging price error: ${error.message}`);
  }

  // Clear previous staging spatial
  await supabase
    .from("staging_fuel_spatial")
    .delete()
    .eq("staging_id", stagingId);

  // Insert staging spatial
  for (let i = 0; i < spatialRows.length; i += BATCH) {
    const batch = spatialRows.slice(i, i + BATCH);
    const { error } = await supabase
      .from("staging_fuel_spatial")
      .upsert(batch, {
        onConflict: "staging_id,source_id",
      });
    if (error) throw new Error(`Staging spatial error: ${error.message}`);
  }

  spatialCount = spatialRows.length;

  return { pricesCount: stagedPrices.length, spatialCount };
}

// ---------------------------------------------------------------------------
// Step 10: Consistency check
// ---------------------------------------------------------------------------

async function consistencyCheck(
  supabase: ReturnType<typeof getSupabase>,
  stagingId: string,
): Promise<{ valid: boolean; count: number }> {
  const { count, error } = await supabase
    .from("staging_fuel_prices")
    .select("*", { count: "exact", head: true })
    .eq("staging_id", stagingId);

  if (error) throw new Error(`Consistency check error: ${error.message}`);
  return { valid: (count ?? 0) > 0, count: count ?? 0 };
}

// ---------------------------------------------------------------------------
// Step 11: Atomic publish
// ---------------------------------------------------------------------------

async function publishAtomically(
  supabase: ReturnType<typeof getSupabase>,
  stagingId: string,
): Promise<void> {
  // Upsert fuel_prices from staging
  const { data: staged, error: fetchErr } = await supabase
    .from("staging_fuel_prices")
    .select("station_id, fuel_type, price_per_liter, reported_at, availability, rupture_type, rupture_started_at")
    .eq("staging_id", stagingId);

  if (fetchErr) throw new Error(`Fetch staged prices error: ${fetchErr.message}`);
  if (!staged || staged.length === 0) return;

  const BATCH = 500;
  const priceUpserts = staged.map((r) => ({
    station_id: r.station_id,
    fuel_type: r.fuel_type,
    price_per_liter: r.price_per_liter,
    reported_at: r.reported_at,
    availability: r.availability ?? "unknown",
    rupture_type: r.rupture_type ?? null,
    rupture_started_at: r.rupture_started_at ?? null,
    data_synchronized_at: new Date().toISOString(),
  }));

  for (let i = 0; i < priceUpserts.length; i += BATCH) {
    const batch = priceUpserts.slice(i, i + BATCH);
    const { error } = await supabase
      .from("fuel_prices")
      .upsert(batch, { onConflict: "station_id,fuel_type" });
    if (error) throw new Error(`Publish price error: ${error.message}`);
  }

  // Update stations last_seen_at from staging spatial
  const { data: spatialRows } = await supabase
    .from("staging_fuel_spatial")
    .select("source_id")
    .eq("staging_id", stagingId);

  if (spatialRows && spatialRows.length > 0) {
    const ids = spatialRows.map((r) => r.source_id);
    const BATCH2 = 500;
    for (let i = 0; i < ids.length; i += BATCH2) {
      const batch = ids.slice(i, i + BATCH2);
      await supabase
        .from("stations")
        .update({
          last_seen_at: new Date().toISOString(),
          data_synchronized_at: new Date().toISOString(),
          active: true,
        })
        .in("source_id", batch);
    }
  }
}

// ---------------------------------------------------------------------------
// Step 11b: Cleanup staging
// ---------------------------------------------------------------------------

async function cleanupStaging(
  supabase: ReturnType<typeof getSupabase>,
  stagingId: string,
): Promise<void> {
  await supabase
    .from("staging_fuel_prices")
    .delete()
    .eq("staging_id", stagingId);
  await supabase
    .from("staging_fuel_spatial")
    .delete()
    .eq("staging_id", stagingId);
}

// ---------------------------------------------------------------------------
// Step 12: Record synchronization run
// ---------------------------------------------------------------------------

async function recordSyncRun(
  supabase: ReturnType<typeof getSupabase>,
  data: {
    status: string;
    sourceUrl: string;
    sourceRetrievedAt: string;
    recordsSeen: number;
    recordsAccepted: number;
    recordsRejected: number;
    errorMessage?: string | null;
    schemaVersion?: string;
  },
): Promise<void> {
  const { error } = await supabase.from("synchronization_runs").insert({
    status: data.status,
    source_url: data.sourceUrl,
    source_retrieved_at: data.sourceRetrievedAt,
    completed_at: new Date().toISOString(),
    records_seen: data.recordsSeen,
    records_accepted: data.recordsAccepted,
    records_rejected: data.recordsRejected,
    error_message: data.errorMessage ?? null,
    schema_version: data.schemaVersion ?? "csv-v3",
  });

  if (error) {
    console.error("Failed to record sync run:", error.message);
  }
}

// ---------------------------------------------------------------------------
// Main handler
// ---------------------------------------------------------------------------

Deno.serve(async (req: Request): Promise<Response> => {
  const pipelineStart = Date.now();
  const supabase = getSupabase();
  let recordsSeen = 0;
  let recordsAccepted = 0;
  let recordsRejected = 0;
  let sourceUrl = "";
  let sourceRetrievedAt = "";

  try {
    // Step 1: Download
    sourceRetrievedAt = new Date().toISOString();
    logObservability("ingestion_start", { ts: sourceRetrievedAt });

    let feedText: string;
    try {
      const feed = await fetchFeed();
      feedText = feed.text;
      sourceUrl = feed.url;
    } catch (e) {
      const msg = `Download failed: ${e}`;
      await recordSyncRun(supabase, {
        status: "failed",
        sourceUrl,
        sourceRetrievedAt,
        recordsSeen: 0,
        recordsAccepted: 0,
        recordsRejected: 0,
        errorMessage: msg,
      });
      return handleError(req, msg, "download");
    }

    // Step 2: Parse CSV
    let rawRows: RawCsvRow[];
    try {
      rawRows = parseCsvLines(feedText);
      recordsSeen = rawRows.length;
      logObservability("csv_parsed", { rows: recordsSeen });
    } catch (e) {
      const msg = `CSV parse error: ${e}`;
      await recordSyncRun(supabase, {
        status: "failed",
        sourceUrl,
        sourceRetrievedAt,
        recordsSeen: 0,
        recordsAccepted: 0,
        recordsRejected: 0,
        errorMessage: msg,
      });
      return handleError(req, msg, "parsing");
    }

    // Step 3: Schema validate
    let schemaRows: RawCsvRow[];
    try {
      schemaRows = schemaValidate(rawRows);
      recordsRejected += rawRows.length - schemaRows.length;
      logObservability("schema_validated", {
        valid: schemaRows.length,
        rejected: rawRows.length - schemaRows.length,
      });
    } catch (e) {
      const msg = `Schema validation error: ${e}`;
      await recordSyncRun(supabase, {
        status: "failed",
        sourceUrl,
        sourceRetrievedAt,
        recordsSeen,
        recordsAccepted: 0,
        recordsRejected: recordsSeen,
        errorMessage: msg,
      });
      return handleError(req, msg, "schema_validation");
    }

    // Step 4: Safe parse & business validate
    let stations: ParsedStation[];
    try {
      const parsed = safeParse(schemaRows);
      stations = businessValidate(parsed);
      recordsAccepted = stations.reduce((sum, s) => sum + s.fuels.length, 0);
      recordsRejected += schemaRows.length -
        stations.reduce((sum, s) => sum + s.fuels.length, 0);
      logObservability("business_validated", {
        stations: stations.length,
        prices: recordsAccepted,
      });
    } catch (e) {
      const msg = `Business validation error: ${e}`;
      await recordSyncRun(supabase, {
        status: "failed",
        sourceUrl,
        sourceRetrievedAt,
        recordsSeen,
        recordsAccepted: 0,
        recordsRejected: recordsSeen,
        errorMessage: msg,
      });
      return handleError(req, msg, "business_validation");
    }

    if (stations.length === 0) {
      await recordSyncRun(supabase, {
        status: "success",
        sourceUrl,
        sourceRetrievedAt,
        recordsSeen,
        recordsAccepted: 0,
        recordsRejected: recordsSeen,
      });
      return new Response(
        JSON.stringify({
          status: "success",
          recordsSeen,
          recordsAccepted: 0,
          recordsRejected: recordsSeen,
          message: "No valid station records after filtering",
        }),
        { headers: { "Content-Type": "application/json" }, status: 200 },
      );
    }

    // Step 5: Stage
    const stagingId = `staging_${Date.now()}`;
    let pricesCount: number;
    try {
      const result = await stageRecords(supabase, stations, stagingId);
      pricesCount = result.pricesCount;
      logObservability("staged", {
        stagingId,
        prices: result.pricesCount,
        spatial: result.spatialCount,
      });
    } catch (e) {
      const msg = `Staging error: ${e}`;
      await recordSyncRun(supabase, {
        status: "failed",
        sourceUrl,
        sourceRetrievedAt,
        recordsSeen,
        recordsAccepted: 0,
        recordsRejected: recordsSeen,
        errorMessage: msg,
      });
      return handleError(req, msg, "staging");
    }

    // Step 6: Consistency check
    try {
      const check = await consistencyCheck(supabase, stagingId);
      if (!check.valid) {
        await cleanupStaging(supabase, stagingId);
        const msg = `Consistency check failed: 0 staged records`;
        await recordSyncRun(supabase, {
          status: "failed",
          sourceUrl,
          sourceRetrievedAt,
          recordsSeen,
          recordsAccepted: 0,
          recordsRejected: recordsSeen,
          errorMessage: msg,
        });
        return handleError(req, msg, "consistency_check");
      }
      logObservability("consistency_ok", { count: check.count });
    } catch (e) {
      await cleanupStaging(supabase, stagingId);
      const msg = `Consistency check error: ${e}`;
      await recordSyncRun(supabase, {
        status: "failed",
        sourceUrl,
        sourceRetrievedAt,
        recordsSeen,
        recordsAccepted: 0,
        recordsRejected: recordsSeen,
        errorMessage: msg,
      });
      return handleError(req, msg, "consistency_check");
    }

    // Step 7: Atomic publish
    try {
      await publishAtomically(supabase, stagingId);
      logObservability("published", { pricesCount });
    } catch (e) {
      await cleanupStaging(supabase, stagingId);
      const msg = `Publish error: ${e}`;
      await recordSyncRun(supabase, {
        status: "failed",
        sourceUrl,
        sourceRetrievedAt,
        recordsSeen,
        recordsAccepted: 0,
        recordsRejected: recordsSeen,
        errorMessage: msg,
      });
      return handleError(req, msg, "publication");
    }

    // Step 8: Cleanup staging
    await cleanupStaging(supabase, stagingId);

    // Step 9: Record sync run
    await recordSyncRun(supabase, {
      status: "success",
      sourceUrl,
      sourceRetrievedAt,
      recordsSeen,
      recordsAccepted,
      recordsRejected,
    });

    const duration = Date.now() - pipelineStart;
    logObservability("ingestion_complete", {
      duration,
      recordsSeen,
      recordsAccepted,
      recordsRejected,
    });

    return new Response(
      JSON.stringify({
        status: "success",
        recordsSeen,
        recordsAccepted,
        recordsRejected,
        duration,
        synchronizedAt: new Date().toISOString(),
      }),
      { headers: { "Content-Type": "application/json" }, status: 200 },
    );
  } catch (e) {
    const msg = `Unexpected error: ${e}`;
    logObservability("ingestion_critical_failure", { error: msg });
    try {
      await recordSyncRun(supabase, {
        status: "failed",
        sourceUrl,
        sourceRetrievedAt,
        recordsSeen,
        recordsAccepted: 0,
        recordsRejected: recordsSeen,
        errorMessage: msg,
      });
    } catch (_) {
      // Best effort
    }
    return new Response(
      JSON.stringify({
        status: "error",
        error: "Ingestion pipeline failed - previous dataset preserved",
      }),
      { headers: { "Content-Type": "application/json" }, status: 500 },
    );
  }
});
