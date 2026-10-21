// Local protocol spike only; not a GUI operation or SDK/versioning certification.
import { randomUUID } from "node:crypto";

const endpoint = "http://127.0.0.1:4566";
const bucket = `stackemu-delete-spike-${randomUUID().replaceAll("-", "")}`;
const object = "conditional.txt";
const guard = "guard.txt";
const request = (method, suffix = "", body, headers) =>
  globalThis.fetch(`${endpoint}/${bucket}${suffix}`, {
    method,
    body,
    headers,
    redirect: "error",
    signal: globalThis.AbortSignal.timeout(3000),
  });
let created = false;
let versionBucketCreated = false;
const versionBucket = `${bucket}-v`;
const versionRequest = (method, suffix = "", body) =>
  globalThis.fetch(`${endpoint}/${versionBucket}${suffix}`, {
    method,
    body,
    headers: { "Content-Type": "application/xml" },
    redirect: "error",
    signal: globalThis.AbortSignal.timeout(3000),
  });
const result = { endpoint, bucket, protocolOnly: true, checks: {} };
try {
  const creation = await request("PUT");
  if (creation.status !== 200) throw new Error(`Fixture bucket creation: ${creation.status}`);
  created = true;
  const version = await request("GET", "?versioning");
  const versionBody = await version.text();
  result.versioning = {
    status: version.status,
    state: /<Status>([^<]+)<\/Status>/.exec(versionBody)?.[1] ?? "not-reported",
  };
  for (const [key, body] of [
    [object, "generation-a"],
    [guard, "guard-unchanged"],
  ]) {
    if ((await request("PUT", `/${key}`, body)).status !== 200)
      throw new Error("Fixture upload failed");
  }
  const original = await request("HEAD", `/${object}`);
  const originalEtag = original.headers.get("etag");
  if (!originalEtag || original.status !== 200) throw new Error("Missing original ETag");
  const mismatched = await request("DELETE", `/${object}`, undefined, {
    "If-Match": '"00000000000000000000000000000000"',
  });
  const afterMismatch = await request("GET", `/${object}`);
  const mismatchBody = await afterMismatch.text();
  result.checks.mismatch = {
    status: mismatched.status,
    objectRetained: afterMismatch.status === 200 && mismatchBody === "generation-a",
  };
  if ((await request("PUT", `/${object}`, "generation-b")).status !== 200)
    throw new Error("Fixture replacement failed");
  const stale = await request("DELETE", `/${object}`, undefined, { "If-Match": originalEtag });
  const afterStale = await request("GET", `/${object}`);
  const staleBody = await afterStale.text();
  result.checks.stale = {
    status: stale.status,
    replacementRetained: afterStale.status === 200 && staleBody === "generation-b",
  };
  if ((await request("PUT", `/${object}`, "generation-c")).status !== 200)
    throw new Error("Fixture final upload failed");
  const current = await request("HEAD", `/${object}`);
  const currentEtag = current.headers.get("etag");
  if (!currentEtag || current.status !== 200) throw new Error("Missing current ETag");
  const matching = await request("DELETE", `/${object}`, undefined, { "If-Match": currentEtag });
  const afterMatch = await request("HEAD", `/${object}`);
  result.checks.match = { status: matching.status, objectAbsent: afterMatch.status === 404 };
  const guardResponse = await request("GET", `/${guard}`);
  result.checks.guardRetained =
    guardResponse.status === 200 && (await guardResponse.text()) === "guard-unchanged";
  if ((await versionRequest("PUT")).status !== 200)
    throw new Error("Version fixture creation failed");
  versionBucketCreated = true;
  result.versionStateChecks = {};
  for (const state of ["Enabled", "Suspended"]) {
    const update = await versionRequest(
      "PUT",
      "?versioning",
      `<VersioningConfiguration xmlns="http://s3.amazonaws.com/doc/2006-03-01/"><Status>${state}</Status></VersioningConfiguration>`,
    );
    const read = await versionRequest("GET", "?versioning");
    const body = await read.text();
    const reported = /<Status>([^<]+)<\/Status>/.exec(body)?.[1] ?? "not-reported";
    result.versionStateChecks[state] = {
      updateStatus: update.status,
      readStatus: read.status,
      reported,
    };
  }
  result.versionStateGatePassed = Object.entries(result.versionStateChecks).every(
    ([expected, actual]) =>
      actual.updateStatus === 200 && actual.readStatus === 200 && actual.reported === expected,
  );
  result.conditionalDeleteGatePassed =
    result.checks.mismatch.status === 412 &&
    result.checks.mismatch.objectRetained &&
    result.checks.stale.status === 412 &&
    result.checks.stale.replacementRetained &&
    result.checks.match.status === 204 &&
    result.checks.match.objectAbsent &&
    result.checks.guardRetained;
  if (!result.conditionalDeleteGatePassed || !result.versionStateGatePassed) process.exitCode = 1;
} finally {
  if (created) {
    // Exact UUID-owned targets only. Never enumerate or delete unrelated resources.
    const cleanup = [];
    for (const key of [object, guard]) cleanup.push((await request("DELETE", `/${key}`)).status);
    cleanup.push((await request("DELETE")).status);
    result.cleanup = cleanup;
    if (cleanup.some((status) => status !== 204)) process.exitCode = 1;
  }
  if (versionBucketCreated) {
    // This separate version-state fixture never receives objects or versions.
    result.versionBucketCleanup = (await versionRequest("DELETE")).status;
    if (result.versionBucketCleanup !== 204) process.exitCode = 1;
  }
  globalThis.console.log(JSON.stringify(result, null, 2));
}
