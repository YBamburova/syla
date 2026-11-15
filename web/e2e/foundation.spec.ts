import { expect, test, type Page } from "@playwright/test";
import { readFile } from "node:fs/promises";
import { randomUUID } from "node:crypto";

test.beforeEach(({ page }) => {
  page.on("pageerror", (error) =>
    console.error("Browser error:", error.message),
  );
});

async function registerAthlete(page: Page) {
  const username = "athlete-" + randomUUID().slice(0, 12),
    password = "Syla-" + randomUUID() + "!";
  await page.goto("/");
  await page
    .getByRole("link", { name: "Sign in or create an account" })
    .click();
  await page.getByRole("link", { name: "Register", exact: true }).click();
  await page.getByLabel("Username").fill(username);
  await page.getByLabel("Email").fill(username + "@example.invalid");
  await page.getByLabel("First name").fill("Test");
  await page.getByLabel("Last name").fill("Athlete");
  await page.locator("input#password").fill(password);
  await page.locator("input#password-confirm").fill(password);
  await page.getByRole("button", { name: "Register", exact: true }).click();
  await expect(
    page.getByRole("heading", { name: "Your training. Your choice." }),
  ).toBeVisible();
  await page.getByRole("checkbox").check();
  await page.getByRole("button", { name: "Continue to my activities" }).click();
  await expect(
    page.getByRole("heading", { name: "Every session counts." }),
  ).toBeVisible();
}

test("logs all four sports, reloads, edits, exports, and deletes through real authenticated APIs", async ({
  page,
}, testInfo) => {
  test.setTimeout(120_000);
  await registerAthlete(page);
  const scenarios = [
    { sport: "Running", title: "Morning 5K" },
    { sport: "Swimming", title: "Pool technique" },
    { sport: "Cycling", title: "Evening ride" },
    { sport: "Workout", title: "Gym session" },
  ];
  for (const scenario of scenarios) {
    await page.getByRole("link", { name: "Log activity", exact: true }).click();
    await page
      .getByRole("radio", { name: scenario.sport, exact: true })
      .check();
    await expect(
      page.getByRole("radio", { name: scenario.sport, exact: true }),
    ).toBeChecked();
    await page
      .getByRole("textbox", { name: "Activity title (optional)" })
      .fill(scenario.title);
    await expect(
      page.getByRole("radio", { name: scenario.sport, exact: true }),
    ).toBeChecked();
    await page
      .getByRole("spinbutton", { name: "Minutes", exact: true })
      .fill("30");
    await expect(
      page.getByRole("radio", { name: scenario.sport, exact: true }),
    ).toBeChecked();
    if (scenario.sport === "Swimming") {
      await page
        .getByRole("button", { name: "Pool lengths", exact: true })
        .click();
      await page
        .getByRole("spinbutton", { name: "Lengths swum", exact: true })
        .fill("40");
      await expect(page.getByText("3:00 /100 m")).toBeAttached();
    } else if (scenario.sport !== "Workout")
      await page
        .getByRole("spinbutton", { name: "Distance (optional)" })
        .fill(scenario.sport === "Cycling" ? "15" : "5");
    if (scenario.sport === "Running")
      await page.screenshot({
        path: testInfo.outputPath("logging-form.png"),
        fullPage: true,
      });
    const enteredDate = await page
      .getByLabel("Date", { exact: true })
      .inputValue();
    await page
      .getByRole("button", { name: "Save activity", exact: true })
      .click();
    await expect(page).toHaveURL(/\/activities\/[0-9a-f-]{36}$/);
    await expect(
      page.getByRole("heading", { name: scenario.title, exact: true }),
    ).toBeVisible();
    const saved = await page.request.get(
      "/api/v1/workouts/" + page.url().split("/").pop(),
    );
    expect((await saved.json()).activity.trainingDate).toBe(enteredDate);
    await page.reload();
    await expect(
      page.getByRole("heading", { name: scenario.title, exact: true }),
    ).toBeVisible();
    await page.getByRole("link", { name: "Back to activities" }).click();
  }
  await expect(page.getByRole("link", { name: /Morning 5K/ })).toBeVisible();
  await page.screenshot({
    path: testInfo.outputPath("journal.png"),
    fullPage: true,
  });
  await page.getByRole("button", { name: "Swimming", exact: true }).click();
  await expect(
    page.getByRole("link", { name: /Pool technique/ }),
  ).toBeVisible();
  await expect(page.getByRole("link", { name: /Morning 5K/ })).toHaveCount(0);
  await page
    .getByRole("button", { name: "All activities", exact: true })
    .click();
  await expect(page.getByRole("link", { name: /Morning 5K/ })).toBeVisible();
  await page.getByRole("link", { name: /Morning 5K/ }).click();
  await page.getByRole("link", { name: "Edit activity" }).click();
  await page
    .getByRole("textbox", { name: "Activity title (optional)" })
    .fill("Morning run, corrected");
  await page.getByRole("button", { name: "Save changes" }).click();
  await expect(
    page.getByRole("heading", { name: "Morning run, corrected", exact: true }),
  ).toBeVisible();
  await page
    .getByRole("button", { name: "Delete activity", exact: true })
    .click();
  await page
    .getByRole("dialog")
    .getByRole("button", { name: "Delete activity", exact: true })
    .click();
  await expect(
    page.getByRole("heading", { name: "Every session counts." }),
  ).toBeVisible();
  const exportResponse = await page.request.get("/api/v1/account/export");
  expect(exportResponse.ok()).toBe(true);
  expect((await exportResponse.json()).activities).toHaveLength(3);
  await page.getByRole("link", { name: "Privacy & data" }).click();
  const downloadReady = page.waitForEvent("download");
  await page.getByRole("button", { name: "Export my data" }).click();
  const download = await downloadReady;
  const downloadPath = await download.path();
  if (!downloadPath) throw new Error("Export download was not saved");
  const exported = JSON.parse(await readFile(downloadPath, "utf8"));
  expect(exported.activities).toHaveLength(3);
  for (const entry of exported.activities) {
    expect(entry.activity.trainingDate).toMatch(/^\d{4}-\d{2}-\d{2}$/);
  }

  await page.getByRole("button", { name: "Delete my account" }).click();
  await page
    .getByRole("button", { name: "Permanently delete account" })
    .click();
  await expect(
    page.getByRole("heading", { name: "Your account has been deleted." }),
  ).toBeVisible();
  expect((await page.request.get("/api/v1/workouts")).status()).toBe(401);
});
