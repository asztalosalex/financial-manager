import { useState, type SyntheticEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import ProfileSidebar from '../components/ProfileSidebar';
import CategoriesTab from '../components/CategoriesTab';
import ChangePasswordForm from '../components/ChangePasswordForm';
import TransactionsPlaceholder from '../components/TransactionsPlaceholder';
import FieldError from '../components/FieldError';
import { deleteAccount, updateProfile } from '../api/users';
import { toFormError } from '../api/ApiError';
import { useAuth } from '../auth/useAuth';
import type { UpdateProfileDto } from '../api/types';

type ProfileTab = 'profile' | 'transactions' | 'categories';

function formatDate(value: string | null): string {
  return value ? new Date(value).toLocaleDateString() : 'Unknown';
}

function Profile() {
  const navigate = useNavigate();
  const { user, setUser, logout, clearSession } = useAuth();
  const [activeTab, setActiveTab] = useState<ProfileTab>('profile');
  const [isEditing, setIsEditing] = useState(false);
  const [editData, setEditData] = useState<UpdateProfileDto>({ username: '', email: '' });
  const [profileError, setProfileError] = useState('');
  const [profileFieldErrors, setProfileFieldErrors] = useState<Record<string, string>>({});
  const [profileSuccess, setProfileSuccess] = useState('');
  const [isSavingProfile, setIsSavingProfile] = useState(false);
  const [isDeletingAccount, setIsDeletingAccount] = useState(false);

  if (!user) {
    return null;
  }

  const startEditing = () => {
    setProfileError('');
    setProfileFieldErrors({});
    setProfileSuccess('');
    setEditData({ username: user.username ?? '', email: user.email ?? '' });
    setIsEditing(true);
  };

  const cancelEditing = () => {
    setIsEditing(false);
    setProfileError('');
    setProfileFieldErrors({});
    setEditData({ username: '', email: '' });
  };

  const handleProfileUpdate = async (e: SyntheticEvent) => {
    e.preventDefault();
    setProfileError('');
    setProfileFieldErrors({});
    setProfileSuccess('');
    setIsSavingProfile(true);
    try {
      const updated = await updateProfile(user.id, editData);
      setUser(updated);
      setIsEditing(false);
      setProfileSuccess('Profile updated successfully.');
    } catch (err) {
      const formError = toFormError(err);
      setProfileError(formError.message);
      setProfileFieldErrors(formError.fieldErrors);
    } finally {
      setIsSavingProfile(false);
    }
  };

  const handleDeleteAccount = async () => {
    if (!window.confirm('This permanently deletes your account. Continue?')) {
      return;
    }
    setProfileError('');
    setIsDeletingAccount(true);
    try {
      await deleteAccount(user.id);
      clearSession();
      navigate('/', { replace: true });
    } catch (err) {
      setProfileError(toFormError(err).message);
    } finally {
      setIsDeletingAccount(false);
    }
  };

  const renderProfileTab = () => (
    <div className="tab-content">
      <div className="profile-content-top flex-between">
        <h2>Profile Data</h2>
        {!isEditing ? (
          <button className="btn-secondary" onClick={startEditing}>
            Edit Profile
          </button>
        ) : (
          <div className="flex-gap-2">
            <button className="btn-secondary" onClick={cancelEditing} disabled={isSavingProfile}>
              Cancel
            </button>
            <button className="btn-primary" onClick={handleProfileUpdate} disabled={isSavingProfile}>
              {isSavingProfile ? 'Saving...' : 'Save Changes'}
            </button>
          </div>
        )}
      </div>

      {profileError && <div className="auth-error" role="alert">{profileError}</div>}
      {profileSuccess && <div className="auth-success" role="status">{profileSuccess}</div>}

      <div className="profile-info">
        {isEditing ? (
          <form onSubmit={handleProfileUpdate} noValidate>
            <div className="profile-field">
              <label htmlFor="profile-email">Email:</label>
              <input
                type="email"
                id="profile-email"
                value={editData.email}
                onChange={(e) => setEditData({ ...editData, email: e.target.value })}
                className="form-input"
                aria-invalid={Boolean(profileFieldErrors.email)}
                required
              />
              <FieldError message={profileFieldErrors.email} />
            </div>
            <div className="profile-field">
              <label htmlFor="profile-username">Username:</label>
              <input
                type="text"
                id="profile-username"
                value={editData.username}
                onChange={(e) => setEditData({ ...editData, username: e.target.value })}
                className="form-input"
                aria-invalid={Boolean(profileFieldErrors.username)}
                required
              />
              <FieldError message={profileFieldErrors.username} />
            </div>
          </form>
        ) : (
          <>
            <div className="profile-field">
              <label>Email:</label>
              <span>{user.email}</span>
            </div>
            <div className="profile-field">
              <label>Username:</label>
              <span>{user.username || 'Not provided'}</span>
            </div>
          </>
        )}
        <div className="profile-field">
          <label>Member since:</label>
          <span>{formatDate(user.createdAt)}</span>
        </div>
      </div>

      <div className="profile-section">
        <h3>Change Password</h3>
        <ChangePasswordForm />
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

  const renderTabContent = () => {
    switch (activeTab) {
      case 'profile':
        return renderProfileTab();
      case 'transactions':
        return <TransactionsPlaceholder />;
      case 'categories':
        return <CategoriesTab />;
      default:
        return null;
    }
  };

  return (
    <div className="profile-page">
      <div className="profile-layout">
        <ProfileSidebar activeTab={activeTab} onTabChange={setActiveTab} onLogout={logout} />
        <div className="profile-main">
          <div className="profile-header">
            <h1>Profile</h1>
            <p>Last login: {formatDate(user.lastLogin)}</p>
          </div>
          <div className="profile-content">{renderTabContent()}</div>
        </div>
      </div>
    </div>
  );
}

export default Profile;
