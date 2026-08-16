export interface TransactionRowItem {
  id: number
  isIncome: boolean
  description: string
  categoryLabel: string
  amountLabel: string
}

export interface TransactionRowProps {
  item: TransactionRowItem
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

function TransactionRow({ item }: TransactionRowProps) {
  return (
    <li className="transaction-row">
      <span
        className={`transaction-row-icon transaction-row-icon--${item.isIncome ? 'income' : 'expense'}`}
        aria-hidden="true"
      >
        {item.isIncome ? <IncomeIcon /> : <ExpenseIcon />}
      </span>
      <div className="transaction-row-body">
        <p className="transaction-row-description">{item.description}</p>
        <p className="transaction-row-meta">{item.categoryLabel}</p>
      </div>
      <span
        className={`transaction-row-amount transaction-row-amount--${item.isIncome ? 'income' : 'expense'}`}
      >
        {item.amountLabel}
      </span>
    </li>
  )
}

export default TransactionRow
