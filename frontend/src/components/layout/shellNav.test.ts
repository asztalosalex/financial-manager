import { readSourceFile } from '../../test/nodeFileAccess'
import { describe, expect, it } from 'vitest'
import { MOBILE_BREAKPOINT_PX, MOBILE_NAV_QUERY } from './shellNav'

const appCssText = await readSourceFile('../../App.css', import.meta.url)

describe('MOBILE_BREAKPOINT_PX', () => {
  it('is used to build MOBILE_NAV_QUERY', () => {
    expect(MOBILE_NAV_QUERY).toBe(`(max-width: ${MOBILE_BREAKPOINT_PX}px)`)
  })

  it('matches the breakpoint number declared in App.css media queries', () => {
    const mediaQueryPixelValues = [...appCssText.matchAll(/@media \(max-width: ([\d.]+)px\)/g)].map((match) =>
      Number.parseFloat(match[1]),
    )
    expect(mediaQueryPixelValues.length).toBeGreaterThan(0)
    expect(mediaQueryPixelValues).toContain(MOBILE_BREAKPOINT_PX)
  })
})

describe('a negative control proving the App.css assertion above can fail', () => {
  it('would not find a mismatched breakpoint value among the real media queries', () => {
    const mediaQueryPixelValues = [...appCssText.matchAll(/@media \(max-width: ([\d.]+)px\)/g)].map((match) =>
      Number.parseFloat(match[1]),
    )
    expect(mediaQueryPixelValues).not.toContain(MOBILE_BREAKPOINT_PX + 1)
  })
})
