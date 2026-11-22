import { NavLink, Outlet } from 'react-router'

export function Layout() {
  return (
    <div className="app">
      <header className="topbar">
        <span className="brand">SmartRoute Dispatcher</span>
        <nav>
          <NavLink to="/" end>Novo despacho</NavLink>
          <NavLink to="/trips">Viagens</NavLink>
        </nav>
      </header>
      <main className="content">
        <Outlet />
      </main>
    </div>
  )
}
