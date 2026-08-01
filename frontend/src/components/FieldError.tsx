function FieldError({ message }: { message?: string }) {
  if (message === undefined || message.length === 0) {
    return null
  }
  return (
    <span className="field-error" role="alert">
      {message}
    </span>
  )
}

export default FieldError
