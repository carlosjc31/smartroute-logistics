import { request } from '../../shared/api/client'
import type { GeocodeResult } from '../../shared/api/types'

/** Converte endereço ou cidade em coordenadas (spec 002). A busca não é registrada em log (LGPD). */
export function geocode(query: string): Promise<GeocodeResult> {
  return request(`/api/v1/geocode?${new URLSearchParams({ q: query })}`)
}
