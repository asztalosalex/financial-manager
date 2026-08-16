import { buildConicGradient, type DonutStop } from '../../lib/charts'

export interface DonutLegendItem {
  color: string
  label: string
  percentageLabel: string
}

export interface CategoryDonutProps {
  stops: DonutStop[]
  legend: DonutLegendItem[]
  centerValueLabel: string
  isEmpty: boolean
}

function CategoryDonut({ stops, legend, centerValueLabel, isEmpty }: CategoryDonutProps) {
  return (
    <div className="chart-card donut-card">
      <div className="chart-card-header">
        <h2 className="chart-card-title">Expenses by Category</h2>
      </div>

      {isEmpty ? (
        <div className="empty-state">
          <p>No category data yet.</p>
        </div>
      ) : (
        <>
          <div className="donut-graphic" style={{ background: buildConicGradient(stops) }}>
            <div className="donut-center">
              <span className="donut-value">{centerValueLabel}</span>
              <span className="donut-sublabel">total expense</span>
            </div>
          </div>
          <ul className="donut-legend">
            {legend.map((item, index) => (
              <li key={index} className="donut-legend-item">
                <span
                  className="donut-dot"
                  style={{ background: item.color }}
                  aria-hidden="true"
                />
                <span className="donut-legend-label">{item.label}</span>
                <span className="donut-legend-percentage">{item.percentageLabel}</span>
              </li>
            ))}
          </ul>
        </>
      )}
    </div>
  )
}

export default CategoryDonut
