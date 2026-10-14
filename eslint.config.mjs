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
    files: ["**/*.mjs"],
    languageOptions: { globals: { process: "readonly", Buffer: "readonly", URL: "readonly" } },
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
