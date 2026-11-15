import react from "@vitejs/plugin-react";
import { defineConfig } from "vitest/config";

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    strictPort: true,
    proxy: {
      "/oauth2": "http://127.0.0.1:8080",
      "/login": "http://127.0.0.1:8080",
      "/logout": "http://127.0.0.1:8080",
      "/api": "http://127.0.0.1:8080",
      "/v3/api-docs": "http://127.0.0.1:8080",
      "/swagger-ui": "http://127.0.0.1:8080",
    },
  },
  test: {
    environment: "jsdom",
    setupFiles: ["./src/test-setup.ts"],
    include: ["src/**/*.test.tsx"],
    clearMocks: true,
    restoreMocks: true,
  },
});
