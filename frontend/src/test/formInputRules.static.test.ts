import { readSourceFile } from './nodeFileAccess'
import { describe, expect, it } from 'vitest'

const appCssText = await readSourceFile('../App.css', import.meta.url)

const OLD_REST_RULE_SHAPE = `
.form-input,
.filter-select,
.setting-select,
.auth-form input[type='text'],
.auth-form input[type='email'],
.auth-form input[type='password'],
.category-form input,
.category-form textarea {
  width: 100%;
  background: var(--bg);
  border: 1px solid var(--border-strong);
  color: var(--text);
`

describe('a negative control proving the static assertions below can fail', () => {
  it('the rest-state regex would NOT match the pre-fix rule text using --border-strong', () => {
    const restRuleRegex =
      /\.form-input,\s*\.filter-select,\s*\.setting-select,\s*\.auth-form input\[type='text'\],\s*\.auth-form input\[type='email'\],\s*\.auth-form input\[type='password'\],\s*\.category-form input,\s*\.category-form textarea\s*\{[^}]*border:\s*1px solid var\(--neutral-strong\);[^}]*\}/
    expect(restRuleRegex.test(OLD_REST_RULE_SHAPE + '}')).toBe(false)
  })
})

describe('the shared form-input rest rule in App.css', () => {
  it('uses --neutral-strong for the border, not the old --border-strong', () => {
    const restRuleRegex =
      /\.form-input,\s*\.filter-select,\s*\.setting-select,\s*\.auth-form input\[type='text'\],\s*\.auth-form input\[type='email'\],\s*\.auth-form input\[type='password'\],\s*\.category-form input,\s*\.category-form textarea\s*\{[^}]*\}/
    const match = appCssText.match(restRuleRegex)
    expect(match).not.toBeNull()
    const block = match![0]
    expect(block).toContain("border: 1px solid var(--neutral-strong);")
    expect(block).not.toContain('var(--border-strong)')
  })
})

describe('the :focus rule for the same six selectors', () => {
  it('still keeps the accent border-color, accent-soft box-shadow, and suppresses the native outline', () => {
    const focusRuleRegex =
      /\.form-input:focus,\s*\.filter-select:focus,\s*\.setting-select:focus,\s*\.auth-form input:focus,\s*\.category-form input:focus,\s*\.category-form textarea:focus\s*\{([^}]*)\}/
    const match = appCssText.match(focusRuleRegex)
    expect(match).not.toBeNull()
    const body = match![1]
    expect(body).toContain('outline: none;')
    expect(body).toContain('border-color: var(--accent);')
    expect(body).toContain('box-shadow: 0 0 0 3px var(--accent-soft);')
  })
})

describe('the new :focus-visible rule for the same six selectors', () => {
  it('exists as its own rule adding a visible keyboard-focus outline', () => {
    const focusVisibleRuleRegex =
      /\.form-input:focus-visible,\s*\.filter-select:focus-visible,\s*\.setting-select:focus-visible,\s*\.auth-form input:focus-visible,\s*\.category-form input:focus-visible,\s*\.category-form textarea:focus-visible\s*\{([^}]*)\}/
    const match = appCssText.match(focusVisibleRuleRegex)
    expect(match).not.toBeNull()
    const body = match![1]
    expect(body).toContain('outline: 2px solid var(--accent);')
    expect(body).toContain('outline-offset: 2px;')
  })

  it('appears exactly once in the file, not duplicated or conflicting elsewhere', () => {
    const occurrences = appCssText.match(/\.form-input:focus-visible/g) ?? []
    expect(occurrences).toHaveLength(1)
  })
})

describe('--border-strong token and its other consumers are untouched', () => {
  it('the token definition itself is not redefined inside App.css', () => {
    expect(appCssText).not.toMatch(/--border-strong:\s*#/)
  })

  it('still has its other pre-existing usages elsewhere in the file, unrelated to the fixed rule', () => {
    const allUsages = appCssText.match(/var\(--border-strong\)/g) ?? []
    expect(allUsages.length).toBeGreaterThanOrEqual(9)
  })

  it('the fixed rest-state rule no longer references it', () => {
    const restRuleRegex =
      /\.form-input,\s*\.filter-select,\s*\.setting-select,\s*\.auth-form input\[type='text'\],\s*\.auth-form input\[type='email'\],\s*\.auth-form input\[type='password'\],\s*\.category-form input,\s*\.category-form textarea\s*\{[^}]*\}/
    const match = appCssText.match(restRuleRegex)
    expect(match![0]).not.toContain('var(--border-strong)')
  })
})

describe('pre-existing, unrelated :focus-visible rules elsewhere in App.css are unaffected', () => {
  it.each(['.shell-nav-toggle', '.shell-logo', '.shell-nav-link'])(
    '%s:focus-visible still declares the original accent outline',
    (selector) => {
      const escaped = selector.replace('.', '\\.')
      const regex = new RegExp(`${escaped}:focus-visible\\s*\\{([^}]*)\\}`)
      const match = appCssText.match(regex)
      expect(match).not.toBeNull()
      const body = match![1]
      expect(body).toContain('outline: 2px solid var(--accent);')
      expect(body).toContain('outline-offset: 2px;')
    },
  )
})
