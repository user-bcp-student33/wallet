# Spec Delta

## Purpose

Define la secuencia guiada de pasos del onboarding de Wallet Perú, el paso actual en todo momento y la activación de la wallet al finalizar. Contrato de origen: Issue #21 (HU-01), criterios 1 y 3.

## ADDED Requirements

### Requirement: Secuencia fija de pasos de onboarding
El sistema SHALL definir el onboarding como tres pasos ordenados, tomados del prototipo `mockups.html` ("PASO 1 DE 3"): `ACCOUNT_SETUP` (Configura tu cuenta), `WALLET_PREPARATION` (Preparando tu billetera) y `WALLET_READY` (Cuenta lista). El paso `ACCOUNT_SETUP` se completa al registrar la cuenta.

#### Scenario: Secuencia tras el registro
- **WHEN** una cuenta se registra correctamente
- **THEN** su onboarding indica 3 pasos en total, 1 completado y `WALLET_PREPARATION` como paso actual

### Requirement: Consulta del estado del onboarding
El sistema SHALL exponer `GET /accounts/{accountId}/onboarding`, que responde 200 con el estado de la cuenta (`PENDING_ONBOARDING` o `ACTIVE`), el total de pasos, los pasos completados y el paso actual (nulo cuando la cuenta está activa), para que el cliente muestre en todo momento la secuencia y el paso actual y pueda reanudar el flujo.

#### Scenario: Consulta de una cuenta en onboarding
- **WHEN** se consulta el onboarding de una cuenta con 1 paso completado
- **THEN** el sistema responde 200 con estado `PENDING_ONBOARDING`, `completedSteps` igual a 1, `totalSteps` igual a 3 y `currentStep` igual a `WALLET_PREPARATION`

#### Scenario: Cuenta inexistente
- **WHEN** se consulta el onboarding de un `accountId` que no existe
- **THEN** el sistema responde 404 con un mensaje genérico

### Requirement: Finalización de pasos en orden
El sistema SHALL exponer `POST /accounts/{accountId}/onboarding/steps/{step}/complete`, que marca como completado el paso indicado solo si es el paso actual. El sistema MUST responder 409 si el paso no es el actual o si el onboarding ya finalizó, sin modificar la cuenta.

#### Scenario: Completar el paso actual
- **WHEN** se completa `WALLET_PREPARATION` en una cuenta cuyo paso actual es `WALLET_PREPARATION`
- **THEN** el sistema responde 200 con 2 pasos completados y `WALLET_READY` como paso actual

#### Scenario: Saltar un paso
- **WHEN** se intenta completar `WALLET_READY` en una cuenta cuyo paso actual es `WALLET_PREPARATION`
- **THEN** el sistema responde 409 y la cuenta mantiene sus pasos completados

#### Scenario: Repetir un paso ya completado
- **WHEN** se intenta completar `ACCOUNT_SETUP` en una cuenta que ya lo completó
- **THEN** el sistema responde 409

#### Scenario: Paso desconocido
- **WHEN** se solicita completar un paso que no pertenece a la secuencia
- **THEN** el sistema responde 400 indicando que el paso no es válido

### Requirement: Activación de la wallet al finalizar el onboarding
La cuenta SHALL pasar a `ACTIVE` solo al completar `WALLET_READY`. Mientras esté `PENDING_ONBOARDING`, el frontend MUST NOT mostrar el punto de entrada a pagos de bajo monto. Los endpoints de pago quedan en HU-02.

#### Scenario: Activación al completar el último paso
- **WHEN** se completa `WALLET_READY` en una cuenta con 2 pasos completados
- **THEN** el sistema responde 200 con estado `ACTIVE`, 3 pasos completados y `currentStep` nulo

#### Scenario: Cuenta incompleta sigue pendiente
- **WHEN** una cuenta tiene menos de 3 pasos completados
- **THEN** su estado es `PENDING_ONBOARDING`
