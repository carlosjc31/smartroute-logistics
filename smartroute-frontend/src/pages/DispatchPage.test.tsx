import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { DispatchPage } from './DispatchPage'
import { aTrip, jsonResponse, mockFetch, renderRoute } from '../test/utils'

const PLACES: Record<string, { displayName: string; lat: number; lng: number }> = {
  'São Paulo': { displayName: 'São Paulo, Brasil', lat: -23.5505, lng: -46.6333 },
  Campinas: { displayName: 'Campinas, São Paulo, Brasil', lat: -22.9056, lng: -47.0608 },
}

function renderDispatch() {
  return renderRoute('/', [
    { path: '/', element: <DispatchPage /> },
    { path: '/trips/:id', element: <p>Página da viagem</p> },
  ])
}

async function fillForm(user: ReturnType<typeof userEvent.setup>) {
  await user.type(screen.getByLabelText('Cidade de origem'), ' São Paulo ')
  await user.type(screen.getByLabelText('Cidade de destino'), 'Campinas')
  await user.type(screen.getByLabelText('Carga (toneladas)'), '12.5')
  const departure = screen.getByLabelText('Horário de saída')
  await user.clear(departure)
  await user.type(departure, '2030-01-10T08:00')
}

describe('DispatchPage', () => {
  // @req TRIP-009
  it('localiza as cidades, cria a viagem e abre o detalhe', async () => {
    const calls = mockFetch((url, init) => {
      if (url.pathname === '/api/v1/geocode') {
        const place = PLACES[url.searchParams.get('q') ?? '']
        return place ? jsonResponse(place) : jsonResponse({ detail: 'não encontrado' }, 404)
      }
      if (url.pathname === '/api/v1/trips' && init?.method === 'POST') return jsonResponse(aTrip({ id: 'nova' }), 201)
      throw new Error(`requisição inesperada: ${url}`)
    })
    const user = userEvent.setup()
    const { router } = renderDispatch()

    await fillForm(user)
    await user.click(screen.getByRole('button', { name: 'Calcular rota com IA' }))

    await waitFor(() => expect(router.state.location.pathname).toBe('/trips/nova'))
    expect(screen.getByText('Página da viagem')).toBeInTheDocument()
    const queries = calls.filter((c) => c.url.pathname === '/api/v1/geocode').map((c) => c.url.searchParams.get('q'))
    expect(queries.sort()).toEqual(['Campinas', 'São Paulo'])
    const post = calls.find((c) => c.init?.method === 'POST')
    expect(JSON.parse(String(post?.init?.body))).toEqual({
      originLat: -23.5505,
      originLng: -46.6333,
      destLat: -22.9056,
      destLng: -47.0608,
      originLabel: 'São Paulo',
      destinationLabel: 'Campinas',
      payloadWeight: 12.5,
      departureTime: '2030-01-10T08:00:00-03:00',
    })
  })

  // @req TRIP-009
  it('desabilita o botão enquanto calcula', async () => {
    let release: (r: Response) => void = () => {}
    mockFetch((url) =>
      url.pathname === '/api/v1/geocode'
        ? jsonResponse(PLACES[url.searchParams.get('q') ?? ''] ?? PLACES.Campinas)
        : new Promise<Response>((resolve) => (release = resolve)),
    )
    const user = userEvent.setup()
    renderDispatch()

    await fillForm(user)
    await user.click(screen.getByRole('button', { name: 'Calcular rota com IA' }))

    const button = await screen.findByRole('button', { name: 'Calculando…' })
    expect(button).toBeDisabled()
    release(jsonResponse(aTrip(), 201))
  })

  // @req TRIP-002
  it('mostra a mensagem e os erros por campo devolvidos pela API', async () => {
    mockFetch((url) =>
      url.pathname === '/api/v1/geocode'
        ? jsonResponse(PLACES[url.searchParams.get('q') ?? ''] ?? PLACES.Campinas)
        : jsonResponse({ detail: 'Dados inválidos', errors: { departureTime: 'deve estar no futuro' } }, 400),
    )
    const user = userEvent.setup()
    renderDispatch()

    await fillForm(user)
    await user.click(screen.getByRole('button', { name: 'Calcular rota com IA' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Dados inválidos')
    expect(screen.getByText('deve estar no futuro')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Calcular rota com IA' })).toBeEnabled()
  })

  // @req GEO-002
  it('avisa quando uma cidade não é encontrada', async () => {
    mockFetch(() => jsonResponse({ title: 'Not Found', detail: 'Local não encontrado: Atlântida' }, 404))
    const user = userEvent.setup()
    renderDispatch()

    await fillForm(user)
    await user.click(screen.getByRole('button', { name: 'Calcular rota com IA' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Local não encontrado')
  })
})
