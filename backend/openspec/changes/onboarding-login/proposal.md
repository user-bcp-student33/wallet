# Proposal

## Why

Wallet Perú aún no permite completar el onboarding ni autenticar a un usuario. El Issue #21 (HU-01, "Registro y activación del MVP de wallet") es el contrato de este cambio: un flujo guiado con pasos y paso actual visibles, validación que bloquea el avance con mensajes accionables, activación de la wallet al finalizar, eventos analíticos y una salida mensual para verificar la mediana de onboarding (8 a 3 minutos), los nuevos usuarios activos (1,000 a 2,000) y el CAC (PEN 10).

Nota de trazabilidad: la versión previa de este cambio citaba "Issue #1", que en realidad es un PR cerrado de Dependabot (JaCoCo en `auth/`). El Issue correcto es el #21; consultado con github-official. Las diferencias de esta versión respecto de la anterior están marcadas en los specs.

## What Changes

- Nuevo endpoint `POST /accounts` que registra una cuenta con celular, contraseña y datos de una tarjeta en una sola operación atómica. Es el paso 1 del onboarding; la cuenta queda en `PENDING_ONBOARDING`.
- Onboarding de 3 pasos (`ACCOUNT_SETUP`, `WALLET_PREPARATION`, `WALLET_READY`) tomados del prototipo `mockups.html`, con `GET /accounts/{accountId}/onboarding` y `POST /accounts/{accountId}/onboarding/steps/{step}/complete`. Al completar el último paso la cuenta pasa a `ACTIVE` (criterios 1 y 3).
- Validación con mensajes accionables por campo en español (criterio 2): celular peruano de 9 dígitos que empieza en 9, contraseña de 8 a 128 caracteres, tarjeta de hasta 16 dígitos numéricos sin otra validación (sin Luhn ni lista de prueba), CVV de exactamente 3 dígitos. El límite de tarjeta y CVV concreta el prototipo por solicitud del responsable.
- CVV: se valida en formato y se descarta. No se persiste, no se registra en logs y no se devuelve.
- Persistencia en PostgreSQL: cuenta con celular único, contraseña solo como hash salado, estado y pasos completados; tarjeta vinculada con solo marca y últimos 4 dígitos.
- Eventos de onboarding (`ONBOARDING_STARTED`, `STEP_COMPLETED`, `ONBOARDING_COMPLETED`) y canal de adquisición opcional en el registro (criterios 4 y 6).
- `GET /analytics/onboarding/monthly?month=YYYY-MM` protegido con clave, con onboardings iniciados, nuevos usuarios activos, mediana de duración, gasto y CAC por canal (criterios 5 y 6). El gasto sale de la tabla `acquisition_spend`, sin endpoint de escritura.
- `POST /auth/login` que autentica con celular y contraseña y devuelve datos del usuario, estado de la cuenta y estado del onboarding. No devuelve token ni sesión en esta versión.
- Respuestas de error sin detalles internos y CORS configurable.
- Frontend Angular en `web/` que reemplaza ReqFlow (decisión del responsable) y implementa las pantallas del prototipo: inicio, registro, login, preparación, cuenta lista y contactos, con el stepper "PASO N DE 3".
- Pruebas automatizadas unitarias, de capa web e integración (backend) y unitarias (web).

**Fuera de alcance:** el flujo de pago posterior a elegir un contacto (HU-02; `payment`, `review`, `done` del prototipo), saldo real (la UI muestra `S/ 0.00` fijo), KYC y documentos (el Issue indica que cumplimiento y producto deben definirlos antes; no hay requisitos aún, por lo que el onboarding no valida la tarjeta más allá de su formato), MAU por uso de pagos (requiere eventos de HU-02), un endpoint de carga del gasto de adquisición, tokens o sesiones, bloqueo por intentos fallidos, recuperación de contraseña, verificación OTP, cobros o transacciones reales y tarjetas reales.

## Capabilities

### New Capabilities
- `account-onboarding`: paso 1 del onboarding, registro con celular, contraseña y tarjeta, validación accionable y canal de adquisición.
- `onboarding-flow`: secuencia de pasos, paso actual, finalización en orden y activación de la wallet.
- `onboarding-analytics`: eventos de onboarding y resumen mensual con mediana, nuevos usuarios activos, gasto y CAC.
- `user-authentication`: autenticación con celular y contraseña, con estado de onboarding y respuestas uniformes ante credenciales inválidas.
- `wallet-web-app`: experiencia web del prototipo con stepper, validación local y acceso a pagos solo con wallet activa.

### Modified Capabilities
- Ninguna. No existen specs previos en `openspec/specs/`.

## Impact

- **Código backend:** paquetes `account`, `auth`, `onboarding`, `analytics`, `security`, `config` y `error` bajo `com.bcp.wallet.backend`. Se separan controlador, servicio, repositorio, validación y hashing.
- **Código web:** se reemplazan los componentes de ReqFlow (`login`, `dashboard`, `utils/legacy-format`) por los de Wallet. AGENTS.md pide conservar `/auth/login` y `/api/tasks`; `/auth/login` se conserva como ruta del backend Wallet con un contrato nuevo, y `/api/tasks` deja de usarse por decisión del responsable.
- **APIs nuevas:** `POST /accounts`, `GET /accounts/{accountId}/onboarding`, `POST /accounts/{accountId}/onboarding/steps/{step}/complete`, `POST /auth/login`, `GET /analytics/onboarding/monthly`.
- **Base de datos:** tablas `accounts`, `linked_cards`, `onboarding_events` y `acquisition_spend` en PostgreSQL 17 (`compose.yml`), creadas por `spring.jpa.hibernate.ddl-auto=update`.
- **Configuración:** `wallet.cors.allowed-origins`, `wallet.security.pbkdf2-iterations` y `wallet.analytics.api-key`.
- **Dependencias:** `spring-boot-starter-validation`, H2 y JaCoCo solo para pruebas y cobertura. No se agregan librerías de seguridad de producción; el hash usa el JDK. En `web/` se retiran `axios` y `lodash` si ya no se usan.
- **Breaking:** ninguno en el backend (endpoints nuevos). En `web/` se reemplaza la aplicación ReqFlow.
