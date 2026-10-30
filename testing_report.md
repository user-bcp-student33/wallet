# Informe de pruebas — HU-01 Onboarding y login (`onboarding-login`)

- Repositorio: `user-bcp-student33/wallet`, rama `feature/HU-01-user-onboarding`
- Componentes: backend Spring Boot (`backend/`), web Angular (`web/`)
- Fuente de criterios: `backend/openspec/changes/onboarding-login/specs/*/spec.md` (cinco deltas)
- Identificadores: `H-API-NN` (backend de cuenta, onboarding y analítica), `H-AUTH-NN` (login), `H-WEB-NN` (frontend). Son una adaptación de la convención de IDs de la instrucción del repositorio a este proyecto.
- Alcance: no se modificó código de producción ni de prueba. Las correcciones se proponen en la sección 7. Se creó y eliminó una sonda temporal (sección 6.3).

## 0. Resumen de resultados

| Bloque | Ejecutado | Resultado | Cobertura real |
|---|---|---|---|
| Backend (`mvnw verify`) | 71 pruebas | 71 PASA, 0 FALLA | JaCoCo: instrucciones 97.9 %, ramas 92.1 % (calculado del CSV de JaCoCo) |
| Frontend (`vitest run`) | 48 pruebas en 6 archivos | 48 PASA, 0 FALLA | V8: líneas 100 %, ramas 98.88 %, funciones 97.5 % |
| Build de producción web (`ng build`) | 1 build | PASA | Sin cobertura |
| Sonda de validación (salto de línea en teléfono) | 1 prueba temporal | PASA (comportamiento esperado: 400) | No aplica |
| Tareas de verificación real (tasks 1.2, 10.4, 11.3) | No ejecutadas | NO EJECUTADO | No aplica |

No hay pruebas FALLA. Hay huecos de cobertura de criterios del spec, marcados como NO EJECUTADO en la matriz.

## 1. Matriz de trazabilidad

Criterio de aceptación (spec) -> ID de prueba -> resultado esperado. Los IDs de clase se detallan en la sección 2.

### 1.1 Backend: `account-onboarding`

| Criterio | IDs de prueba | Resultado esperado | Estado |
|---|---|---|---|
| Registro exitoso | H-API-01, H-API-38 | 201 con `accountId`, tarjeta (marca y últimos 4) y estado de onboarding; sin contraseña ni número completo en la respuesta | PASA |
| Campos obligatorios ausentes | H-API-09 | 400 con un error por cada campo faltante; el servicio no se invoca | PASA |
| Varios campos inválidos | H-API-10 | 400 con todos los errores a la vez | PASA |
| Cuenta no creada ante datos inválidos | H-API-02, H-API-10 | 400 y `AccountService` sin interacción | PASA |
| Canal informado | H-API-11 | El canal llega al servicio sin cambios | PASA |
| Canal omitido (default `direct`) | Sin prueba backend (ver brecha G-01). Cubierto solo en web por H-WEB-06 ("registers with digits only and the default channel...") | El canal llega como `direct` | NO EJECUTADO (backend) |
| Canal con formato inválido | H-API-12 | 400 con mensaje de canal | PASA |
| Celular válido | H-API-01 (`987654321`) | Aceptado | PASA |
| Celular con longitud incorrecta | H-API-02 (8 y 10 dígitos) | 400 | PASA |
| Celular que no inicia en 9 | H-API-02 (`887654321`) | 400 | PASA |
| Celular con caracteres no numéricos | H-API-02 (`987 654 321`, `+51987654321`) | 400 | PASA |
| Contraseña de 8 caracteres válida | H-API-05 | Aceptada | PASA |
| Contraseña de 128 caracteres válida | H-API-05 | Aceptada | PASA |
| Contraseña demasiado corta | H-API-03 | 400 | PASA |
| Contraseña de 129 caracteres | H-API-04 | 400 | PASA |
| Tarjeta numérica válida | H-API-08 | Aceptada sin otra validación | PASA |
| Tarjeta que no cumple Luhn | H-API-08 (los parámetros aceptan cualquier dígito; ver nota N-1) | Aceptada | PASA (parcial, ver N-1) |
| Tarjeta con caracteres no numéricos | H-API-07 | 400 | PASA |
| Tarjeta de más de 16 dígitos | H-API-07 | 400 | PASA |
| CVV con formato válido | H-API-01 (CVV `123`) | Aceptado | PASA |
| CVV con formato inválido | H-API-06 | 400 | PASA |
| CVV ausente en la base de datos | H-API-39 | Ni CVV ni número completo persistidos | PASA |
| CVV ausente en logs | Sin prueba (brecha G-02) | Sin CVV en la salida de logs | NO EJECUTADO |
| Contraseña no almacenada en texto plano | H-API-38, H-API-44 a H-API-46 | Hash PBKDF2 almacenado, sin texto plano | PASA |
| Número completo de tarjeta no almacenado | H-API-38, H-API-39 | Solo últimos 4 dígitos | PASA |
| Fallo al vincular tarjeta no deja cuenta parcial | H-API-42 | Rollback de cuenta y eventos | PASA |
| Registro con celular existente | H-API-14, H-API-37 | 409 `ACCOUNT_CONFLICT`; una sola cuenta | PASA |
| Error interno durante el registro | H-API-15 | 500 `INTERNAL_ERROR` sin detalles internos | PASA |

### 1.2 Backend: `onboarding-flow`

