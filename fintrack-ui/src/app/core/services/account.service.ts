import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable } from 'rxjs';
import { map, tap, shareReplay } from 'rxjs/operators';
import { Account } from '../models/models';

@Injectable({ providedIn: 'root' })
export class AccountService {
  private http = inject(HttpClient);
  private readonly API = '/api/v1/accounts';

  private accountCache$: Observable<Account[]> | null = null;
  private accountSubject = new BehaviorSubject<Account | null>(null);
  
  // Shared account state — components subscribe to this
  currentAccount$ = this.accountSubject.asObservable();

  getMyAccounts(): Observable<Account[]> {
    if (!this.accountCache$) {
      this.accountCache$ = this.http.get<Account>(`${this.API}/me`).pipe(
        tap(account => this.accountSubject.next(account)),
        map(account => [account])
      );
    }
    return this.accountCache$;
  }

  getCurrentAccount(): Account | null {
    return this.accountSubject.value;
  }

  clearCache() {
    this.accountCache$ = null;
    this.accountSubject.next(null);
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
