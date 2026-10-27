# ResolveBid deployment

## One-command Docker deployment

Requirements: Docker Desktop or Docker Engine with Compose.

1. Copy `.env.example` to `.env`.
2. Replace all three password values with long random secrets. `ADMIN_PASSWORD` must be at least 12 characters.
3. Run:

```powershell
docker compose up --build -d
```

4. Check the application:

```powershell
docker compose ps
curl http://localhost:8090/health
```

Open `http://localhost:8090/login` and sign in with `ADMIN_USERNAME` and `ADMIN_PASSWORD` from `.env`.

## Public production deployment

Put the app behind an HTTPS reverse proxy or a managed load balancer, set `SECURE_COOKIES=true`, and expose only the proxy publicly. Back up the `mysql_data` Docker volume. Never commit `.env`.

Configuration is supplied through `APP_ENV`, `PORT`, `DB_URL`, `DB_USER`, `DB_PASSWORD`, `ADMIN_USERNAME`, `ADMIN_PASSWORD`, and `SECURE_COOKIES`. The application exits at startup if a production admin password is missing or too short.

## Operations

```powershell
docker compose logs -f app
docker compose restart app
docker compose down
```

`docker compose down` preserves named database volumes. Do not add `-v` unless you intentionally want to delete all application data.
