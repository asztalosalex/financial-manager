function TransactionsPlaceholder() {
  return (
    <div className="tab-content">
      <h2>Income &amp; Expenses</h2>
      <div className="empty-state">
        <p>No income or expense data is available yet.</p>
        <p>
          Transaction tracking and the monthly income, expense and balance totals are not connected
          to the server yet. Nothing is shown here rather than showing figures that are not yours.
        </p>
      </div>
    </div>
  )
}

export default TransactionsPlaceholder
