import { test, expect } from "@playwright/test";
import { randomUUID, createHash } from "node:crypto";

const emulatorURL = process.env.STACKEMU_BROWSER_EMULATOR || "http://127.0.0.1:4566";
const endpoint = new URL(emulatorURL);
if (
  endpoint.protocol !== "http:" ||
  endpoint.port !== "4566" ||
  !["localhost", "127.0.0.1", "ministack", "host.docker.internal"].includes(endpoint.hostname)
) {
  throw new Error("Upload spike targets local emulator only");
}

test("real htmx multipart transport distinguishes upload and response progress", async ({
  page,
  request,
}, testInfo) => {
  const bucket = `stackemu-transport-${randomUUID().replaceAll("-", "")}`;
  const filename = "ação +% transport.txt";
  const key = `spike/${filename}`;
  const bytes = Buffer.alloc(1024 * 1024, "x");
  const hash = (value) => createHash("sha256").update(value).digest("hex");
  let created = false;
  try {
    expect((await request.put(`${emulatorURL}/${bucket}`)).status()).toBe(200);
    created = true;
    await page.goto(`/s3/write?action=upload&bucket=${bucket}&prefix=spike%2F&lang=en`);
    await page
      .locator("#mutation-file")
      .setInputFiles({ name: filename, mimeType: "text/plain", buffer: bytes });
    await page.getByRole("checkbox").check();
    const evidence = await page.evaluate(async () => {
      const form = document.querySelector(".mutation-form");
      form.setAttribute("hx-encoding", "multipart/form-data");
      const evidence = {
        upload: [],
        response: [],
        forwardedUpload: [],
        forwardedResponse: [],
        status: null,
      };
      let pendingUpload = null;
      const sample = (event) => ({
        loaded: event.loaded,
        total: event.total,
        computable: event.lengthComputable,
      });
      document.addEventListener("htmx:beforeRequest", (event) => {
        if (event.detail.elt !== form) return;
        event.detail.xhr.upload.addEventListener("progress", (native) => {
          pendingUpload = sample(native);
          evidence.upload.push(pendingUpload);
        });
        event.detail.xhr.addEventListener("progress", (native) =>
          evidence.response.push(sample(native)),
        );
      });
      form.addEventListener("htmx:xhr:progress", (event) => {
        const progress = sample(event.detail);
        if (pendingUpload) {
          evidence.forwardedUpload.push({
            progress,
            matches: JSON.stringify(progress) === JSON.stringify(pendingUpload),
          });
          pendingUpload = null;
        } else evidence.forwardedResponse.push(progress);
      });
      document.addEventListener("htmx:afterRequest", (event) => {
        if (event.detail.elt === form) evidence.status = event.detail.xhr.status;
      });
      await window.htmx.ajax("POST", form.action, { source: form, target: form, swap: "none" });
      return evidence;
    });
    expect(evidence.status).toBe(200);
    expect(evidence.upload.length).toBeGreaterThan(0);
    expect(evidence.response.length).toBeGreaterThan(0);
    expect(evidence.forwardedUpload.length).toBe(evidence.upload.length);
    expect(evidence.forwardedUpload.every((sample) => sample.matches)).toBe(true);
    expect(evidence.forwardedResponse.length).toBe(evidence.response.length);
    expect(evidence.upload.at(-1).loaded).toBeGreaterThan(bytes.length);
    await testInfo.attach("real-transport-events", {
      body: JSON.stringify(evidence, null, 2),
      contentType: "application/json",
    });
    const response = await request.get(
      `/s3/download?bucket=${bucket}&key=${encodeURIComponent(key)}`,
    );
    expect(response.status()).toBe(200);
    expect(hash(await response.body())).toBe(hash(bytes));
    expect(response.headers()["content-type"]).toContain("text/plain");
  } finally {
    if (created) {
      expect(
        (await request.delete(`${emulatorURL}/${bucket}/${encodeURIComponent(key)}`)).status(),
      ).toBe(204);
      expect((await request.delete(`${emulatorURL}/${bucket}`)).status()).toBe(204);
    }
  }
});
