import { Link, useSearchParams } from 'react-router'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { listTrips, tripKeys } from '../api/trips'
import { TRIP_STATUSES, type TripStatus } from '../api/types'
import { StatusBadge } from '../components/StatusBadge'
import { formatDateTime, placeLabel, STATUS_LABELS } from '../lib/format'

const PAGE_SIZE = 20

export function TripsPage() {
  const [params, setParams] = useSearchParams()
  const status = (params.get('status') as TripStatus | null) ?? undefined
  const page = Number(params.get('page') ?? 0)
  const filters = { status, page, size: PAGE_SIZE }

  const { data, error, isPending, isFetching } = useQuery({
    queryKey: tripKeys.list(filters),
    queryFn: () => listTrips(filters),
    placeholderData: keepPreviousData,
  })

  const setFilter = (next: { status?: string; page?: number }) => {
    const updated = new URLSearchParams(params)
    if (next.status !== undefined) {
      if (next.status) updated.set('status', next.status)
      else updated.delete('status')
      updated.delete('page')
    }
    if (next.page !== undefined) updated.set('page', String(next.page))
    setParams(updated)
  }

  return (
    <section className="card">
      <div className="toolbar">
        <h1>Viagens</h1>
        <select value={status ?? ''} onChange={(e) => setFilter({ status: e.target.value })} aria-label="Filtrar por status">
          <option value="">Todos os status</option>
          {TRIP_STATUSES.map((s) => (
            <option key={s} value={s}>{STATUS_LABELS[s]}</option>
          ))}
        </select>
      </div>

      {isPending && <p className="muted">Carregando…</p>}
      {error && <p className="alert error" role="alert">{error.message}</p>}

      {data && data.items.length === 0 && <p className="muted">Nenhuma viagem encontrada.</p>}

      {data && data.items.length > 0 && (
        <table className={isFetching ? 'table loading' : 'table'}>
          <thead>
            <tr>
              <th>Rota</th>
              <th>Status</th>
              <th>Saída</th>
              <th>Chegada prevista</th>
              <th>Carga</th>
            </tr>
          </thead>
          <tbody>
            {data.items.map((trip) => (
              <tr key={trip.id}>
                <td>
                  <Link to={`/trips/${trip.id}`}>
                    {placeLabel(trip.origin.label, trip.origin.lat, trip.origin.lng)} →{' '}
                    {placeLabel(trip.destination.label, trip.destination.lat, trip.destination.lng)}
                  </Link>
                </td>
                <td><StatusBadge status={trip.status} /></td>
                <td>{formatDateTime(trip.departureTime)}</td>
                <td>{formatDateTime(trip.predictedArrival)}</td>
                <td>{trip.payloadWeight} t</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {data && data.totalPages > 1 && (
        <div className="pagination">
          <button className="button" disabled={page === 0} onClick={() => setFilter({ page: page - 1 })}>Anterior</button>
          <span>Página {page + 1} de {data.totalPages}</span>
          <button className="button" disabled={page + 1 >= data.totalPages} onClick={() => setFilter({ page: page + 1 })}>Próxima</button>
        </div>
      )}
    </section>
  )
}
