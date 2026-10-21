import { defineConfig } from "@playwright/test";
import base from "./playwright.config.mjs";

// Host diagnostics only; never update or compare the pinned Linux baselines here.
export default defineConfig({
  ...base,
  testMatch: /(?:visual-depth|dashboard|density|motion|drawer|notifications)\.spec\.mjs/,
  use: { ...base.use, channel: "chrome" },
});
