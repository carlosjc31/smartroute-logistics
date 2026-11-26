import type { Problem } from './types'
import { getAccessToken } from '../auth/config'

const BASE_URL = import.meta.env.VITE_API_URL ?? ''

export class ApiError extends Error {
  readonly status: number
  readonly fieldErrors: Record<string, string>

  constructor(status: number, problem: Problem) {
    super(problem.detail ?? problem.title ?? `Erro ${status}`)
    this.status = status
    this.fieldErrors = problem.errors ?? {}
  }
}

export async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const token = getAccessToken()
  let response: Response
  try {
    response = await fetch(`${BASE_URL}${path}`, {
      ...init,
      headers: {
        'Content-Type': 'application/json',
        Accept: 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...init?.headers,
      },
    })
  } catch {
    throw new ApiError(0, { detail: 'Não foi possível conectar ao servidor.' })
  }

  if (!response.ok) {
    const problem: Problem = await response.json().catch(() => ({}))
    throw new ApiError(response.status, problem)
  }
  return (await response.json()) as T
}
