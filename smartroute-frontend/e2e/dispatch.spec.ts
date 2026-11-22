import { expect, test } from '@playwright/test'

/**
 * Fluxo principal: abrir o painel → despachar uma viagem → ver o ETA → avançar o status → listar.
 * Backend, banco e ai-engine são reais. Só a geocodificação é simulada no navegador, para o teste
 * não depender do Nominatim (serviço público com limite de 1 req/s).
 */
const PLACES: Record<string, { lat: number; lng: number }> = {
  origem: { lat: -23.5505, lng: -46.6333 },
  destino: { lat: -22.9068, lng: -43.1729 },
}

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
  await expect(page.getByRole('heading', { name: 'Novo despacho' })).toBeVisible()

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
})
