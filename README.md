# PQRSF SIAU — API Backend · IPS Clinical House

Servicio backend REST para la gestión integral de **Peticiones, Quejas, Reclamos, Sugerencias, Felicitaciones y Tutelas (PQRSF)** del Servicio de Información y Atención al Usuario (SIAU) de **IPS Clinical House**.

---

## 🚀 Tecnologías

- **Framework:** Spring Boot 3.3.5
- **Lenguaje:** Java 21 (LTS)
- **Seguridad:** Spring Security 6 + JWT (JSON Web Tokens) + BCrypt
- **Persistencia:** Spring Data JPA / Hibernate
- **Base de Datos:** PostgreSQL 14+ / 17
- **Notificaciones:** Spring Boot Starter Mail (SMTP Gmail) con plantillas HTML responsivas
- **Construcción y empaquetado:** Maven (incluye `mvnw.cmd`)

---

## 📦 Estructura del Proyecto

```
backend/
├── database/                   # Scripts SQL de esquema, migraciones y diccionario de datos
│   ├── init_database.sql       # Creación de tablas, secuencias, funciones y disparadores
│   ├── migration_v2.sql        # Migraciones de campos y esquemas
│   ├── update_responsables.sql # Actualización y mantenimiento de usuarios
│   └── diccionario_datos.md    # Diccionario técnico de tablas y columnas
├── src/
│   ├── main/
│   │   ├── java/co/clinicalhouse/pqrsf/
│   │   │   ├── config/         # Seguridad, CORS, manejo global de excepciones
│   │   │   ├── controller/     # Endpoints REST (PQRSF, Auth, Configuración, Auditoría)
│   │   │   ├── dto/            # Objetos de transferencia de datos
│   │   │   ├── mail/           # Plantillas HTML y servicio de correo electrónico
│   │   │   ├── model/          # Entidades JPA (Pqrsf, Usuario, Traza, Auditoría)
│   │   │   ├── repository/     # Repositorios Spring Data JPA
│   │   │   ├── security/       # Filtros JWT, token provider y gate de superadministrador
│   │   │   ├── service/        # Lógica de negocio (SLA, archivos, usuarios, auditoría)
│   │   │   └── sla/            # Políticas de semáforo y cálculo de plazos
│   │   └── resources/
│   │       └── application.properties # Configuración centralizada de Spring Boot
├── .env.example                # Plantilla de variables de entorno (sin secretos)
├── pom.xml                     # Dependencias y configuración de Maven
└── mvnw.cmd                    # Maven Wrapper para Windows
```

---

## 💻 Requisitos Previos

- **Java Development Kit (JDK):** Versión 21 o superior (ej. JetBrains JBR 21, Eclipse Temurin 21, OpenJDK 21).
- **PostgreSQL:** Versión 14 o superior (recomendado PostgreSQL 17).
- **Maven:** 3.8+ (o utilizar el `mvnw.cmd` incluido).

---

## ⚙️ Configuración del Entorno

1. Copie el archivo de ejemplo para crear su archivo de configuración local:
   ```bash
   cp .env.example .env
   ```
2. Abra `.env` y configure sus credenciales locales:
   - **DB_URL, DB_USER, DB_PASSWORD:** Parámetros de conexión a PostgreSQL.
   - **JWT_SECRET:** Clave secreta para firmar los tokens JWT (mínimo 64 caracteres aleatorios).
   - **MAIL_USERNAME, MAIL_PASSWORD:** Cuenta Gmail y su contraseña de aplicación (16 caracteres).
   - **CORS_ORIGINS:** Orígenes permitidos (ej. `http://localhost:5173`).

> ⚠️ **IMPORTANTE:** El archivo `.env` está ignorado por Git en `.gitignore`. Nunca guarde ni suba contraseñas reales al repositorio.

---

## 🗄️ Inicialización de la Base de Datos

En PostgreSQL (puerto 5432, base de datos `pqrsf-siau`), ejecute los scripts ubicados en la carpeta `database/`:
```bash
psql -U postgres -d pqrsf-siau -f database/init_database.sql
psql -U postgres -d pqrsf-siau -f database/migration_v2.sql
```

---

## ▶️ Ejecución del Servidor

### Modo desarrollo:
```bash
.\mvnw.cmd spring-boot:run
```
El backend iniciará en el puerto **8080** (`http://localhost:8080`).

### Compilar y empaquetar en JAR:
```bash
.\mvnw.cmd clean package -DskipTests
java -jar target/pqrsf-siau-backend-1.0.0.jar
```

---

## 🔒 Seguridad y Buenas Prácticas
- **Autenticación:** JWT Stateless en encabezado `Authorization: Bearer <token>`.
- **Doble factor en módulo de usuarios:** El endpoint `/api/auth/usuarios` exige autorización previa en la cabecera `X-Acceso-Usuarios`.
- **Manejo de errores seguro:** `server.error.include-stacktrace=never` y `server.error.include-message=never` para evitar fugas de información interna.
- **Auditoría inmutable:** Todas las operaciones críticas se registran en `auditoria_pqrsf` con IP, fecha y usuario responsable.
