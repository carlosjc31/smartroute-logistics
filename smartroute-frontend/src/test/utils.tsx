import type { ReactNode } from 'react'
import { render } from '@testing-library/react'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { vi } from 'vitest'
import type { Trip } from '../shared/api/types'

/** Renderiza uma rota com QueryClient e router em memória, como no app real. */
export function renderRoute(path: string, routes: { path: string; element: ReactNode }[]) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  const router = createMemoryRouter(routes, { initialEntries: [path] })
  render(
    <QueryClientProvider client={queryClient}>
      <RouterProvider router={router} />
    </QueryClientProvider>,
  )
  return { router }
}

export function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': status >= 400 ? 'application/problem+json' : 'application/json' },
  })
}

type Handler = (url: URL, init?: RequestInit) => Response | Promise<Response>

/** Substitui o fetch global; cada chamada é registrada para inspeção. */
export function mockFetch(handler: Handler) {
  const calls: { url: URL; init?: RequestInit }[] = []
  const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = new URL(String(input), 'http://localhost')
    calls.push({ url, init })
    return handler(url, init)
  })
  vi.stubGlobal('fetch', fetchMock)
  return calls
}

export function aTrip(overrides: Partial<Trip> = {}): Trip {
  return {
    id: 'trip-1',
    origin: { label: 'São Paulo', lat: -23.5505, lng: -46.6333 },
    destination: { label: 'Rio de Janeiro', lat: -22.9068, lng: -43.1729 },
    payloadWeight: 12,
    distanceKm: 360.9,
    status: 'PENDING',
    allowedTransitions: ['IN_TRANSIT', 'CANCELLED'],
    departureTime: '2030-01-10T08:00:00-03:00',
    predictedArrival: '2030-01-10T15:10:00-03:00',
    eta: { source: 'MODEL', modelVersion: 'v1.0-random-forest' },
    actualArrival: null,
    createdAt: '2030-01-09T10:00:00Z',
    ...overrides,
  }
}
