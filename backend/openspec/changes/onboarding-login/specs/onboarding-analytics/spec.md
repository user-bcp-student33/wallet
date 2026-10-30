# Spec Delta

## Purpose

Define los eventos analíticos del onboarding y la salida mensual necesaria para verificar las metas del MVP en Perú. Contrato de origen: Issue #21 (HU-01), criterios 4, 5 y 6.

## ADDED Requirements

### Requirement: Registro de eventos de onboarding
El sistema SHALL persistir eventos `ONBOARDING_STARTED`, `STEP_COMPLETED` y `ONBOARDING_COMPLETED` en los puntos respectivos del flujo. Cada evento SHALL incluir `accountId` y fecha UTC, y MUST NOT contener datos personales ni sensibles.

#### Scenario: Eventos tras el registro
- **WHEN** una cuenta se registra correctamente
- **THEN** existen un evento `ONBOARDING_STARTED` y un evento `STEP_COMPLETED` para `ACCOUNT_SETUP` de esa cuenta

#### Scenario: Evento por paso
- **WHEN** se completa el paso `WALLET_PREPARATION`
- **THEN** se registra un evento `STEP_COMPLETED` con el paso `WALLET_PREPARATION`

#### Scenario: Cierre del flujo
- **WHEN** se completa el último paso
- **THEN** se registran un evento `STEP_COMPLETED` para `WALLET_READY` y un evento `ONBOARDING_COMPLETED`

#### Scenario: Pasos rechazados no generan eventos
- **WHEN** un intento de completar un paso es rechazado con 409
- **THEN** no se registra ningún evento

### Requirement: Resumen mensual de onboarding
`GET /analytics/onboarding/monthly?month=YYYY-MM` SHALL responder con conteos de inicio y finalización y la mediana en segundos de los flujos completados en el mes `America/Lima`. La mediana SHALL ser nula si no hubo finalizaciones.

#### Scenario: Mediana con número impar de onboardings
- **WHEN** en el mes se completaron onboardings con duraciones de 120, 180 y 600 segundos
- **THEN** el sistema responde `medianCompletionSeconds` igual a 180 y `newActiveUsers` igual a 3

#### Scenario: Mediana con número par de onboardings
- **WHEN** en el mes se completaron onboardings con duraciones de 120 y 180 segundos
- **THEN** el sistema responde `medianCompletionSeconds` igual a 150

#### Scenario: Mes sin onboardings completados
- **WHEN** se consulta un mes sin onboardings completados
- **THEN** el sistema responde 200 con `newActiveUsers` igual a 0 y `medianCompletionSeconds` nulo

#### Scenario: Onboarding iniciado y no completado
- **WHEN** una cuenta inició el onboarding en el mes y no lo completó
- **THEN** cuenta en `startedCount` y no cuenta en `newActiveUsers`

#### Scenario: Mes con formato inválido
- **WHEN** se consulta con `month` distinto de `YYYY-MM`
- **THEN** el sistema responde 400 indicando el formato esperado

### Requirement: Atribución del gasto de adquisición y CAC
El resumen SHALL incluir usuarios activos nuevos, gasto en PEN y CAC por canal y total. El gasto SHALL leerse de `acquisition_spend`, cargado fuera de la API; no habrá endpoint de escritura. El CAC SHALL ser nulo si no hay usuarios activos.

#### Scenario: CAC por canal
- **WHEN** el canal `campania-lima` tiene 100 nuevos usuarios activos en el mes y un gasto de PEN 1,000.00
- **THEN** el resumen muestra para ese canal `newActiveUsers` 100, `acquisitionSpendPen` 1000.00 y `cacPen` 10.00

#### Scenario: Canal con gasto y sin usuarios
- **WHEN** un canal tiene gasto en el mes y ningún nuevo usuario activo
- **THEN** el resumen incluye el canal con `newActiveUsers` 0 y `cacPen` nulo

#### Scenario: Totales
- **WHEN** hay varios canales con gasto y usuarios en el mes
- **THEN** los totales son la suma de usuarios y de gasto y el CAC total es esa suma de gasto entre esa suma de usuarios

### Requirement: Acceso restringido a la salida analítica
El sistema SHALL exigir el encabezado `X-Analytics-Key` con el valor configurado en `wallet.analytics.api-key` para consultar `/analytics/**`. El sistema MUST responder 403 si el encabezado falta o no coincide, y MUST responder 403 siempre cuando la clave no está configurada. La comparación SHALL ser en tiempo constante.

#### Scenario: Clave correcta
- **WHEN** se consulta el resumen con la clave configurada
- **THEN** el sistema responde 200

#### Scenario: Clave ausente o incorrecta
- **WHEN** se consulta el resumen sin clave o con una clave distinta
- **THEN** el sistema responde 403 sin datos analíticos

#### Scenario: Clave no configurada
- **WHEN** `wallet.analytics.api-key` está vacía
- **THEN** el sistema responde 403 a toda consulta de analítica
