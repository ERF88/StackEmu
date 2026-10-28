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
  owned.set(
    info.testId,
    await createOwned(request, ["folder/", "folder/child.txt", "folder-other/keep.txt"]),
  );
});
test.afterEach(async ({ playwright }, info) => {
  await removeOwned(playwright, owned.get(info.testId));
  owned.delete(info.testId);
});
test("folder removal includes reviewed contents and preserves similar prefix", async ({
  page,
  request,
}, info) => {
  const fixture = owned.get(info.testId);
  const { review, id } = await canonicalReview(
    page,
    request,
    `/s3/delete-object?bucket=${fixture.bucket}&key=folder%2F`,
  );
  await expect(
    review.locator('input[name="bucketName"],input[type="checkbox"],details'),
  ).toHaveCount(0);
  await review.locator('button[type="submit"]').click();
  expect((await stableOperation(request, id)).state).toBe("SUCCEEDED");
  expect((await request.get(`${emulator}/${fixture.bucket}/folder%2Fchild.txt`)).status()).toBe(
    404,
  );
  expect(
    (await request.get(`${emulator}/${fixture.bucket}/folder-other%2Fkeep.txt`)).status(),
  ).toBe(200);
});
test("late folder object is not silently adopted", async ({ page, request }, info) => {
  const fixture = owned.get(info.testId);
  const { review, id } = await canonicalReview(
    page,
    request,
    `/s3/delete-object?bucket=${fixture.bucket}&key=folder%2F`,
  );
  fixture.keys.add("folder/late.txt");
  await request.put(`${emulator}/${fixture.bucket}/folder%2Flate.txt`, { data: "late" });
  await review.locator('button[type="submit"]').click();
  expect((await stableOperation(request, id)).state).toBe("PARTIAL");
  expect(await (await request.get(`${emulator}/${fixture.bucket}/folder%2Flate.txt`)).text()).toBe(
    "late",
  );
});
