export interface PaginationProps {
  page: number
  totalPages: number
  totalElements: number
  first: boolean
  last: boolean
  onPageChange: (page: number) => void
}

function Pagination({ page, totalPages, totalElements, first, last, onPageChange }: PaginationProps) {
  if (totalElements === 0) {
    return null
  }

  return (
    <div className="pagination">
      <button
        type="button"
        className="btn-secondary pagination-btn"
        onClick={() => onPageChange(page - 1)}
        disabled={first}
      >
        ◂ Prev
      </button>
      <span className="pagination-label">
        Page {page + 1} of {totalPages} ({totalElements} total)
      </span>
      <button
        type="button"
        className="btn-secondary pagination-btn"
        onClick={() => onPageChange(page + 1)}
        disabled={last}
      >
        Next ▸
      </button>
    </div>
  )
}

export default Pagination
