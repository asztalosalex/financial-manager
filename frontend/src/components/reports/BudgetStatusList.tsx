export interface BudgetStatusRowItem {
  categoryId: number
  categoryName: string
  budgetedLabel: string
  spentLabel: string
  remainingLabel: string
  remainingTone: 'success' | 'danger'
  percentageLabel: string
  barWidthPercent: number
  barTone: 'accent' | 'danger'
}

export interface BudgetStatusListProps {
  items: BudgetStatusRowItem[]
  totalBudgetedLabel: string
  totalSpentLabel: string
  unbudgetedSpendingLabel: string | null
  monthLabel: string
  isEmpty: boolean
}

function BudgetStatusList({
  items,
  totalBudgetedLabel,
  totalSpentLabel,
  unbudgetedSpendingLabel,
  monthLabel,
  isEmpty,
}: BudgetStatusListProps) {
  return (
    <div className="chart-card budget-status-card">
      <div className="chart-card-header">
        <div>
          <h2 className="chart-card-title">Budget vs Actual</h2>
          <p className="chart-card-subtitle">{monthLabel}</p>
        </div>
        <div className="budget-status-totals">
          <span className="budget-status-totals-item">
            Budgeted <strong>{totalBudgetedLabel}</strong>
          </span>
          <span className="budget-status-totals-item">
            Spent <strong>{totalSpentLabel}</strong>
          </span>
        </div>
      </div>

      {isEmpty ? (
        <div className="empty-state">
          <p>
            {unbudgetedSpendingLabel === null
              ? 'No budgets set for this month, and no spending recorded.'
              : 'No budgets set for this month.'}
          </p>
        </div>
      ) : (
        <ul className="budget-status-list">
          {items.map((item) => (
            <li key={item.categoryId} className="budget-status-item">
              <div className="budget-status-item-header">
                <span className="budget-status-item-label">{item.categoryName}</span>
                <span className="budget-status-item-amounts">
                  {item.budgetedLabel} / {item.spentLabel}
                </span>
              </div>
              <div className="budget-status-item-progress">
                {item.percentageLabel === '—' ? (
                  <span className="budget-status-item-percentage budget-status-item-percentage--empty">
                    —
                  </span>
                ) : (
                  <>
                    <div className="budget-status-track">
                      <div
                        className={`budget-status-fill budget-status-fill--${item.barTone}`}
                        style={{ width: `${item.barWidthPercent}%` }}
                      />
                    </div>
                    <span
                      className={`budget-status-item-percentage budget-status-item-percentage--${item.barTone}`}
                    >
                      {item.percentageLabel}
                    </span>
                  </>
                )}
              </div>
              <span
                className={`budget-status-item-remaining budget-status-item-remaining--${item.remainingTone}`}
              >
                {item.remainingLabel}
              </span>
            </li>
          ))}
        </ul>
      )}

      {unbudgetedSpendingLabel !== null && (
        <p className="budget-status-unbudgeted">Unbudgeted spending: {unbudgetedSpendingLabel}</p>
      )}
    </div>
  )
}

export default BudgetStatusList
