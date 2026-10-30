# Tasks

## 1. Preparación del proyecto

- [x] 1.1 Agregar `spring-boot-starter-validation`, H2 de prueba, JaCoCo y el gate de cobertura de línea del 85%; verificar compilación.
- [ ] 1.2 Agregar las propiedades Wallet a `application.properties`. El contexto de Spring y las pruebas arrancan con H2; falta verificar el arranque contra PostgreSQL de `compose.yml` (Docker no está disponible en este entorno).

## 2. Validación de datos de registro

- [x] 2.1 Implementar y probar validaciones de celular, contraseña y CVV con mensajes accionables.
- [x] 2.2 Implementar y probar la validación de tarjeta: solo dígitos, hasta 16, sin otra regla.

## 3. Hash de contraseña

- [x] 3.1 Implementar PBKDF2-HMAC-SHA256 con sal e iteraciones configurables; probar hashes distintos, verificación correcta/incorrecta y formatos malformados.

## 4. Persistencia

- [x] 4.1 Crear las entidades y repositorios de cuenta y tarjeta; verificar esquema y unicidad con integración H2 en modo PostgreSQL.
- [x] 4.2 Verificar por integración que no se persisten CVV ni PAN completo.

## 5. Registro de cuenta

- [x] 5.1 Implementar el servicio transaccional; verificar registro, duplicados y rollback ante fallo al vincular tarjeta con integración.
- [x] 5.2 Implementar `POST /accounts`, respuesta 201 y validación accionable; verificar contratos 201/400/409 con `@WebMvcTest`.
- [x] 5.3 Verificar atomicidad y ausencia de datos sensibles con integración H2.

## 6. Login

- [x] 6.1 Implementar login con estado de onboarding y errores uniformes; verificar contratos y ausencia de token o sesión.
- [x] 6.2 Verificar por integración el login posterior al registro y el hash de contraseña.

## 7. Errores y CORS

- [x] 7.1 Implementar el manejador global; verificar respuestas sin detalles internos.
- [x] 7.2 Configurar CORS restringido y probar preflight desde orígenes permitidos y denegados.

## 8. Flujo de onboarding y eventos

- [x] 8.1 Implementar consulta y avance de pasos, activación y errores 404/409/400; verificar escenarios por integración y capa web.
- [x] 8.2 Implementar eventos transaccionales y probar el flujo y los pasos rechazados.

## 9. Analítica mensual

- [x] 9.1 Implementar resumen mensual, mediana, adquisición y CAC por canal/total; probar meses impares, pares, vacíos y canales sin usuarios.
- [x] 9.2 Implementar endpoint analítico protegido y validar clave/mes; probar contrato e integración.

## 10. Frontend web (`web/`)

- [x] 10.1 Implementar las rutas y el shell Wallet con estilos del prototipo y `HttpClient`; verificar build.
- [x] 10.2 Implementar y probar validadores puros y estado de sesión.
- [x] 10.3 Implementar pantallas, rutas con guardas y transiciones (`/`, `/registro`, `/login`, `/preparacion`, `/cuenta-lista`, `/contactos`); verificar pruebas Vitest, cobertura de líneas superior al 85% y navegación por clic en el navegador con el backend simulado.
- [ ] 10.4 Revisar en navegador el flujo completo contra el backend real. La interfaz se revisó en escritorio y a 360 px; falta el flujo live porque Docker/PostgreSQL no está disponible.

## 11. Verificación integral

- [x] 11.1 Ejecutar `.\mvnw.cmd verify`; termina con BUILD SUCCESS.
- [x] 11.2 Generar JaCoCo y verificar el gate de cobertura del 85% (BUILD SUCCESS).
- [ ] 11.3 Ejecutar escenarios manuales con `curl` contra PostgreSQL real y guardar evidencia (pendiente de Docker/PostgreSQL disponible).

## Workflow follow-up

- Archivar el cambio con `openspec archive onboarding-login` una vez revisado y aprobado por el responsable.
- Verificar que los specs de `account-onboarding`, `onboarding-flow`, `onboarding-analytics`, `user-authentication` y `wallet-web-app` se copiaron a `openspec/specs/`.
