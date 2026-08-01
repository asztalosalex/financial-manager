import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { signup } from '../api/auth';
import { toFormError } from '../api/ApiError';
import FieldError from '../components/FieldError';

function Register() {
  const navigate = useNavigate();
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setError('');
    setFieldErrors({});
    setLoading(true);
    try {
      await signup({ username, password, email });
      navigate('/login', { replace: true });
    } catch (err) {
      const formError = toFormError(err);
      setError(formError.message);
      setFieldErrors(formError.fieldErrors);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-container">
      <div className="auth-card">
        <h2>Create account</h2>
        {error && <div className="auth-error" role="alert">{error}</div>}
        <form onSubmit={handleSubmit} className="auth-form" noValidate>
          <label>
            Username
            <input
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              aria-invalid={Boolean(fieldErrors.username)}
              required
            />
            <FieldError message={fieldErrors.username} />
          </label>
          <label>
            Email
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              aria-invalid={Boolean(fieldErrors.email)}
              required
            />
            <FieldError message={fieldErrors.email} />
          </label>
          <label>
            Password
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              aria-invalid={Boolean(fieldErrors.password)}
              minLength={8}
              required
            />
            <FieldError message={fieldErrors.password} />
          </label>
          <button type="submit" disabled={loading} className="btn-primary">
            {loading ? 'Creating...' : 'Create account'}
          </button>
        </form>
        <p className="auth-switch">Have an account? <Link to="/login">Log in</Link></p>
      </div>
    </div>
  );
}

export default Register;
