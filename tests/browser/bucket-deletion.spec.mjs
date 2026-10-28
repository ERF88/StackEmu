import { test, expect } from "@playwright/test";
import {
  createOwned,
  removeOwned,
  canonicalReview,
  stableOperation,
  emulator,
} from "./operation-fixtures.mjs";
const owned = new Map();
test.beforeEach(async ({ request }, info) => {
  owned.set(info.testId, await createOwned(request, []));
});
test.afterEach(async ({ playwright }, info) => {
  await removeOwned(playwright, owned.get(info.testId));
  owned.delete(info.testId);
});
test("empty bucket requires exact name and rejects CSRF-free submission", async ({
  page,
  request,
}, info) => {
  const fixture = owned.get(info.testId);
  const { review, id } = await canonicalReview(
    page,
    request,
    `/s3/delete-bucket?bucket=${fixture.bucket}`,
  );
  expect(
    (
      await request.post(`/operations/${id}/start`, { form: { bucketName: fixture.bucket } })
    ).status(),
  ).toBe(400);
  await review.locator('input[name="bucketName"]').fill("wrong");
  await review.locator('button[type="submit"]').click();
  await expect(page.locator('[aria-invalid="true"]')).toBeVisible();
  expect((await request.get(`${emulator}/${fixture.bucket}`)).status()).toBe(200);
  await page.locator('input[name="bucketName"]').fill(fixture.bucket);
  await page.locator('button[type="submit"]').click();
  expect((await stableOperation(request, id)).state).toBe("SUCCEEDED");
});
test("content added after review is retained", async ({ page, request }, info) => {
  const fixture = owned.get(info.testId),
    key = "late.txt";
  const { review, id } = await canonicalReview(
    page,
    request,
    `/s3/delete-bucket?bucket=${fixture.bucket}`,
  );
  fixture.keys.add(key);
  await request.put(`${emulator}/${fixture.bucket}/${key}`, { data: "late" });
  await review.locator('input[name="bucketName"]').fill(fixture.bucket);
  await review.locator('button[type="submit"]').click();
  expect((await stableOperation(request, id)).state).toBe("FAILED");
  expect(await (await request.get(`${emulator}/${fixture.bucket}/${key}`)).text()).toBe("late");
});
