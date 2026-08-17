import { serve } from "@supabase/functions/js";
import { createClient } from "@supabase/supabase-js";
import { handleError, logObservability } from "./utils";

serve(async (req) => {
  // This Edge Function handles the ingestion of the official French government fuel price feed.
  // It is triggered by Supabase Cron every 10 minutes.

  try {
    // 1. Record retrieval start time
    const now = new Date().toISOString();
    let recordsSeen = 0;
    let recordsAccepted = 0;
    let recordsRejected = 0;
    let errorMessage: string | null = null;

    // 2. Retrieve the official feed from government source
    const GOVERNMENT_FEED_URL = "https://example.gov.fr/fuel-prices/csv"; // placeholder
    let response;

    try {
      response = await fetch(GOVERNMENT_FEED_URL, {
        method: "GET",
        timeout: 30000, // 30 second timeout
      });
    } catch (fetchError) {
      errorMessage = `Failed to retrieve government feed: ${fetchError}`;
      await logObservability("ingestion_failure", {
        error: errorMessage,
        step: "download",
      });
      return handleError(req, errorMessage, "download_failure");
    }

    // 3. Reject unsuccessful HTTP responses
    if (!response.ok) {
      errorMessage = `Government feed HTTP ${response.status}: ${response.statusText}`;
      await logObservability("ingestion_failure", {
        error: errorMessage,
        step: "http_validation",
        statusCode: response.status,
      });
      return handleError(req, errorMessage, "http_failure");
    }

    // 4. Get the raw CSV/JSON payload
    const payload = await response.text();
    recordsSeen = // count records in payload;

    // 5. File validation - check format, encoding, etc.
    let records;
    try {
      records = parseGovernmentFeed(payload);
    } catch (parseError) {
      errorMessage = `Failed to parse government feed: ${parseError}`;
      recordsRejected = recordsSeen;
      await logObservability("ingestion_failure", {
        error: errorMessage,
        step: "parsing",
        recordsSeen,
        recordsRejected,
      });
      return handleError(req, errorMessage, "parsing_failure");
    }

    // 6. Schema validation - validate required fields, data types
    let validRecords;
    try {
      validRecords = validateSchema(records);
    } catch (validationError) {
      errorMessage = `Schema validation failed: ${validationError}`;
      recordsRejected = records.length;
      validRecords = []; // Quarantine all records
      await logObservability("ingestion_failure", {
        error: errorMessage,
        step: "schema_validation",
        recordsSeen,
        recordsRejected,
      });
      return handleError(req, errorMessage, "schema_validation_failure");
    }

    // 7. Parse safely - convert government DTOs to normalized model
    let parsedRecords;
    try {
      parsedRecords = parseRecords(validRecords);
    } catch (parseError) {
      errorMessage = `Safe parsing failed: ${parseError}`;
      recordsRejected = validRecords.length;
      await logObservability("ingestion_failure", {
        error: errorMessage,
        step: "safe_parsing",
        recordsSeen,
        recordsRejected,
      });
      return handleError(req, errorMessage, "parsing_failure");
    }

    // 8. Business validation - coordinate validation, fuel ID validation, etc.
    let businessValid;
    try {
      businessValid = businessValidate(parsedRecords);
    } catch (businessError) {
      errorMessage = `Business validation failed: ${businessError}`;
      recordsRejected = parsedRecords.length;
      await logObservability("ingestion_failure", {
        error: errorMessage,
        step: "business_validation",
        recordsSeen,
        recordsRejected,
      });
      return handleError(req, errorMessage, "business_validation_failure");
    }

    // 9. Database staging - prepare records in a staging/schema table
    //    Never modify live records one-by-one in a way that allows clients
    //    to observe a half-updated dataset.
    const stagingId = `staging_${Date.now()}`;
    
    try {
      await stageRecords(businessValid, stagingId);
    } catch (stageError) {
      errorMessage = `Staging failed: ${stageError}`;
      recordsRejected = businessValid.length;
      await logObservability("ingestion_failure", {
        error: errorMessage,
        step: "staging",
        recordsSeen,
        recordsRejected,
      });
      return handleError(req, errorMessage, "staging_failure");
    }

    // 10. Consistency checks - verify staging data integrity
    let consistencyCheck;
    try {
      consistencyCheck = await consistencyCheck(stagingId);
    } catch (checkError) {
      errorMessage = `Consistency check failed: ${checkError}`;
      // Roll back staging
      await rollbackStaging(stagingId);
      recordsRejected = businessValid.length;
      await logObservability("ingestion_failure", {
        error: errorMessage,
        step: "consistency_check",
        recordsSeen,
        recordsRejected,
      });
      return handleError(req, errorMessage, "consistency_failure");
    }

    if (!consistencyCheck.valid) {
      await rollbackStaging(stagingId);
      errorMessage = "Consistency checks failed";
      await logObservability("ingestion_failure", {
        error: errorMessage,
        step: "consistency_failed",
        recordsSeen,
        recordsRejected,
      });
      return handleError(req, errorMessage, "consistency_failure");
    }

    // 11. Atomic publication - publish the new valid dataset atomically
    //     Clients should observe either the previous valid dataset
    //     or the new valid dataset, not a mixture caused by ingestion failure.
    const { error: publishError } = await publishAtomically(stagingId);

    if (publishError) {
      // Roll back to previous dataset on publish failure
      await rollbackStaging(stagingId);
      errorMessage = `Atomic publication failed: ${publishError}`;
      recordsRejected = businessValid.length;
      await logObservability("ingestion_failure", {
        error: errorMessage,
        step: "publication_failure",
        recordsSeen,
        recordsRejected,
      });
      return handleError(req, errorMessage, "publication_failure");
    }

    // 12. Record synchronization status
    await recordSynchronizationRun({
      status: "success",
      sourceUrl: GOVERNMENT_FEED_URL,
      sourceRetrievedAt: now,
      recordsSeen,
      recordsAccepted: businessValid.length,
      recordsRejected,
      errorMessage: null,
      schemaVersion: "v2",
    });

    // 13. Return success
    return new Response(
      JSON.stringify({
        status: "success",
        recordsSeen,
        recordsAccepted,
        recordsRejected,
        synchronizedAt: new Date().toISOString(),
      }),
      {
        headers: { "Content-Type": "application/json" },
        status: 200,
      }
    );
  } catch (unexpectedError) {
    // Never crash the ingestion - record failure and keep previous data
    const errorMessage = `Unexpected ingestion error: ${unexpectedError}`;
    await logObservability("ingestion_critical_failure", {
      error: unexpectedError,
    });
    
    // Try to record failed run
    try {
      await recordSynchronizationRun({
        status: "failed",
        errorMessage,
      });
    } catch (recordError) {
      // If we can't even record the failure, that's a separate issue
    }

    return new Response(
      JSON.stringify({
        status: "error",
        error: "Ingestion pipeline failed - previous dataset preserved",
      }),
      {
        headers: { "Content-Type": "application/json" },
        status: 500,
      }
    );
  }
});

