import { useCallback, useEffect, useState, type ChangeEvent, type FormEvent } from 'react';
import {
  createCategory,
  deleteCategory,
  fetchCategories,
  updateCategory
} from '../api/categories';
import { isAbortError, toFormError } from '../api/ApiError';
import FieldError from './FieldError';
import type { CategoryResponseDto, CreateCategoryDto } from '../api/types';

const EMPTY_FORM: CreateCategoryDto = { name: '', description: '' };

function CategoriesTab() {
  const [categories, setCategories] = useState<CategoryResponseDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [editingCategory, setEditingCategory] = useState<CategoryResponseDto | null>(null);
  const [formData, setFormData] = useState<CreateCategoryDto>(EMPTY_FORM);
  const [formError, setFormError] = useState('');
  const [formFieldErrors, setFormFieldErrors] = useState<Record<string, string>>({});
  const [formSuccess, setFormSuccess] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const loadCategories = useCallback(async (signal: AbortSignal) => {
    try {
      const data = await fetchCategories(signal);
      setCategories(data);
      setError('');
    } catch (err) {
      if (isAbortError(err)) {
        return;
      }
      setError(toFormError(err).message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    void loadCategories(controller.signal);
    return () => controller.abort();
  }, [loadCategories]);

  const handleInputChange = (e: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    setFormError('');
  };

  const resetForm = () => {
    setFormData(EMPTY_FORM);
    setFormError('');
    setFormFieldErrors({});
    setShowCreateForm(false);
    setEditingCategory(null);
  };

  const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setFormError('');
    setFormFieldErrors({});
    setFormSuccess('');

    if (!formData.name.trim() || !formData.description.trim()) {
      setFormError('Both name and description are required');
      return;
    }

    setSubmitting(true);
    try {
      if (editingCategory) {
        const updated = await updateCategory(editingCategory.id, formData);
        setCategories((prev) => prev.map((cat) => (cat.id === updated.id ? updated : cat)));
        setFormSuccess('Category updated successfully');
      } else {
        const created = await createCategory(formData);
        setCategories((prev) => [...prev, created]);
        setFormSuccess('Category created successfully');
      }
      resetForm();
    } catch (err) {
      const parsed = toFormError(err);
      setFormError(parsed.message);
      setFormFieldErrors(parsed.fieldErrors);
    } finally {
      setSubmitting(false);
    }
  };

  const handleEdit = (category: CategoryResponseDto) => {
    setEditingCategory(category);
    setFormData({ name: category.name, description: category.description });
    setFormError('');
    setFormFieldErrors({});
    setShowCreateForm(true);
  };

  const handleDelete = async (categoryId: number) => {
    if (!window.confirm('Are you sure you want to delete this category?')) {
      return;
    }

    setFormError('');
    setFormSuccess('');
    try {
      await deleteCategory(categoryId);
      setCategories((prev) => prev.filter((cat) => cat.id !== categoryId));
      setFormSuccess('Category deleted successfully');
    } catch (err) {
      setFormError(toFormError(err).message);
    }
  };

  if (loading) {
    return (
      <div className="tab-content">
        <h2>Categories</h2>
        <div className="loading" role="status">Loading categories...</div>
      </div>
    );
  }

  return (
    <div className="tab-content">
      <div className="categories-header">
        <h2>Categories</h2>
        <button className="btn-primary" onClick={() => setShowCreateForm(true)}>
          Add New Category
        </button>
      </div>

      {error && <div className="auth-error" role="alert">{error}</div>}
      {formError && <div className="auth-error" role="alert">{formError}</div>}
      {formSuccess && <div className="auth-success" role="status">{formSuccess}</div>}

      {showCreateForm && (
        <div className="category-form-section">
          <h3>{editingCategory ? 'Edit Category' : 'Create New Category'}</h3>
          <form onSubmit={handleSubmit} className="category-form" noValidate>
            <div className="form-group">
              <label htmlFor="name">Category Name:</label>
              <input
                type="text"
                id="name"
                name="name"
                value={formData.name}
                onChange={handleInputChange}
                placeholder="Enter category name"
                aria-invalid={Boolean(formFieldErrors.name)}
                required
              />
              <FieldError message={formFieldErrors.name} />
            </div>
            <div className="form-group">
              <label htmlFor="description">Description:</label>
              <textarea
                id="description"
                name="description"
                value={formData.description}
                onChange={handleInputChange}
                placeholder="Enter category description"
                rows={3}
                aria-invalid={Boolean(formFieldErrors.description)}
                required
              />
              <FieldError message={formFieldErrors.description} />
            </div>
            <div className="form-actions">
              <button type="submit" className="btn-primary" disabled={submitting}>
                {submitting ? 'Saving...' : editingCategory ? 'Update Category' : 'Create Category'}
              </button>
              <button type="button" className="btn-secondary" onClick={resetForm} disabled={submitting}>
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
                      aria-label={`Edit ${category.name}`}
                      title="Edit category"
                    >
                      ✏️
                    </button>
                    <button
                      className="btn-delete"
                      onClick={() => handleDelete(category.id)}
                      aria-label={`Delete ${category.name}`}
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
