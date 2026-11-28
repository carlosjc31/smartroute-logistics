import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { createBrowserRouter, RouterProvider } from 'react-router'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { AuthProvider } from 'react-oidc-context'
import type { User } from 'oidc-client-ts'
import { DispatchPage, TripDetailPage, TripsPage } from './features/trips'
import { oidcSettings, safeReturnPath, type ReturnState } from './shared/auth/config'
import { RequireAuth } from './shared/auth/RequireAuth'
import { Layout } from './shared/layout/Layout'
import './index.css'

const queryClient = new QueryClient({
  defaultOptions: { queries: { retry: 1, staleTime: 10_000 } },
})

const router = createBrowserRouter([
  {
    path: '/',
    element: (
      <RequireAuth>
        <Layout />
      </RequireAuth>
    ),
    children: [
      { index: true, Component: DispatchPage },
      { path: 'trips', Component: TripsPage },
      { path: 'trips/:id', Component: TripDetailPage },
    ],
  },
])

// Depois do login, remove ?code=&state= da URL e volta para a página em que o usuário estava
function onSigninCallback(user: User | void) {
  const returnTo = safeReturnPath((user?.state as ReturnState | undefined)?.returnTo)
  void router.navigate(returnTo, { replace: true })
}

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <AuthProvider {...oidcSettings} onSigninCallback={onSigninCallback}>
      <QueryClientProvider client={queryClient}>
        <RouterProvider router={router} />
      </QueryClientProvider>
    </AuthProvider>
  </StrictMode>,
)
