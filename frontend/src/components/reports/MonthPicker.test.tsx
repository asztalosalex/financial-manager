import { describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen } from '@testing-library/react'
import MonthPicker from './MonthPicker'

describe('MonthPicker', () => {
  it('renders a native month input', () => {
    render(<MonthPicker value="2026-08" onChange={vi.fn()} />)

    const input = screen.getByLabelText('Month')
    expect(input).toHaveAttribute('type', 'month')
  })

  it('reflects the given value on the input', () => {
    render(<MonthPicker value="2026-08" onChange={vi.fn()} />)

    expect(screen.getByLabelText('Month')).toHaveValue('2026-08')
  })

  it('calls onChange with the new value immediately, with no Apply step', () => {
    const onChange = vi.fn()
    render(<MonthPicker value="2026-08" onChange={onChange} />)

    fireEvent.change(screen.getByLabelText('Month'), { target: { value: '2026-05' } })

    expect(onChange).toHaveBeenCalledTimes(1)
    expect(onChange).toHaveBeenCalledWith('2026-05')
  })

  it('does not mutate its own value when changed, since it is controlled by the parent', () => {
    const onChange = vi.fn()
    const { rerender } = render(<MonthPicker value="2026-08" onChange={onChange} />)

    fireEvent.change(screen.getByLabelText('Month'), { target: { value: '2026-05' } })
    expect(screen.getByLabelText('Month')).toHaveValue('2026-08')

    rerender(<MonthPicker value="2026-05" onChange={onChange} />)
    expect(screen.getByLabelText('Month')).toHaveValue('2026-05')
  })
})
