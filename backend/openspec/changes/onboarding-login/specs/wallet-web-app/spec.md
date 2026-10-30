# Spec Delta

## Purpose

Define la experiencia web de onboarding y acceso de Wallet Perú, basada en el prototipo `mockups.html` y en el Issue #21 (HU-01), criterios 1, 2 y 3. Reemplaza la aplicación ReqFlow de `web/` por decisión del responsable.

## ADDED Requirements

### Requirement: Secuencia y paso actual visibles en el onboarding
La aplicación web SHALL mostrar en todas las pantallas del onboarding (registro, preparación de la billetera y cuenta lista) la secuencia de 3 pasos, el número del paso actual con el texto "PASO N DE 3" y una barra de progreso proporcional a ese paso.

#### Scenario: Pantalla de registro
- **WHEN** la persona abre el registro
- **THEN** ve "PASO 1 DE 3", el nombre "Configura tu cuenta" y el progreso del primer paso

#### Scenario: Preparación de la billetera
- **WHEN** el registro se completó correctamente
- **THEN** la pantalla muestra "PASO 2 DE 3" y "Preparando tu billetera"

#### Scenario: Cuenta lista
- **WHEN** la preparación terminó
- **THEN** la pantalla muestra "PASO 3 DE 3" y "Cuenta lista"

### Requirement: Validación local y mensajes accionables
La aplicación SHALL validar registro y login según las reglas del backend, bloquear datos inválidos y mostrar mensajes accionables. Enviará tarjeta y celular solo con dígitos y mostrará errores del backend como texto, nunca como HTML.

#### Scenario: Celular inválido
- **WHEN** la persona intenta continuar con un celular de 8 dígitos
- **THEN** no se envía ninguna petición y se muestra "Ingresa un celular peruano válido de 9 dígitos."

#### Scenario: Error del backend
- **WHEN** el backend responde 400 con errores por campo
- **THEN** la aplicación permanece en el paso actual y muestra el mensaje del backend para cada campo

#### Scenario: Celular ya registrado
- **WHEN** el backend responde 409
- **THEN** la aplicación permanece en el paso 1 y muestra bajo el campo de celular "Este número de celular ya está en uso."

#### Scenario: Credenciales incorrectas en el login
- **WHEN** el backend responde 401 al iniciar sesión
- **THEN** la aplicación permanece en el login, muestra "Celular o contraseña incorrectos." y vuelve a habilitar el botón "Ingresar"

#### Scenario: Backend no disponible
- **WHEN** la petición de registro, login o avance falla sin respuesta del backend
- **THEN** la aplicación muestra "No pudimos conectar con Wallet Perú. Inténtalo nuevamente." y vuelve a habilitar el botón de la acción

### Requirement: Avance del onboarding sincronizado con el backend
La aplicación web SHALL completar cada paso llamando al backend: el paso 1 con `POST /accounts`, el paso 2 con `POST /accounts/{accountId}/onboarding/steps/WALLET_PREPARATION/complete` y el paso 3 con `.../WALLET_READY/complete`. La aplicación MUST NOT conservar tarjeta, CVV ni contraseña después del envío; solo conserva el `accountId` y el estado del onboarding.

#### Scenario: Flujo completo
- **WHEN** la persona completa el registro y los dos pasos restantes
- **THEN** la aplicación recibe el estado `ACTIVE` y habilita la pantalla de contactos

#### Scenario: Datos sensibles
- **WHEN** el registro es enviado
- **THEN** la aplicación no guarda tarjeta, CVV ni contraseña en almacenamiento del navegador ni en el estado de la aplicación

#### Scenario: Canal de adquisición fijo
- **WHEN** la persona envía el registro
- **THEN** la aplicación envía `acquisitionChannel` igual a `direct` sin mostrar ningún campo para editarlo

### Requirement: Navegación por rutas entre pantallas
La aplicación web SHALL asociar cada pantalla a una ruta (`/`, `/registro`, `/login`, `/preparacion`, `/cuenta-lista`, `/contactos`) y cada botón o enlace de acción SHALL navegar a su pantalla destino sin recargar la página. Las pantallas de preparación y cuenta lista SHALL abrirse solo cuando son el paso actual del onboarding; en otro caso la aplicación SHALL redirigir al inicio.

#### Scenario: Botones de inicio, registro y login
- **WHEN** la persona pulsa "Crear una cuenta", "Ya tengo una cuenta", "Volver al inicio" o el enlace "Crear una cuenta" del login
- **THEN** la aplicación abre `/registro`, `/login`, `/` y `/registro` respectivamente

#### Scenario: Avance del onboarding
- **WHEN** el backend confirma el registro, la preparación o la activación
- **THEN** la aplicación abre `/preparacion`, `/cuenta-lista` y `/contactos` respectivamente

#### Scenario: Botón atrás del navegador
- **WHEN** la persona usa atrás o adelante del navegador entre pantallas permitidas
- **THEN** la aplicación muestra la pantalla de esa ruta sin perder la sesión en memoria

#### Scenario: Paso no disponible
- **WHEN** se abre `/preparacion` o `/cuenta-lista` sin ser el paso actual del onboarding
- **THEN** la aplicación redirige al inicio

### Requirement: Punto de entrada de pagos solo con wallet activa
La aplicación web SHALL permitir el acceso a la pantalla de contactos (punto de entrada de pagos de bajo monto) únicamente cuando el estado de la cuenta es `ACTIVE`. Si la cuenta está `PENDING_ONBOARDING`, la aplicación SHALL llevar a la persona al paso actual del onboarding. El flujo de pago posterior a la selección del contacto queda fuera de alcance (HU-02).

#### Scenario: Login con cuenta activa
- **WHEN** la persona inicia sesión y el backend responde estado `ACTIVE`
- **THEN** la aplicación abre la pantalla de contactos

#### Scenario: Login con onboarding pendiente
- **WHEN** la persona inicia sesión y el backend responde `PENDING_ONBOARDING` con paso actual `WALLET_PREPARATION`
- **THEN** la aplicación abre `/preparacion`, el paso 2 del onboarding

#### Scenario: Acceso directo a contactos sin sesión activa
- **WHEN** se abre la ruta de contactos sin una cuenta con estado `ACTIVE`
- **THEN** la aplicación redirige al inicio

### Requirement: Pantallas del prototipo
La aplicación web SHALL ofrecer las pantallas inicio, registro, login, preparación de la billetera, cuenta lista y contactos del prototipo, con saldo fijo `S/ 0.00` y la lista de contactos de demostración del prototipo. La aplicación SHALL poder usarse en pantallas de 360 px de ancho sin desplazamiento horizontal.

#### Scenario: Cerrar sesión
- **WHEN** la persona pulsa "Cerrar sesión" en contactos
- **THEN** la aplicación borra el estado de la cuenta y vuelve al inicio
