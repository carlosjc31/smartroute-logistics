import { useAuth } from 'react-oidc-context'

export function UserMenu() {
  const auth = useAuth()
  const name = auth.user?.profile.preferred_username ?? auth.user?.profile.name
  if (!auth.isAuthenticated) return null
  return (
    <div className="user-menu">
      <span className="muted">{name}</span>
      <button type="button" className="button" onClick={() => void auth.signoutRedirect()}>
        Sair
      </button>
    </div>
  )
}
