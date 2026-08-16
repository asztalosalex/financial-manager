import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import CategoryBreakdown from './CategoryBreakdown'

const ITEMS = [
  { color: 'red', label: 'Food', amountLabel: '145 000 Ft', percentage: 46.5 },
  { color: 'green', label: 'Rent', amountLabel: '92 000 Ft', percentage: 29.5 },
]

describe('CategoryBreakdown', () => {
  it('renders every category, in order, with label and amount', () => {
    render(<CategoryBreakdown items={ITEMS} isEmpty={false} />)

    const labels = screen.getAllByText(/^(Food|Rent)$/)
    expect(labels.map((el) => el.textContent)).toEqual(['Food', 'Rent'])
    expect(screen.getByText('145 000 Ft')).toBeInTheDocument()
    expect(screen.getByText('92 000 Ft')).toBeInTheDocument()
  })

  it('sizes the fill bar width to the given percentage', () => {
    const { container } = render(<CategoryBreakdown items={ITEMS} isEmpty={false} />)

    const fills = container.querySelectorAll('.breakdown-fill')
    expect(fills).toHaveLength(2)
    expect((fills[0] as HTMLElement).style.width).toBe('46.5%')
    expect((fills[1] as HTMLElement).style.width).toBe('29.5%')
  })

  it('does not collapse categories, even when there are more than 5', () => {
    const many = Array.from({ length: 8 }, (_, i) => ({
      color: 'red',
      label: `Cat ${i}`,
      amountLabel: '1 000 Ft',
      percentage: 5,
    }))

    const { container } = render(<CategoryBreakdown items={many} isEmpty={false} />)

    expect(container.querySelectorAll('.breakdown-item')).toHaveLength(8)
  })

  it('shows an empty state and no list when isEmpty is true', () => {
    const { container } = render(<CategoryBreakdown items={[]} isEmpty={true} />)

    expect(container.querySelector('.empty-state')).not.toBeNull()
    expect(container.querySelector('.breakdown-list')).toBeNull()
  })

  it('shows the list and no empty state when isEmpty is false', () => {
    const { container } = render(<CategoryBreakdown items={ITEMS} isEmpty={false} />)

    expect(container.querySelector('.empty-state')).toBeNull()
    expect(container.querySelector('.breakdown-list')).not.toBeNull()
  })
})
