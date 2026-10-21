import { defineConfig } from "@playwright/test";
import base from "./playwright.config.mjs";

// Host checks only; pinned Linux visual baselines remain untouched until final validation.
export default defineConfig({
  ...base,
  testIgnore: /(?:visual|upload)\.spec\.mjs/,
  use: { ...base.use, channel: "chrome" },
});
