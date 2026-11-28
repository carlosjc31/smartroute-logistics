import { expect, test, type Page } from '@playwright/test'

/**
 * Fluxo principal: login no Keycloak → despachar uma viagem → ver o ETA → avançar o status → listar.
 * Keycloak, backend, banco e ai-engine são reais. Só a geocodificação é simulada no navegador, para o
 * teste não depender do Nominatim (serviço público com limite de 1 req/s).
 */
const USERNAME = process.env.E2E_USERNAME ?? 'despacho.dev'
const PASSWORD = process.env.E2E_PASSWORD ?? process.env.KEYCLOAK_DEV_USERS_PASSWORD

async function login(page: Page) {
  if (!PASSWORD) throw new Error('Defina E2E_PASSWORD (senha dos usuários de desenvolvimento do Keycloak)')
  await page.waitForURL(/\/realms\/smartroute\/protocol\/openid-connect\/auth/)
  await page.locator('#username').fill(USERNAME)
  await page.locator('#password').fill(PASSWORD)
  await page.locator('#kc-login').click()
}
const PLACES: Record<string, { lat: number; lng: number }> = {
  origem: { lat: -23.5505, lng: -46.6333 },
  destino: { lat: -22.9068, lng: -43.1729 },
}

// @req TRIP-009, TRIP-007, TRIP-005
test('despacha uma viagem, acompanha o status e encontra na lista', async ({ page }) => {
  const runId = Date.now().toString(36)
  const origin = `Origem E2E ${runId}`
  const destination = `Destino E2E ${runId}`

  await page.route('**/api/v1/geocode?*', async (route) => {
    const q = new URL(route.request().url()).searchParams.get('q') ?? ''
    const place = q.startsWith('Origem') ? PLACES.origem : PLACES.destino
    await route.fulfill({ json: { displayName: q, ...place } })
  })

  await page.goto('/')
  await login(page)
  await expect(page.getByRole('heading', { name: 'Novo despacho' })).toBeVisible()
  await expect(page.getByText(USERNAME)).toBeVisible()

  await page.getByLabel('Cidade de origem').fill(origin)
  await page.getByLabel('Cidade de destino').fill(destination)
  await page.getByLabel('Carga (toneladas)').fill('12')
  await page.getByRole('button', { name: 'Calcular rota com IA' }).click()

  await expect(page).toHaveURL(/\/trips\/[0-9a-f-]{36}$/)
  await expect(page.getByRole('heading', { name: `${origin} → ${destination}` })).toBeVisible()
  await expect(page.getByText('Chegada prevista')).toBeVisible()
  await expect(page.locator('.eta strong')).toHaveText(/\d{2}\/\d{2}\/\d{4}, \d{2}:\d{2}/)
  const modelInfo = page.getByText(/^Modelo v/)
  const fallbackInfo = page.getByText(/estimativa heurística/)
  const expected = process.env.E2E_EXPECT_ETA_SOURCE
  if (expected === 'MODEL') await expect(modelInfo).toBeVisible()
  else if (expected === 'FALLBACK') await expect(fallbackInfo).toBeVisible()
  else await expect(modelInfo.or(fallbackInfo)).toBeVisible()
  await expect(page.locator('.badge')).toHaveText('Pendente')

  await page.getByRole('button', { name: 'Marcar como em trânsito' }).click()
  await expect(page.locator('.badge')).toHaveText('Em trânsito')
  await page.getByRole('button', { name: 'Marcar como concluída' }).click()
  await expect(page.locator('.badge')).toHaveText('Concluída')
  await expect(page.getByRole('button', { name: /^Marcar como/ })).toHaveCount(0)

  await page.getByRole('navigation').getByRole('link', { name: 'Viagens' }).click()
  await page.getByLabel('Filtrar por status').selectOption('COMPLETED')
  const row = page.getByRole('row', { name: new RegExp(`${origin} → ${destination}`) })
  await expect(row).toBeVisible()
  await expect(row).toContainText('Concluída')

  await page.getByRole('button', { name: 'Sair' }).click()
  await page.waitForURL(/\/realms\/smartroute\//)
})
