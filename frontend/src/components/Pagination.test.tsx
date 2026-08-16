import { describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen } from '@testing-library/react'
import Pagination from './Pagination'

describe('Pagination', () => {
  it('renders nothing when totalElements is 0', () => {
    const { container } = render(
      <Pagination page={0} totalPages={0} totalElements={0} first={true} last={true} onPageChange={vi.fn()} />,
    )

    expect(container.firstChild).toBeNull()
  })

  it('renders the bar when totalElements is greater than 0', () => {
    const { container } = render(
      <Pagination page={0} totalPages={7} totalElements={137} first={true} last={false} onPageChange={vi.fn()} />,
    )

    expect(container.querySelector('.pagination')).not.toBeNull()
  })

  it('renders the page label as 1-indexed, with the total page and element counts', () => {
    render(
      <Pagination page={1} totalPages={7} totalElements={137} first={false} last={false} onPageChange={vi.fn()} />,
    )

    expect(screen.getByText('Page 2 of 7 (137 total)')).toBeInTheDocument()
  })

  it('disables Prev when first is true', () => {
    render(
      <Pagination page={0} totalPages={7} totalElements={137} first={true} last={false} onPageChange={vi.fn()} />,
    )

    expect(screen.getByRole('button', { name: /Prev/ })).toBeDisabled()
  })

  it('enables Prev when first is false', () => {
    render(
      <Pagination page={1} totalPages={7} totalElements={137} first={false} last={false} onPageChange={vi.fn()} />,
    )

    expect(screen.getByRole('button', { name: /Prev/ })).not.toBeDisabled()
  })

  it('disables Next when last is true', () => {
    render(
      <Pagination page={6} totalPages={7} totalElements={137} first={false} last={true} onPageChange={vi.fn()} />,
    )

    expect(screen.getByRole('button', { name: /Next/ })).toBeDisabled()
  })

  it('enables Next when last is false', () => {
    render(
      <Pagination page={5} totalPages={7} totalElements={137} first={false} last={false} onPageChange={vi.fn()} />,
    )

    expect(screen.getByRole('button', { name: /Next/ })).not.toBeDisabled()
  })

  it('calls onPageChange with page - 1 when Prev is clicked', () => {
    const onPageChange = vi.fn()
    render(
      <Pagination
        page={3}
        totalPages={7}
        totalElements={137}
        first={false}
        last={false}
        onPageChange={onPageChange}
      />,
    )

    fireEvent.click(screen.getByRole('button', { name: /Prev/ }))

    expect(onPageChange).toHaveBeenCalledWith(2)
  })

  it('calls onPageChange with page + 1 when Next is clicked', () => {
    const onPageChange = vi.fn()
    render(
      <Pagination
        page={3}
        totalPages={7}
        totalElements={137}
        first={false}
        last={false}
        onPageChange={onPageChange}
      />,
    )

    fireEvent.click(screen.getByRole('button', { name: /Next/ }))

    expect(onPageChange).toHaveBeenCalledWith(4)
  })

  it('does not call onPageChange when the disabled Prev button is clicked', () => {
    const onPageChange = vi.fn()
    render(
      <Pagination page={0} totalPages={7} totalElements={137} first={true} last={false} onPageChange={onPageChange} />,
    )

    fireEvent.click(screen.getByRole('button', { name: /Prev/ }))

    expect(onPageChange).not.toHaveBeenCalled()
  })

  it('does not call onPageChange when the disabled Next button is clicked', () => {
    const onPageChange = vi.fn()
    render(
      <Pagination page={6} totalPages={7} totalElements={137} first={false} last={true} onPageChange={onPageChange} />,
    )

    fireEvent.click(screen.getByRole('button', { name: /Next/ }))

    expect(onPageChange).not.toHaveBeenCalled()
  })
})
