import type { BudgetResponseDto } from '../../api/types'

export interface BudgetRowItem {
  id: number
  categoryName: string
  monthLabel: string
  amountLabel: string
  source: BudgetResponseDto
}

export interface BudgetRowProps {
  item: BudgetRowItem
  onEdit: (budget: BudgetResponseDto) => void
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

function BudgetRow({ item, onEdit, onDelete }: BudgetRowProps) {
  return (
    <li className="budget-row">
      <span className="budget-row-category">{item.categoryName}</span>
      <span className="budget-row-month">{item.monthLabel}</span>
      <span className="budget-row-amount">{item.amountLabel}</span>
      <div className="budget-row-actions">
        <button
          type="button"
          className="btn-edit"
          onClick={() => onEdit(item.source)}
          aria-label={`Edit ${item.categoryName}, ${item.monthLabel}`}
        >
          <EditIcon />
        </button>
        <button
          type="button"
          className="btn-delete"
          onClick={() => onDelete(item.id)}
          aria-label={`Delete ${item.categoryName}, ${item.monthLabel}`}
        >
          <DeleteIcon />
        </button>
      </div>
    </li>
  )
}

export default BudgetRow
