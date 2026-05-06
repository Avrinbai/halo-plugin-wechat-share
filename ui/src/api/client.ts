import axios from 'axios'

export const client = axios.create({
  baseURL: '/apis/plugins/wechat-share/admin',
  headers: { Accept: 'application/json' },
})

export type Envelope<T> = { ok: boolean; data: T; message?: string | null; code?: string | null }

export class ApiError extends Error {
  readonly code?: string

  constructor(message: string, code?: string | null) {
    super(message)
    this.name = 'ApiError'
    this.code = code ?? undefined
  }
}

type ErrorCodeMessageMap = Partial<Record<string, string>>

function toApiError<T>(envelope?: Envelope<T>): ApiError {
  const message = envelope?.message || '请求失败'
  return new ApiError(message, envelope?.code)
}

export function getApiErrorMessage(
  error: unknown,
  fallback: string,
  codeMessageMap?: ErrorCodeMessageMap,
): string {
  if (error instanceof ApiError) {
    if (error.code && codeMessageMap?.[error.code]) return codeMessageMap[error.code] as string
    return error.message || fallback
  }
  if (error instanceof Error) return error.message || fallback
  return fallback
}

export async function getData<T>(path: string, config?: Parameters<typeof client.get>[1]): Promise<T> {
  const res = await client.get<Envelope<T>>(path, config)
  if (!res.data?.ok) throw toApiError(res.data)
  return res.data.data
}

export async function postData<T>(path: string, body: unknown): Promise<T> {
  const res = await client.post<Envelope<T>>(path, body, {
    headers: { 'Content-Type': 'application/json' },
  })
  if (!res.data?.ok) throw toApiError(res.data)
  return res.data.data
}

export async function putData<T>(path: string, body: unknown): Promise<T> {
  const res = await client.put<Envelope<T>>(path, body, {
    headers: { 'Content-Type': 'application/json' },
  })
  if (!res.data?.ok) throw toApiError(res.data)
  return res.data.data
}

export async function deleteData<T>(path: string): Promise<T> {
  const res = await client.delete<Envelope<T>>(path)
  if (!res.data?.ok) throw toApiError(res.data)
  return res.data.data
}
