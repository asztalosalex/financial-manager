import { describe, expect, it } from 'vitest'
import { DEFAULT_REDIRECT_PATH, resolveRedirectPath } from './redirect'

const SAME_ORIGIN = 'https://app.example'

const REJECTED: ReadonlyArray<readonly [string, unknown]> = [
  ['an https absolute URL', 'https://evil.example/'],
  ['an http absolute URL', 'http://evil.example/steal'],
  ['a scheme-relative URL with credentials', '//user:pass@evil.example/'],
  ['an uppercase scheme', 'HTTPS://evil.example/'],
  ['a protocol-relative URL', '//evil.example/'],
  ['a bare double slash', '//'],
  ['a double-backslash authority', '\\\\evil.example/'],
  ['a slash-backslash authority', '/\\evil.example'],
  ['a backslash-slash authority', '\\/evil.example'],
  ['a triple slash', '///evil.example'],
  ['a tab between the slashes', '/\t/evil.example'],
  ['a newline between the slashes', '/\n/evil.example'],
  ['a carriage return between the slashes', '/\r/evil.example'],
  ['a leading tab before a protocol-relative URL', '\t//evil.example'],
  ['a tab before a backslash authority', '/\t\\evil.example'],
  ['CRLF smuggled into an authority', '/\r\n/evil.example'],
  ['a javascript: URL', 'javascript:alert(1)'],
  ['a javascript: URL with a control character', 'java\tscript:alert(1)'],
  ['a data: URL', 'data:text/html,<script>alert(1)</script>'],
  ['a vbscript: URL', 'vbscript:msgbox(1)'],
  ['a mailto: URL', 'mailto:victim@evil.example'],
  ['a percent-encoded leading slash', '%2Fprofile'],
  ['a lowercase percent-encoded protocol-relative URL', '%2f%2fevil.example'],
  ['a percent-encoded backslash authority', '%5C%5Cevil.example'],
  ['a bare relative path', 'profile'],
  ['a dot-segment relative path', '../admin'],
  ['a space-prefixed path', ' /profile'],
  ['an empty string', ''],
  ['a number', 42],
  ['null', null],
  ['undefined', undefined],
  ['a boolean', true],
  ['an object that looks like a location', { pathname: '/profile' }],
  ['an array holding a valid path', ['/profile/transactions']],
  ['a String object rather than a primitive', new String('/profile/transactions')],
]

const ACCEPTED: ReadonlyArray<readonly [string, string, string]> = [
  ['a plain path', '/profile', '/profile'],
  ['a nested path', '/profile/transactions', '/profile/transactions'],
  ['the root path', '/', '/'],
  ['a path with a query and a hash', '/profile?tab=1#top', '/profile?tab=1#top'],
  [
    'a query string that merely looks protocol-relative',
    '/profile?next=//evil.example',
    '/profile?next=//evil.example',
  ],
  ['a percent-encoded slash inside the path', '/%2F%2Fevil.example', '/%2F%2Fevil.example'],
  ['a percent-encoded backslash inside the path', '/%5Cevil.example', '/%5Cevil.example'],
  ['a percent-encoded tab inside the path', '/%09/evil.example', '/%09/evil.example'],
  ['a leading tab that strips down to a safe path', '\t/profile', '/profile'],
  ['a newline inside a path segment', '/pro\nfile', '/profile'],
  ['a backslash used as a separator', '/profile\\settings', '/profile/settings'],
  ['the documented dot-segment near miss', '/./\\evil.example', '/.//evil.example'],
]

function originOf(path: string): string {
  return new URL(path, SAME_ORIGIN).origin
}

describe('resolveRedirectPath', () => {
  it('exports /dashboard as the default redirect target', () => {
    expect(DEFAULT_REDIRECT_PATH).toBe('/dashboard')
  })

  it.each(REJECTED)('rejects %s and falls back', (_label, hostile) => {
    expect(resolveRedirectPath(hostile)).toBe(DEFAULT_REDIRECT_PATH)
  })

  it.each(ACCEPTED)('accepts %s', (_label, input, expected) => {
    expect(resolveRedirectPath(input)).toBe(expected)
  })

  it('never returns a path that a URL parser resolves to a foreign origin', () => {
    const corpus = [
      ...REJECTED.map(([, value]) => value),
      ...ACCEPTED.map(([, input]) => input),
    ]

    for (const value of corpus) {
      const resolved = resolveRedirectPath(value)
      expect(originOf(resolved)).toBe(SAME_ORIGIN)
    }
  })

  it('proves the foreign-origin check can see a foreign origin at all', () => {
    expect(originOf('//evil.example/')).toBe('https://evil.example')
    expect(originOf('https://evil.example/')).toBe('https://evil.example')
  })

  describe('the fallback parameter', () => {
    it('is /dashboard when omitted', () => {
      expect(resolveRedirectPath(undefined)).toBe('/dashboard')
    })

    it('is honoured instead of the default for a non-string candidate', () => {
      expect(resolveRedirectPath(null, '/login')).toBe('/login')
    })

    it('is honoured instead of the default for a rejected protocol-relative URL', () => {
      expect(resolveRedirectPath('//evil.example/', '/settings')).toBe('/settings')
    })

    it('is honoured instead of the default for a rejected relative path', () => {
      expect(resolveRedirectPath('profile', '/')).toBe('/')
    })

    it('is ignored when the candidate is a valid path', () => {
      expect(resolveRedirectPath('/profile/transactions', '/settings')).toBe(
        '/profile/transactions',
      )
    })

    it('does not leak into the accepted branch even when it differs from the default', () => {
      expect(resolveRedirectPath('/', '/settings')).toBe('/')
    })
  })
})
