import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { useAuth, type AuthContextProps } from 'react-oidc-context'
import { RequireAuth } from './RequireAuth'
import { UserMenu } from './UserMenu'
import { safeReturnPath } from './config'

vi.mock('react-oidc-context', () => ({ useAuth: vi.fn() }))

const signinRedirect = vi.fn().mockResolvedValue(undefined)
const signoutRedirect = vi.fn().mockResolvedValue(undefined)

function mockAuth(state: Partial<AuthContextProps>) {
  vi.mocked(useAuth).mockReturnValue({
    isLoading: false,
    isAuthenticated: false,
    activeNavigator: undefined,
    error: undefined,
    user: null,
    signinRedirect,
    signoutRedirect,
    ...state,
  } as unknown as AuthContextProps)
}

describe('RequireAuth', () => {
  beforeEach(() => {
    signinRedirect.mockClear()
    window.history.replaceState({}, '', '/trips?status=PENDING')
  })

  // @req SEC-030
  it('mostra o painel para usuário autenticado', () => {
    mockAuth({ isAuthenticated: true })

    render(<RequireAuth><p>painel</p></RequireAuth>)

    expect(screen.getByText('painel')).toBeInTheDocument()
    expect(signinRedirect).not.toHaveBeenCalled()
  })

  // @req SEC-030
  it('redireciona ao login guardando a página atual', () => {
    mockAuth({ isAuthenticated: false })

    render(<RequireAuth><p>painel</p></RequireAuth>)

    expect(screen.queryByText('painel')).not.toBeInTheDocument()
    expect(screen.getByRole('status')).toHaveTextContent('Redirecionando para o login')
    expect(signinRedirect).toHaveBeenCalledOnce()
    expect(signinRedirect).toHaveBeenCalledWith({ state: { returnTo: '/trips?status=PENDING' } })
  })

  // @req SEC-030
  it('não redireciona enquanto a sessão está carregando', () => {
    mockAuth({ isLoading: true })

    render(<RequireAuth><p>painel</p></RequireAuth>)

    expect(signinRedirect).not.toHaveBeenCalled()
  })

  // @req SEC-030
  it('mostra o erro do login e permite tentar de novo', async () => {
    mockAuth({ error: new Error('Network Error') as AuthContextProps['error'] })

    render(<RequireAuth><p>painel</p></RequireAuth>)

    expect(screen.getByRole('alert')).toHaveTextContent('Network Error')
    expect(signinRedirect).not.toHaveBeenCalled()
    await userEvent.click(screen.getByRole('button', { name: 'Tentar novamente' }))
    expect(signinRedirect).toHaveBeenCalledOnce()
  })
})

describe('UserMenu', () => {
  // @req SEC-030
  it('mostra o usuário e faz logout', async () => {
    mockAuth({
      isAuthenticated: true,
      user: { profile: { preferred_username: 'despacho.dev' } } as AuthContextProps['user'],
    })

    render(<UserMenu />)

    expect(screen.getByText('despacho.dev')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Sair' }))
    expect(signoutRedirect).toHaveBeenCalledOnce()
  })
})

describe('safeReturnPath', () => {
  it.each([
    ['/trips/1', '/trips/1'],
    ['//evil.example/x', '/'],
    ['https://evil.example', '/'],
    [undefined, '/'],
  ])('%s → %s', (input, expected) => {
    expect(safeReturnPath(input)).toBe(expected)
  })
})
