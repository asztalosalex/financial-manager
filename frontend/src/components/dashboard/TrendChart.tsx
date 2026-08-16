export interface TrendChartPoint {
  monthLabel: string
  incomeHeightPx: number
  expenseHeightPx: number
  incomeLabel: string
  expenseLabel: string
}

export interface TrendChartProps {
  points: TrendChartPoint[]
  isEmpty: boolean
}

function TrendChart({ points, isEmpty }: TrendChartProps) {
  return (
    <div className="chart-card trend-card">
      <div className="chart-card-header">
        <div>
          <h2 className="chart-card-title">Income &amp; Expense Trend</h2>
          <p className="chart-card-subtitle">Last 6 months</p>
        </div>
        <ul className="trend-legend">
          <li className="trend-legend-item">
            <span className="legend-dot legend-dot--income" aria-hidden="true" />
            Income
          </li>
          <li className="trend-legend-item">
            <span className="legend-dot legend-dot--expense" aria-hidden="true" />
            Expense
          </li>
        </ul>
      </div>

      {isEmpty ? (
        <div className="empty-state">
          <p>No trend data yet.</p>
        </div>
      ) : (
        <div className="trend-graph">
          {points.map((point, index) => (
            <div
              key={index}
              className="trend-column-group"
              role="img"
              aria-label={`${point.monthLabel}: ${point.incomeLabel} income, ${point.expenseLabel} expense`}
            >
              <div className="trend-bars">
                <span
                  className="trend-bar trend-bar--income"
                  style={{ height: `${point.incomeHeightPx}px` }}
                />
                <span
                  className="trend-bar trend-bar--expense"
                  style={{ height: `${point.expenseHeightPx}px` }}
                />
              </div>
              <span className="trend-month">{point.monthLabel}</span>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

export default TrendChart