| Criterio | IDs de prueba | Resultado esperado | Estado |
|---|---|---|---|
| Secuencia tras el registro | H-API-18, H-API-36 | 3 pasos, 1 completado, `WALLET_PREPARATION` actual | PASA |
| Consulta de una cuenta en onboarding | H-API-18 | 200 con estado y pasos | PASA |
| Cuenta inexistente | H-API-19, H-API-40 | 404 `ACCOUNT_NOT_FOUND` con mensaje genérico | PASA |
| Completar el paso actual | H-API-20, H-API-36 | 200 y avance al siguiente paso | PASA |
| Saltar un paso | H-API-21 | 409 `ONBOARDING_STEP_CONFLICT` | PASA |
| Repetir un paso ya completado | H-API-21 (no es el paso actual) | 409 | PASA (cubierto por la misma condición; sin prueba dedicada) |
| Paso desconocido | H-API-22 | 400 | PASA |
| Activación al completar el último paso | H-API-20, H-API-36 | 200, estado `ACTIVE`, 3 pasos completados, `currentStep` nulo | PASA |
| Cuenta incompleta sigue pendiente | H-AUTH-06 y H-API-36 (`PENDING_ONBOARDING` tras el registro). Tras 2 pasos: prueba propuesta G-05 (sección 7), verificada en sonda temporal | Estado `PENDING_ONBOARDING` tras el registro y tras 1 o 2 pasos | PASA (registro); PASA en sonda temporal (tras 2 pasos, ver sección 7); tras 1 paso: NO EJECUTADO |

### 1.3 Backend: `user-authentication`

| Criterio | IDs de prueba | Resultado esperado | Estado |
|---|---|---|---|
| Login exitoso | H-AUTH-01 | 200 con `PENDING_ONBOARDING`, `completedSteps` 1, `WALLET_PREPARATION` | PASA |
| Login con onboarding pendiente | H-AUTH-01, H-AUTH-06 | Mismo estado que en registro | PASA |
| Login tras registro | H-AUTH-06 (integración) | Misma cuenta y estado pendiente | PASA |
| Contraseña incorrecta | H-AUTH-02, H-AUTH-06 | 401 `INVALID_CREDENTIALS` con cuerpo idéntico | PASA |
| Celular no registrado | H-AUTH-02, H-AUTH-06 | 401 idéntico al de contraseña incorrecta | PASA |
| Celular con formato inválido | H-AUTH-03 | 400 | PASA |
| Contraseña ausente | H-AUTH-04 | 400 indicando contraseña obligatoria | PASA |
| Contraseña fuera de la política de registro en login | H-AUTH-05 | No se aplica la política de registro en login | PASA |
| Contraseña verificada contra hash | H-API-44 a H-API-47 | Verificación correcta sin texto plano | PASA |
| Contraseña ausente en logs | Sin prueba (brecha G-02) | Sin contraseña en logs | NO EJECUTADO |
| Respuesta sin credenciales de sesión | H-AUTH-01 | Sin token, cookie ni encabezados de sesión | PASA |
| Origen configurado | H-AUTH-07 | Preflight permitido solo desde `http://localhost:4200` | PASA |
| Origen no configurado | H-AUTH-07, H-API-17 | Sin encabezados CORS que permitan leer la respuesta | PASA |

### 1.4 Backend: `onboarding-analytics`

| Criterio | IDs de prueba | Resultado esperado | Estado |
|---|---|---|---|
| Eventos tras el registro | H-API-36 | Eventos `ONBOARDING_STARTED`, `STEP_COMPLETED` x3, `ONBOARDING_COMPLETED` | PASA |
| Evento por paso | H-API-36 | Un `STEP_COMPLETED` por cada `OnboardingStep` | PASA |
| Cierre del flujo | H-API-36 | Evento `ONBOARDING_COMPLETED` presente | PASA |
| Pasos rechazados no generan eventos | H-API-36 (conteo 2 tras dos 409, línea 105; lista exacta de 5 eventos tras 409 final, líneas 113-121) | Sin eventos nuevos tras 409 | PASA |
| Mediana con número impar | H-API-29 | 180 s con tres valores | PASA |
| Mediana con número par | H-API-30 | Mediana del par | PASA |
| Mes sin onboardings completados | H-API-31 | Mediana y CAC nulos | PASA |
| Onboarding iniciado y no completado | H-API-41 (`startedCount` 3, `newActiveUsers` 2, líneas 204-205) | Solo completados cuentan como activos | PASA |
| Mes con formato inválido | H-API-27, H-API-28 | 400 | PASA |
| CAC por canal | H-API-33, H-API-41 | CAC por canal | PASA |
| Canal con gasto y sin usuarios | Sin prueba explícita (brecha G-04) | CAC nulo en ese canal | NO EJECUTADO |
| Totales | H-API-33 | Totales de usuarios y CAC | PASA |
| Acceso con clave correcta | H-API-24 | 200 | PASA |
| Clave ausente o incorrecta | H-API-25, H-API-26, H-API-34 | 403 | PASA |
| Clave no configurada | H-API-35 | 403 a toda consulta | PASA |
| Ventana de mes en zona America/Lima | H-API-32 | Mes calendario de Lima | PASA |

### 1.5 Frontend: `wallet-web-app`

Los IDs web agrupan por archivo (sección 2.7). Se leyeron las aserciones de los specs de componente, guards, validación, sesión y API. Las filas "PASA" de esta matriz están verificadas en el código fuente; la única fila PARCIAL es "Datos sensibles".

