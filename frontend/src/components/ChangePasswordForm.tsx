import { useState, type FormEvent } from 'react'
import { changePassword } from '../api/users'
import { toFormError } from '../api/ApiError'
import FieldError from './FieldError'

const EMPTY_FORM = { currentPassword: '', newPassword: '', confirmPassword: '' }

const MIN_PASSWORD_LENGTH = 6

type FormState = typeof EMPTY_FORM

function validate(form: FormState): Record<string, string> {
  const errors: Record<string, string> = {}
  if (form.currentPassword.length === 0) {
    errors.currentPassword = 'Current password is required.'
  }
  if (form.newPassword.length < MIN_PASSWORD_LENGTH) {
    errors.newPassword = `New password must be at least ${MIN_PASSWORD_LENGTH} characters long.`
  } else if (form.newPassword === form.currentPassword) {
    errors.newPassword = 'New password must be different from the current one.'
  }
  if (form.confirmPassword !== form.newPassword) {
    errors.confirmPassword = 'The two passwords do not match.'
  }
  return errors
}

function ChangePasswordForm() {
  const [form, setForm] = useState<FormState>(EMPTY_FORM)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [submitting, setSubmitting] = useState(false)

  const handleChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = event.target
    const nextForm = { ...form, [name]: value }
    setForm(nextForm)
    setError('')
    setSuccess('')
    setFieldErrors((previous) => {
      const revalidated = validate(nextForm)
      const next: Record<string, string> = {}
      for (const key of Object.keys(previous)) {
        if (revalidated[key] !== undefined) {
          next[key] = revalidated[key]
        }
      }
      return next
    })
  }

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setError('')
    setSuccess('')

    const validationErrors = validate(form)
    if (Object.keys(validationErrors).length > 0) {
      setFieldErrors(validationErrors)
      return
    }

    setFieldErrors({})
    setSubmitting(true)
    try {
      await changePassword({
        currentPassword: form.currentPassword,
        newPassword: form.newPassword,
      })
      setForm(EMPTY_FORM)
      setSuccess('Password changed successfully.')
    } catch (caught) {
      const formError = toFormError(caught)
      const hasFieldErrors = Object.keys(formError.fieldErrors).length > 0
      setFieldErrors(formError.fieldErrors)
      setError(hasFieldErrors ? '' : formError.message)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} className="security-form" noValidate>
      {error.length > 0 && <div className="auth-error" role="alert">{error}</div>}
      {success.length > 0 && <div className="auth-success" role="status">{success}</div>}
      <div className="form-group">
        <label htmlFor="currentPassword">Current password:</label>
        <input
          type="password"
          id="currentPassword"
          name="currentPassword"
          value={form.currentPassword}
          onChange={handleChange}
          className="form-input"
          placeholder="Enter your current password"
          aria-invalid={Boolean(fieldErrors.currentPassword)}
          disabled={submitting}
        />
        <FieldError message={fieldErrors.currentPassword} />
      </div>
      <div className="form-group">
        <label htmlFor="newPassword">New password:</label>
        <input
          type="password"
          id="newPassword"
          name="newPassword"
          value={form.newPassword}
          onChange={handleChange}
          className="form-input"
          placeholder="Enter your new password"
          aria-invalid={Boolean(fieldErrors.newPassword)}
          disabled={submitting}
        />
        <FieldError message={fieldErrors.newPassword} />
      </div>
      <div className="form-group">
        <label htmlFor="confirmPassword">Confirm new password:</label>
        <input
          type="password"
          id="confirmPassword"
          name="confirmPassword"
          value={form.confirmPassword}
          onChange={handleChange}
          className="form-input"
          placeholder="Confirm your new password"
          aria-invalid={Boolean(fieldErrors.confirmPassword)}
          disabled={submitting}
        />
        <FieldError message={fieldErrors.confirmPassword} />
      </div>
      <div className="profile-actions">
        <button type="submit" className="btn-primary" disabled={submitting}>
          {submitting ? 'Changing password...' : 'Change password'}
        </button>
      </div>
    </form>
  )
}

export default ChangePasswordForm
