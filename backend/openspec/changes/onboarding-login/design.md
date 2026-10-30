# Design

## Context

El backend (`backend/`) es una aplicación Spring Boot 4.1.2 con Java 21, Spring Data JPA, Spring Web MVC y PostgreSQL 17 (`compose.yml`). Hoy solo existe el DTO `CreateAccountRequest` en `com.bcp.wallet.backend.account.dto`, con los campos `cardNumber`, `cvv`, `phoneNumber` y `password`, y no hay entidades, servicios ni endpoints. `application.properties` usa `spring.jpa.hibernate.ddl-auto=update`.

No hay HTML de aplicación en el backend. El mockup de referencia es `mockups.html` (raíz del repositorio), un prototipo de una sola página con estas pantallas y su relación con la API:

| Pantalla del HTML | Acción | Endpoint de este cambio |
|-------------------|--------|-------------------------|
| Inicio (`home`) | Crear cuenta / Ya tengo cuenta | Ninguno |
| Registro (`register`, "PASO 1 DE 3") | Tarjeta, CVV, celular y contraseña → "Continuar" | `POST /accounts` (paso `ACCOUNT_SETUP`) |
| Carga (`loading`, "Preparando tu billetera") | Espera antes de mostrar el resultado | `POST /accounts/{id}/onboarding/steps/WALLET_PREPARATION/complete` |
| Cuenta lista (`approved`) | "Ir a mis contactos" | `POST /accounts/{id}/onboarding/steps/WALLET_READY/complete` (activa la wallet) |
| Login (`login`) | Celular y contraseña → "Ingresar" | `POST /auth/login` |
| Contactos (`contacts`) | Punto de entrada de pagos; solo con wallet `ACTIVE` | Ninguno (datos de demostración) |
| Pago, revisión, listo (`payment`, `review`, `done`) | Flujo de transferencia simulado | Fuera de alcance (HU-02) |

Reglas acordadas para el cambio: el celular se envía solo con dígitos (`987654321`), la tarjeta se envía solo con dígitos (hasta 16, sin otra validación), el CVV tiene exactamente 3 dígitos y la contraseña tiene mínimo 8 caracteres. El límite de 16 dígitos y CVV de 3 concreta el prototipo por solicitud del responsable.

Contrato de trabajo: Issue #21 (HU-01), consultado con github-official. Mapa de criterios: 1 y 3 en `onboarding-flow` y `wallet-web-app`; 2 en `account-onboarding` y `wallet-web-app`; 4, 5 y 6 en `onboarding-analytics`.


## Goals / Non-Goals

**Goals:**
- Implementar los requisitos de `account-onboarding`, `onboarding-flow`, `onboarding-analytics`, `user-authentication` y `wallet-web-app` en una estructura por capas: controlador, servicio, repositorio, validación y seguridad de contraseña.
- Mantener el CVV fuera de cualquier persistencia, log o respuesta.
- Ofrecer pruebas unitarias, de capa web y de integración sobre una base relacional.
- Alcanzar al menos 85% de cobertura de línea en el código nuevo.

**Non-Goals:**
- Tokens, sesiones, cookies o autorización de endpoints (ver Decisiones 8).
- Limitación de intentos, bloqueo de cuentas o recuperación de contraseña.
- Integración con pasarelas de pago reales. La tarjeta solo se registra como marca y últimos 4 dígitos.
- Migraciones versionadas de base de datos.

## Decisions

**1. Endpoints y paquetes.** `POST /accounts` y `POST /auth/login`, con controladores en `account` y en un paquete `auth`. Alternativa descartada: prefijo `/api/v1`. Se eligió el mismo nivel de ruta que el contrato de login de los otros componentes del laboratorio para facilitar el consumo, y el paquete `account` ya existente.

**2. Hash de contraseña con PBKDF2-HMAC-SHA256 del JDK.** Se usa `SecretKeyFactory` con sal aleatoria de 16 bytes y un número de iteraciones configurado (por defecto 210 000, variable `WALLET_PBKDF2_ITERATIONS`; reducible en pruebas). El valor almacenado incluye algoritmo, iteraciones, sal y hash para poder verificarlo sin configuración externa. El valor inicial de 600 000 tardaba 250 a 630 ms por login (medido en el entorno de desarrollo); 210 000 tarda unos 90 ms a costa de menor resistencia a fuerza bruta, por lo que debe revisarse antes de producción. El hasher se precalienta al arrancar y, tras un login exitoso, un hash con distinto número de iteraciones se reemplaza por uno actual.
- Alternativa: BCrypt de `spring-security-crypto`. Descartada para no agregar una dependencia de producción en este cambio; el costo es implementar el formato de almacenamiento y la comparación en tiempo constante, que se cubre con pruebas.

