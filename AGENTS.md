# AGENTS.md

## Project structure

Three independent apps in one repo:
- `server/` — Spring Boot 3.2 + MyBatis Plus backend (Java 17, Maven)
- `admin/` — Vue 3 + Vite + Element Plus admin panel
- `miniprogram/` — Native WeChat Mini Program

## Developer commands

### Start services (run from repo root)

| Command | What it does |
|---------|-------------|
| `.\scripts\start-local-server.ps1` | Starts Spring Boot via `mvn spring-boot:run` with `local` profile on port 8080 |
| `.\scripts\start-local-admin.ps1` | Runs `npm install` if needed, then `npm run dev` (Vite, port 5173) |
| `docker compose -f docker-compose.local.yml up -d` | Starts local MySQL 8.0 on port 3306, auto-runs `schema.sql` on first init |

### Startup order

1. MySQL (`docker compose ...`)
2. Spring Boot server (`start-local-server.ps1`)
3. Admin panel (`start-local-admin.ps1`)
4. Mini Program (import into WeChat DevTools)

### Verify backend

```
http://127.0.0.1:8080/actuator/health
```

### Default credentials

```
admin / admin123
```

### API docs

Knife4j OpenAPI UI at `http://127.0.0.1:8080/doc.html` (when server is running).

## Configuration

### Config files contain secrets

Real configs are gitignored. Copy examples before first run:

```powershell
Copy-Item server\src\main\resources\application-local.example.yml server\src\main\resources\application-local.yml
Copy-Item server\src\main\resources\application-prod.example.yml server\src\main\resources\application-prod.yml
Copy-Item docker-compose.local.example.yml docker-compose.local.yml
```

### Spring profiles

- `local` → `application-local.yml`
- `prod` → `application-prod.yml`

The start script forces `local` profile. Dockerfile forces `prod`.

### Mini Program API mode

`miniprogram/utils/config.js` controls routing:
- `mode: 'local'` → direct HTTP to Spring Boot via `wx.request`
- `mode: 'cloud'` → WeChat CloudBase via `wx.cloud.callContainer`

Set to `cloud` before production deploy.

### Admin Vite proxy

`admin/vite.config.js` proxies `/api` and `/uploads` to `http://localhost:8080`. Do not change the backend URL in admin code; use the proxy.

## Database

- MySQL 8.0, database `xiaoyu_yinran`
- Schema lives at `server/src/main/resources/db/schema.sql`
- docker-compose mounts schema as init script; manual init uses `scripts/init-local-db.sql`
- Root password in local docker-compose: `5522`

## Server tech stack

- Spring Boot 3.2.5, Java 17
- Spring Security + JWT auth
- MyBatis Plus 3.5.7
- Tencent COS (object storage for images)
- Lombok
- Knife4j (OpenAPI/Swagger)

## Deployment

- Server deploys to WeChat CloudBase Run environment `xiaoyu-yinran-server`
- Dockerfile uses `prod` profile
- Before deploy: verify `application-prod.yml` has real DB and COS credentials, and set mini program `mode` to `cloud`

## Conventions

- PowerShell scripts for local dev (Windows paths hardcoded to `D:\code\fzxcx`)
- No test framework configured beyond Spring Boot starter; verify manually via API
- No linter/formatter configured; follow existing file style
