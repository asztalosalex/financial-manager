export interface ErrorResponse {
  timestamp?: string
  status?: number
  error?: string
  message?: string
  path?: string
  fieldErrors?: Record<string, string>
}

export interface UserResponseDto {
  id: number
  username: string
  email: string
  createdAt: string | null
  lastLogin: string | null
}

export interface RegisterUserDto {
  username: string
  email: string
  password: string
}

export interface LoginUserDto {
  email: string
  password: string
}

export interface LoginResponse {
  expiresIn: number | null
  message: string
}

export interface UpdateProfileDto {
  username: string
  email: string
}

export interface ChangePasswordRequestDto {
  currentPassword: string
  newPassword: string
}

export interface CategoryResponseDto {
  id: number
  name: string
  description: string
}

export interface CreateCategoryDto {
  name: string
  description: string
}

export type TransactionType = 'INCOME' | 'EXPENSE'

export interface TransactionResponseDto {
  id: number
  type: TransactionType
  description: string | null
  categoryId: number
  categoryName: string
  amount: string
  date: string
}

export interface CreateTransactionDto {
  type: TransactionType
  description?: string
  categoryId: number
  amount: string
  date: string
}

export interface BudgetResponseDto {
  id: number
  amount: string
  month: string
  categoryId: number
  categoryName: string
}

export interface CreateBudgetDto {
  amount: string
  month: string
  categoryId: number
}
