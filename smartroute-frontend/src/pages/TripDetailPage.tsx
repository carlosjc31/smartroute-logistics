import { Link, useParams } from 'react-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { getTrip, tripKeys, updateTripStatus } from '../api/trips'
import type { TripStatus } from '../api/types'
import { StatusBadge } from '../components/StatusBadge'
import { formatDateTime, placeLabel, STATUS_LABELS } from '../lib/format'

export function TripDetailPage() {
  const { id = '' } = useParams()
  const queryClient = useQueryClient()
  const { data: trip, error, isPending } = useQuery({ queryKey: tripKeys.detail(id), queryFn: () => getTrip(id) })

  const changeStatus = useMutation({
    mutationFn: (status: TripStatus) => updateTripStatus(id, status),
    onSuccess: (updated) => {
      queryClient.setQueryData(tripKeys.detail(id), updated)
      queryClient.invalidateQueries({ queryKey: ['trips', 'list'] })
    },
  })

  if (isPending) return <p className="muted">Carregando…</p>
  if (error) return <p className="alert error" role="alert">{error.message}</p>

  return (
    <section className="card narrow">
      <Link to="/trips" className="back">← Viagens</Link>
      <div className="toolbar">
        <h1>
          {placeLabel(trip.origin.label, trip.origin.lat, trip.origin.lng)} →{' '}
          {placeLabel(trip.destination.label, trip.destination.lat, trip.destination.lng)}
        </h1>
        <StatusBadge status={trip.status} />
      </div>

      <div className="eta">
        <span className="muted">Chegada prevista</span>
        <strong>{formatDateTime(trip.predictedArrival)}</strong>
        {trip.eta?.source === 'FALLBACK' && (
          <small className="alert warning">
            O serviço de IA estava indisponível: esta é uma estimativa heurística.
          </small>
        )}
        {trip.eta?.source === 'MODEL' && <small className="muted">Modelo {trip.eta.modelVersion}</small>}
      </div>

      <dl className="details">
        <dt>Saída</dt><dd>{formatDateTime(trip.departureTime)}</dd>
        <dt>Chegada real</dt><dd>{formatDateTime(trip.actualArrival)}</dd>
        <dt>Distância (linha reta)</dt><dd>{trip.distanceKm} km</dd>
        <dt>Carga</dt><dd>{trip.payloadWeight} t</dd>
        <dt>Criada em</dt><dd>{formatDateTime(trip.createdAt)}</dd>
      </dl>

      {trip.allowedTransitions.length > 0 && (
        <div className="actions">
          {trip.allowedTransitions.map((status) => (
            <button
              key={status}
              className={status === 'CANCELLED' ? 'button danger' : 'button primary'}
              disabled={changeStatus.isPending}
              onClick={() => changeStatus.mutate(status)}
            >
              Marcar como {STATUS_LABELS[status].toLowerCase()}
            </button>
          ))}
        </div>
      )}
      {changeStatus.error && <p className="alert error" role="alert">{changeStatus.error.message}</p>}
    </section>
  )
}
