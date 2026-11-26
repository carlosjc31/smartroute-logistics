import { describe, expect, it, vi } from 'vitest'
import { ApiError, request } from './client'
import { jsonResponse, mockFetch } from '../test/utils'

describe('request', () => {
  it('devolve o JSON da resposta e envia cabeçalhos JSON', async () => {
    const calls = mockFetch(() => jsonResponse({ ok: true }))

    await expect(request('/api/v1/ping')).resolves.toEqual({ ok: true })
    expect(calls[0]?.url.pathname).toBe('/api/v1/ping')
    expect(new Headers(calls[0]?.init?.headers).get('Content-Type')).toBe('application/json')
  })

  it('converte um ProblemDetail em ApiError com os erros por campo', async () => {
    mockFetch(() =>
      jsonResponse({ title: 'Bad Request', detail: 'Dados inválidos', errors: { payloadWeight: 'deve ser maior que 0' } }, 400),
    )

    const error = await request('/api/v1/trips').catch((e: unknown) => e)

    expect(error).toBeInstanceOf(ApiError)
    expect(error).toMatchObject({ status: 400, message: 'Dados inválidos', fieldErrors: { payloadWeight: 'deve ser maior que 0' } })
  })

  it('usa uma mensagem genérica quando o erro não tem corpo', async () => {
    mockFetch(() => new Response('', { status: 502 }))

    await expect(request('/api/v1/trips')).rejects.toMatchObject({ status: 502, message: 'Erro 502' })
  })

  it('informa falha de conexão', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')))

    await expect(request('/api/v1/trips')).rejects.toMatchObject({
      status: 0,
      message: 'Não foi possível conectar ao servidor.',
    })
  })
})
