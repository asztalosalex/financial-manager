import type { ChangeEvent } from 'react'

export interface MonthPickerProps {
  value: string
  onChange: (value: string) => void
}

function MonthPicker({ value, onChange }: MonthPickerProps) {
  const handleChange = (event: ChangeEvent<HTMLInputElement>) => {
    onChange(event.target.value)
  }

  return (
    <div className="month-picker">
      <label htmlFor="reports-month-picker">Month</label>
      <input id="reports-month-picker" type="month" value={value} onChange={handleChange} />
    </div>
  )
}

export default MonthPicker
