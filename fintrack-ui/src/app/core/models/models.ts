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
  accountNumber: string;
  balance: number;
  currency: string;
  type: string;
  status: string;
  userUuid: string;
}

export interface Transaction {
  uuid: string;
  sourceAccountUuid: string;
  destinationAccountUuid: string;
  amount: number;
  currency: string;
  description: string;
  status: 'INITIATED' | 'COMPLETED' | 'FAILED' | 'COMPENSATED';
  riskLevel: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  feeAmount: number;
  createdAt: string;
}

export interface TransferRequest {
  sourceAccountUuid: string;
  destinationAccountUuid: string;
  amount: number;
  currency: string;
  description: string;
}
