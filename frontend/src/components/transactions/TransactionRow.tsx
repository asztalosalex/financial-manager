import type { TransactionResponseDto } from '../../api/types'

export interface TransactionRowItem {
  id: number
  isIncome: boolean
  description: string
  categoryLabel: string
  amountLabel: string
  source: TransactionResponseDto
}

export interface TransactionRowProps {
  item: TransactionRowItem
  onEdit: (transaction: TransactionResponseDto) => void
  onDelete: (id: number) => void
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

function EditIcon() {
  return (
    <svg {...ICON_PROPS}>
      <path d="M12 20h9" />
      <path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4Z" />
    </svg>
  )
}

function DeleteIcon() {
  return (
    <svg {...ICON_PROPS}>
      <polyline points="3 6 5 6 21 6" />
      <path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6" />
      <path d="M10 11v6" />
      <path d="M14 11v6" />
      <path d="M9 6V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2" />
    </svg>
  )
}

function TransactionRow({ item, onEdit, onDelete }: TransactionRowProps) {
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
      <div className="transaction-row-actions">
        <button
          type="button"
          className="btn-edit"
          onClick={() => onEdit(item.source)}
          aria-label={`Edit ${item.categoryLabel}`}
        >
          <EditIcon />
        </button>
        <button
          type="button"
          className="btn-delete"
          onClick={() => onDelete(item.id)}
          aria-label={`Delete ${item.categoryLabel}`}
        >
          <DeleteIcon />
        </button>
      </div>
    </li>
  )
}

export default TransactionRow
