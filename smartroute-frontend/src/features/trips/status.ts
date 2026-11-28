import type { TripStatus } from '../../shared/api/types'

export const STATUS_LABELS: Record<TripStatus, string> = {
  PENDING: 'Pendente',
  IN_TRANSIT: 'Em trânsito',
  DELAYED: 'Atrasada',
  COMPLETED: 'Concluída',
  CANCELLED: 'Cancelada',
}

// `satisfies` falha no typecheck se o contrato ganhar ou perder um status sem este mapa acompanhar.
const STATUS_ORDER = {
  PENDING: 0,
  IN_TRANSIT: 1,
  DELAYED: 2,
  COMPLETED: 3,
  CANCELLED: 4,
} as const satisfies Record<TripStatus, number>

/** Status na ordem do fluxo da viagem (filtro do painel). */
export const TRIP_STATUSES = (Object.keys(STATUS_ORDER) as TripStatus[]).sort(
  (a, b) => STATUS_ORDER[a] - STATUS_ORDER[b],
)
