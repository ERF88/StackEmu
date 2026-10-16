import { defineConfig } from "@playwright/test";
import base from "./playwright.config.mjs";

// Host Chrome only for preview slices, not the Linux upload-proxy or snapshot suites.
export default defineConfig({
  ...base,
  testMatch: /(?:image-preview|preview)\.spec\.mjs/,
  use: { ...base.use, channel: "chrome" },
});
