import { Link } from 'react-router-dom';

function NotFound() {
  return (
    <div
      style={{
        minHeight: '100dvh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: 'clamp(1.5rem, 5vw, 3rem) clamp(16px, 4vw, 24px)',
        background: 'var(--bg)',
      }}
    >
      <div
        className="auth-card"
        style={{ textAlign: 'center' }}
      >
        <p
          style={{
            fontFamily: 'var(--font-display)',
            fontSize: '3rem',
            fontWeight: 700,
            color: 'var(--accent)',
            margin: 0,
          }}
        >
          404
        </p>
        <h2>Page not found</h2>
        <p className="subtitle">
          The page you are looking for does not exist or may have been moved.
        </p>
        <Link className="btn-primary" to="/">
          Back to homepage
        </Link>
      </div>
    </div>
  );
}

export default NotFound;
