import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import ProfileSidebar from '../components/ProfileSidebar';
import CategoriesTab from '../components/CategoriesTab';

function Profile() {
  const navigate = useNavigate();
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [activeTab, setActiveTab] = useState('profile');
  const [isEditing, setIsEditing] = useState(false);
  const [editData, setEditData] = useState({
    username: '',
    email: ''
  });
  const [passwordData, setPasswordData] = useState({
    currentPassword: '',
    newPassword: '',
    confirmPassword: ''
  });
  const [passwordError, setPasswordError] = useState('');
  const [passwordSuccess, setPasswordSuccess] = useState('');
  const [isChangingPassword, setIsChangingPassword] = useState(false);
  const [isDeletingAccount, setIsDeletingAccount] = useState(false);
  useEffect(() => {
    const fetchUserProfile = async () => {
      

      try {
        const res = await fetch('/api/users/profile', {
          credentials: 'include'
        });

        if (!res.ok) {
          if (res.status === 401) {
            navigate('/login');
            return;
          }
          
          throw new Error('You need to login first');
        }

        const userData = await res.json();
        setUser(userData);
      } catch (err) {
        setError(err.message);
      } finally {
        setLoading(false);
      }
    };

    fetchUserProfile();
  }, [navigate]);

  const handleLogout = () => {
    // Dispatch custom event to notify header of auth state change
    window.dispatchEvent(new CustomEvent('authStateChanged'));
    navigate('/');
  };

  const handleEditClick = () => {
    setIsEditing(true);
    setEditData({
      username: user?.username || '',
      email: user?.email || ''
    });
  };

  const handleCancelEdit = () => {
    setIsEditing(false);
    setEditData({
      username: '',
      email: ''
    });
  };

  const handlePasswordChange = (e) => {
    const { name, value } = e.target;
    setPasswordData(prev => ({
      ...prev,
      [name]: value
    }));
    // Clear errors when user starts typing
    if (passwordError) setPasswordError('');
    if (passwordSuccess) setPasswordSuccess('');
  };

  const validatePasswordForm = () => {
    const { currentPassword, newPassword, confirmPassword } = passwordData;

    if (!currentPassword || !newPassword || !confirmPassword) {
      setPasswordError('All fields are required');
      return false;
    }

    if (newPassword.length < 6) {
      setPasswordError('New password must be at least 6 characters long');
      return false;
    }

    if (newPassword !== confirmPassword) {
      setPasswordError('New passwords do not match');
      return false;
    }

    if (currentPassword === newPassword) {
      setPasswordError('New password must be different from current password');
      return false;
    }

    return true;
  };

  const handlePasswordSubmit = async (e) => {
    e.preventDefault();
    
    if (!validatePasswordForm()) {
      return;
    }

    setIsChangingPassword(true);
    setPasswordError('');
    setPasswordSuccess('');

    try {
      const response = await fetch('/api/users/change-password', {
        method: 'POST',
        credentials: 'include',
        body: JSON.stringify({
          currentPassword: passwordData.currentPassword,
          newPassword: passwordData.newPassword
        })
      });

      if (!response.ok) {
        if (response.status === 401) {
          setPasswordError('Current password is incorrect');
        } else if (response.status === 400) {
          const errorData = await response.json();
          setPasswordError(errorData.message || 'Invalid password data');
        } else {
          setPasswordError('Failed to change password. Please try again.');
        }
        return;
      }

      // Success
      setPasswordSuccess('Password changed successfully!');
      setPasswordData({
        currentPassword: '',
        newPassword: '',
        confirmPassword: ''
      });

    } catch (error) {
      console.error('Password change error:', error);
      setPasswordError('Network error. Please try again.');
    } finally {
      setIsChangingPassword(false);
    }
  };

  const handleProfileUpdate = async (e) => {
    e.preventDefault();
    
    try {
      const response = await fetch(`/api/users/${user.id}`, {
        method: 'PUT',
        credentials: 'include',
        body: JSON.stringify(editData)
      });
      if (response.ok) {
        const updatedUser = await response.json();
        setUser(updatedUser);
        setIsEditing(false);
        setPasswordSuccess('Profile updated successfully!');
        setTimeout(() => setPasswordSuccess(''), 3000);
      } else {
        const errorData = await response.json();
        setPasswordError(errorData.message || 'Failed to update profile');
      }
    } catch {
      setPasswordError('Network error occurred');
    }
  };

  const handleDeleteAccount = async () => {
    setIsDeletingAccount(true);
    try {
      const userId = user.id;
      const response = await fetch(`/api/users/${userId}`, {
        method: 'DELETE',
        credentials: 'include'
      });
      if (!response.ok) {
        if (response.status === 401) {
          navigate('/login');
          return;
        } else if (response.status === 403) {
          const errorData = await response.text();
          throw new Error(errorData || 'You can only delete your own account.');
        } else {
          throw new Error('Failed to delete account. Please try again.');
        }
      }
      navigate('/');
    } catch (error) {
      console.error('Delete account error:', error);
      setError(error.message || 'Failed to delete account. Please try again.');
    } finally {
      setIsDeletingAccount(false);
    }
  };

  const renderTabContent = () => {
    switch (activeTab) {
      case 'profile':
        return (
          <div className="tab-content">
            <div className="profile-content-top flex-between">
              <h2>Profile Data</h2>
              {!isEditing ? (
                <button className="btn-secondary" onClick={handleEditClick}>
                  Edit Profile
                </button>
              ) : (
                <div className="flex-gap-2">
                  <button className="btn-secondary" onClick={handleCancelEdit}>
                    Cancel
                  </button>
                  <button className="btn-primary" onClick={handleProfileUpdate}>
                    Save Changes
                  </button>
                </div>
              )}
            </div>
            {user && (
              <div className="profile-info">
                {isEditing ? (
                  <form onSubmit={handleProfileUpdate}>
                    <div className="profile-field">
                      <label>Email:</label>
                      <input
                        type="email"
                        value={editData.email}
                        onChange={(e) => setEditData({...editData, email: e.target.value})}
                        className="form-input"
                        required
                      />
                    </div>
                    <div className="profile-field">
                      <label>Username:</label>
                      <input
                        type="text"
                        value={editData.username}
                        onChange={(e) => setEditData({...editData, username: e.target.value})}
                        className="form-input"
                        required
                      />
                    </div>
                  </form>
                ) : (
                  // Meglévő read-only megjelenítés
                  <div className="profile-field">
                    <label>Email:</label>
                    <span>{user.email}</span>
                  </div>
                )}
                <div className="profile-field">
                  <label>Username:</label>
                  <span>{user.username || 'Not provided'}</span>
                </div>
                <div className="profile-field">
                  <label>Member since:</label>
                  <span>{user.createdAt ? new Date(user.createdAt).toLocaleDateString() : 'Unknown'}</span>
                </div>
              </div>
            )}
            
            <div className="profile-section">
              <h3>Change Password</h3>
              <form onSubmit={handlePasswordSubmit} className="security-form">
                {passwordError && (
                  <div className="auth-error">{passwordError}</div>
                )}
                {passwordSuccess && (
                  <div className="auth-success">{passwordSuccess}</div>
                )}
                <div className="form-group">
                  <label>Current password:</label>
                  <input 
                    type="password" 
                    name="currentPassword"
                    value={passwordData.currentPassword}
                    onChange={handlePasswordChange}
                    className="form-input" 
                    placeholder="Enter your current password"
                    disabled={isChangingPassword}
                    required
                  />
                </div>
                <div className="form-group">
                  <label>New password:</label>
                  <input 
                    type="password" 
                    name="newPassword"
                    value={passwordData.newPassword}
                    onChange={handlePasswordChange}
                    className="form-input" 
                    placeholder="Enter your new password"
                    disabled={isChangingPassword}
                    required
                    minLength="6"
                  />
                </div>
                <div className="form-group">
                  <label>Confirm new password:</label>
                  <input 
                    type="password" 
                    name="confirmPassword"
                    value={passwordData.confirmPassword}
                    onChange={handlePasswordChange}
                    className="form-input" 
                    placeholder="Confirm your new password"
                    disabled={isChangingPassword}
                    required
                  />
                </div>
                <div className="profile-actions">
                  <button 
                    type="submit" 
                    className="btn-primary"
                    disabled={isChangingPassword}
                  >
                    {isChangingPassword ? 'Changing Password...' : 'Change Password'}
                  </button>
                </div>
              </form>
              <button 
                className="sidebar-logout-btn" 
                onClick={handleDeleteAccount}
                disabled={isDeletingAccount}
              >
                {isDeletingAccount ? 'Deleting Account...' : 'Delete Account'}
              </button>
            </div>
          </div>
        );
      case 'transactions':
        return (
          <div className="tab-content">
            <h2>Income & Expenses</h2>
            
            <div className="financial-overview">
              <div className="overview-cards">
                <div className="overview-card income">
                  <div className="card-icon">📈</div>
                  <div className="card-content">
                    <h3>Total Income</h3>
                    <div className="card-amount">$2,450.00</div>
                    <div className="card-period">This month</div>
                  </div>
                </div>
                <div className="overview-card expense">
                  <div className="card-icon">📉</div>
                  <div className="card-content">
                    <h3>Total Expenses</h3>
                    <div className="card-amount">$1,890.50</div>
                    <div className="card-period">This month</div>
                  </div>
                </div>
                <div className="overview-card balance">
                  <div className="card-icon">💰</div>
                  <div className="card-content">
                    <h3>Net Balance</h3>
                    <div className="card-amount positive">$559.50</div>
                    <div className="card-period">This month</div>
                  </div>
                </div>
              </div>
            </div>

            <div className="transactions-section">
              <div className="section-header">
                <h3>Recent Transactions</h3>
                <div className="filter-controls">
                  <select className="filter-select">
                    <option value="all">All Transactions</option>
                    <option value="income">Income Only</option>
                    <option value="expense">Expenses Only</option>
                  </select>
                  <button className="btn-secondary">Add Transaction</button>
                </div>
              </div>
              
              <div className="transactions-list">
                <div className="transaction-item income">
                  <div className="transaction-icon">💼</div>
                  <div className="transaction-details">
                    <div className="transaction-title">Salary</div>
                    <div className="transaction-category">Work</div>
                    <div className="transaction-date">Dec 15, 2024</div>
                  </div>
                  <div className="transaction-amount positive">+$2,500.00</div>
                </div>
                
                <div className="transaction-item expense">
                  <div className="transaction-icon">🛒</div>
                  <div className="transaction-details">
                    <div className="transaction-title">Grocery Shopping</div>
                    <div className="transaction-category">Food</div>
                    <div className="transaction-date">Dec 14, 2024</div>
                  </div>
                  <div className="transaction-amount negative">-$85.50</div>
                </div>
                
                <div className="transaction-item expense">
                  <div className="transaction-icon">⛽</div>
                  <div className="transaction-details">
                    <div className="transaction-title">Gas Station</div>
                    <div className="transaction-category">Transportation</div>
                    <div className="transaction-date">Dec 13, 2024</div>
                  </div>
                  <div className="transaction-amount negative">-$45.00</div>
                </div>
                
                <div className="transaction-item income">
                  <div className="transaction-icon">🎁</div>
                  <div className="transaction-details">
                    <div className="transaction-title">Freelance Work</div>
                    <div className="transaction-category">Work</div>
                    <div className="transaction-date">Dec 12, 2024</div>
                  </div>
                  <div className="transaction-amount positive">+$300.00</div>
                </div>
                
                <div className="transaction-item expense">
                  <div className="transaction-icon">🍕</div>
                  <div className="transaction-details">
                    <div className="transaction-title">Restaurant</div>
                    <div className="transaction-category">Food</div>
                    <div className="transaction-date">Dec 11, 2024</div>
                  </div>
                  <div className="transaction-amount negative">-$32.75</div>
                </div>
              </div>
            </div>
          </div>
        );
      case 'categories':
        return <CategoriesTab />;
      default:
        return null;
    }
  };

  if (loading) {
    return (
      <div className="profile-page">
        <div className="profile-loading">
          <div>Loading...</div>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="profile-page">
        <div className="profile-error">
          <div className="auth-error">{error}</div>
          <button onClick={() => window.location.reload()} className="btn-primary">
            Retry
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="profile-page">
      <div className="profile-layout">
        <ProfileSidebar activeTab={activeTab} onTabChange={setActiveTab} onLogout={handleLogout} />
        <div className="profile-main">
          <div className="profile-header">
            <h1>Profile</h1>
            <p>Last login: {user.lastLogin ? new Date(user.lastLogin).toLocaleDateString() : 'Unknown'}</p>
          </div>
          <div className="profile-content">
            {renderTabContent()}
          </div>
        </div>
      </div>
    </div>
  );
}

export default Profile;
