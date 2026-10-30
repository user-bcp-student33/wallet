# Spec Delta

## Purpose

Define cómo una persona completa el paso 1 del onboarding (`ACCOUNT_SETUP`, "Configura tu cuenta") creando su cuenta de Wallet Perú con celular y contraseña y vinculando una tarjeta de prueba, garantizando que el CVV nunca se almacena ni se devuelve. Contrato de origen: Issue #21 (HU-01), criterios 1, 2 y 6.

## ADDED Requirements

### Requirement: Registro de cuenta con celular, contraseña y tarjeta
El sistema SHALL exponer `POST /accounts`. Un registro válido SHALL responder 201 con `accountId`, marca, últimos 4 dígitos y estado del onboarding. La cuenta SHALL iniciar como `PENDING_ONBOARDING`, con el primer paso completado, y no podrá usarse para pagos hasta activarse (Issue #21, criterio 3).

#### Scenario: Registro exitoso
- **WHEN** se envía un celular válido, una contraseña válida, una tarjeta numérica válida y un CVV válido
- **THEN** el sistema responde 201 con `accountId`, la marca y los últimos 4 dígitos de la tarjeta
- **AND** la respuesta incluye el estado `PENDING_ONBOARDING`, 1 paso completado de 3 y `WALLET_PREPARATION` como paso actual
- **AND** la respuesta no contiene la contraseña, el CVV ni el número completo de la tarjeta

#### Scenario: Campos obligatorios ausentes
- **WHEN** se envía una petición sin celular, contraseña, tarjeta o CVV
- **THEN** el sistema responde 400 indicando cada campo inválido
- **AND** no crea ninguna cuenta

### Requirement: Bloqueo del avance con mensajes accionables
El sistema SHALL bloquear el avance del onboarding cuando falte información obligatoria o haya datos inválidos. Cada respuesta 400 MUST incluir, por cada campo inválido, el nombre del campo y un mensaje en español que indique cómo corregirlo (por ejemplo "Ingresa un celular peruano válido de 9 dígitos."), sin eco del valor enviado.

#### Scenario: Varios campos inválidos
- **WHEN** se envía un celular de 8 dígitos y una contraseña de 5 caracteres
- **THEN** el sistema responde 400 con un error por cada uno de los campos `phoneNumber` y `password`
- **AND** cada error contiene un mensaje que explica cómo corregir el campo
- **AND** la respuesta no repite los valores enviados

#### Scenario: Cuenta no creada ante datos inválidos
- **WHEN** el registro es rechazado con 400
- **THEN** no existe cuenta, tarjeta ni evento de onboarding para ese intento

### Requirement: Canal de adquisición del registro
El sistema SHALL aceptar un campo opcional `acquisitionChannel` (entre 1 y 40 caracteres en minúscula, dígitos, guion o guion bajo) y guardarlo con la cuenta para la atribución del gasto de adquisición. Si no se envía, el sistema SHALL usar `direct`.

#### Scenario: Canal informado
- **WHEN** se envía un registro válido con `acquisitionChannel` igual a `campania-lima`
- **THEN** la cuenta queda asociada al canal `campania-lima`

#### Scenario: Canal omitido
- **WHEN** se envía un registro válido sin `acquisitionChannel`
- **THEN** la cuenta queda asociada al canal `direct`

#### Scenario: Canal con formato inválido
- **WHEN** se envía `acquisitionChannel` con espacios o mayúsculas
- **THEN** el sistema responde 400 indicando que el canal es inválido

### Requirement: Validación del celular
El sistema SHALL aceptar como celular únicamente exactamente 9 dígitos que empiecen con 9, sin espacios, guiones ni prefijos de país. El sistema MUST rechazar cualquier otro valor con 400 sin normalizarlo.

#### Scenario: Celular válido
- **WHEN** se envía el celular `987654321`
- **THEN** el sistema acepta el celular y continúa con la validación de los demás campos

#### Scenario: Celular con longitud incorrecta
- **WHEN** se envía el celular `98765432` (8 dígitos) o `9876543210` (10 dígitos)
- **THEN** el sistema responde 400 indicando que el celular es inválido

#### Scenario: Celular que no inicia en 9
- **WHEN** se envía el celular `887654321`
- **THEN** el sistema responde 400 indicando que el celular es inválido

#### Scenario: Celular con caracteres no numéricos
- **WHEN** se envía el celular `987 654 321` o `+51987654321`
- **THEN** el sistema responde 400 indicando que el celular es inválido

### Requirement: Validación de la contraseña
El sistema SHALL aceptar contraseñas de entre 8 y 128 caracteres, de cualquier composición, tal como indica el campo "Mínimo 8 caracteres" del formulario de registro. El sistema MUST rechazar con 400 cualquier contraseña fuera de ese rango, sin devolver la contraseña en la respuesta.

#### Scenario: Contraseña válida de longitud mínima
- **WHEN** se envía la contraseña `abcdefgh` (8 caracteres, solo letras)
- **THEN** el sistema acepta la contraseña

#### Scenario: Contraseña válida de longitud máxima
- **WHEN** se envía una contraseña de 128 caracteres
- **THEN** el sistema acepta la contraseña

#### Scenario: Contraseña demasiado corta
- **WHEN** se envía la contraseña `abc1234` (7 caracteres)
- **THEN** el sistema responde 400 indicando que la contraseña no cumple la política

#### Scenario: Contraseña demasiado larga
- **WHEN** se envía una contraseña de 129 caracteres
- **THEN** el sistema responde 400 indicando que la contraseña no cumple la política

### Requirement: Validación de la tarjeta
El sistema SHALL aceptar como número de tarjeta únicamente una cadena de 1 a 16 dígitos numéricos y MUST rechazar con 400 cualquier otro valor. El sistema MUST NOT aplicar otra validación al número (algoritmo de Luhn, lista de tarjetas, prefijos o longitud mínima). La marca se informa solo como dato derivado del primer dígito (4 `VISA`, 5 `MASTERCARD`, otro `OTHER`). Esta regla reemplaza la validación anterior (13 a 19 dígitos, Luhn y lista de prueba) por solicitud del responsable.

#### Scenario: Tarjeta numérica válida
- **WHEN** se envía un número de 1 a 16 dígitos, por ejemplo `1234567890123456`
- **THEN** el sistema acepta la tarjeta sin otra validación

#### Scenario: Tarjeta que no cumple Luhn
- **WHEN** se envía un número de 16 dígitos que no cumple Luhn
- **THEN** el sistema acepta la tarjeta

#### Scenario: Tarjeta con caracteres no numéricos
- **WHEN** se envía `4111 1111 1111 1111`, `4111-1111` o `abcd`
- **THEN** el sistema responde 400 indicando que la tarjeta solo admite números, hasta 16 dígitos

#### Scenario: Tarjeta de más de 16 dígitos
- **WHEN** se envía un número de 17 dígitos
- **THEN** el sistema responde 400 indicando que la tarjeta solo admite números, hasta 16 dígitos

### Requirement: Tratamiento del CVV
El sistema SHALL exigir un CVV de exactamente 3 dígitos, usarlo únicamente para validar el formato de la solicitud y descartarlo. El sistema MUST NOT almacenar, registrar en logs ni devolver el CVV en ninguna respuesta. El largo exacto concreta el prototipo por solicitud del responsable.

#### Scenario: CVV con formato válido
- **WHEN** se envía un CVV de exactamente 3 dígitos
- **THEN** el sistema continúa con el registro sin persistir el CVV

#### Scenario: CVV con formato inválido
- **WHEN** se envía un CVV de 2 o 4 dígitos, o con letras
- **THEN** el sistema responde 400 indicando que el CVV es inválido y no crea la cuenta

#### Scenario: CVV ausente en la base de datos y en logs
- **WHEN** un registro exitoso se completa
- **THEN** ningún registro persistido ni ninguna línea de log contiene el valor del CVV

### Requirement: Persistencia segura de la cuenta y la tarjeta
El sistema SHALL guardar la cuenta con el celular como identificador único y la contraseña solo como hash salado no reversible. La tarjeta vinculada SHALL guardar únicamente la marca y los últimos 4 dígitos, nunca el número completo. Ambos registros MUST crearse en una misma operación atómica.

#### Scenario: Contraseña no almacenada en texto plano
- **WHEN** un registro exitoso se completa
- **THEN** la base de datos no contiene la contraseña en texto plano
- **AND** el valor almacenado es distinto en dos registros que usen la misma contraseña

#### Scenario: Número completo de tarjeta no almacenado
- **WHEN** un registro exitoso se completa
- **THEN** la base de datos solo contiene la marca y los últimos 4 dígitos de la tarjeta

#### Scenario: Fallo al vincular la tarjeta no deja cuenta parcial
- **WHEN** la operación de vinculación de la tarjeta falla después de crear la cuenta
- **THEN** el sistema responde con error 500 genérico
- **AND** no queda ninguna cuenta, tarjeta ni evento de onboarding persistido para ese intento

### Requirement: Celular único por cuenta
El sistema SHALL permitir una sola cuenta por celular. Si el celular ya está registrado, el sistema MUST responder 409 con el mensaje "Este número de celular ya está en uso." y sin ningún otro dato de la cuenta existente. El Issue #21 no lo prohíbe y el responsable pidió informar al usuario; esto permite saber si un celular está registrado (ver riesgo de enumeración en `design.md`).

#### Scenario: Registro con celular existente
- **WHEN** se envía un registro con un celular que ya tiene una cuenta
- **THEN** el sistema responde 409 con el mensaje "Este número de celular ya está en uso."
- **AND** no crea una nueva cuenta ni modifica la existente

### Requirement: Respuestas de error sin detalles internos
El sistema SHALL responder los errores de registro con un código HTTP y un mensaje genérico. El sistema MUST NOT incluir trazas de pila, nombres de tablas, consultas SQL ni valores de entrada sensibles en las respuestas de error.

#### Scenario: Error interno durante el registro
- **WHEN** ocurre una excepción no controlada durante el registro
- **THEN** el sistema responde 500 con un mensaje genérico
- **AND** la respuesta no contiene trazas, consultas SQL ni la contraseña enviada
