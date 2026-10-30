# Wallet Perú

Wallet Perú es el MVP de una billetera digital: registro y activación guiados (onboarding de 3 pasos), autenticación con celular y contraseña, y un resumen analítico mensual del onboarding.

## Tecnologías

| Capa | Tecnología | Versión | Uso |
|------|------------|---------|-----|
| Backend | Java | 21 | Lenguaje del servicio |
| Backend | Spring Boot (parent) | 4.1.2 | Framework base |
| Backend | Spring Web MVC | Gestionada por Spring Boot | API REST (`@RestController`) |
| Backend | Spring Data JPA / Hibernate | Gestionada por Spring Boot | Persistencia y repositorios (`JpaRepository`) |
| Backend | Bean Validation (`spring-boot-starter-validation`) | Gestionada por Spring Boot | Validación de solicitudes |
| Backend | Lombok | Gestionada por Spring Boot | Reducción de código repetitivo (`@Getter`) |
| Backend | Maven Wrapper (`mvnw`) | - | Compilación, pruebas y ejecución |
| Backend | PBKDF2WithHmacSHA256 (JDK) | - | Hash salado de contraseñas (`PasswordHasher`) |
| Base de datos | PostgreSQL | 17 (alpine) | Base de datos de producción/desarrollo |
| Base de datos | H2 | Gestionada por Spring Boot | Base de datos solo para pruebas |
| Frontend | Angular | 21.2.19 | Aplicación web (componentes, router, formularios, `HttpClient`) |
| Frontend | TypeScript | 5.9.3 | Lenguaje del frontend |
| Frontend | RxJS / zone.js | 7.8.2 / 0.15.1 | Flujos asíncronos y detección de cambios |
| Frontend | Angular CLI / build-angular | 21.2.19 | Servidor de desarrollo y build |
| Frontend | Node.js | >= 22.12.0 | Entorno de ejecución y build |
| Infraestructura | Docker Compose | - | PostgreSQL local (`backend/compose.yml`) |
| Infraestructura | Nginx | 1.29 (alpine) | Servido de la SPA en contenedor (`web/Dockerfile`, `web/nginx.conf`) |
| Calidad | JUnit / Spring Boot Test / MockMvc | Gestionada por Spring Boot | Pruebas unitarias, de capa web e integración |
| Calidad | JaCoCo 0.8.14 | - | Cobertura del backend; cobertura mínima de líneas 85% |
| Calidad | Vitest 3.2.4 + `@vitest/coverage-v8` | 3.2.4 | Pruebas unitarias del frontend y cobertura de `src/app/wallet` |

## Estructura del repositorio

```text
wallet/
├── backend/          # API REST en Spring Boot (Java 21, Maven)
│   ├── compose.yml   # PostgreSQL 17
│   └── src/
│       ├── main/java/com/bcp/wallet/backend/
│       │   ├── account/     # Registro de cuentas y tarjetas vinculadas
│       │   ├── analytics/   # Resumen mensual del onboarding (protegido con clave)
│       │   ├── auth/        # Login con celular y contraseña
│       │   ├── onboarding/  # Pasos y eventos del onboarding
│       │   ├── security/    # Hash de contraseñas
│       │   ├── config/      # Clock, CORS y propiedades de la wallet
│       │   └── error/       # Manejo uniforme de errores
│       └── test/            # Pruebas unitarias, de capa web e integración
├── web/              # SPA en Angular 21 (TypeScript, Vitest)
│   ├── Dockerfile    # Build con Node 22 y servido con Nginx
│   └── src/app/wallet/  # Servicios, guards, validaciones y componentes
├── openspec/ y backend/openspec/  # Especificaciones y propuestas de cambio
├── mockups.html      # Prototipo de referencia de las pantallas
└── RETO.md           # Enunciado del reto
```

## API principal

| Método | Ruta | Descripción |
|--------|------|-------------|
| `POST` | `/accounts` | Registra una cuenta con celular, contraseña y tarjeta (paso 1 del onboarding) |
| `GET` | `/accounts/{accountId}/onboarding` | Estado del onboarding |
| `POST` | `/accounts/{accountId}/onboarding/steps/{step}/complete` | Completa un paso; al finalizar, la cuenta queda `ACTIVE` |
| `POST` | `/auth/login` | Autenticación con celular y contraseña |
| `GET` | `/analytics/onboarding/monthly?month=YYYY-MM` | Resumen mensual (requiere clave de analítica) |

## Configuración

Variables y propiedades relevantes en `backend/src/main/resources/application.properties`:

- `spring.datasource.*`: conexión a PostgreSQL (`localhost:5432/wallet_db`).
- `spring.jpa.hibernate.ddl-auto=update`: crea/actualiza las tablas al iniciar.
- `wallet.cors.allowed-origins`: origen permitido para el frontend (`http://localhost:4200`).
- `wallet.security.pbkdf2-iterations`: iteraciones de PBKDF2 (600000).
- `WALLET_ANALYTICS_API_KEY`: clave para habilitar `/analytics/**`. Si está vacía, el endpoint queda deshabilitado.

No se incluyen secretos reales en el repositorio; las credenciales de `compose.yml` y `application.properties` son solo de desarrollo local.

## Ejecución local

1. Levantar PostgreSQL:

   ```bash
   cd backend
   docker compose up -d
   ```

2. Ejecutar el backend:

   ```bash
   cd backend
   ./mvnw spring-boot:run
   ```

3. Ejecutar el frontend (Node.js 22.12 o superior):

   ```bash
   cd web
   npm ci
   npm start
   ```

   La aplicación queda disponible en `http://localhost:4200`.

## Pruebas

- Backend: `cd backend && ./mvnw verify` (ejecuta las pruebas y valida la cobertura de JaCoCo).
- Frontend: `cd web && npm test` o `npm run test:coverage`.
- Auditoría de dependencias del frontend: `cd web && npm run audit:workshop`.
