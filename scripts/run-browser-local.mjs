// Linux Docker test helper: expose the host GUI through a trusted loopback origin.
// No TLS bypass, Chromium security flags, arbitrary target or runtime proxy is introduced.
import { createServer, request } from "node:http";
import { spawn } from "node:child_process";
import { setTimeout, clearTimeout } from "node:timers";

if (process.platform !== "linux")
  throw new Error("Use this helper in the pinned Linux browser container");
// CI uses host networking: the GUI already owns 127.0.0.1:8080.
const hostNetwork = process.env.STACKEMU_BROWSER_HOST_NETWORK === "true";
const listenHost = hostNetwork ? "127.0.0.2" : "127.0.0.1";
const heldResponses = new Map();
const server = createServer((incoming, outgoing) => {
  const url = new URL(incoming.url, "http://127.0.0.1:8080");
  const token = url.searchParams.get("token");
  if (url.pathname.startsWith("/__stackemu_test/")) {
    if (!/^[a-f0-9]{32}$/.test(token ?? "")) {
      outgoing.writeHead(400).end();
      return;
    }
    if (incoming.method === "GET" && url.pathname === "/__stackemu_test/upload-status") {
      outgoing.writeHead(200, { "Content-Type": "application/json" });
      outgoing.end(JSON.stringify({ held: heldResponses.has(token) }));
    } else if (incoming.method === "POST" && url.pathname === "/__stackemu_test/upload-release") {
      heldResponses.get(token)?.();
      outgoing.writeHead(204).end();
    } else outgoing.writeHead(404).end();
    return;
  }
  const hold = incoming.headers["x-stackemu-test-hold"];
  const shouldHold =
    incoming.method === "POST" &&
    url.pathname === "/s3/write/upload" &&
    typeof hold === "string" &&
    /^[a-f0-9]{32}$/.test(hold);
  const headers = { ...incoming.headers };
  delete headers["x-stackemu-test-hold"];
  const upstream = request(
    {
      hostname: hostNetwork ? "127.0.0.1" : "host.docker.internal",
      port: 8080,
      path: incoming.url,
      method: incoming.method,
      headers,
    },
    (response) => {
      const flush = () => {
        outgoing.writeHead(response.statusCode, response.headers);
        response.pipe(outgoing);
      };
      if (shouldHold && heldResponses.size < 8 && !heldResponses.has(hold)) {
        response.pause();
        const release = () => {
          if (!heldResponses.delete(hold)) return;
          clearTimeout(timer);
          if (outgoing.destroyed) response.destroy();
          else flush();
        };
        const timer = setTimeout(release, 20000);
        heldResponses.set(hold, release);
      } else flush();
    },
  );
  upstream.on("error", () => {
    if (!outgoing.headersSent) outgoing.writeHead(502, { "Content-Type": "text/plain" });
    outgoing.end("Local GUI proxy failed");
  });
  incoming.on("aborted", () => upstream.destroy());
  outgoing.on("close", () => {
    if (!outgoing.writableEnded) upstream.destroy();
  });
  incoming.pipe(upstream);
});
server.listen(8080, listenHost, () => {
  const child = spawn(
    process.execPath,
    ["node_modules/@playwright/test/cli.js", "test", ...process.argv.slice(2)],
    {
      stdio: "inherit",
      env: { ...process.env, STACKEMU_BROWSER_URL: `http://${listenHost}:8080` },
    },
  );
  child.on("error", () => {
    process.exitCode = 1;
    server.close();
  });
  child.on("exit", (code) => {
    for (const release of heldResponses.values()) release();
    process.exitCode = code ?? 1;
    server.close();
    server.closeAllConnections();
  });
});