| Criterio (escenario del spec) | IDs de prueba | Resultado esperado | Estado |
|---|---|---|---|
| Pantalla de registro / Preparación / Cuenta lista | H-WEB-06 ("derives the visible progress label...", "moves from preparation to ready...") | Paso y progreso visibles | PASA (por nombre) |
| Celular inválido | H-WEB-02 ("detects invalid registration fields"), H-WEB-06 ("blocks invalid registrations before calling the API") | Bloqueo antes de llamar a la API | PASA (por nombre) |
| Error del backend (campos) | H-WEB-06 ("shows field-level messages returned by the backend") | Mensajes por campo del backend | PASA (por nombre) |
| Celular ya registrado | H-WEB-06 ("shows that the phone is in use under the phone field...") | Mensaje bajo el campo de celular; se permanece en registro | PASA (por nombre) |
| Credenciales incorrectas en el login | H-WEB-06 ("shows a uniform message for invalid login credentials") | Mensaje uniforme | PASA (por nombre) |
| Backend no disponible | Sin prueba dedicada (brecha G-06). Parcial: H-WEB-06 ("keeps the current page and shows the error when a step cannot be completed"), que solo cubre fallo de red al completar un paso, no de registro ni de login | Error visible y se mantiene la página | NO EJECUTADO (dedicada) |
| Flujo completo | H-WEB-06 ("registers with digits only...", "moves from preparation to ready...") | Avance según el backend | PASA (por nombre) |
| Datos sensibles | H-WEB-06 ("registers with digits only..." limpia tarjeta, CVV y contraseña), H-WEB-04 (estado solo en memoria) | Sin tarjeta, CVV ni contraseña en almacenamiento | PARCIAL: limpieza verificada; ausencia en `localStorage`/`sessionStorage` sin aserción |
| Canal de adquisición fijo | H-WEB-06 ("registers with digits only and the default channel...") | Canal `direct` por defecto | PASA (por nombre) |
| Botones de inicio, registro y login | H-WEB-06 ("takes the page to display from the route and defaults to home") | Navegación por rutas | PASA (por nombre) |
| Botón atrás del navegador | Sin prueba dedicada | Navegación correcta | NO EJECUTADO |
| Paso no disponible | H-WEB-05 ("allows an onboarding page only while it is the current step") | Redirección a la página del paso actual | PASA (por nombre) |
| Login con cuenta activa | H-WEB-06 ("opens contacts for an active account and clears the password") | Acceso a contactos y limpieza de contraseña | PASA (por nombre) |
| Login con onboarding pendiente | H-WEB-06 ("blocks login with an invalid phone or missing password"), H-WEB-05 ("maps the onboarding state to its page") | No se permite contactos | PASA (por nombre) |
| Acceso directo a contactos sin sesión | H-WEB-05 ("allows contacts only for an active wallet") | Solo wallet activa | PASA (por nombre) |
| Cerrar sesión | H-WEB-06 ("clears the in-memory session and returns home on logout") | Limpieza y regreso al inicio | PASA (por nombre) |

## 2. Casos de prueba

Para cada grupo se indican preparación, datos, comandos y limpieza. Los comandos son reproducibles desde la raíz del repositorio en PowerShell.

### 2.1 Backend de cuenta, validación y CORS (H-API-01 a H-API-17)

Clase: `backend/src/test/java/com/bcp/wallet/backend/account/AccountControllerTest.java` (30 pruebas, incluidas las parametrizadas).

- Tipo: slice `@WebMvcTest(AccountController.class)` con MockMvc. `AccountService` es `@MockitoBean`.
- Preparación: ninguna. Sin base de datos ni red.
- Datos: tarjeta `4111111111111111`, CVV `123`, celular `987654321`, contraseña `clave-secreta-1`. Parametrizados: celulares `98765432`, `9876543210`, `887654321`, `987 654 321`, `+51987654321`; contraseñas de 7, 129 caracteres y de 8 a 128; CVV inválidos; tarjetas con no dígitos y de 17 dígitos.
- Comando: `cd backend; .\mvnw.cmd -B test -Dtest=AccountControllerTest`
- Limpieza: ninguna. El contexto se descarta al final del ciclo de pruebas.

| ID | Prueba (método) | Tipo |
|---|---|---|
| H-API-01 | `returnsCreatedWithAccountCardAndOnboardingState` | Positivo |
| H-API-02 | `rejectsInvalidPhonesWithAnActionableMessage` (5 valores) | Negativo |
| H-API-03 | `rejectsPasswordsShorterThanEightCharacters` | Negativo / borde |
| H-API-04 | `rejectsPasswordsLongerThan128Characters` | Negativo / borde |
| H-API-05 | `acceptsPasswordsOfAnyCompositionBetween8And128Characters` | Borde positivo |
| H-API-06 | `rejectsInvalidCvv` | Negativo |
| H-API-07 | `rejectsCardsWithNonDigitsOrMoreThan16Digits` | Negativo |
| H-API-08 | `acceptsAnyDigitsOnlyCardUpTo16Digits` | Borde positivo |
| H-API-09 | `reportsEveryMissingField` | Negativo |
| H-API-10 | `reportsSeveralInvalidFieldsAtOnce` | Negativo |
| H-API-11 | `passesTheAcquisitionChannelToTheService` | Positivo |
| H-API-12 | `rejectsAnInvalidAcquisitionChannel` | Negativo |
| H-API-13 | `rejectsMalformedJsonWithoutDetails` | Negativo |
| H-API-14 | `translatesADuplicatedPhoneToConflict` | Negativo |
| H-API-15 | `hidesInternalErrorDetails` | Negativo (seguridad) |
| H-API-16 | `allowsPreflightFromTheConfiguredOriginOnly` / `allowsPreflightFromTheConfiguredOrigin` | Integración CORS |
| H-API-17 | `rejectsPreflightFromAnotherOrigin` | Negativo (CORS) |

Nota: `AccountControllerTest` no verifica el canal por defecto (`direct`). Ver G-01.

### 2.2 Backend de onboarding (H-API-18 a H-API-23)

Clase: `backend/src/test/java/com/bcp/wallet/backend/onboarding/OnboardingControllerTest.java` (6 pruebas). Slice `@WebMvcTest(OnboardingController.class)`.

