import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { act, fireEvent, render, screen } from '@testing-library/react'
import ChangePasswordForm from './ChangePasswordForm'
import { setUnauthorizedHandler } from '../api/client'
import { emptyResponse, jsonResponse } from '../test/helpers'

function fillAndSubmit() {
  fireEvent.change(screen.getByLabelText('Current password:'), {
    target: { value: 'wrongpass' },
  })
  fireEvent.change(screen.getByLabelText('New password:'), {
    target: { value: 'newsecret' },
  })
  fireEvent.change(screen.getByLabelText('Confirm new password:'), {
    target: { value: 'newsecret' },
  })
  return act(async () => {
    fireEvent.click(screen.getByRole('button', { name: 'Change password' }))
  })
}

describe('ChangePasswordForm', () => {
  beforeEach(() => {
    setUnauthorizedHandler(null)
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('shows a wrong current password beside the field, not as a banner', async () => {
    vi.mocked(globalThis.fetch).mockResolvedValue(
      jsonResponse(400, {
        status: 400,
        message: 'Validation failed',
        fieldErrors: { currentPassword: 'Current password is incorrect' },
      }),
    )

    render(<ChangePasswordForm />)
    await fillAndSubmit()

    const fieldError = screen.getByText('Current password is incorrect')
    expect(fieldError).toHaveClass('field-error')
    expect(document.querySelector('.auth-error')).toBeNull()
    expect(screen.queryByText('Validation failed')).not.toBeInTheDocument()
    expect(screen.getByLabelText('Current password:')).toHaveAttribute('aria-invalid', 'true')
  })

  it('sends the request through the API layer with a JSON content type', async () => {
    vi.mocked(globalThis.fetch).mockResolvedValue(emptyResponse(204))

    render(<ChangePasswordForm />)
    await fillAndSubmit()

    const [url, init] = vi.mocked(globalThis.fetch).mock.calls[0]
    expect(url).toBe('/api/users/change-password')
    expect(init?.method).toBe('POST')
    expect((init?.headers as Headers).get('Content-Type')).toBe('application/json')
    expect(screen.getByText('Password changed successfully.')).toBeInTheDocument()
  })

  it('falls back to a banner when the failure has no field errors', async () => {
    vi.mocked(globalThis.fetch).mockResolvedValue(
      jsonResponse(500, { status: 500, message: 'Boom' }),
    )

    render(<ChangePasswordForm />)
    await fillAndSubmit()

    expect(screen.getByText('Boom')).toHaveClass('auth-error')
  })

  describe('client-side validation', () => {
    it('rejects an empty current password without calling the API', async () => {
      render(<ChangePasswordForm />)

      fireEvent.change(screen.getByLabelText('New password:'), {
        target: { value: 'newsecret' },
      })
      fireEvent.change(screen.getByLabelText('Confirm new password:'), {
        target: { value: 'newsecret' },
      })
      await act(async () => {
        fireEvent.click(screen.getByRole('button', { name: 'Change password' }))
      })

      expect(screen.getByText('Current password is required.')).toHaveClass('field-error')
      expect(screen.getByLabelText('Current password:')).toHaveAttribute('aria-invalid', 'true')
      expect(globalThis.fetch).not.toHaveBeenCalled()
    })

    it('rejects a new password shorter than the minimum length without calling the API', async () => {
      render(<ChangePasswordForm />)

      fireEvent.change(screen.getByLabelText('Current password:'), {
        target: { value: 'oldsecret' },
      })
      fireEvent.change(screen.getByLabelText('New password:'), {
        target: { value: 'ab1' },
      })
      fireEvent.change(screen.getByLabelText('Confirm new password:'), {
        target: { value: 'ab1' },
      })
      await act(async () => {
        fireEvent.click(screen.getByRole('button', { name: 'Change password' }))
      })

      expect(
        screen.getByText('New password must be at least 6 characters long.'),
      ).toHaveClass('field-error')
      expect(screen.getByLabelText('New password:')).toHaveAttribute('aria-invalid', 'true')
      expect(globalThis.fetch).not.toHaveBeenCalled()
    })

    it('rejects a new password identical to the current password without calling the API', async () => {
      render(<ChangePasswordForm />)

      fireEvent.change(screen.getByLabelText('Current password:'), {
        target: { value: 'samesecret' },
      })
      fireEvent.change(screen.getByLabelText('New password:'), {
        target: { value: 'samesecret' },
      })
      fireEvent.change(screen.getByLabelText('Confirm new password:'), {
        target: { value: 'samesecret' },
      })
      await act(async () => {
        fireEvent.click(screen.getByRole('button', { name: 'Change password' }))
      })

      expect(
        screen.getByText('New password must be different from the current one.'),
      ).toHaveClass('field-error')
      expect(screen.getByLabelText('New password:')).toHaveAttribute('aria-invalid', 'true')
      expect(globalThis.fetch).not.toHaveBeenCalled()
    })

    it('rejects mismatched confirmation without calling the API', async () => {
      render(<ChangePasswordForm />)

      fireEvent.change(screen.getByLabelText('Current password:'), {
        target: { value: 'oldsecret' },
      })
      fireEvent.change(screen.getByLabelText('New password:'), {
        target: { value: 'newsecret' },
      })
      fireEvent.change(screen.getByLabelText('Confirm new password:'), {
        target: { value: 'differentsecret' },
      })
      await act(async () => {
        fireEvent.click(screen.getByRole('button', { name: 'Change password' }))
      })

      expect(screen.getByText('The two passwords do not match.')).toHaveClass('field-error')
      expect(screen.getByLabelText('Confirm new password:')).toHaveAttribute(
        'aria-invalid',
        'true',
      )
      expect(globalThis.fetch).not.toHaveBeenCalled()
    })

    it('shows only the too-short error when the new password is both too short and identical to the current password', async () => {
      render(<ChangePasswordForm />)

      fireEvent.change(screen.getByLabelText('Current password:'), {
        target: { value: 'ab1' },
      })
      fireEvent.change(screen.getByLabelText('New password:'), {
        target: { value: 'ab1' },
      })
      fireEvent.change(screen.getByLabelText('Confirm new password:'), {
        target: { value: 'ab1' },
      })
      await act(async () => {
        fireEvent.click(screen.getByRole('button', { name: 'Change password' }))
      })

      expect(
        screen.getByText('New password must be at least 6 characters long.'),
      ).toHaveClass('field-error')
      expect(
        screen.queryByText('New password must be different from the current one.'),
      ).not.toBeInTheDocument()
      expect(globalThis.fetch).not.toHaveBeenCalled()
    })

    it('keeps showing a stale confirmation-mismatch error after editing the new password to actually match it', async () => {
      render(<ChangePasswordForm />)

      fireEvent.change(screen.getByLabelText('Current password:'), {
        target: { value: 'oldsecret' },
      })
      fireEvent.change(screen.getByLabelText('New password:'), {
        target: { value: 'firstvalue' },
      })
      fireEvent.change(screen.getByLabelText('Confirm new password:'), {
        target: { value: 'secondvalue' },
      })
      await act(async () => {
        fireEvent.click(screen.getByRole('button', { name: 'Change password' }))
      })

      expect(screen.getByText('The two passwords do not match.')).toHaveClass('field-error')

      fireEvent.change(screen.getByLabelText('New password:'), {
        target: { value: 'secondvalue' },
      })

      expect(
        screen.queryByText('The two passwords do not match.'),
      ).not.toBeInTheDocument()
    })

    it('keeps showing a stale must-differ error after editing the current password to actually differ', async () => {
      render(<ChangePasswordForm />)

      fireEvent.change(screen.getByLabelText('Current password:'), {
        target: { value: 'samesecret' },
      })
      fireEvent.change(screen.getByLabelText('New password:'), {
        target: { value: 'samesecret' },
      })
      fireEvent.change(screen.getByLabelText('Confirm new password:'), {
        target: { value: 'samesecret' },
      })
      await act(async () => {
        fireEvent.click(screen.getByRole('button', { name: 'Change password' }))
      })

      expect(
        screen.getByText('New password must be different from the current one.'),
      ).toHaveClass('field-error')

      fireEvent.change(screen.getByLabelText('Current password:'), {
        target: { value: 'differentsecret' },
      })

      expect(
        screen.queryByText('New password must be different from the current one.'),
      ).not.toBeInTheDocument()
    })
  })
})
