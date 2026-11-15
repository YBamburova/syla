import { defineConfig } from "@playwright/test";

export default defineConfig({
  testDir: "./e2e",
  use: {
    baseURL: "http://localhost:5173",
    actionTimeout: 10000,
    navigationTimeout: 30000,
  },
  projects: [
    {
      name: "mobile",
      use: { viewport: { width: 390, height: 844 }, timezoneId: "Europe/Kyiv" },
    },
    {
      name: "desktop",
      use: {
        viewport: { width: 1440, height: 1000 },
        timezoneId: "America/Los_Angeles",
      },
    },
  ],
  webServer: {
    command: "npm run dev",
    url: "http://localhost:5173",
    reuseExistingServer: !process.env.CI,
    timeout: 30_000,
  },
});