- Preparación y limpieza: ninguna (servicio mockeado).
- Comando: `cd backend; .\mvnw.cmd -B test -Dtest=OnboardingControllerTest`

| ID | Prueba | Tipo |
|---|---|---|
| H-API-18 | `returnsTheStepSequenceAndCurrentStep` | Positivo |
| H-API-19 | `returnsNotFoundWithAGenericMessage` | Negativo |
| H-API-20 | `activatesTheAccountWhenTheLastStepIsCompleted` | Positivo |
| H-API-21 | `returnsConflictWhenTheStepIsNotTheCurrentOne` | Negativo |
| H-API-22 | `rejectsAnUnknownStep` | Negativo |
| H-API-23 | `rejectsAMalformedAccountId` | Negativo |

### 2.3 Backend de login (H-AUTH-01 a H-AUTH-07)

Clases: `backend/src/test/java/com/bcp/wallet/backend/auth/AuthControllerTest.java` (6 pruebas) e integración `OnboardingFlowIntegrationTest` (H-AUTH-06).

- Preparación: slice `@WebMvcTest(AuthController.class)` con `AuthService` mockeado.
- Comando: `cd backend; .\mvnw.cmd -B test -Dtest=AuthControllerTest`
- Limpieza: ninguna.

| ID | Prueba | Tipo |
|---|---|---|
| H-AUTH-01 | `returnsTheUserStatusAndOnboardingWithoutCredentialsOrSession` | Positivo |
| H-AUTH-02 | `usesTheSameBodyForUnknownPhoneAndWrongPassword` | Negativo |
| H-AUTH-03 | `rejectsAnInvalidPhoneFormat` | Negativo |
| H-AUTH-04 | `rejectsAMissingPassword` | Negativo |
| H-AUTH-05 | `doesNotApplyTheRegistrationPasswordPolicyOnLogin` | Borde |
| H-AUTH-07 | `allowsPreflightFromTheConfiguredOriginOnly` | CORS |
| H-AUTH-06 | `OnboardingFlowIntegrationTest.loginAfterRegistrationReturnsTheSameAccountAndItsPendingState` y `rejectsWrongPasswordAndUnknownPhoneWithTheSameBody` | Integración |

### 2.4 Backend de analítica (H-API-24 a H-API-35)

Clases: `analytics/AnalyticsControllerTest.java` (5), `analytics/OnboardingAnalyticsServiceTest.java` (7, Mockito puro).

- Preparación: el servicio usa repositorios mockeados; no hay base de datos. El controlador usa `@Import(AnalyticsAccessGuard.class)`.
- Datos: clave `test-analytics-key` (de `backend/src/test/resources/application.properties`).
- Comandos: `cd backend; .\mvnw.cmd -B test -Dtest=AnalyticsControllerTest,OnboardingAnalyticsServiceTest`
- Limpieza: ninguna.

| ID | Prueba | Tipo |
|---|---|---|
| H-API-24 | `AnalyticsControllerTest.returnsTheMonthlySummaryWithTheConfiguredKey` | Positivo |
| H-API-25 | `AnalyticsControllerTest.rejectsAMissingKey` | Negativo |
| H-API-26 | `AnalyticsControllerTest.rejectsAWrongKey` | Negativo |
| H-API-27 | `AnalyticsControllerTest.rejectsAnInvalidMonthFormat` | Negativo |
| H-API-28 | `AnalyticsControllerTest.rejectsAMissingMonth` | Negativo |
| H-API-29 | `OnboardingAnalyticsServiceTest.medianOfAnOddNumberOfOnboardings` | Positivo |
| H-API-30 | `OnboardingAnalyticsServiceTest.medianOfAnEvenNumberOfOnboardings` | Borde |
| H-API-31 | `OnboardingAnalyticsServiceTest.emptyMonthHasNoMedianAndNoCac` | Borde |
| H-API-32 | `OnboardingAnalyticsServiceTest.usesTheLimaCalendarMonthAsTheWindow` | Borde |
| H-API-33 | `OnboardingAnalyticsServiceTest.computesCacPerChannelAndInTotal` | Positivo |
| H-API-34 | `OnboardingAnalyticsServiceTest.accessGuardAcceptsOnlyTheConfiguredKey` | Positivo / negativo |
| H-API-35 | `OnboardingAnalyticsServiceTest.accessGuardDeniesEverythingWhenNoKeyIsConfigured` | Negativo |

### 2.5 Backend de integración (H-API-36 a H-API-42, H-AUTH-06)

Clases: `backend/src/test/java/com/bcp/wallet/backend/OnboardingFlowIntegrationTest.java` (8 pruebas), `AccountRegistrationRollbackTest.java` (1), `BackendApplicationTests.java` (1, `contextLoads`).

- Tipo: `@SpringBootTest` con `@AutoConfigureMockMvc`, base H2 en memoria `jdbc:h2:mem:wallet;MODE=PostgreSQL;DB_CLOSE_DELAY=-1`, `ddl-auto=create-drop`, iteraciones PBKDF2 = 1000.
- Preparación: el contexto arranca con H2. `OnboardingFlowIntegrationTest` limpia tablas en `@BeforeEach` (`cleanDatabase`).
- Datos: flujo real de registro, pasos y login sobre H2. `AccountRegistrationRollbackTest` simula con `@MockitoBean` un fallo en `LinkedCardRepository`.
- Comando: `cd backend; .\mvnw.cmd -B test -Dtest=OnboardingFlowIntegrationTest,AccountRegistrationRollbackTest,BackendApplicationTests`
- Limpieza: `cleanDatabase()` antes de cada prueba y el H2 en memoria al terminar la JVM.

