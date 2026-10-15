import js from "@eslint/js";

export default [
  {
    ignores: [
      "node_modules/**",
      "target/**",
      "work/**",
      "outputs/**",
      "src/main/resources/META-INF/resources/vendor/**",
    ],
  },
  js.configs.recommended,
  {
    files: ["src/main/resources/META-INF/resources/app.js"],
    languageOptions: { globals: { document: "readonly", window: "readonly" } },
  },
  {
    files: ["**/*.mjs"],
    languageOptions: {
      globals: {
        process: "readonly",
        Buffer: "readonly",
        URL: "readonly",
        URLSearchParams: "readonly",
      },
    },
  },
  {
    files: ["tests/browser/**/*.mjs"],
    languageOptions: {
      globals: { document: "readonly", window: "readonly", performance: "readonly" },
    },
  },
  {
    files: ["tests/browser/**/*.mjs"],
    languageOptions: {
      globals: { document: "readonly", window: "readonly", performance: "readonly" },
    },
  },
];
