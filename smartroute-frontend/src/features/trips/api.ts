import { request } from '../../shared/api/client'
import type { CreateTripRequest, Trip, TripPage, TripStatus } from '../../shared/api/types'

export interface TripFilters {
  status?: TripStatus
  page?: number
  size?: number
}

export const tripKeys = {
  all: ['trips'] as const,
  list: (filters: TripFilters) => ['trips', 'list', filters] as const,
  detail: (id: string) => ['trips', 'detail', id] as const,
}

export function listTrips({ status, page = 0, size = 20 }: TripFilters): Promise<TripPage> {
  const params = new URLSearchParams({ page: String(page), size: String(size) })
  if (status) params.set('status', status)
  return request(`/api/v1/trips?${params}`)
}

export function getTrip(id: string): Promise<Trip> {
  return request(`/api/v1/trips/${encodeURIComponent(id)}`)
}

export function createTrip(body: CreateTripRequest): Promise<Trip> {
  return request('/api/v1/trips', { method: 'POST', body: JSON.stringify(body) })
}

export function updateTripStatus(id: string, status: TripStatus): Promise<Trip> {
  return request(`/api/v1/trips/${encodeURIComponent(id)}/status`, {
    method: 'PATCH',
    body: JSON.stringify({ status }),
  })
}
