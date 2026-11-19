# Despliegue del backend con Docker (VPS)

El **frontend** se publica en Hostinger y el **backend** corre en un VPS con Docker:

```
Ciudadano / funcionario
   │
   ├──> https://correspondencia.clinicalhouse.co        Hostinger  (frontend/dist)
   │
   └──> https://api-correspondencia.clinicalhouse.co    VPS
            caddy :443 (HTTPS automático)
              └── /api/*  → backend:8080 (Spring Boot)
                               └── db:5432 (PostgreSQL 16)
```

Solo Caddy abre puertos (80 y 443). El backend y PostgreSQL no son accesibles desde internet.

| Archivo | Para qué |
|---|---|
| `Dockerfile` | Compila el `.jar` con Maven y lo ejecuta en un JRE 21 con usuario sin privilegios |
| `docker-compose.yml` | Levanta `db` + `backend` + `caddy` |
| `Caddyfile` | Proxy HTTPS con certificado Let's Encrypt automático |
| `.env.docker.example` | Plantilla de variables para el VPS |

## 1. Antes de empezar

1. **DNS**: crear un registro **A** `api-correspondencia.clinicalhouse.co` → IP del VPS (en el panel DNS donde
   esté el dominio, p. ej. Hostinger). Debe resolver antes de arrancar, o Caddy no obtiene el certificado.
2. **VPS** (Ubuntu/Debian) con Docker y el plugin compose:
   ```bash
   curl -fsSL https://get.docker.com | sh
   ```
3. **Firewall**: abrir solo 22, 80 y 443.
   ```bash
   ufw allow OpenSSH && ufw allow 80 && ufw allow 443/tcp && ufw allow 443/udp && ufw enable
   ```

## 2. Subir y configurar

Copiar la carpeta `backend/` al VPS (git clone, `scp` o `rsync`), por ejemplo a `/opt/pqrsf-siau/backend`.
No hace falta subir `target/`, `uploads/`, `logs/` ni el `.env` local.

```bash
cd /opt/pqrsf-siau/backend
cp .env.docker.example .env
nano .env          # completar
chmod 600 .env
```

Valores obligatorios: `API_DOMAIN`, `CORS_ORIGINS` (dominio del frontend en Hostinger, exacto,
con `https://` y sin barra final), `APP_PUBLIC_URL`, `DB_PASSWORD`, `JWT_SECRET`
(`openssl rand -base64 48`) y `MAIL_PASSWORD`.

## 3. Arrancar

```bash
docker compose up -d --build
docker compose ps                      # los tres servicios "Up", backend "healthy"
docker compose logs -f backend         # buscar "Started PqrsfApplication" y "CORREO OK"
```

En una base de datos nueva aparece una sola vez en el log:
`Cuenta inicial creada: admin@clinicalhouse.co · contraseña temporal: …` (igual para la jefe SIAU).
Ingresar y cambiarla en **Mi perfil**.

Prueba rápida desde cualquier equipo:

```bash
curl -i https://api-correspondencia.clinicalhouse.co/api/pqrsf     # debe responder 401 (sin sesión)
```

## 4. Frontend en Hostinger

En el equipo de desarrollo:

```powershell
cd frontend
# .env.production ya trae VITE_API_URL=https://api-correspondencia.clinicalhouse.co/api
npm ci
npm run build
```

Subir el **contenido** de `frontend/dist/` a `public_html` (o la carpeta del subdominio) en Hostinger.
El `.htaccess` incluido en `dist/` hace que `/gestion-siau` y `/admin` funcionen al recargar la página.

> `VITE_API_URL` queda fijado al compilar: si cambia el dominio del backend hay que volver a compilar y subir.

## 5. Operación diaria

```bash
# Actualizar a una nueva versión del código
docker compose up -d --build backend

# Reiniciar / detener
docker compose restart backend
docker compose down                    # detiene; los datos se conservan

# Registros
docker compose logs --tail 200 backend
```

**Nunca** usar `docker compose down -v`: borra la base de datos y los adjuntos.

## 6. Copias de seguridad

Los datos viven en dos volúmenes: `pgdata` (base de datos) y `uploads` (adjuntos de los ciudadanos).

```bash
# Base de datos
docker compose exec -T db pg_dump -U pqrsf -Fc pqrsf-siau > backup_$(date +%F).dump

# Adjuntos
docker run --rm -v pqrsf-siau_uploads:/data -v "$PWD":/out alpine \
  tar czf /out/uploads_$(date +%F).tar.gz -C /data .
```

Programarlo con `crontab -e` y copiar los archivos fuera del VPS.

## 7. Migrar los datos actuales (opcional)

Si ya hay información en el PostgreSQL local que deba pasar al VPS:

```powershell
# En el equipo actual
pg_dump -U postgres -Fc -d pqrsf-siau -f pqrsf.dump
```

```bash
# En el VPS, con el stack arriba (base nueva)
docker compose stop backend
docker compose exec -T db pg_restore -U pqrsf -d pqrsf-siau --clean --if-exists --no-owner < pqrsf.dump
# Adjuntos: copiar el contenido de backend/uploads/ del equipo actual al volumen
docker compose cp ./uploads/. backend:/app/uploads/
docker compose start backend
```

## 8. Si el VPS ya tiene Nginx/Apache en los puertos 80/443

Quitar el servicio `caddy` del compose, publicar el backend solo en localhost
(`ports: ["127.0.0.1:8080:8080"]` en el servicio `backend`) y en el Nginx existente
apuntar `location /api/ { proxy_pass http://127.0.0.1:8080; proxy_set_header X-Forwarded-For $remote_addr; }`
con `client_max_body_size 60m;`.
