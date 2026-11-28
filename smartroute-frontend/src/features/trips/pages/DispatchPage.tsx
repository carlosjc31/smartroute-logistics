import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { ApiError } from '../../../shared/api/client'
import { localInputToOffsetIso, nextFullHourLocal } from '../../../shared/lib/format'
import { geocode } from '../../geocoding'
import { createTrip, tripKeys } from '../api'

interface DispatchForm {
  origin: string
  destination: string
  weight: string
  departure: string
}

export function DispatchPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [form, setForm] = useState<DispatchForm>(() => ({
    origin: '',
    destination: '',
    weight: '',
    departure: nextFullHourLocal(),
  }))

  const dispatch = useMutation({
    mutationFn: async (values: DispatchForm) => {
      const originName = values.origin.trim()
      const destinationName = values.destination.trim()
      const [origin, destination] = await Promise.all([geocode(originName), geocode(destinationName)])
      return createTrip({
        originLat: origin.lat,
        originLng: origin.lng,
        destLat: destination.lat,
        destLng: destination.lng,
        originLabel: originName,
        destinationLabel: destinationName,
        payloadWeight: Number.parseFloat(values.weight),
        departureTime: localInputToOffsetIso(values.departure),
      })
    },
    onSuccess: (trip) => {
      queryClient.invalidateQueries({ queryKey: tripKeys.all })
      navigate(`/trips/${trip.id}`)
    },
  })

  const update = (field: keyof DispatchForm) => (e: { target: { value: string } }) =>
    setForm((current) => ({ ...current, [field]: e.target.value }))

  const onSubmit = (e: FormEvent) => {
    e.preventDefault()
    dispatch.mutate(form)
  }

  const error = dispatch.error
  const fieldErrors = error instanceof ApiError ? error.fieldErrors : {}

  return (
    <section className="card narrow">
      <h1>Novo despacho</h1>
      <p className="muted">Informe as cidades e a carga. A chegada é prevista pelo modelo de IA.</p>

      <form onSubmit={onSubmit} className="form">
        <label>
          Cidade de origem
          <input value={form.origin} onChange={update('origin')} required maxLength={200} placeholder="Ex.: São Paulo" />
        </label>
        <label>
          Cidade de destino
          <input value={form.destination} onChange={update('destination')} required maxLength={200} placeholder="Ex.: Rio de Janeiro" />
        </label>
        <div className="row">
          <label>
            Carga (toneladas)
            <input type="number" min="0.1" step="0.1" value={form.weight} onChange={update('weight')} required />
            {fieldErrors.payloadWeight && <small className="field-error">{fieldErrors.payloadWeight}</small>}
          </label>
          <label>
            Horário de saída
            <input type="datetime-local" value={form.departure} onChange={update('departure')} required />
            {fieldErrors.departureTime && <small className="field-error">{fieldErrors.departureTime}</small>}
          </label>
        </div>

        <button type="submit" className="button primary" disabled={dispatch.isPending}>
          {dispatch.isPending ? 'Calculando…' : 'Calcular rota com IA'}
        </button>

        {error && <p className="alert error" role="alert">{error.message}</p>}
      </form>
    </section>
  )
}
