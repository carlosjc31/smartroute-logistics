import { NavLink, Outlet } from 'react-router'
import { UserMenu } from '../auth/UserMenu'

export function Layout() {
  return (
    <div className="app">
      <header className="topbar">
        <span className="brand">SmartRoute Dispatcher</span>
        <nav>
          <NavLink to="/" end>Novo despacho</NavLink>
          <NavLink to="/trips">Viagens</NavLink>
        </nav>
        <UserMenu />
      </header>
      <main className="content">
        <Outlet />
      </main>
    </div>
  )
}
