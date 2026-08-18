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

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}

export type TransactionType = 'INCOME' | 'EXPENSE'

export interface TransactionResponseDto {
  id: number
  type: TransactionType
  description: string | null
  categoryId: number
  categoryName: string
  amount: number
  date: string
  budgetWarning: BudgetStatusItem | null
}

export interface CreateTransactionDto {
  type: TransactionType
  description?: string
  categoryId: number
  amount: number
  date: string
}

export interface BudgetResponseDto {
  id: number
  amount: number
  month: string
  categoryId: number
  categoryName: string
}

export interface CreateBudgetDto {
  amount: number
  month: string
  categoryId: number
}

export interface ReportMetric {
  current: number
  previous: number
  deltaPercent: number | null
}

export interface SavingsRateMetric {
  current: number | null
  previous: number | null
  deltaPoints: number | null
}

export interface ReportsSummaryResponse {
  month: string
  previousMonth: string
  balance: ReportMetric
  income: ReportMetric
  expense: ReportMetric
  savingsRate: SavingsRateMetric
}

export interface CategoryReportItem {
  categoryId: number
  categoryName: string
  total: number
  percentage: number | null
}

export interface CategoryReportResponse {
  month: string
  total: number
  categories: CategoryReportItem[]
}

export interface TrendPoint {
  month: string
  income: number
  expense: number
}

export interface TrendReportResponse {
  month: string
  months: number
  points: TrendPoint[]
}

export interface BudgetStatusItem {
  categoryId: number
  categoryName: string
  budgeted: number
  spent: number
  remaining: number
  percentageUsed: number | null
}

export interface BudgetStatusResponse {
  month: string
  totalBudgeted: number
  totalSpent: number
  unbudgetedSpending: number
  categories: BudgetStatusItem[]
}
