import { expect } from "@playwright/test";
import { randomUUID } from "node:crypto";

export const emulator = "http://127.0.0.1:4566";
export async function createOwned(request, keys = ["folder/ação +%.txt"]) {
  const bucket = `stackemu-modern-${randomUUID().replaceAll("-", "")}`;
  const neighbor = `stackemu-guard-${randomUUID().replaceAll("-", "")}`;
  for (const name of [bucket, neighbor])
    expect((await request.put(`${emulator}/${name}`)).status()).toBe(200);
  for (const key of keys)
    expect(
      (
        await request.put(`${emulator}/${bucket}/${encodeURIComponent(key)}`, {
          data: key.endsWith("/") ? "" : "original bytes",
          headers: { "Content-Type": "text/plain" },
        })
      ).status(),
    ).toBe(200);
  expect((await request.put(`${emulator}/${neighbor}/guard.txt`, { data: "guard" })).status()).toBe(
    200,
  );
  return { bucket, neighbor, keys: new Set(keys) };
}
export async function removeOwned(playwright, fixture) {
  if (!fixture) return;
  const request = await playwright.request.newContext();
  try {
    for (const key of fixture.keys)
      expect([204, 404]).toContain(
        (await request.delete(`${emulator}/${fixture.bucket}/${encodeURIComponent(key)}`)).status(),
      );
    expect([204, 404]).toContain((await request.delete(`${emulator}/${fixture.bucket}`)).status());
    expect((await request.delete(`${emulator}/${fixture.neighbor}/guard.txt`)).status()).toBe(204);
    expect((await request.delete(`${emulator}/${fixture.neighbor}`)).status()).toBe(204);
  } finally {
    await request.dispose();
  }
}
export async function stableOperation(request, id) {
  let state;
  await expect
    .poll(
      async () => {
        const response = await request.get(`/operations/${id}/state`);
        expect(response.status()).toBe(200);
        state = await response.json();
        return state.active;
      },
      { timeout: 20000 },
    )
    .toBe(false);
  return state;
}
export async function canonicalReview(page, request, href, query = "lang=en&theme=light") {
  await page.goto(`${href}&${query}`);
  const id = await page.locator("#action-workspace").getAttribute("data-operation-id");
  await stableOperation(request, id);
  await page.goto(`/operations/${id}/review?${query}`);
  return { review: page.locator("#action-workspace"), id };
}
export async function readyDialog(page) {
  const dialog = page.locator("#action-dialog");
  await expect(dialog.locator('form[action$="/start"]')).toBeVisible({ timeout: 15000 });
  return {
    dialog,
    id: await dialog.locator("#action-workspace").getAttribute("data-operation-id"),
  };
}
