const dateTime = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' })

export function formatDateTime(iso: string | null): string {
  return iso ? dateTime.format(new Date(iso)) : '—'
}

/** Próxima hora cheia no fuso local, no formato de <input type="datetime-local">. */
export function nextFullHourLocal(now = new Date()): string {
  const date = new Date(now)
  date.setHours(date.getHours() + 1, 0, 0, 0)
  return toLocalInputValue(date)
}

function pad(n: number): string {
  return String(Math.abs(n)).padStart(2, '0')
}

function toLocalInputValue(date: Date): string {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`
}

/**
 * Converte o valor de um datetime-local em ISO-8601 com o fuso do navegador
 * (ex.: 2030-01-01T08:00:00-03:00). O backend usa a hora local para prever o trânsito.
 */
export function localInputToOffsetIso(value: string): string {
  const date = new Date(value)
  const offset = -date.getTimezoneOffset()
  const sign = offset >= 0 ? '+' : '-'
  const hours = pad(Math.trunc(offset / 60))
  const minutes = pad(offset % 60)
  return `${toLocalInputValue(date)}:00${sign}${hours}:${minutes}`
}

export function placeLabel(label: string | null, lat: number, lng: number): string {
  return label ?? `${lat.toFixed(4)}, ${lng.toFixed(4)}`
}
