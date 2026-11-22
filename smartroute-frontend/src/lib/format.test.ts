import { describe, expect, it } from 'vitest'
import { formatDateTime, localInputToOffsetIso, nextFullHourLocal, placeLabel } from './format'

describe('datas', () => {
  it('envia a hora local com o fuso do navegador', () => {
    expect(localInputToOffsetIso('2030-01-01T08:00')).toBe('2030-01-01T08:00:00-03:00')
  })

  it('sugere a próxima hora cheia', () => {
    expect(nextFullHourLocal(new Date(2030, 0, 1, 7, 30))).toBe('2030-01-01T08:00')
    expect(nextFullHourLocal(new Date(2030, 0, 1, 23, 5))).toBe('2030-01-02T00:00')
  })

  it('formata datas ausentes como travessão', () => {
    expect(formatDateTime(null)).toBe('—')
    expect(formatDateTime('2030-01-10T15:10:00-03:00')).toBe('10/01/2030, 15:10')
  })
})

describe('placeLabel', () => {
  it('usa o nome quando existe e as coordenadas como alternativa', () => {
    expect(placeLabel('Campinas', -22.9, -47.06)).toBe('Campinas')
    expect(placeLabel(null, -22.90561, -47.06081)).toBe('-22.9056, -47.0608')
  })
})
