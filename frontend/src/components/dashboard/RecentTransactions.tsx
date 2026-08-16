import { Link } from 'react-router-dom'

export interface RecentTransactionItem {
  id: number
  isIncome: boolean
  description: string
  categoryLabel: string
  amountLabel: string
}

export interface RecentTransactionsProps {
  items: RecentTransactionItem[]
  isEmpty: boolean
  viewAllHref: string
}

const ICON_PROPS = {
  width: 16,
  height: 16,
  viewBox: '0 0 24 24',
  fill: 'none',
  stroke: 'currentColor',
  strokeWidth: 2.2,
  strokeLinecap: 'round',
  strokeLinejoin: 'round',
  'aria-hidden': true,
} as const

function IncomeIcon() {
  return (
    <svg {...ICON_PROPS}>
      <polyline points="23 6 13.5 15.5 8.5 10.5 1 18" />
      <polyline points="17 6 23 6 23 12" />
    </svg>
  )
}

function ExpenseIcon() {
  return (
    <svg {...ICON_PROPS}>
      <polyline points="23 18 13.5 8.5 8.5 13.5 1 6" />
      <polyline points="17 18 23 18 23 12" />
    </svg>
  )
}

function RecentTransactions({ items, isEmpty, viewAllHref }: RecentTransactionsProps) {
  return (
    <div className="chart-card recent-transactions-card">
      <div className="chart-card-header">
        <h2 className="chart-card-title">Recent Transactions</h2>
        <Link to={viewAllHref} className="view-all-link">
          View all
        </Link>
      </div>

      {isEmpty ? (
        <div className="empty-state">
          <p>No transactions yet.</p>
        </div>
      ) : (
        <ul className="recent-transactions-list" role="list">
          {items.map((item) => (
            <li key={item.id} className="recent-transaction-item">
              <span
                className={`recent-transaction-icon recent-transaction-icon--${
                  item.isIncome ? 'income' : 'expense'
                }`}
                aria-hidden="true"
              >
                {item.isIncome ? <IncomeIcon /> : <ExpenseIcon />}
              </span>
              <div className="recent-transaction-body">
                <p className="recent-transaction-description">{item.description}</p>
                <p className="recent-transaction-meta">{item.categoryLabel}</p>
              </div>
              <span
                className={`recent-transaction-amount recent-transaction-amount--${
                  item.isIncome ? 'income' : 'expense'
                }`}
              >
                {item.amountLabel}
              </span>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}

export default RecentTransactions