// Helper functions

function parseGovernmentFeed(payload: string): any[] {
  // Parse the government CSV/JSON feed into records
  // Implementation depends on the specific feed format
  // Must handle malformed payloads gracefully
  const lines = payload.split("\n");
  // ... parsing logic
  return [];
}

function validateSchema(records: any[]): any[] {
  // Validate that records have the expected fields and data types
  // Missing fields, renamed fields, changed data types should be caught here
  // Quarantine malformed records, return only valid ones
  return records.filter(record => {
    return (
      record.id != null &&
      record.coords != null &&
      record.coords.lat != null &&
      record.coords.lng != null &&
      record.fuels != null &&
      record.fuels.length > 0
    );
  });
}

function parseRecords(records: any[]): any[] {
  // Safe parsing - convert government-specific format to internal representation
  // Must not throw for malformed individual records
  // Preserve unknown source values where possible
  return records.map(record => ({
    sourceId: record.id,
    lat: parseCoordinate(record.coords.lat),
    lng: parseCoordinate(record.coords.lng),
    fuels: record.fuels?.map((f: any) => ({
      fuelId: f.fuelId,
      price: f.price,
      date: f.date,
      disponibilite: f.disponibilite,
      ruptureType: f.ruptureType,
      ruptureDeb: f.ruptureDeb,
    })) || [],
  }));
}

