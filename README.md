# Payment Processing System

Spring Boot + React/Vite payment system with Docker, Docker Compose, and Jenkins CI/CD support.

## Tech Stack

- Backend: Java 17, Spring Boot, Maven, Flyway, JPA
- Frontend: React 18, Vite
- Database: MySQL 8.4
- Deployment: Docker + Docker Compose
- CI/CD: Jenkins pipeline in `Jenkinsfile`

## Container Architecture

- `frontend` (Nginx): serves static React app on port `80`
- `backend` (Spring Boot): API on port `8080`
- `mysql` (MySQL 8.4): database on port `3306`
- Frontend proxies `/api` -> `backend:8080` via `frontend/nginx.conf`

## Files Added For Deployment

- `Dockerfile` (backend image)
- `frontend/Dockerfile` (frontend image)
- `frontend/nginx.conf` (SPA + API reverse proxy)
- `docker-compose.yml` (full stack)
- `.dockerignore` (smaller build context)
- `.env.example` (environment template)
- `Jenkinsfile` (CI/CD pipeline)

## Linux Deployment (Docker Compose)

1. Create runtime env file from template.
2. Update secrets before starting services.
3. Validate compose file.
4. Start all services.

```bash
cp .env.example .env
# Edit .env and set strong MYSQL_PASSWORD and MYSQL_ROOT_PASSWORD
docker compose --env-file .env config
docker compose --env-file .env up -d --build
```

Useful commands:

```bash
docker compose ps
docker compose logs -f backend
docker compose logs -f frontend
docker compose logs -f mysql
docker compose down
```

## Environment Variables

Main runtime values are documented in `.env.example`.

Required for deployment:

- `MYSQL_PASSWORD`
- `MYSQL_ROOT_PASSWORD`

Common optional settings:

- `MAIL_USERNAME`
- `MAIL_PASSWORD`
- `MAIL_FROM`
- `APP_MAIL_ENABLED`

## Jenkins CI/CD

Pipeline file: `Jenkinsfile`

Default pipeline flow:

1. Checkout source
2. Run backend tests (`./mvnw clean verify`)
3. Build frontend (`npm ci` + `npm run build`)
4. Build Docker images
5. Optional image push
6. Optional `docker compose` deployment

Key Jenkins parameters:

- `BUILD_DOCKER_IMAGES`
- `PUSH_IMAGES`
- `DEPLOY_WITH_COMPOSE`
- `DOCKER_REGISTRY`
- `DOCKER_REPOSITORY`
- `DOCKER_REGISTRY_CREDENTIALS_ID`
- `IMAGE_TAG`

If deployment is enabled, Jenkins requires:

- `MYSQL_PASSWORD`
- `MYSQL_ROOT_PASSWORD`

## Local Backend Without Docker (Optional)

```powershell
.\mvnw.cmd spring-boot:run
```

## Frontend Development (Optional)

```powershell
cd frontend
npm install
npm run dev
```