| ID | Prueba | Tipo |
|---|---|---|
| H-API-36 | `completesTheWholeOnboardingAndActivatesTheWallet` | Integración positiva |
| H-AUTH-06 | `loginAfterRegistrationReturnsTheSameAccountAndItsPendingState` | Integración |
| H-API-37 | `rejectsADuplicatedPhoneAndKeepsASingleAccount` | Integración negativa |
| H-API-38 | `storesOnlyAHashOfThePasswordAndTheLastFourCardDigits` | Persistencia |
| H-API-39 | `persistsNeitherTheCvvNorTheFullCardNumber` | Persistencia |
| H-API-40 | `returnsNotFoundForAnUnknownAccount` | Integración negativa |
| H-API-41 | `summarizesTheMonthWithNewActiveUsersAndCacPerChannel` | Integración |
| H-API-42 | `rollsBackAccountAndOnboardingEventsWhenCardLinkingFails` | Transaccional |
| H-API-43 | `BackendApplicationTests.contextLoads` | Arranque |

### 2.6 Backend de seguridad (H-API-44 a H-API-50)

Clase: `backend/src/test/java/com/bcp/wallet/backend/security/PasswordHasherTest.java` (7 pruebas, unitaria).

- Preparación y limpieza: ninguna.
- Comando: `cd backend; .\mvnw.cmd -B test -Dtest=PasswordHasherTest`

| ID | Prueba | Tipo |
|---|---|---|
| H-API-44 | `twoHashesOfTheSamePasswordDiffer` | Positivo (sal) |
| H-API-45 | `storedHashNeverContainsThePlainPassword` | Seguridad |
| H-API-46 | `acceptsTheCorrectPassword` | Positivo |
| H-API-47 | `rejectsAWrongPassword` | Negativo |
| H-API-48 | `flagsHashesWithAnotherIterationCountOrFormatForRehash` | Borde |
| H-API-49 | `warmUpDoesNotFail` | Borde |
| H-API-50 | `rejectsMalformedStoredHashes` | Negativo |

### 2.7 Frontend (H-WEB-01 a H-WEB-06, agrupadas por archivo)

Suite Vitest (`web/src/app/**/*.spec.ts`, 48 pruebas en 6 archivos). Cada ID es un archivo; las pruebas individuales se citan por nombre en la matriz 1.5.

- Preparación: ninguna. Vitest 3.2.4 con la configuración de `web/vitest.config.ts`.
- Comandos: `cd web; npm ci` (una vez), luego `npm test`, `npm run test:coverage` o `npm run build`.
- Limpieza: ninguna. `coverage/` y `dist/` están en `.gitignore`.

| ID | Archivo | Pruebas | Tipo |
|---|---|---|---|
| H-WEB-01 | `utils/legacy-format.spec.ts` | 1 | Positivo |
| H-WEB-02 | `wallet/wallet-validation.spec.ts` | 23 casos (salida Vitest, sección 5: `wallet-validation.spec.ts (23 tests)`) | Positivo, negativo, borde |
| H-WEB-03 | `wallet/wallet-api.service.spec.ts` | 3: registro a `/accounts` sin cambios, login a `/auth/login`, completar paso actual | Positivo (contrato de URL y cuerpo) |
| H-WEB-04 | `wallet/wallet-session.service.spec.ts` | 1: solo estado en memoria | Seguridad |
| H-WEB-05 | `wallet/wallet.guards.spec.ts` | 3: mapeo estado a página, contactos solo con wallet activa, paso actual | Positivo, negativo |
| H-WEB-06 | `wallet/wallet.component.spec.ts` | 17 casos (salida Vitest: `wallet.component.spec.ts (17 tests)`) | Positivo, negativo (ver matriz 1.5) |

## 3. Contratos de API verificados

Verificados por las pruebas listadas (estado PASA):

| Endpoint | Caso | Código | Esquema / error | IDs |
|---|---|---|---|---|
| `POST /accounts` | Registro válido | 201 | `accountId`, `card{brand,lastFour}`, `onboarding{status,completedSteps,totalSteps,currentStep}` | H-API-01 |
| `POST /accounts` | Campos inválidos | 400 | `VALIDATION_ERROR` con `fieldErrors[{field,message}]` | H-API-02, H-API-09, H-API-10 |
| `POST /accounts` | JSON mal formado | 400 | Sin detalles internos | H-API-13 |
| `POST /accounts` | Celular duplicado | 409 | `ACCOUNT_CONFLICT` | H-API-14, H-API-37 |
| `POST /accounts` | Error interno | 500 | `INTERNAL_ERROR` genérico | H-API-15 |
| `GET /accounts/{id}/onboarding` | Existe | 200 | Pasos y paso actual | H-API-18 |
| `GET /accounts/{id}/onboarding` | No existe | 404 | `ACCOUNT_NOT_FOUND` genérico | H-API-19, H-API-40 |
| `GET /accounts/{id}/onboarding` | ID mal formado | 400 | Error de tipo | H-API-23 |
| `POST /accounts/{id}/onboarding/steps/{step}/complete` | Paso actual | 200 | Estado actualizado; `ACTIVE` al terminar | H-API-20, H-API-36 |
| `POST .../steps/{step}/complete` | Paso no actual o cuenta activa | 409 | `ONBOARDING_STEP_CONFLICT` | H-API-21 |
| `POST .../steps/{step}/complete` | Paso desconocido | 400 | Error de tipo | H-API-22 |
| `POST /auth/login` | Credenciales válidas | 200 | `PENDING_ONBOARDING`, sin token ni cookie | H-AUTH-01 |
| `POST /auth/login` | Contraseña incorrecta o celular no registrado | 401 | `INVALID_CREDENTIALS`, cuerpo idéntico | H-AUTH-02 |
| `POST /auth/login` | Celular inválido o contraseña ausente | 400 | `VALIDATION_ERROR` | H-AUTH-03, H-AUTH-04 |
| `GET /analytics/onboarding/monthly?month=` | Clave correcta | 200 | Resumen mensual | H-API-24 |
| `GET /analytics/onboarding/monthly` | Clave ausente o incorrecta, o no configurada | 403 | `ACCESS_DENIED` | H-API-25, H-API-26, H-API-35 |
| `GET /analytics/onboarding/monthly` | Mes inválido o ausente | 400 | Error de validación | H-API-27, H-API-28 |
| CORS | Origen `http://localhost:4200`, `/accounts/**` y `/auth/login` | Preflight permitido (GET, POST, OPTIONS; `Content-Type`) | Otros orígenes rechazados | H-API-16, H-API-17, H-AUTH-07 |

