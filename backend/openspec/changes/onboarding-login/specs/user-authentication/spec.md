# Spec Delta

## Purpose

Define cómo un usuario registrado de Wallet Perú se autentica con su celular y su contraseña, con respuestas uniformes ante credenciales inválidas y sin exponer datos sensibles.

## ADDED Requirements

### Requirement: Login con celular y contraseña
El sistema SHALL exponer `POST /auth/login`, que recibe un celular y una contraseña. Si ambas coinciden con una cuenta registrada, el sistema SHALL responder 200 con el `accountId`, el celular, el estado de la cuenta y el estado del onboarding, tanto si la cuenta está `ACTIVE` como si está `PENDING_ONBOARDING`, para que el cliente reanude el onboarding o habilite el punto de entrada de pagos según el estado.

#### Scenario: Login exitoso
- **WHEN** se envía el celular y la contraseña de una cuenta registrada y activa
- **THEN** el sistema responde 200 con `accountId`, el celular y el estado `ACTIVE`
- **AND** la respuesta no contiene la contraseña ni su hash

#### Scenario: Login con onboarding pendiente
- **WHEN** se envía el celular y la contraseña de una cuenta con estado `PENDING_ONBOARDING` y 1 paso completado
- **THEN** el sistema responde 200 con el estado `PENDING_ONBOARDING`, `completedSteps` igual a 1 y `WALLET_PREPARATION` como paso actual

#### Scenario: Login tras registro
- **WHEN** una persona se registra con `POST /accounts` y luego envía las mismas credenciales a `POST /auth/login`
- **THEN** el sistema responde 200 con el mismo `accountId` devuelto en el registro

### Requirement: Respuesta uniforme ante credenciales inválidas
El sistema SHALL responder 401 con el mismo cuerpo y el mismo mensaje genérico tanto si el celular no está registrado como si la contraseña es incorrecta, para no revelar qué celulares existen.

#### Scenario: Contraseña incorrecta
- **WHEN** se envía el celular de una cuenta registrada con una contraseña distinta a la guardada
- **THEN** el sistema responde 401 con el mensaje genérico de credenciales inválidas

#### Scenario: Celular no registrado
- **WHEN** se envía un celular sin cuenta asociada
- **THEN** el sistema responde 401 con el mismo cuerpo y mensaje que en el caso de contraseña incorrecta

### Requirement: Validación de formato antes de autenticar
El sistema SHALL rechazar con 400 las solicitudes de login con celular con formato inválido o con contraseña ausente, sin indicar si el celular existe. El sistema MUST NOT aplicar la política de longitud de la contraseña de registro en el login, para no rechazar credenciales de cuentas ya existentes.

#### Scenario: Celular con formato inválido
- **WHEN** se envía un celular que no tiene 9 dígitos iniciados en 9
- **THEN** el sistema responde 400 indicando que el celular es inválido

#### Scenario: Contraseña ausente
- **WHEN** se envía un login sin contraseña
- **THEN** el sistema responde 400 indicando que la contraseña es obligatoria

#### Scenario: Contraseña fuera de la política de registro en login
- **WHEN** se envía un login con una contraseña de 5 caracteres
- **THEN** el sistema no responde 400 por longitud y la trata como credencial incorrecta, respondiendo 401 si no coincide con la guardada

### Requirement: Verificación de contraseña sin texto plano
El sistema SHALL verificar la contraseña comparándola contra el hash salado almacenado en el registro de la cuenta. El sistema MUST NOT registrar la contraseña ni su hash en logs ni en respuestas.

#### Scenario: Contraseña verificada contra hash
- **WHEN** se envía una contraseña para autenticar
- **THEN** el sistema compara la contraseña con el hash almacenado sin recuperar la contraseña original

#### Scenario: Contraseña ausente en logs
- **WHEN** ocurre un login exitoso o fallido
- **THEN** ninguna línea de log contiene la contraseña enviada ni su hash

### Requirement: Respuesta de login sin token ni sesión
En esta versión el sistema SHALL responder el login exitoso únicamente con los datos del usuario. El sistema MUST NOT emitir tokens, cookies de sesión ni otro mecanismo de sesión en `POST /auth/login`.

#### Scenario: Respuesta sin credenciales de sesión
- **WHEN** un login es exitoso
- **THEN** la respuesta no contiene token, cookie de sesión ni encabezado de autorización

### Requirement: Consumo desde el frontend
El sistema SHALL permitir que el frontend consuma `POST /accounts`, `GET /accounts/{accountId}/onboarding`, `POST /accounts/{accountId}/onboarding/steps/{step}/complete` y `POST /auth/login` desde los orígenes configurados en CORS, y MUST rechazar peticiones desde orígenes no configurados.

#### Scenario: Origen configurado
- **WHEN** una petición de login llega desde un origen incluido en la configuración CORS
- **THEN** la respuesta incluye los encabezados CORS que permiten al navegador leerla

#### Scenario: Origen no configurado
- **WHEN** una petición llega desde un origen que no está en la configuración CORS
- **THEN** la respuesta no incluye encabezados CORS que permitan al navegador leerla
