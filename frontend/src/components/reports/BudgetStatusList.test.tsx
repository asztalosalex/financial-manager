import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import BudgetStatusList, { type BudgetStatusRowItem } from './BudgetStatusList'

const NORMAL_ROW: BudgetStatusRowItem = {
  categoryId: 1,
  categoryName: 'Housing',
  budgetedLabel: '100 000 Ft',
  spentLabel: '80 000 Ft',
  remainingLabel: '20 000 Ft left',
  remainingTone: 'success',
  percentageLabel: '80.0%',
  barWidthPercent: 80,
  barTone: 'accent',
}

const OVER_ROW: BudgetStatusRowItem = {
  categoryId: 2,
  categoryName: 'Food',
  budgetedLabel: '50 000 Ft',
  spentLabel: '62 000 Ft',
  remainingLabel: '12 000 Ft over',
  remainingTone: 'danger',
  percentageLabel: '124.0%',
  barWidthPercent: 100,
  barTone: 'danger',
}

const NULL_PERCENT_ROW: BudgetStatusRowItem = {
  categoryId: 3,
  categoryName: 'Misc',
  budgetedLabel: '30 000 Ft',
  spentLabel: '0 Ft',
  remainingLabel: '30 000 Ft left',
  remainingTone: 'success',
  percentageLabel: '—',
  barWidthPercent: 0,
  barTone: 'accent',
}

