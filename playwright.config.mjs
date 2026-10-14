import { defineConfig } from "@playwright/test";

const baseURL = process.env.STACKEMU_BROWSER_URL || "http://127.0.0.1:8080";
const endpoint = new URL(baseURL);
if (
  !["127.0.0.1", "localhost", "gui", "host.docker.internal"].includes(endpoint.hostname) ||
  endpoint.protocol !== "http:" ||
  endpoint.port !== "8080"
) {
  throw new Error("Browser tests may only target the local StackEmu GUI on port 8080");
}

export default defineConfig({
  testDir: "./tests/browser",
  timeout: 60000,
  expect: { timeout: 10000 },
  workers: 1,
  retries: 0,
  outputDir: "work/browser-results",
  reporter: [["list"], ["json", { outputFile: "work/browser-report.json" }]],
  use: {
    baseURL,
    browserName: "chromium",
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
  },
});
