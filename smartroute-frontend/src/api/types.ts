export const TRIP_STATUSES = ['PENDING', 'IN_TRANSIT', 'DELAYED', 'COMPLETED', 'CANCELLED'] as const
export type TripStatus = (typeof TRIP_STATUSES)[number]

export interface Location {
  label: string | null
  lat: number
  lng: number
}

export interface Trip {
  id: string
  origin: Location
  destination: Location
  payloadWeight: number
  distanceKm: number
  status: TripStatus
  allowedTransitions: TripStatus[]
  departureTime: string
  predictedArrival: string | null
  eta: { source: 'MODEL' | 'FALLBACK'; modelVersion: string | null } | null
  actualArrival: string | null
  createdAt: string
}

export interface Page<T> {
  items: T[]
  page: number
  size: number
  totalItems: number
  totalPages: number
}

export interface CreateTripRequest {
  originLat: number
  originLng: number
  destLat: number
  destLng: number
  originLabel?: string
  destinationLabel?: string
  payloadWeight: number
  departureTime: string
}

export interface GeocodeResult {
  displayName: string
  lat: number
  lng: number
}

/** Corpo de erro RFC 7807 devolvido pelo backend. */
export interface Problem {
  title?: string
  detail?: string
  status?: number
  errors?: Record<string, string>
}
