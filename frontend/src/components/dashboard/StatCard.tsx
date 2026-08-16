import type { ReactNode } from 'react'
import type { DeltaTone } from '../../lib/format'

export interface StatCardProps {
  label: string
  value: string
  deltaText: string | null
  deltaTone: DeltaTone
  icon: ReactNode
  iconVariant: 'accent' | 'success' | 'danger'
}

function StatCard({ label, value, deltaText, deltaTone, icon, iconVariant }: StatCardProps) {
  return (
    <div className="stat-card">
      <div className="stat-card-top">
        <span className="stat-card-label">{label}</span>
        <span className={`stat-card-icon stat-card-icon--${iconVariant}`} aria-hidden="true">
          {icon}
        </span>
      </div>
      <p className="stat-card-value">{value}</p>
      {deltaText !== null && (
        <p className={`stat-card-delta stat-card-delta--${deltaTone}`}>{deltaText}</p>
      )}
    </div>
  )
}

export default StatCard
