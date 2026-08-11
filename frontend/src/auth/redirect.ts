export const DEFAULT_REDIRECT_PATH = '/dashboard'

function normalizeCandidate(value: string): string {
  return value.replace(/[\t\n\r]/g, '').replace(/\\/g, '/')
}

export function resolveRedirectPath(
  candidate: unknown,
  fallback: string = DEFAULT_REDIRECT_PATH,
): string {
  if (typeof candidate !== 'string') {
    return fallback
  }

  const path = normalizeCandidate(candidate)

  if (!path.startsWith('/')) {
    return fallback
  }

  if (path.startsWith('//')) {
    return fallback
  }

  return path
}