Contratos NO verificados (NO EJECUTADO):
- Concurrencia en `POST .../complete` (sin prueba).
- Contrato contra PostgreSQL real (tasks 1.2 y 11.3).

## 4. Contratos de persistencia verificados

| Aspecto | Estado antes | Estado después | Prueba | Resultado |
|---|---|---|---|---|
| Registro | Sin cuenta con ese celular | 1 cuenta; 1 tarjeta con últimos 4 dígitos; hash PBKDF2 en lugar de contraseña; eventos `ONBOARDING_STARTED` y `STEP_COMPLETED` | H-API-36, H-API-38 | PASA |
| Registro sin CVV ni número completo | — | Ni CVV ni número completo en `accounts`/`linked_cards` | H-API-39 | PASA |
| Celular duplicado | 1 cuenta | Sigue 1 cuenta | H-API-37 | PASA |
| Fallo al vincular tarjeta | Sin cuenta | Rollback: sin cuenta ni eventos (transacción de `AccountService.register`) | H-API-42 | PASA |
| Activación | Pasos 1 a 2 | Estado `ACTIVE`, 3 pasos, eventos `ONBOARDING_COMPLETED` | H-API-36 | PASA |
| Paso rechazado (409) | Paso no actual | Sin evento nuevo | H-API-36 (líneas 105 y 113-121) | PASA |
| Analítica | Eventos y gasto de canal | Resumen con medianas y CAC; sin escritura de gasto | H-API-41 | PASA |
| Integridad: unicidad de celular | Constraint `accounts.phone` único | 409 y sin duplicado | H-API-37 | PASA (H2 en modo PostgreSQL) |
| Integridad: una tarjeta por cuenta | `linked_cards.account_id` único | Sin prueba directa | — | NO EJECUTADO |
| Integridad de `acquisition_spend` (`spend_month`, `channel` únicos) | — | Sin prueba directa | — | NO EJECUTADO |
| Persistencia en PostgreSQL real | — | — | tasks 1.2, 10.4, 11.3 | NO EJECUTADO |

Transacciones: `AccountService.register` es `@Transactional` (verificado por H-API-42). No hay `@Version` en `Account` (ver G-04).

## 5. Resultados

Cada salida es real y se copió de la ejecución en esta sesión (extractos de las líneas relevantes). Log completo del backend en `backend/target/verify-run.log`.

### 5.1 Backend: `cd backend; .\mvnw.cmd -B verify`

```
[INFO] Tests run: 30, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 9.489 s -- in com.bcp.wallet.backend.account.AccountControllerTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 9.949 s -- in com.bcp.wallet.backend.AccountRegistrationRollbackTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.829 s -- in com.bcp.wallet.backend.analytics.AnalyticsControllerTest
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.323 s -- in com.bcp.wallet.backend.analytics.OnboardingAnalyticsServiceTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.795 s -- in com.bcp.wallet.backend.auth.AuthControllerTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.919 s -- in com.bcp.wallet.backend.BackendApplicationTests
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.672 s -- in com.bcp.wallet.backend.onboarding.OnboardingControllerTest
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.684 s -- in com.bcp.wallet.backend.OnboardingFlowIntegrationTest
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.029 s -- in com.bcp.wallet.backend.security.PasswordHasherTest
[INFO] Tests run: 71, Failures: 0, Errors: 0, Skipped: 0
[INFO] All coverage checks have been met.
[INFO] BUILD SUCCESS
[INFO] Total time:  36.723 s
```

Cobertura JaCoCo (calculada del `backend/target/site/jacoco/jacoco.csv` de esta ejecución):

| Clase | Instrucciones | Ramas |
|---|---|---|
| Total del proyecto | 1366/1395 (97.9 %) | 35/38 (92.1 %) |
| `OnboardingAnalyticsService` | 176/176 (100 %) | 4/4 |
| `AnalyticsAccessGuard` | 34/34 | 8/8 |
| `AccountService` | 80/80 | 0/0 |
| `OnboardingService` | 83/83 | 4/4 |
| `AuthService` | 41/47 (87.2 %) | 1/2 |
| `PasswordHasher` | 149/156 (95.5 %) | 8/8 |

Meta de 85 % del código modificado: cumplida en todas las clases de la feature listadas.

### 5.2 Frontend: `cd web; npm run test:coverage`

```
 ✓ src/app/utils/legacy-format.spec.ts (1 test) 9ms
 ✓ src/app/wallet/wallet-validation.spec.ts (23 tests) 21ms
 ✓ src/app/wallet/wallet-session.service.spec.ts (1 test) 9ms
 ✓ src/app/wallet/wallet-api.service.spec.ts (3 tests) 26ms
 ✓ src/app/wallet/wallet.guards.spec.ts (3 tests) 16ms
 ✓ src/app/wallet/wallet.component.spec.ts (17 tests) 45ms

 Test Files  6 passed (6)
      Tests  48 passed (48)
   Duration  3.45s

 % Coverage report from v8
All files     |     100 |    98.88 |    97.5 |     100 |
 ...api.service.ts |     100 |      100 |      80 |     100 |
 ...-navigation.ts |     100 |      100 |     100 |     100 |
 ...ion.service.ts |     100 |      100 |     100 |     100 |
 ...-validation.ts |     100 |      100 |     100 |     100 |
 ...t.component.ts |     100 |    98.03 |     100 |     100 | 83
 wallet.guards.ts  |     100 |      100 |     100 |     100 |
```

