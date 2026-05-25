import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Transaction, TransferRequest } from '../models/models';

@Injectable({ providedIn: 'root' })
export class TransactionService {
  private http = inject(HttpClient);
  private readonly API = '/api/v1/transactions';

  transfer(req: TransferRequest) {
    return this.http.post<Transaction>(this.API, req);
  }

  getMyTransactions(page = 0, size = 20) {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<{ content: Transaction[]; totalElements: number }
    >(`${this.API}/by-account/${accountUuid}`, { params });
  }

  getTransaction(uuid: string) {
    return this.http.get<Transaction>(`${this.API}/${uuid}`);
  }
}
