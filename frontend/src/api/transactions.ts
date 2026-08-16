import { api } from './client'
import type { CreateTransactionDto, PageResponse, TransactionResponseDto, TransactionType } from './types'

export type TransactionSortField = 'date' | 'amount' | 'id'

export type SortDirection = 'asc' | 'desc'

export type TransactionSort = `${TransactionSortField},${SortDirection}`

export interface TransactionQuery {
  page?: number
  size?: number
  sort?: TransactionSort
  from?: string
  to?: string
  categoryId?: number
  type?: TransactionType
}

const TRANSACTIONS_PATH = '/api/transactions'

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

function buildQueryString(query: TransactionQuery): string {
  const params = new URLSearchParams()
  appendParam(params, 'page', query.page)
  appendParam(params, 'size', query.size)
  appendParam(params, 'sort', query.sort)
  appendParam(params, 'from', query.from)
  appendParam(params, 'to', query.to)
  appendParam(params, 'categoryId', query.categoryId)
  appendParam(params, 'type', query.type)

  const serialized = params.toString()
  return serialized.length === 0 ? '' : `?${serialized}`
}

export function fetchTransactions(
  query: TransactionQuery = {},
  signal?: AbortSignal,
): Promise<PageResponse<TransactionResponseDto>> {
  return api.get<PageResponse<TransactionResponseDto>>(
    `${TRANSACTIONS_PATH}${buildQueryString(query)}`,
    { signal },
  )
}

export function createTransaction(payload: CreateTransactionDto): Promise<TransactionResponseDto> {
  return api.post<TransactionResponseDto>(TRANSACTIONS_PATH, payload)
}

export function updateTransaction(
  id: number,
  payload: CreateTransactionDto,
): Promise<TransactionResponseDto> {
  return api.put<TransactionResponseDto>(`${TRANSACTIONS_PATH}/${id}`, payload)
}

export function deleteTransaction(id: number): Promise<void> {
  return api.delete<void>(`${TRANSACTIONS_PATH}/${id}`)
}
