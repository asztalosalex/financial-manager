import { api } from './client'
import type { CategoryResponseDto, CreateCategoryDto } from './types'

export function fetchCategories(signal?: AbortSignal): Promise<CategoryResponseDto[]> {
  return api.get<CategoryResponseDto[]>('/api/categories/user', { signal })
}

export function createCategory(payload: CreateCategoryDto): Promise<CategoryResponseDto> {
  return api.post<CategoryResponseDto>('/api/categories', payload)
}

export function updateCategory(
  categoryId: number,
  payload: CreateCategoryDto,
): Promise<CategoryResponseDto> {
  return api.put<CategoryResponseDto>(`/api/categories/${categoryId}`, payload)
}

export function deleteCategory(categoryId: number): Promise<void> {
  return api.delete<void>(`/api/categories/${categoryId}`)
}
