# Payment Processing System Frontend

React + Vite frontend for the Spring Boot payment processing backend.

## Features

- Payment initiation with client-side validation
- Transactions list with status filters and retry support
- Customer directory with account lookup and transaction history
- Support dashboard with metrics and operational filters
- Shared loading, empty, and error states

## Prerequisites

- Node.js 18+
- npm 9+
- Backend running on `http://localhost:8080`

## Development

```powershell
cd "C:\Users\Administrator\Downloads\107-InfiniteLoopers-PaymentProcessingSystem-main\107-InfiniteLoopers-PaymentProcessingSystem\frontend"
npm install
npm run dev
```

Open the frontend at:

```text
http://localhost:3000
```

## Backend integration

- The app uses `VITE_API_BASE_URL`, which defaults to `http://localhost:8080/api`
- `vite.config.js` also contains a local `/api` proxy if you later prefer relative API paths during development
- The Vite dev server binds to local development by default and listens on port `3000`
- If you change frontend or preview ports, update backend CORS accordingly

## Production build

```powershell
npm run build
npm run preview
```

Preview URL:

```text
http://localhost:4173
```

## Run Backend + Frontend Together

1. Start backend in local mode (default):

```powershell
cd "C:\Users\Administrator\Downloads\107-InfiniteLoopers-PaymentProcessingSystem-main\107-InfiniteLoopers-PaymentProcessingSystem"
.\mvnw.cmd spring-boot:run
```

2. Start frontend:

```powershell
cd "C:\Users\Administrator\Downloads\107-InfiniteLoopers-PaymentProcessingSystem-main\107-InfiniteLoopers-PaymentProcessingSystem\frontend"
npm install
npm run dev
```

3. Open:

```text
Frontend: http://localhost:3000
Swagger:  http://localhost:8080/swagger-ui.html
```

## Test High-Value Email Notification (>10000)

By default, local profile disables outbound mail. To test real email, run backend in `mysql` profile with mail env vars:

```powershell
cd "C:\Users\Administrator\Downloads\107-InfiniteLoopers-PaymentProcessingSystem-main\107-InfiniteLoopers-PaymentProcessingSystem"
$env:SPRING_PROFILES_ACTIVE="mysql"
$env:MAIL_USERNAME="your@gmail.com"
$env:MAIL_PASSWORD="your-gmail-app-password"
$env:MAIL_FROM="your@gmail.com"
.\mvnw.cmd spring-boot:run
```

Then from frontend `Payments` page submit a payment with amount greater than `10000`.

Expected backend log line:

```text
High-value transaction notification sent for: <transaction-id>
```
