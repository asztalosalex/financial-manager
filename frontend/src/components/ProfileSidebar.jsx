function ProfileSidebar({ activeTab, onTabChange, onLogout }) {
  const menuItems = [
    { id: 'profile', label: 'Profile Data', icon: '👤' },
    { id: 'transactions', label: 'Income & Expenses', icon: '💰' },
    { id: 'categories', label: 'Categories', icon: '📂' }
  ];

  return (
    <div className="profile-sidebar">
      <div className="sidebar-header">
        <h3>Profile Menu</h3>
      </div>
      <nav className="sidebar-nav">
        <ul className="sidebar-menu">
          {menuItems.map((item) => (
            <li key={item.id} className="sidebar-item">
              <button
                className={`sidebar-link ${activeTab === item.id ? 'active' : ''}`}
                onClick={() => onTabChange(item.id)}
              >
                <span className="sidebar-icon">{item.icon}</span>
                <span className="sidebar-label">{item.label}</span>
              </button>
            </li>
          ))}
          <li className="sidebar-item sidebar-logout">
            <button
              className="sidebar-link sidebar-logout-btn"
              onClick={onLogout}
            >
              <span className="sidebar-icon">🚪</span>
              <span className="sidebar-label">Logout</span>
            </button>
          </li>
        </ul>
      </nav>
    </div>
  );
}

export default ProfileSidebar;
