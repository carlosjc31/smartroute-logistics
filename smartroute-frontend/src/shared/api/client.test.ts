import { afterEach, describe, expect, it, vi } from 'vitest'
import { User } from 'oidc-client-ts'
import { ApiError, request } from './client'
import { OIDC_AUTHORITY, OIDC_CLIENT_ID } from '../auth/config'
import { jsonResponse, mockFetch } from '../../test/utils'

describe('request', () => {
  it('devolve o JSON da resposta e envia cabeçalhos JSON', async () => {
    const calls = mockFetch(() => jsonResponse({ ok: true }))

    await expect(request('/api/v1/ping')).resolves.toEqual({ ok: true })
    expect(calls[0]?.url.pathname).toBe('/api/v1/ping')
    expect(new Headers(calls[0]?.init?.headers).get('Content-Type')).toBe('application/json')
  })

  // @req SEC-032
  it('converte um ProblemDetail em ApiError com os erros por campo', async () => {
    mockFetch(() =>
      jsonResponse({ title: 'Bad Request', detail: 'Dados inválidos', errors: { payloadWeight: 'deve ser maior que 0' } }, 400),
    )

    const error = await request('/api/v1/trips').catch((e: unknown) => e)

    expect(error).toBeInstanceOf(ApiError)
    expect(error).toMatchObject({ status: 400, message: 'Dados inválidos', fieldErrors: { payloadWeight: 'deve ser maior que 0' } })
  })

  // @req SEC-032
  it('usa uma mensagem genérica quando o erro não tem corpo', async () => {
    mockFetch(() => new Response('', { status: 502 }))

    await expect(request('/api/v1/trips')).rejects.toMatchObject({ status: 502, message: 'Erro 502' })
  })

  // @req SEC-032
  it('informa falha de conexão', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')))

    await expect(request('/api/v1/trips')).rejects.toMatchObject({
      status: 0,
      message: 'Não foi possível conectar ao servidor.',
    })
  })
})

describe('autenticação', () => {
  const key = `oidc.user:${OIDC_AUTHORITY}:${OIDC_CLIENT_ID}`
  const storeUser = (expiresInSeconds: number) =>
    sessionStorage.setItem(
      key,
      new User({
        access_token: 'token-de-acesso',
        token_type: 'Bearer',
        profile: { sub: 'u1', iss: OIDC_AUTHORITY, aud: OIDC_CLIENT_ID, exp: 0, iat: 0 },
        expires_at: Math.floor(Date.now() / 1000) + expiresInSeconds,
      }).toStorageString(),
    )

  afterEach(() => sessionStorage.clear())

  // @req SEC-031
  it('envia o token do usuário logado no header Authorization', async () => {
    storeUser(300)
    const calls = mockFetch(() => jsonResponse({}))

    await request('/api/v1/trips')

    expect(new Headers(calls[0]?.init?.headers).get('Authorization')).toBe('Bearer token-de-acesso')
  })

  // @req SEC-031
  it('não envia token expirado nem quando não há sessão', async () => {
    const calls = mockFetch(() => jsonResponse({}))

    await request('/api/v1/trips')
    storeUser(-10)
    await request('/api/v1/trips')

    expect(calls.map((c) => new Headers(c.init?.headers).has('Authorization'))).toEqual([false, false])
  })

  // @req SEC-032
  it('transforma 401 da API em ApiError com a mensagem do servidor', async () => {
    mockFetch(() => jsonResponse({ title: 'Não autenticado', detail: 'Faça login para acessar este recurso.' }, 401))

    await expect(request('/api/v1/trips')).rejects.toMatchObject({ status: 401, message: 'Faça login para acessar este recurso.' })
  })
})
