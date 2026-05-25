import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Account } from '../models/models';

@Injectable({ providedIn: 'root' })
export class AccountService {
  private http = inject(HttpClient);
  private readonly API = '/api/v1/accounts';

  getMyAccounts() {
    return this.http.get<Account[]>(`${this.API}/me`);
  }

  getAccount(uuid: string) {
    return this.http.get<Account>(`${this.API}/${uuid}`);
  }

  getBalance(uuid: string) {
    return this.http.get<{ balance: number }>(`${this.API}/${uuid}/balance`);
  }

  openAccount(currency: string = 'USD') {
    return this.http.post<Account>(this.API, { currency });
  }
}
