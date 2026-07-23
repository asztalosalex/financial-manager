//import './Home.css';
import {useState, useEffect} from 'react';

function Home() {
  const [backendResponse, setBackendResponse] = useState("Nothing");

  useEffect(() => {
    const api = async () => {
      try {
        const res = await fetch("/api/users/count");
        if (!res.ok) {
          throw new Error(`HTTP ${res.status}`);
        }
        const text = await res.text();
        setBackendResponse(text);
      } catch (error) {
        console.log("Backend not available:", error);
        setBackendResponse("Backend not available");
      }
    };
    api();
  }, []);

  return (
    <div className="app">
      <section className="hero">
        <div className="hero-container">
          <div className="hero-content">
            <h1 className="hero-title">
              Take Control of Your
              <span className="highlight"> Financial Future</span>
            </h1>
            <p className="hero-description">
              Manage your finances with confidence using our comprehensive financial management platform. 
              Track expenses, monitor investments, and make informed financial decisions.
            </p>
            <div className="hero-buttons">
              <a className="btn-primary" href="/register">Start Managing</a>
              <a className="btn-secondary" href="#features">Learn More</a>
            </div>
            <div className="hero-status">
              <p>Users registered: <span className="status-indicator">{backendResponse}</span></p>
            </div>
          </div>
          <div className="hero-image">
            <div className="financial-dashboard">
              <div className="dashboard-card">
                <h3>Portfolio Overview</h3>
                <div className="metric">$125,430</div>
                <div className="change positive">+5.2%</div>
              </div>
              <div className="dashboard-card">
                <h3>Monthly Budget</h3>
                <div className="metric">$3,200</div>
                <div className="progress-bar">
                  <div className="progress" style={{width: '75%'}}></div>
                </div>
              </div>
              <div className="dashboard-card">
                <h3>Total users</h3>
                <div className="metric">{backendResponse}</div>
                <div className="change positive">+12.3%</div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section id="features" className="features">
        <div className="container">
          <h2 className="section-title">Why Choose Financial Manager?</h2>
          <div className="features-grid">
            <div className="feature-card">
              <div className="feature-icon">📊</div>
              <h3>Real-time Analytics</h3>
              <p>Get instant insights into your financial health with comprehensive analytics and reporting tools.</p>
            </div>
            <div className="feature-card">
              <div className="feature-icon">💳</div>
              <h3>Expense Tracking</h3>
              <p>Easily categorize and track your expenses to understand where your money goes each month.</p>
            </div>
            <div className="feature-card">
              <div className="feature-icon">🎯</div>
              <h3>Goal Setting</h3>
              <p>Set and track financial goals with personalized recommendations and progress monitoring.</p>
            </div>
            <div className="feature-card">
              <div className="feature-icon">🔒</div>
              <h3>Secure & Private</h3>
              <p>Your financial data is protected with bank-level security and privacy controls.</p>
            </div>
            <div className="feature-card">
              <div className="feature-icon">📱</div>
              <h3>Mobile Access</h3>
              <p>Access your financial information anywhere with our responsive mobile-friendly interface.</p>
            </div>
            <div className="feature-card">
              <div className="feature-icon">🤖</div>
              <h3>Smart Insights</h3>
              <p>AI-powered recommendations help you make better financial decisions and optimize your spending.</p>
            </div>
          </div>
        </div>
      </section>

      <section className="cta">
        <div className="container">
          <div className="cta-content">
            <h2>Ready to Transform Your Financial Life?</h2>
            <p>Join thousands of users who have already taken control of their finances.</p>
            <a className="btn-primary btn-large" href="/register">Get Started Today</a>
          </div>
        </div>
      </section>

      
    </div>
  );
}

export default Home;


