# FinTrack UI

Angular 17 + Tailwind CSS frontend for the FinTrack microservices platform.

## Prerequisites

- Node.js 18+
- Angular CLI 17: `npm install -g @angular/cli@17`
- FinTrack backend running at `localhost:8080`

## Setup

```bash
# Install dependencies
npm install

# Start dev server (proxies /api to localhost:8080)
npm start

# Open browser
http://localhost:4200
```

## Project structure

```
src/app/
├── core/
│   ├── guards/          # AuthGuard — redirects to /login if no JWT
│   ├── interceptors/    # JWT interceptor + error interceptor (401 → logout)
│   ├── models/          # TypeScript interfaces for all API responses
│   └── services/        # AuthService, AccountService, TransactionService
├── features/
│   ├── auth/            # Login + Register components
│   ├── dashboard/       # Balance overview + recent transactions
│   ├── transfer/        # Transfer form + live saga status
│   ├── history/         # Paginated transaction history with filters
│   └── admin/           # Service health + platform metrics
└── shared/
    └── layout/          # Nav bar + router outlet wrapper
```

## API routes (via gateway at localhost:8080)

| Feature | Method | Endpoint |
|---------|--------|----------|
| Login | POST | /api/v1/auth/login |
| Register | POST | /api/v1/auth/register |
| Get accounts | GET | /api/v1/accounts/my |
| Transfer | POST | /api/v1/transactions |
| Transaction history | GET | /api/v1/transactions/my |
| Health | GET | /actuator/health |

## Production build

```bash
npm run build:prod
```

Output in `dist/fintrack-ui/` — serve with nginx or copy to Oracle Cloud VM.
