import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import CategoryDonut from './CategoryDonut'

const STOPS = [
  { color: 'red', startPercent: 0, endPercent: 40 },
  { color: 'green', startPercent: 40, endPercent: 100 },
]

const LEGEND = [
  { color: 'red', label: 'Food', percentageLabel: '40.0%' },
  { color: 'green', label: 'Rent', percentageLabel: '60.0%' },
]

describe('CategoryDonut', () => {
  it('renders the center value label and sublabel', () => {
    render(
      <CategoryDonut
        stops={STOPS}
        legend={LEGEND}
        centerValueLabel="312 000 Ft"
        isEmpty={false}
      />,
    )

    expect(screen.getByText('312 000 Ft')).toBeInTheDocument()
    expect(screen.getByText('total expense')).toBeInTheDocument()
  })

  it('renders the donut graphic element when there is data', () => {
    const { container } = render(
      <CategoryDonut
        stops={STOPS}
        legend={LEGEND}
        centerValueLabel="312 000 Ft"
        isEmpty={false}
      />,
    )

    expect(container.querySelector('.donut-graphic')).not.toBeNull()
  })

  it('renders a real list with a legend item per entry, showing label and percentage', () => {
    render(
      <CategoryDonut
        stops={STOPS}
        legend={LEGEND}
        centerValueLabel="312 000 Ft"
        isEmpty={false}
      />,
    )

    const list = screen.getByRole('list')
    const items = screen.getAllByRole('listitem')
    expect(list).toBeInTheDocument()
    expect(items).toHaveLength(2)
    expect(screen.getByText('Food')).toBeInTheDocument()
    expect(screen.getByText('40.0%')).toBeInTheDocument()
    expect(screen.getByText('Rent')).toBeInTheDocument()
    expect(screen.getByText('60.0%')).toBeInTheDocument()
  })

  it('shows an em dash percentage label when provided', () => {
    render(
      <CategoryDonut
        stops={STOPS}
        legend={[{ color: 'red', label: 'Food', percentageLabel: '—' }]}
        centerValueLabel="0 Ft"
        isEmpty={false}
      />,
    )

    expect(screen.getByText('—')).toBeInTheDocument()
  })

  it('shows an empty state and no graphic or legend when isEmpty is true', () => {
    const { container } = render(
      <CategoryDonut stops={[]} legend={[]} centerValueLabel="—" isEmpty={true} />,
    )

    expect(container.querySelector('.empty-state')).not.toBeNull()
    expect(container.querySelector('.donut-graphic')).toBeNull()
    expect(container.querySelector('.donut-legend')).toBeNull()
  })
})
