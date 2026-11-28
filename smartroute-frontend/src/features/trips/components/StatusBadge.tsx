import type { TripStatus } from '../../../shared/api/types'
import { STATUS_LABELS } from '../status'

export function StatusBadge({ status }: { status: TripStatus }) {
  return <span className={`badge badge-${status.toLowerCase()}`}>{STATUS_LABELS[status]}</span>
}
