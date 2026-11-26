import { User, WebStorageStateStore, type UserManagerSettings } from 'oidc-client-ts'

/**
 * Login OIDC no Keycloak com Authorization Code + PKCE (padrão do oidc-client-ts). O token fica no
 * sessionStorage (some ao fechar a aba) e é renovado em segundo plano antes de expirar.
 */
export const OIDC_AUTHORITY = import.meta.env.VITE_OIDC_AUTHORITY ?? 'http://localhost:8180/realms/smartroute'
export const OIDC_CLIENT_ID = import.meta.env.VITE_OIDC_CLIENT_ID ?? 'smartroute-frontend'

export const oidcSettings: UserManagerSettings = {
  authority: OIDC_AUTHORITY,
  client_id: OIDC_CLIENT_ID,
  redirect_uri: `${window.location.origin}/`,
  post_logout_redirect_uri: `${window.location.origin}/`,
  response_type: 'code',
  scope: 'openid profile',
  automaticSilentRenew: true,
  userStore: new WebStorageStateStore({ store: window.sessionStorage }),
}

const storageKey = () => `oidc.user:${OIDC_AUTHORITY}:${OIDC_CLIENT_ID}`

/** Token de acesso atual, para o cliente HTTP (fora da árvore React). */
export function getAccessToken(): string | undefined {
  const stored = window.sessionStorage.getItem(storageKey())
  if (!stored) return undefined
  const user = User.fromStorageString(stored)
  return user.expired ? undefined : user.access_token
}

/** Caminho interno para voltar após o login. Rejeita URLs externas (open redirect). */
export function safeReturnPath(value: unknown): string {
  return typeof value === 'string' && value.startsWith('/') && !value.startsWith('//') ? value : '/'
}

export type ReturnState = { returnTo?: string }