Nota: `wallet-api.service.ts` tiene 80 % de funciones cubiertas. Las no cubiertas no se identificaron en esta sesión.

### 5.3 Frontend: `cd web; npm run build`

```
Initial total    | 283.15 kB |   79.18 kB
Lazy chunk files | wallet-component | 48.63 kB | 11.35 kB
Application bundle generation complete. [7.477 seconds]
Output location: ...\web\dist\workshop-login-frontend
```
Resultado: PASA (exit code 0).

### 5.4 Sonda temporal: teléfono con salto de línea final

Hipótesis probada: `$` en Java regex coincide antes de un salto de línea final, lo que podría aceptar `"987654321\n"`. Resultado de la sonda con la salida real:

```
PROBE_NEWLINE_PHONE_STATUS=400
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

Conclusión: la hipótesis se descarta. Jakarta `@Pattern` usa coincidencia completa, y el valor se rechaza con 400. No es un defecto. La sonda se eliminó y no queda en el repositorio.

### 5.5 Resultados NO EJECUTADO

| Ítem | Motivo |
|---|---|
| Tasks 1.2 y 11.3: arranque y `curl` contra PostgreSQL de `compose.yml` | Docker no está instalado. Salida real de `docker version`: `El término "docker" no se reconoce como nombre de un cmdlet, función, archivo de script o programa ejecutable.` Ver también `tasks.md` líneas 6 y 59. |
| Task 10.4: flujo live en navegador contra backend real | Requiere PostgreSQL vía Docker, que no está instalado (ver tarea 1.2). `tasks.md` línea 53. |
| G-01 canal omitido -> `direct` (backend) | Sin prueba backend. |
| G-02 contraseña, CVV y tarjeta completa ausentes en logs | Sin prueba. |
| G-04 concurrencia en completar pasos | Sin prueba. |
| G-05 cuenta incompleta sigue pendiente | Falta aserción tras 1 y 2 pasos. Sonda temporal tras 2 pasos: PASA. Falta tras 1 paso. |
| G-06 backend no disponible en web | Sin prueba dedicada. |
| Canal con gasto y sin usuarios (CAC nulo) | Sin prueba explícita. |
| Botón atrás del navegador (web) | Sin prueba dedicada. |
| Tiempo de respuesta del login (diferencia celular inexistente vs contraseña incorrecta) | Sin prueba. El spec no fija umbral medible. |

## 6. Comandos reproducibles desde cero

Requisitos: JDK 21 (el proyecto usa Maven Wrapper), Node.js 22.12+ y npm. Ejecutar en PowerShell desde la raíz del repositorio.

```powershell
# 0. Estado limpio
git status --short            # debe estar vacío

# 1. Backend: suite completa, JaCoCo y reglas de cobertura
cd backend
.\mvnw.cmd -B clean verify    # reporta 71 pruebas, "All coverage checks have been met", BUILD SUCCESS
# Reporte HTML: backend\target\site\jacoco\index.html

# 2. Backend por bloque
.\mvnw.cmd -B test -Dtest=AccountControllerTest
.\mvnw.cmd -B test -Dtest=AuthControllerTest
.\mvnw.cmd -B test -Dtest=OnboardingControllerTest
.\mvnw.cmd -B test -Dtest=AnalyticsControllerTest,OnboardingAnalyticsServiceTest
.\mvnw.cmd -B test -Dtest=OnboardingFlowIntegrationTest,AccountRegistrationRollbackTest,BackendApplicationTests
.\mvnw.cmd -B test -Dtest=PasswordHasherTest
cd ..

# 3. Frontend: instalación, pruebas, cobertura y build
cd web
npm ci
npm test                      # 6 archivos, 48 pruebas
npm run test:coverage         # reporte V8 en web\coverage
npm run build                 # genera web\dist
cd ..

