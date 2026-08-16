import { api } from './client'
import type { BudgetResponseDto, CreateBudgetDto, PageResponse } from './types'

export type BudgetSortField = 'month' | 'amount' | 'id'

export type SortDirection = 'asc' | 'desc'

export type BudgetSort = `${BudgetSortField},${SortDirection}`

export interface BudgetQuery {
  page?: number
  size?: number
  sort?: BudgetSort
  month?: string
  categoryId?: number
}

const BUDGETS_PATH = '/api/budgets'

function appendParam(
  params: URLSearchParams,
  name: string,
  value: string | number | undefined,
): void {
  if (value === undefined) {
    return
  }
  if (typeof value === 'number') {
    if (!Number.isFinite(value)) {
      return
    }
    params.set(name, String(value))
    return
  }
  if (value.trim().length === 0) {
    return
  }
  params.set(name, value)
}

function buildQueryString(query: BudgetQuery): string {
  const params = new URLSearchParams()
  appendParam(params, 'page', query.page)
  appendParam(params, 'size', query.size)
  appendParam(params, 'sort', query.sort)
  appendParam(params, 'month', query.month)
  appendParam(params, 'categoryId', query.categoryId)

  const serialized = params.toString()
  return serialized.length === 0 ? '' : `?${serialized}`
}

export function fetchBudgets(
  query: BudgetQuery = {},
  signal?: AbortSignal,
): Promise<PageResponse<BudgetResponseDto>> {
  return api.get<PageResponse<BudgetResponseDto>>(
    `${BUDGETS_PATH}${buildQueryString(query)}`,
    { signal },
  )
}

export function createBudget(payload: CreateBudgetDto): Promise<BudgetResponseDto> {
  return api.post<BudgetResponseDto>(BUDGETS_PATH, payload)
}

export function updateBudget(id: number, payload: CreateBudgetDto): Promise<BudgetResponseDto> {
  return api.put<BudgetResponseDto>(`${BUDGETS_PATH}/${id}`, payload)
}

export function deleteBudget(id: number): Promise<void> {
  return api.delete<void>(`${BUDGETS_PATH}/${id}`)
}
