import { useState, useEffect } from 'react';

function CategoriesTab() {
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [editingCategory, setEditingCategory] = useState(null);
  const [formData, setFormData] = useState({
    name: '',
    description: ''
  });
  const [formError, setFormError] = useState('');
  const [formSuccess, setFormSuccess] = useState('');

  useEffect(() => {
    fetchCategories();
  }, []);

  const fetchCategories = async () => {
    try {
      const res = await fetch('/api/categories/user', {});

      if (!res.ok) {
        if (res.status === 401) {
          window.location.href = '/login';
          return;
        }
        throw new Error('Failed to fetch categories');
      }

      const data = await res.json();
      setCategories(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
    setFormError('');
  };

  const resetForm = () => {
    setFormData({ name: '', description: '' });
    setFormError('');
    setFormSuccess('');
    setShowCreateForm(false);
    setEditingCategory(null);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setFormError('');
    setFormSuccess('');

    if (!formData.name.trim() || !formData.description.trim()) {
      setFormError('Both name and description are required');
      return;
    }

    try {
      let url = '/api/categories';
      let method = 'POST';

      if (editingCategory) {
        url = `/api/categories/${editingCategory.id}`;
        method = 'PUT';
      }

      const res = await fetch(url, {
        method,
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(formData)
      });

      if (!res.ok) {
        if (res.status === 401) {
          window.location.href = '/login';
          return;
        }
        const errorData = await res.json();
        throw new Error(errorData.message || 'Failed to save category');
      }

      if (editingCategory) {
        setFormSuccess('Category updated successfully');
        // Update the category in the list
        setCategories(prev => prev.map(cat => 
          cat.id === editingCategory.id 
            ? { ...cat, name: formData.name, description: formData.description }
            : cat
        ));
      } else {
        const newCategory = await res.json();
        setCategories(prev => [...prev, newCategory]);
        setFormSuccess('Category created successfully');
      }

      resetForm();
    } catch (err) {
      setFormError(err.message);
    }
  };

  const handleEdit = (category) => {
    setEditingCategory(category);
    setFormData({
      name: category.name,
      description: category.description
    });
    setShowCreateForm(true);
  };

  const handleDelete = async (categoryId) => {
    if (!window.confirm('Are you sure you want to delete this category?')) {
      return;
    }

    try {
      const res = await fetch(`/api/categories/${categoryId}`, {
        method: 'DELETE'
      });

      if (!res.ok) {
        if (res.status === 401) {
          window.location.href = '/login';
          return;
        }
        throw new Error('Failed to delete category');
      }

      setCategories(prev => prev.filter(cat => cat.id !== categoryId));
      setFormSuccess('Category deleted successfully');
    } catch (err) {
      setFormError(err.message);
    }
  };

  if (loading) {
    return (
      <div className="tab-content">
        <h2>Categories</h2>
        <div className="loading">Loading categories...</div>
      </div>
    );
  }

  return (
    <div className="tab-content">
      <div className="categories-header">
        <h2>Categories</h2>
        <button 
          className="btn-primary"
          onClick={() => setShowCreateForm(true)}
        >
          Add New Category
        </button>
      </div>

      {error && (
        <div className="auth-error">{error}</div>
      )}

      {formError && (
        <div className="auth-error">{formError}</div>
      )}

      {formSuccess && (
        <div className="auth-success">{formSuccess}</div>
      )}

      {showCreateForm && (
        <div className="category-form-section">
          <h3>{editingCategory ? 'Edit Category' : 'Create New Category'}</h3>
          <form onSubmit={handleSubmit} className="category-form">
            <div className="form-group">
              <label htmlFor="name">Category Name:</label>
              <input
                type="text"
                id="name"
                name="name"
                value={formData.name}
                onChange={handleInputChange}
                placeholder="Enter category name"
                required
              />
            </div>
            <div className="form-group">
              <label htmlFor="description">Description:</label>
              <textarea
                id="description"
                name="description"
                value={formData.description}
                onChange={handleInputChange}
                placeholder="Enter category description"
                rows="3"
                required
              />
            </div>
            <div className="form-actions">
              <button type="submit" className="btn-primary">
                {editingCategory ? 'Update Category' : 'Create Category'}
              </button>
              <button 
                type="button" 
                className="btn-secondary"
                onClick={resetForm}
              >
                Cancel
              </button>
            </div>
          </form>
        </div>
      )}

      <div className="categories-list">
        <h3>Your Categories</h3>
        {categories.length === 0 ? (
          <div className="empty-state">
            <p>No categories found. Create your first category to get started!</p>
          </div>
        ) : (
          <div className="categories-grid">
            {categories.map((category) => (
              <div key={category.id} className="category-card">
                <div className="category-header">
                  <h4>{category.name}</h4>
                  <div className="category-actions">
                    <button 
                      className="btn-edit"
                      onClick={() => handleEdit(category)}
                      title="Edit category"
                    >
                      ✏️
                    </button>
                    <button 
                      className="btn-delete"
                      onClick={() => handleDelete(category.id)}
                      title="Delete category"
                    >
                      🗑️
                    </button>
                  </div>
                </div>
                <p className="category-description">{category.description}</p>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

export default CategoriesTab;