# 4. Limpieza de artefactos generados (opcional; ambos están en .gitignore)
Remove-Item -Recurse -Force web\coverage, web\dist -ErrorAction SilentlyContinue
cd backend; .\mvnw.cmd -B clean; cd ..
```

Nota: `tools/lab.py` no sirve para este repositorio. Su rama `auth` usa Quarkus y su rama `api` usa FastAPI, distintos del backend Spring Boot. Por eso los comandos son directos.

Verificación pendiente (NO EJECUTADO): tasks 1.2, 10.4 y 11.3 requieren `docker compose up` con PostgreSQL.

## 7. Correcciones propuestas (no aplicadas)

Ningún cambio se aplicó a producción. Cada corrección indica la prueba que la detecta y cómo verificarla.

| ID | Brecha / defecto | Corrección propuesta | Prueba que la detecta | Verificación |
|---|---|---|---|---|
| G-01 | Canal omitido no se verifica en backend | Añadir prueba en `AccountControllerTest` que omite `acquisitionChannel` y captura `CreateAccountCommand` con `ArgumentCaptor`; esperado `direct` | Nueva prueba en `AccountControllerTest` | `cd backend; .\mvnw.cmd -B test -Dtest=AccountControllerTest` |
| G-02 | Sin prueba de que contraseña, CVV o tarjeta completa no aparecen en logs | Añadir prueba con `@ExtendWith(OutputCaptureExtension.class)` sobre registro y login; la salida no debe contener los valores | Nueva prueba en `AccountControllerTest` y `AuthControllerTest` | `cd backend; .\mvnw.cmd -B test -Dtest=AccountControllerTest,AuthControllerTest` |
| G-03 | Resuelto: los 409 no generan evento (H-API-36). Se retira de las correcciones | — | — | — |
| G-04 | Sin control de concurrencia en `Account` (sin `@Version`); dos completados simultáneos podrían avanzar dos pasos. Riesgo, no fallo confirmado | Propuesta separada: añadir `@Version` a la entidad y prueba de concurrencia con dos hilos | Prueba de concurrencia nueva (no ejecutada) | `cd backend; .\mvnw.cmd -B test -Dtest=OnboardingFlowIntegrationTest` (NO EJECUTADO aún) |
| G-05 | Sin aserción de `PENDING_ONBOARDING` tras 1 paso (tras 2 pasos, la sonda temporal pasó: `keepsAccountPendingAfterRegistrationAndPreparation`, 1 test, 0 fallos, `surefire-reports` XML) | Añadir `keepsAccountPendingAfterRegistrationAndPreparation` y una variante tras 1 paso en `OnboardingFlowIntegrationTest` | Nueva prueba en `OnboardingFlowIntegrationTest`; la sonda se revirtió y no está en el repo | `cd backend; .\mvnw.cmd -B test -Dtest=OnboardingFlowIntegrationTest` |
| G-06 | Backend no disponible sin prueba dedicada en web | Añadir prueba en `wallet.component.spec.ts` con `api` que rechaza y verificar que se mantiene la página y se muestra error | Nueva prueba en `wallet.component.spec.ts` | `cd web; npm test` |
| G-07 | Tiempo de respuesta del login distinto según exista el celular (riesgo de enumeración) | Propuesta: ejecutar una comparación de hash ficticia cuando el celular no existe | Prueba de tiempo (no determinista; requiere diseño) | NO EJECUTADO |

## 8. Código de pruebas

Solo la sonda temporal (ejecutada y eliminada). El resto de pruebas ya existe en el repositorio.

```java
// ProbeNewlinePhoneTest.java (temporal, eliminada tras ejecutarla)
package com.bcp.wallet.backend.account;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.bcp.wallet.backend.account.dto.AccountCreatedResponse;
import com.bcp.wallet.backend.account.dto.AccountCreatedResponse.LinkedCardResponse;
import com.bcp.wallet.backend.onboarding.OnboardingStateResponse;
import com.bcp.wallet.backend.onboarding.OnboardingStep;

@WebMvcTest(AccountController.class)
class ProbeNewlinePhoneTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountService accountService;

    @Test
    void probePhoneWithTrailingNewline() throws Exception {
        UUID accountId = UUID.randomUUID();
        when(accountService.register(any())).thenReturn(new AccountCreatedResponse(accountId,
                new LinkedCardResponse(CardBrand.VISA, "1111"),
                new OnboardingStateResponse(accountId, AccountStatus.PENDING_ONBOARDING, 1, 3,
                        OnboardingStep.WALLET_PREPARATION)));
        String json = "{\"cardNumber\":\"4111111111111111\",\"cvv\":\"123\",\"phoneNumber\":\"987654321\\n\",\"password\":\"clave-secreta-1\"}";
        int code = mockMvc.perform(post("/accounts").contentType(MediaType.APPLICATION_JSON).content(json))
                .andReturn().getResponse().getStatus();
        System.out.println("PROBE_NEWLINE_PHONE_STATUS=" + code);
        mockMvc.perform(post("/accounts").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest());
    }
}
```

## 9. Separación de lo verificado y lo supuesto

Verificado con salida real en esta sesión:
- Los 71 tests de backend pasan, JaCoCo cumple y el build termina con BUILD SUCCESS (sección 5.1).
- Los 48 tests de web pasan, con la cobertura de la sección 5.2.
- El build de producción web termina bien (sección 5.3).
- El teléfono con salto de línea final devuelve 400 (sección 5.4).
- La regla de tarjeta sin Luhn coincide con el spec (`account-onboarding`, requisito "Validación de la tarjeta").

Supuesto o no verificado:
- Mapeo de criterios web (sección 1.5) basado en nombres de prueba. Solo se leyó `wallet-api.service.spec.ts`.
- Las referencias por nombre de la matriz 1.5 no se leyeron línea a línea en `wallet.component.spec.ts`.
- Aserciones de `OnboardingFlowIntegrationTest` y de los specs web leídas en las líneas citadas. G-05 tras 2 pasos: sonda temporal en `OnboardingFlowIntegrationTest` con `-Dtest=...#keepsAccountPendingAfterRegistrationAndPreparation`, 1 test, 0 fallos (XML de Surefire, `time="17.39"`). Se revirtió; `git status` solo muestra `testing_report.md`. Sigue abierta la variante tras 1 paso.
- Docker verificado en esta sesión: no instalado (`docker` no se reconoce). PostgreSQL real no probado.
- `backend/target/surefire-reports/` de ejecuciones previas no se usó como resultado; los resultados vienen de la ejecución de esta sesión.

## 10. Preguntas abiertas

1. Ubicación: el informe está en la raíz del repositorio. Confirmar si debe moverse a otra carpeta (por ejemplo, la de la HU).
2. Convención de IDs: se adaptó `H-AUTH`/`H-API`/`H-WEB` a este proyecto. Confirmar.
3. Resuelta: el repositorio es `wallet`. Las instrucciones de `ai-labs` (`tools/lab.py`, reglas del Día 1) no aplican. Los comandos de este informe son los oficiales de Maven y npm del proyecto.