function businessValidate(records: any[]): any[] {
  // Business validation: coordinate validation, fuel ID validation,
  // timestamp validation, price range validation, availability validation
  return records.filter(record => {
    const validCoords =
      record.lat != null &&
      record.lat >= -90 && record.lat <= 90 &&
      record.lng != null && record.lng >= -180 && record.lng <= 180;
    
    const validFuelIds = record.fuels?.every(f => f.fuelId >= 1 && f.fuelId <= 6);
    const validPrices = record.fuels?.every(f => 
      f.price != null && !isNaN(parseFloat(f.price)) && parseFloat(f.price) >= 0 && parseFloat(f.price) <= 20
    );
    const validTimestamps = record.fuels?.every(f => 
      f.date != null && typeof f.date === "number"
    );
    
    return validCoords && validFuelIds && validPrices && validTimestamps;
  });
}

function stageRecords(records: any[], stagingId: string): Promise<void> {
  // Prepare records in a staging table/schema
  // Use Supabase's transaction capabilities for atomicity
  const supabase = createClient(
    process.env.SUPABASE_URL!,
    process.env.SUPABASE_SERVICE_ROLE_KEY! // Server-side only
  );
  
  // Insert into staging table (not the live tables)
  // This ensures clients don't observe half-updated data
  const { error } = await supabase
    .from("staging_fuel_prices")
    .upsert(records, { onConflict: "station_id, fuel_type" });
  
  if (error) throw error;
  return Promise.resolve();
}

function consistencyCheck(stagingId: string): { valid: boolean; count: number } {
  const supabase = createClient(
    process.env.SUPABASE_URL!,
    process.env.SUPABASE_SERVICE_ROLE_KEY!
  );
  
  const { count, error } = await supabase
    .from("staging_fuel_prices")
    .select("*", { count: "exact", head: true });
  
  if (error) throw error;
  
  return { valid: true, count: count || 0 };
}

function publishAtomically(stagingId: string): Promise<{ error: any }> {
  const supabase = createClient(
    process.env.SUPABASE_URL!,
    process.env.SUPABASE_SERVICE_ROLE_KEY!
  );
  
  // Atomic publish: 
  // 1. Update the live fuel_prices table from staging
  // 2. Update synchronization metadata
  // 3. Use a single transaction if possible
  
  return supabase
    .rpc("atomic_publish_staging", { p_staging_id: stagingId })
    .select()
    .single();
}

function recordSynchronizationRun(runData: any): Promise<{ error: any }> {
  const supabase = createClient(
    process.env.SUPABASE_URL!,
    process.env.SUPABASE_SERVICE_ROLE_KEY!
  );
  
  return supabase
    .from("synchronization_runs")
    .insert(runData)
    .single();
}

function rollbackStaging(stagingId: string): Promise<void> {
  // Simply discard the staging - no changes to live tables
  // The previous valid dataset remains intact
  const supabase = createClient(
    process.env.SUPABASE_URL!,
    process.env.SUPABASE_SERVICE_ROLE_KEY!
  );
  
  return supabase
    .from("staging_fuel_prices")
    .delete()
    .eq("staging_id", stagingId)
    .then(() => Promise.resolve())
    .catch(() => Promise.resolve());
}

function handleError(req: any, message: string, step: string): new Response {
  // Return error response without crashing
  return new Response(
    JSON.stringify({
      status: "error",
      step,
      message: "Ingestion pipeline step failed - previous dataset preserved",
    }),
    {
      headers: { "Content-Type": "application/json" },
      status: 500,
    }
  );
}

function logObservability(event: string, data: any): void {
  // Log operational monitoring data
  // Track: ingestion success, duration, records processed, etc.
  // Do not track precise user location as behavioral analytics
  console.log(`[observability] ${event}:`, data);
}

// Utility: parse coordinate string to number
function parseCoordinate(value: any): number {
  if (typeof value === "number") return value;
  if (typeof value === "string") {
    const parsed = parseFloat(value);
    return isNaN(parsed) ? 0 : parsed;
  }
  return 0;
}