describe('BudgetStatusList', () => {
  it('renders the header title and month label', () => {
    render(
      <BudgetStatusList
        items={[NORMAL_ROW]}
        totalBudgetedLabel="100 000 Ft"
        totalSpentLabel="80 000 Ft"
        unbudgetedSpendingLabel={null}
        monthLabel="August 2026"
        isEmpty={false}
      />,
    )

    expect(screen.getByText('Budget vs Actual')).toBeInTheDocument()
    expect(screen.getByText('August 2026')).toBeInTheDocument()
  })

  it('renders every row, in the exact order given, with category name and amounts', () => {
    render(
      <BudgetStatusList
        items={[OVER_ROW, NORMAL_ROW]}
        totalBudgetedLabel="150 000 Ft"
        totalSpentLabel="142 000 Ft"
        unbudgetedSpendingLabel={null}
        monthLabel="August 2026"
        isEmpty={false}
      />,
    )

    const names = screen.getAllByText(/^(Food|Housing)$/)
    expect(names.map((el) => el.textContent)).toEqual(['Food', 'Housing'])
    expect(screen.getByText('50 000 Ft / 62 000 Ft')).toBeInTheDocument()
    expect(screen.getByText('100 000 Ft / 80 000 Ft')).toBeInTheDocument()
  })

  it('renders the totals from props directly, not derived from the items', () => {
    render(
      <BudgetStatusList
        items={[NORMAL_ROW]}
        totalBudgetedLabel="999 999 Ft"
        totalSpentLabel="888 888 Ft"
        unbudgetedSpendingLabel={null}
        monthLabel="August 2026"
        isEmpty={false}
      />,
    )

    expect(screen.getByText('999 999 Ft')).toBeInTheDocument()
    expect(screen.getByText('888 888 Ft')).toBeInTheDocument()
  })

  it('shows the remaining label and success tone for an under-budget row', () => {
    const { container } = render(
      <BudgetStatusList
        items={[NORMAL_ROW]}
        totalBudgetedLabel="100 000 Ft"
        totalSpentLabel="80 000 Ft"
        unbudgetedSpendingLabel={null}
        monthLabel="August 2026"
        isEmpty={false}
      />,
    )

    expect(screen.getByText('20 000 Ft left')).toBeInTheDocument()
    expect(container.querySelector('.budget-status-item-remaining--success')).not.toBeNull()
    expect(container.querySelector('.budget-status-item-remaining--danger')).toBeNull()
  })

  it('shows the remaining label and danger tone for an over-budget row', () => {
    const { container } = render(
      <BudgetStatusList
        items={[OVER_ROW]}
        totalBudgetedLabel="50 000 Ft"
        totalSpentLabel="62 000 Ft"
        unbudgetedSpendingLabel={null}
        monthLabel="August 2026"
        isEmpty={false}
      />,
    )

    expect(screen.getByText('12 000 Ft over')).toBeInTheDocument()
    expect(container.querySelector('.budget-status-item-remaining--danger')).not.toBeNull()
    expect(container.querySelector('.budget-status-item-remaining--success')).toBeNull()
  })

  it('renders the uncapped percentage label alongside a clamped bar width for an over-budget row', () => {
    const { container } = render(
      <BudgetStatusList
        items={[OVER_ROW]}
        totalBudgetedLabel="50 000 Ft"
        totalSpentLabel="62 000 Ft"
        unbudgetedSpendingLabel={null}
        monthLabel="August 2026"
        isEmpty={false}
      />,
    )

    expect(screen.getByText('124.0%')).toBeInTheDocument()
    const fill = container.querySelector('.budget-status-fill') as HTMLElement
    expect(fill.style.width).toBe('100%')
  })

  it('applies the danger tone to the bar fill and percentage text when barTone is danger', () => {
    const { container } = render(
      <BudgetStatusList
        items={[OVER_ROW]}
        totalBudgetedLabel="50 000 Ft"
        totalSpentLabel="62 000 Ft"
        unbudgetedSpendingLabel={null}
        monthLabel="August 2026"
        isEmpty={false}
      />,
    )

    expect(container.querySelector('.budget-status-fill--danger')).not.toBeNull()
    expect(container.querySelector('.budget-status-item-percentage--danger')).not.toBeNull()
  })

  it('applies the accent tone to the bar fill when barTone is accent', () => {
    const { container } = render(
      <BudgetStatusList
        items={[NORMAL_ROW]}
        totalBudgetedLabel="100 000 Ft"
        totalSpentLabel="80 000 Ft"
        unbudgetedSpendingLabel={null}
        monthLabel="August 2026"
        isEmpty={false}
      />,
    )

    expect(container.querySelector('.budget-status-fill--accent')).not.toBeNull()
    expect(container.querySelector('.budget-status-fill--danger')).toBeNull()
  })

  it('renders a dash and no bar for a row with a null percentage, without throwing', () => {
    const { container } = render(
      <BudgetStatusList
        items={[NULL_PERCENT_ROW]}
        totalBudgetedLabel="30 000 Ft"
        totalSpentLabel="0 Ft"
        unbudgetedSpendingLabel={null}
        monthLabel="August 2026"
        isEmpty={false}
      />,
    )

    expect(screen.getByText('—')).toBeInTheDocument()
    expect(container.querySelector('.budget-status-track')).toBeNull()
    expect(container.querySelector('.budget-status-fill')).toBeNull()
  })

  it('still renders the null-percentage row inside the list, not skipped', () => {
    render(
      <BudgetStatusList
        items={[NORMAL_ROW, NULL_PERCENT_ROW]}
        totalBudgetedLabel="130 000 Ft"
        totalSpentLabel="80 000 Ft"
        unbudgetedSpendingLabel={null}
        monthLabel="August 2026"
        isEmpty={false}
      />,
    )

    expect(screen.getByText('Housing')).toBeInTheDocument()
    expect(screen.getByText('Misc')).toBeInTheDocument()
  })

  it('shows the fully-empty message when there are no items and no unbudgeted spending', () => {
    render(
      <BudgetStatusList
        items={[]}
        totalBudgetedLabel="0 Ft"
        totalSpentLabel="0 Ft"
        unbudgetedSpendingLabel={null}
        monthLabel="August 2026"
        isEmpty={true}
      />,
    )

    expect(
      screen.getByText('No budgets set for this month, and no spending recorded.'),
    ).toBeInTheDocument()
    expect(screen.queryByText(/Unbudgeted spending/)).not.toBeInTheDocument()
  })

  it('shows the no-budgets message, distinct from the fully-empty one, when there are no items but there is unbudgeted spending', () => {
    render(
      <BudgetStatusList
        items={[]}
        totalBudgetedLabel="0 Ft"
        totalSpentLabel="15 000 Ft"
        unbudgetedSpendingLabel="15 000 Ft"
        monthLabel="August 2026"
        isEmpty={true}
      />,
    )

    expect(screen.getByText('No budgets set for this month.')).toBeInTheDocument()
    expect(
      screen.queryByText('No budgets set for this month, and no spending recorded.'),
    ).not.toBeInTheDocument()
    expect(screen.getByText('Unbudgeted spending: 15 000 Ft')).toBeInTheDocument()
  })

  it('shows the unbudgeted spending line below a populated list when there is unbudgeted spending', () => {
    render(
      <BudgetStatusList
        items={[NORMAL_ROW]}
        totalBudgetedLabel="100 000 Ft"
        totalSpentLabel="80 000 Ft"
        unbudgetedSpendingLabel="15 000 Ft"
        monthLabel="August 2026"
        isEmpty={false}
      />,
    )

    expect(screen.getByText('Unbudgeted spending: 15 000 Ft')).toBeInTheDocument()
  })

  it('hides the unbudgeted spending line for a populated list when there is no unbudgeted spending', () => {
    render(
      <BudgetStatusList
        items={[NORMAL_ROW]}
        totalBudgetedLabel="100 000 Ft"
        totalSpentLabel="80 000 Ft"
        unbudgetedSpendingLabel={null}
        monthLabel="August 2026"
        isEmpty={false}
      />,
    )

    expect(screen.queryByText(/Unbudgeted spending/)).not.toBeInTheDocument()
  })

  it('does not render the item list when isEmpty is true, even if items were somehow passed', () => {
    const { container } = render(
      <BudgetStatusList
        items={[]}
        totalBudgetedLabel="0 Ft"
        totalSpentLabel="0 Ft"
        unbudgetedSpendingLabel={null}
        monthLabel="August 2026"
        isEmpty={true}
      />,
    )

    expect(container.querySelector('.budget-status-list')).toBeNull()
    expect(container.querySelector('.empty-state')).not.toBeNull()
  })
})