**3. Solo marca y últimos 4 dígitos de la tarjeta.** La tarjeta vinculada guarda marca y `last_four`. No se guarda el PAN completo, lo que reduce el alcance de datos sensibles. Alternativa: cifrar el PAN. Descartada porque el caso de uso es una tarjeta de prueba y no hay necesidad de cobrar.

**4. Validación de tarjeta.** Solo dígitos, de 1 a 16 (`^\d{1,16}$`), por solicitud del responsable; no se valida Luhn, lista de tarjetas, prefijo ni longitud mínima. La marca se deriva del primer dígito (4 = Visa, 5 = Mastercard, otro = `OTHER`) y es solo informativa. Consecuencia: el sistema acepta números arbitrarios, por lo que no debe usarse con tarjetas reales ni para cobros; solo se conservan marca y últimos 4 dígitos (o menos si el número es más corto).

**5. CVV descartado en el borde.** El CVV se valida con un patrón `^\d{3}$` en la capa de entrada y no se pasa al servicio ni al modelo de dominio. Así no puede llegar a una entidad ni a un log por accidente.

**6. Transacción única en el registro.** El servicio de registro es transaccional: valida, crea la cuenta y la tarjeta en una sola transacción. Si la inserción de la tarjeta falla, la cuenta se revierte. La unicidad del celular se garantiza con una restricción `UNIQUE` en base de datos, y la violación se traduce a 409 en la capa de errores. Esto evita una condición de carrera entre una comprobación previa y la inserción.

**7. Modelo de persistencia.** Cuatro entidades: `Account` (id UUID, `phone_number` único, `password_hash`, `status`, `completed_steps`, `acquisition_channel`, `created_at`), `LinkedCard` (id, `account_id` único con FK, `brand`, `last_four`, `created_at`), `OnboardingEvent` (id, `account_id`, `type`, `step` nulo, `occurred_at`) y `AcquisitionSpend` (id, `month`, `channel`, `amount_pen`, único por mes y canal). Relación 1:1 cuenta-tarjeta porque el alcance solo contempla una tarjeta por cuenta.

**8. Login sin token.** `POST /auth/login` devuelve `accountId`, celular, estado y estado del onboarding. Como consecuencia, el backend no protege ningún endpoint con identidad, incluidos `GET /accounts/{id}/onboarding` y `POST .../steps/{step}/complete`: el `accountId` es un UUID aleatorio y actúa como capacidad, no como autenticación. Es una decisión explícita del alcance acordado; un cambio posterior deberá introducir sesión o token y su expiración antes de exponer operaciones de dinero.

**9. Mensajes de error.** Un `@RestControllerAdvice` centraliza el mapeo de excepciones: validación → 400 con campos, duplicado → 409, credenciales → 401 uniforme, cualquier otra excepción → 500 genérico con log de servidor sin datos sensibles. Las constantes (longitudes, patrones, iteraciones, nombres de propiedad) se declaran con nombre en una sola clase de constantes por paquete.

**10. CORS configurable.** Propiedad `wallet.cors.allowed-origins`, con valor por defecto `http://localhost:4200` (puerto de desarrollo de Angular), aplicada a `/accounts/**` y `/auth/login`. Sin comodín `*`. `/analytics/**` no habilita CORS porque es de consumo servidor a servidor.

**11. Pruebas.**
- Unitarias: validadores (celular, contraseña, CVV), hasher PBKDF2, mediana y CAC, y servicios con repositorios simulados.
- Capa web (`@WebMvcTest`): contratos HTTP, códigos de respuesta, errores y CORS.
- Integración (`@SpringBootTest` con H2 en modo PostgreSQL): restricciones de unicidad, atomicidad del registro, ausencia del CVV y del PAN, flujo completo hasta `ACTIVE`, eventos y resumen mensual.
- Cambio respecto de la versión previa: se descartó Testcontainers con PostgreSQL porque el entorno de desarrollo no tiene Docker y las pruebas deben ser ejecutables en cualquier equipo. Riesgo: H2 difiere de PostgreSQL en tipos y restricciones; se mitiga verificando manualmente contra `compose.yml` (tarea de verificación) y manteniendo consultas JPQL estándar.

**12. Estado del onboarding en la cuenta.** `Account` guarda `completed_steps` (0 a 3) y `status` (`PENDING_ONBOARDING`, `ACTIVE`). El paso actual se deriva del orden de `OnboardingStep` (`ACCOUNT_SETUP`, `WALLET_PREPARATION`, `WALLET_READY`). El registro crea la cuenta con 1 paso completado; completar el paso 3 cambia el estado a `ACTIVE`. Los pasos se toman del prototipo ("PASO 1 DE 3", carga, cuenta lista) porque el Issue #21 exige una secuencia visible pero no la enumera; si producto agrega un paso (por ejemplo KYC), solo crece el enum.

