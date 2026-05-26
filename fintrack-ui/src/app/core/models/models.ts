export interface LoginRequest {
  usernameOrEmail: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
  firstName: string;
  lastName: string;
}

export interface UserInfo {
  uuid: string;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  status: string;
  roles: string[];
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresAt: string;
  user: UserInfo;
}

export interface Account {
  uuid: string;
  userUuid: string;
  balance: number;
  currencyCode: string;
  type: string;
  status: string;
  createdAt: string;
  updatedAt: string;
  // computed
  currency?: string;
  type?: string;
  accountNumber?: string;
}

export interface Transaction {
  uuid: string;
  fromAccountUuid: string;
  toAccountUuid: string;
  amount: number;
  fee: number;
  currencyCode: string;
  type: string;
  type: string;
  status: 'INITIATED' | 'COMPLETED' | 'FAILED' | 'COMPENSATED';
  riskLevel?: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  description: string;
  createdAt: string;
  updatedAt: string;
  // aliases for backward compat
  feeAmount?: number;
}

export interface TransferRequest {
  fromAccountUuid: string;
  toAccountUuid: string;
  amount: number;
  currencyCode: string;
  type: string;
  description: string;
}

export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}
