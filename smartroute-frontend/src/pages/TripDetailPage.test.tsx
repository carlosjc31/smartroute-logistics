import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { TripDetailPage } from './TripDetailPage'
import { aTrip, jsonResponse, mockFetch, renderRoute } from '../test/utils'

const renderDetail = () => renderRoute('/trips/trip-1', [{ path: '/trips/:id', element: <TripDetailPage /> }])

describe('TripDetailPage', () => {
  // @req TRIP-006
  it('mostra a chegada prevista e o modelo usado', async () => {
    mockFetch(() => jsonResponse(aTrip()))
    renderDetail()

    expect(await screen.findByText('10/01/2030, 15:10')).toBeInTheDocument()
    expect(screen.getByText('Modelo v1.0-random-forest')).toBeInTheDocument()
    expect(screen.queryByText(/estimativa heurística/)).not.toBeInTheDocument()
  })

  // @req TRIP-004
  it('sinaliza quando a previsão veio do fallback', async () => {
    mockFetch(() => jsonResponse(aTrip({ eta: { source: 'FALLBACK', modelVersion: 'fallback-heuristic-v1' } })))
    renderDetail()

    expect(await screen.findByText(/estimativa heurística/)).toBeInTheDocument()
  })

  // @req TRIP-007
  it('oferece só as transições permitidas e envia a escolhida', async () => {
    const calls = mockFetch((_url, init) =>
      init?.method === 'PATCH'
        ? jsonResponse(aTrip({ status: 'IN_TRANSIT', allowedTransitions: ['DELAYED', 'COMPLETED'] }))
        : jsonResponse(aTrip()),
    )
    const user = userEvent.setup()
    renderDetail()

    await user.click(await screen.findByRole('button', { name: 'Marcar como em trânsito' }))

    expect(await screen.findByRole('button', { name: 'Marcar como concluída' })).toBeInTheDocument()
    expect(screen.getByText('Em trânsito')).toBeInTheDocument()
    const patch = calls.find((c) => c.init?.method === 'PATCH')
    expect(patch?.url.pathname).toBe('/api/v1/trips/trip-1/status')
    expect(JSON.parse(String(patch?.init?.body))).toEqual({ status: 'IN_TRANSIT' })
  })

  // @req TRIP-007
  it('não mostra ações para viagens finalizadas', async () => {
    mockFetch(() => jsonResponse(aTrip({ status: 'COMPLETED', allowedTransitions: [] })))
    renderDetail()

    expect(await screen.findByText('Concluída')).toBeInTheDocument()
    expect(screen.queryByRole('button')).not.toBeInTheDocument()
  })

  // @req TRIP-006
  it('mostra erro quando a viagem não existe', async () => {
    mockFetch(() => jsonResponse({ detail: 'Viagem não encontrada' }, 404))
    renderDetail()

    expect(await screen.findByRole('alert')).toHaveTextContent('Viagem não encontrada')
  })
})