**13. Eventos de onboarding.** `OnboardingService` es el único que escribe en `onboarding_events`, dentro de la misma transacción que el cambio de estado, para que no existan eventos sin cambio ni cambios sin evento. Un paso rechazado con 409 no escribe nada.

**14. Analítica mensual.** Un servicio calcula, para un mes en zona `America/Lima`, iniciados, completados, mediana y, por canal, usuarios, gasto y CAC. Los pares inicio-fin salen de una consulta JPQL que une el evento de inicio, el de cierre y la cuenta; la mediana y el CAC se calculan en Java (volúmenes de MVP: miles de filas). El gasto se lee de `acquisition_spend`; no hay endpoint de escritura porque el Issue no define quién lo carga y un endpoint sin autenticación de usuario sería un riesgo. El acceso usa una clave en `X-Analytics-Key` comparada en tiempo constante, desactivada si no está configurada.

**15. Frontend.** Angular standalone en `web/`, con un servicio HTTP por API (`AccountApiService`), un servicio de estado de sesión en memoria (`WalletSessionService`), un guard para contactos y funciones puras de validación compartidas por los componentes. Usa `HttpClient` en lugar de `axios` y no usa `any`. La URL base y las claves están centralizadas en una constante. El estado vive solo en memoria: al recargar, la persona vuelve a iniciar sesión.

## Risks / Trade-offs

- **[Enumeración de celulares por 409 en registro]** → Se acepta el trade-off porque el registro necesita informar que el celular ya existe. Mitigación futura: limitación de tasa por IP y verificación por OTP en un cambio posterior.
- **[Fuerza bruta sobre login]** → Sin límite de intentos en esta versión. Mitigación: fuera de alcance, documentado; el hash lento de PBKDF2 encarece cada intento.
- **[Login sin sesión]** → Cualquier cliente puede llamar endpoints futuros con el `accountId` sin probar identidad. Mitigación: el siguiente cambio debe introducir sesión antes de exponer operaciones de billetera.
- **[`ddl-auto=update` sin migraciones]** → Hibernate crea o modifica tablas pero no las elimina ni versiona. Mitigación: documentado; se recomienda introducir Flyway en un cambio posterior.
- **[Costo de PBKDF2 en pruebas]** → Iteraciones altas ralentizan las pruebas. Mitigación: la propiedad de iteraciones se reduce solo en el perfil de prueba.
- **[Tarjeta sin validación de negocio]** → Cualquier número de hasta 16 dígitos es aceptado. Mitigación: no se almacena el número completo ni el CVV y no hay cobros en este cambio; antes de procesar pagos reales se debe reintroducir validación con la pasarela.
- **[Activación sin identidad]** → Cualquiera que conozca el `accountId` puede completar pasos. Mitigación: UUID aleatorio no enumerable; el siguiente cambio debe exigir sesión.
- **[Cuenta pendiente que no vuelve]** → Una persona que abandona queda `PENDING_ONBOARDING` y cuenta como iniciada y no completada; el login permite reanudar.
- **[Frontera de la duración]** → La duración empieza al enviar el paso 1, no al abrir la app; el tiempo previo no se mide. Mitigación: pregunta abierta del Issue, ver abajo.
- **[MAU]** → La meta de 1,000 a 2,000 MAU se mide con nuevos usuarios activos como aproximación; el MAU por uso requiere eventos de HU-02.

## Migration Plan

- **Despliegue:** las tablas nuevas se crean al arrancar con `ddl-auto=update`. No hay datos previos que migrar.
- **Rollback:** revertir el código. Hibernate no elimina tablas, así que el rollback debe incluir `DROP TABLE onboarding_events; DROP TABLE acquisition_spend; DROP TABLE linked_cards; DROP TABLE accounts;` ejecutado manualmente, si se quiere limpiar la base.
- **Configuración nueva:** `wallet.cors.allowed-origins`, `wallet.security.pbkdf2-iterations` y `wallet.analytics.api-key` (variable `WALLET_ANALYTICS_API_KEY`) deben existir en `application.properties` con valores por defecto.

## Open Questions

- **Frontera de eventos para la duración (Issue #21, pregunta abierta).** Se implementa de forma provisional inicio = `ONBOARDING_STARTED` (paso 1 enviado) y fin = `ONBOARDING_COMPLETED`. Debe confirmarla el Product Owner.
- **KYC y documentación (Issue #21, supuestos).** Cumplimiento y producto deben definirlos; hasta entonces el onboarding no los incluye.
- **Carga del gasto de adquisición.** Se asume que finanzas carga `acquisition_spend` fuera de la API; falta confirmar el proceso y el formato de los nombres de canal.
- **Los pasos 2 y 3** (antes bloqueantes) se toman del prototipo; el responsable confirmó esta interpretación.
