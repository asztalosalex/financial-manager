export interface CategoryBreakdownItem {
  color: string
  label: string
  amountLabel: string
  percentage: number
}

export interface CategoryBreakdownProps {
  items: CategoryBreakdownItem[]
  isEmpty: boolean
}

function CategoryBreakdown({ items, isEmpty }: CategoryBreakdownProps) {
  return (
    <div className="chart-card breakdown-card">
      <div className="chart-card-header">
        <h2 className="chart-card-title">Categories in Detail</h2>
      </div>

      {isEmpty ? (
        <div className="empty-state">
          <p>No category data yet.</p>
        </div>
      ) : (
        <ul className="breakdown-list">
          {items.map((item, index) => (
            <li key={index} className="breakdown-item">
              <div className="breakdown-item-header">
                <span className="breakdown-item-label">{item.label}</span>
                <span className="breakdown-item-amount">{item.amountLabel}</span>
              </div>
              <div className="breakdown-track">
                <div
                  className="breakdown-fill"
                  style={{ width: `${item.percentage}%`, background: item.color }}
                />
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}

export default CategoryBreakdown
