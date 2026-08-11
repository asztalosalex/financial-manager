import { vi } from 'vitest'

type ChangeListener = (event: MediaQueryListEvent) => void

interface StubbedList {
  media: string
  matches: boolean
  listeners: Set<ChangeListener>
}

const lists = new Set<StubbedList>()
let viewportWidth = 1280

function evaluate(media: string): boolean {
  const maxWidth = /\(max-width:\s*([\d.]+)px\)/.exec(media)
  if (maxWidth) {
    return viewportWidth <= Number(maxWidth[1])
  }

  const minWidth = /\(min-width:\s*([\d.]+)px\)/.exec(media)
  if (minWidth) {
    return viewportWidth >= Number(minWidth[1])
  }

  return false
}

export function stubViewportWidth(width: number): void {
  viewportWidth = width
  lists.clear()

  vi.stubGlobal('matchMedia', (media: string) => {
    const entry: StubbedList = { media, matches: evaluate(media), listeners: new Set() }
    lists.add(entry)

    return {
      media,
      get matches() {
        return entry.matches
      },
      onchange: null,
      addEventListener: (_type: string, listener: ChangeListener) => {
        entry.listeners.add(listener)
      },
      removeEventListener: (_type: string, listener: ChangeListener) => {
        entry.listeners.delete(listener)
      },
      addListener: (listener: ChangeListener) => {
        entry.listeners.add(listener)
      },
      removeListener: (listener: ChangeListener) => {
        entry.listeners.delete(listener)
      },
      dispatchEvent: () => true,
    } as unknown as MediaQueryList
  })
}

export function resizeViewportTo(width: number): void {
  viewportWidth = width

  for (const entry of lists) {
    const matches = evaluate(entry.media)
    if (matches === entry.matches) {
      continue
    }

    entry.matches = matches
    for (const listener of entry.listeners) {
      listener({ matches, media: entry.media } as MediaQueryListEvent)
    }
  }
}
