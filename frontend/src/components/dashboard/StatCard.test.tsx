import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import StatCard from './StatCard'

describe('StatCard', () => {
  it('renders the label and the formatted value', () => {
    render(
      <StatCard
        label="Balance"
        value="1 248 500 Ft"
        deltaText={null}
        deltaTone="neutral"
        icon={<svg data-testid="icon" />}
        iconVariant="accent"
      />,
    )

    expect(screen.getByText('Balance')).toBeInTheDocument()
    expect(screen.getByText('1 248 500 Ft')).toBeInTheDocument()
  })

  it('renders the delta text with the given tone class when provided', () => {
    const { container } = render(
      <StatCard
        label="Monthly Income"
        value="500 000 Ft"
        deltaText="+2.1% vs last month"
        deltaTone="positive"
        icon={<svg />}
        iconVariant="success"
      />,
    )

    const delta = screen.getByText('+2.1% vs last month')
    expect(delta).toBeInTheDocument()
    expect(container.querySelector('.stat-card-delta--positive')).not.toBeNull()
  })

  it('renders no delta row when deltaText is null', () => {
    const { container } = render(
      <StatCard
        label="Savings Rate"
        value="—"
        deltaText={null}
        deltaTone="neutral"
        icon={<svg />}
        iconVariant="accent"
      />,
    )

    expect(container.querySelector('.stat-card-delta')).toBeNull()
  })

  it('applies the negative tone class for a negative delta', () => {
    const { container } = render(
      <StatCard
        label="Monthly Expense"
        value="300 000 Ft"
        deltaText="+8.6% vs last month"
        deltaTone="negative"
        icon={<svg />}
        iconVariant="danger"
      />,
    )

    expect(container.querySelector('.stat-card-delta--negative')).not.toBeNull()
    expect(container.querySelector('.stat-card-delta--positive')).toBeNull()
  })

  it('maps each iconVariant to its own icon tile class, distinct from the others', () => {
    const { container: accentContainer } = render(
      <StatCard
        label="Balance"
        value="1 Ft"
        deltaText={null}
        deltaTone="neutral"
        icon={<svg />}
        iconVariant="accent"
      />,
    )
    const { container: successContainer } = render(
      <StatCard
        label="Monthly Income"
        value="1 Ft"
        deltaText={null}
        deltaTone="neutral"
        icon={<svg />}
        iconVariant="success"
      />,
    )
    const { container: dangerContainer } = render(
      <StatCard
        label="Monthly Expense"
        value="1 Ft"
        deltaText={null}
        deltaTone="neutral"
        icon={<svg />}
        iconVariant="danger"
      />,
    )

    expect(accentContainer.querySelector('.stat-card-icon--accent')).not.toBeNull()
    expect(successContainer.querySelector('.stat-card-icon--success')).not.toBeNull()
    expect(dangerContainer.querySelector('.stat-card-icon--danger')).not.toBeNull()
    expect(accentContainer.querySelector('.stat-card-icon--danger')).toBeNull()
    expect(successContainer.querySelector('.stat-card-icon--accent')).toBeNull()
    expect(dangerContainer.querySelector('.stat-card-icon--success')).toBeNull()
  })
})
