import { expect, test, type Page } from "@playwright/test";
import { readFile } from "node:fs/promises";
import { randomUUID } from "node:crypto";

test.beforeEach(({ page }) => {
  page.on("pageerror", (error) =>
    console.error("Browser error:", error.message),
  );
});

async function registerAthlete(
  page: Page,
  firstName = "Test",
  lastName = "Athlete",
) {
  const username = "athlete-" + randomUUID().slice(0, 12),
    password = "Syla-" + randomUUID() + "!";
  await page.goto("/");
  await page
    .getByRole("link", { name: "Sign in or create an account" })
    .click();
  await page.getByRole("link", { name: "Register", exact: true }).click();
  await page.getByLabel("Username").fill(username);
  await page.getByLabel("Email").fill(username + "@example.invalid");
  await page.getByLabel("First name").fill(firstName);
  await page.getByLabel("Last name").fill(lastName);
  await page.locator("input#password").fill(password);
  await page.locator("input#password-confirm").fill(password);
  await page.getByRole("button", { name: "Register", exact: true }).click();
  await expect(
    page.getByRole("heading", { name: "Your training. Your choice." }),
  ).toBeVisible();
  await page.getByRole("checkbox").check();
  await page.getByRole("button", { name: "Continue to my activities" }).click();
  await expect(
    page.getByRole("heading", { name: "Activities", exact: true }),
  ).toBeVisible();
  return username;
}

test("logs all four sports, reloads, edits, exports, and deletes through real authenticated APIs", async ({
  page,
}, testInfo) => {
  test.setTimeout(120_000);
  await registerAthlete(page);
  const checkLayout = async () => {
    expect(
      await page.evaluate(
        () => document.documentElement.scrollWidth <= window.innerWidth,
      ),
    ).toBe(true);
  };
  await checkLayout();
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
    await checkLayout();
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
  await page.getByRole("button", { name: "All", exact: true }).click();
  await expect(page.getByRole("link", { name: /Morning 5K/ })).toBeVisible();
  await checkLayout();
  await page.getByRole("link", { name: /Morning 5K/ }).click();
  await page.screenshot({
    path: testInfo.outputPath("activity-detail.png"),
    fullPage: true,
  });
  await checkLayout();
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
    page.getByRole("heading", { name: "Activities", exact: true }),
  ).toBeVisible();
  const exportResponse = await page.request.get("/api/v1/account/export");
  expect(exportResponse.ok()).toBe(true);
  expect((await exportResponse.json()).activities).toHaveLength(3);
  await page.getByRole("link", { name: "Privacy & data" }).click();
  await page.screenshot({
    path: testInfo.outputPath("privacy.png"),
    fullPage: true,
  });
  await checkLayout();
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

test("private follow approval, explicit workout sharing and blocking work between real athletes", async ({
  page,
  browser,
}, testInfo) => {
  test.setTimeout(150_000);
  const follower = await browser.newPage({
    baseURL: "http://localhost:5173",
    viewport: page.viewportSize() ?? undefined,
    timezoneId:
      testInfo.project.name === "mobile"
        ? "Europe/Kyiv"
        : "America/Los_Angeles",
  });
  const mutate = async (target: Page, path: string) => {
    const csrf = await (
      await target.request.get("/api/v1/session/csrf")
    ).json();
    return target.request.delete(path, {
      headers: { [csrf.headerName]: csrf.token },
    });
  };
  try {
    const ownerUsername = await registerAthlete(page, "Social", "Owner");
    await registerAthlete(follower, "Social", "Follower");
    const ownerProfile = await (
      await page.request.get("/api/v1/profile")
    ).json();
    const ownerHandle = ownerProfile.profile.handle;
    // Neither athlete visits profile setup. Registration details are ready to use.
    await page.getByRole("link", { name: "Activities", exact: true }).click();
    await page.getByRole("link", { name: "Log activity", exact: true }).click();
    await page
      .getByRole("textbox", { name: "Activity title (optional)" })
      .fill("Shared morning run");
    await page
      .getByRole("spinbutton", { name: "Minutes", exact: true })
      .fill("30");
    await page
      .getByRole("spinbutton", { name: "Distance (optional)" })
      .fill("5");
    await page
      .getByRole("button", { name: "Save activity", exact: true })
      .click();
    await expect(
      page.getByRole("heading", { name: "Shared morning run", exact: true }),
    ).toBeVisible();
    const activityId = page.url().split("/").pop();
    page.once("dialog", (dialog) => dialog.accept());
    await page
      .getByRole("button", { name: "Share with followers", exact: true })
      .click();
    await expect(
      page.getByRole("button", { name: "Make private", exact: true }),
    ).toBeVisible();
    await follower.getByRole("link", { name: "People", exact: true }).click();
    await follower
      .getByRole("textbox", { name: "Find athletes" })
      .fill(ownerUsername);
    await follower
      .getByRole("button", { name: "Search", exact: true })
      .last()
      .click();
    await follower
      .getByRole("link", { name: "Social Owner", exact: true })
      .click();
    await page.getByRole("link", { name: "People", exact: true }).click();
    await page.getByRole("button", { name: "Requests", exact: true }).click();
    await expect(page.getByText("No incoming follow requests.")).toBeVisible();
    await follower
      .getByRole("button", { name: "Request to follow", exact: true })
      .click();
    await expect(
      follower.getByText("Your follow request is waiting for approval."),
    ).toBeVisible();
    expect(
      (
        await follower.request.get(`/api/v1/shared-activities/${activityId}`)
      ).status(),
    ).toBe(404);
    await page.bringToFront();
    await expect(
      page.getByRole("button", { name: "Accept", exact: true }),
    ).toBeVisible({ timeout: 15000 });
    await page.getByRole("button", { name: "Accept", exact: true }).click();
    await expect(page.getByText("No incoming follow requests.")).toBeVisible();
    await follower.reload();
    await follower
      .getByRole("link", { name: "Shared morning run", exact: true })
      .click();
    await expect(
      follower.getByRole("heading", {
        name: "Shared morning run",
        exact: true,
      }),
    ).toBeVisible();
    await expect(follower.getByText("5 km", { exact: true })).toBeVisible();
    await follower.screenshot({
      path: testInfo.outputPath("shared-activity.png"),
      fullPage: true,
    });
    expect(
      await follower.evaluate(
        () => document.documentElement.scrollWidth <= innerWidth,
      ),
    ).toBe(true);
    await page.getByRole("button", { name: "Followers", exact: true }).click();
    await page
      .getByRole("link", { name: "Social Follower", exact: true })
      .click();
    page.once("dialog", (dialog) => dialog.accept());
    await page.getByRole("button", { name: "Block", exact: true }).click();
    await expect(page).toHaveURL(/\/athletes$/);
    await follower.reload();
    await expect(follower.getByText(/Activity unavailable/)).toBeVisible();
    expect(
      (
        await follower.request.get(`/api/v1/athletes/by-handle/${ownerHandle}`)
      ).status(),
    ).toBe(404);
    await page.getByRole("button", { name: "Blocked", exact: true }).click();
    await page.getByRole("button", { name: "Unblock", exact: true }).click();
    await expect(page.getByText("No athletes here yet.")).toBeVisible();
    expect(
      (
        await follower.request.get(`/api/v1/shared-activities/${activityId}`)
      ).status(),
    ).toBe(404);
  } finally {
    expect((await mutate(page, "/api/v1/account")).status()).toBe(204);
    expect((await mutate(follower, "/api/v1/account")).status()).toBe(204);
    await follower.close();
  }
});
