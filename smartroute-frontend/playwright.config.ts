import { defineConfig, devices } from '@playwright/test'

/**
 * E2E do painel contra um backend real (docker compose no CI, ou ./mvnw spring-boot:test-run localmente).
 * O frontend é servido pelo `vite preview`, que repassa /api para E2E_API_URL.
 *
 *   E2E_API_URL            backend-core (padrão http://localhost:8080)
 *   E2E_EXPECT_ETA_SOURCE  MODEL ou FALLBACK para exigir a origem do ETA (padrão: aceita as duas)
 *   PW_CHANNEL             navegador instalado a usar, ex.: chrome (padrão: Chromium do Playwright)
 */
const PORT = 4173
const apiUrl = process.env.E2E_API_URL ?? 'http://localhost:8080'

export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  reporter: process.env.CI ? [['github'], ['html', { open: 'never' }]] : 'list',
  use: {
    baseURL: `http://localhost:${PORT}`,
    trace: 'retain-on-failure',
    locale: 'pt-BR',
    timezoneId: 'America/Sao_Paulo',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'], channel: process.env.PW_CHANNEL } }],
  webServer: {
    command: `npm run build && npx vite preview --port ${PORT} --strictPort`,
    url: `http://localhost:${PORT}`,
    reuseExistingServer: !process.env.CI,
    env: { VITE_PROXY_TARGET: apiUrl },
    timeout: 120_000,
  },
})
