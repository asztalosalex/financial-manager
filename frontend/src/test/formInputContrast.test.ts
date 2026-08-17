import { readSourceFile } from './nodeFileAccess'
import { describe, expect, it } from 'vitest'

const indexCssText = await readSourceFile('../index.css', import.meta.url)

function relativeLuminance(hex: string): number {
  const normalized = hex.replace('#', '')
  const r = parseInt(normalized.substring(0, 2), 16) / 255
  const g = parseInt(normalized.substring(2, 4), 16) / 255
  const b = parseInt(normalized.substring(4, 6), 16) / 255
  const channel = (c: number) => (c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4))
  return 0.2126 * channel(r) + 0.7152 * channel(g) + 0.0722 * channel(b)
}

function contrastRatio(hexA: string, hexB: string): number {
  const lumA = relativeLuminance(hexA)
  const lumB = relativeLuminance(hexB)
  const lighter = Math.max(lumA, lumB)
  const darker = Math.min(lumA, lumB)
  return (lighter + 0.05) / (darker + 0.05)
}

function readToken(cssText: string, tokenName: string): string {
  const match = cssText.match(new RegExp(`${tokenName}:\\s*(#[0-9a-fA-F]{6})`))
  if (!match) {
    throw new Error(`token ${tokenName} not found`)
  }
  return match[1]
}

const bg = readToken(indexCssText, '--bg')
const surface = readToken(indexCssText, '--surface')
const neutralStrong = readToken(indexCssText, '--neutral-strong')
const borderStrong = readToken(indexCssText, '--border-strong')

describe('WCAG 1.4.11 non-text contrast formula sanity', () => {
  it('confirms the tokens read from index.css are the expected hex values', () => {
    expect(bg).toBe('#F5F6F8')
    expect(surface).toBe('#FFFFFF')
    expect(neutralStrong).toBe('#64748B')
    expect(borderStrong).toBe('#D6D9E0')
  })

  it('reproduces known reference ratios to validate the formula implementation', () => {
    expect(contrastRatio('#000000', '#FFFFFF')).toBeCloseTo(21, 0)
    expect(contrastRatio('#FFFFFF', '#FFFFFF')).toBeCloseTo(1, 5)
  })

  it('the new --neutral-strong border color clears the 3:1 AA threshold against the input fill', () => {
    const ratio = contrastRatio(neutralStrong, bg)
    expect(ratio).toBeGreaterThanOrEqual(3)
    expect(ratio).toBeCloseTo(4.4, 1)
  })

  it('the new --neutral-strong border color clears the 3:1 AA threshold against the surrounding card surface', () => {
    const ratio = contrastRatio(neutralStrong, surface)
    expect(ratio).toBeGreaterThanOrEqual(3)
    expect(ratio).toBeCloseTo(4.76, 1)
  })

  it('the old --border-strong color fails the 3:1 threshold against the input fill, matching docs/tasks.md', () => {
    const ratio = contrastRatio(borderStrong, bg)
    expect(ratio).toBeLessThan(3)
    expect(ratio).toBeCloseTo(1.31, 1)
  })

  it('the old --border-strong color fails the 3:1 threshold against the surrounding card surface, matching docs/tasks.md', () => {
    const ratio = contrastRatio(borderStrong, surface)
    expect(ratio).toBeLessThan(3)
    expect(ratio).toBeCloseTo(1.41, 1)
  })
})
