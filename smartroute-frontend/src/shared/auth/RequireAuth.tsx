import { useEffect, useRef, type ReactNode } from 'react'
import { useAuth } from 'react-oidc-context'
import type { ReturnState } from './config'

/** Só renderiza o painel com usuário autenticado; caso contrário, redireciona ao login do Keycloak. */
export function RequireAuth({ children }: { children: ReactNode }) {
  const auth = useAuth()
  const redirected = useRef(false)
  const mustLogin = !auth.isLoading && !auth.isAuthenticated && !auth.activeNavigator && !auth.error

  useEffect(() => {
    if (!mustLogin || redirected.current) return
    redirected.current = true
    const state: ReturnState = { returnTo: window.location.pathname + window.location.search }
    void auth.signinRedirect({ state })
  }, [mustLogin, auth])

  if (auth.error) {
    return (
      <main className="content">
        <div className="card narrow">
          <h1>Não foi possível entrar</h1>
          <p className="alert error" role="alert">
            O serviço de login não respondeu ({auth.error.message}).
          </p>
          <button type="button" className="button primary" onClick={() => void auth.signinRedirect()}>
            Tentar novamente
          </button>
        </div>
      </main>
    )
  }
  if (!auth.isAuthenticated) {
    return (
      <main className="content">
        <p className="muted" role="status">Redirecionando para o login…</p>
      </main>
    )
  }
  return <>{children}</>
}
