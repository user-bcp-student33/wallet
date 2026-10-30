---
name: user-story-html-prototype
description: 'Crea un prototipo HTML interactivo, autocontenido (HTML + CSS + JavaScript sin dependencias), a partir de la descripción de una historia de usuario con criterios de aceptación. Úsalo cuando el usuario pida un prototipo, mockup interactivo o demo navegable de una historia de usuario.'
---

# Prototipo HTML interactivo desde una historia de usuario

Tu objetivo es convertir una historia de usuario en un prototipo HTML navegable que permita validar el flujo con stakeholders antes de implementar la aplicación.

## 1. Recabar la entrada

Antes de generar nada, confirma que tienes:

- **Historia de usuario** en formato `Como <rol>, quiero <acción>, para <beneficio>`.
- **Criterios de aceptación** (si no existen, propón 3 a 5 y pide confirmación).
- **Identificador** de la historia (por ejemplo `HU-01`). Si no lo hay, usa un slug kebab-case derivado del título.

Si falta información crítica (rol, flujo principal o datos de ejemplo), usa `ask_user` con preguntas concisas. No inventes reglas de negocio no descritas; márcalas como supuestos visibles en el prototipo.

## 2. Analizar antes de diseñar

Enumera en una lista corta:

1. Pantallas o estados necesarios (formulario, confirmación, error, vacío, éxito).
2. Acciones del usuario y su resultado esperado.
3. Validaciones y mensajes de error derivados de los criterios.
4. Datos de ejemplo (ficticios; nunca datos reales, credenciales ni tokens).
5. Mapa de criterios de aceptación → elemento del prototipo que los demuestra.

## 3. Ubicación y nombre

- Ruta por defecto: `prototypes/<id-o-slug>/index.html`, desde la raíz del repositorio.
- Si el usuario indica otra ruta, respétala.
- No modifiques código al generar un prototipo.

## 4. Reglas del archivo HTML

Genera **un único archivo** `index.html` que cumpla:

- **Autocontenido:** CSS en `<style>` y JavaScript en `<script>` dentro del mismo archivo. Sin CDN, sin npm, sin build y sin fuentes externas. Debe abrirse con doble clic en el navegador.
- **Idioma:** español, con `<html lang="es">`.
- **Responsivo:** diseño usable en escritorio y móvil (CSS con flexbox/grid y `max-width`).
- **Accesible:** `<label>` asociado a cada campo, `role="alert"` en mensajes de error, foco visible y navegación por teclado.
- **Estado simulado:** el flujo se opera completo con datos en memoria o `localStorage` bajo una clave prefijada (por ejemplo `proto-HU-07-*`). Nunca uses claves ni valores que parezcan reales.
- **Sin llamadas de red:** toda respuesta de "servidor" se simula en JavaScript y el prototipo muestra un banner "Prototipo: datos simulados".
- **Atributos de prueba:** cada elemento interactivo tiene `data-testid` descriptivo (por ejemplo `data-testid="btn-confirmar-transferencia"`) para facilitar pruebas E2E futuras.
- **Trazabilidad visible:** un panel plegable "Criterios de aceptación" lista cada criterio con su estado (cumplido / pendiente) y el ID de la historia.
- **Supuestos visibles:** una sección o nota en el prototipo con los supuestos tomados.

## 5. Estructura recomendada del HTML

```
<header>      Título: "<ID> – <título>" y etiqueta "Prototipo"
<main>        Pantalla(s) del flujo; cambio de vista con JS (sin recargar)
<aside>       Criterios de aceptación y supuestos (plegable)
<footer>      Botón "Reiniciar prototipo" que limpia el estado simulado
<script>      Estado en un objeto único, funciones pequeñas y nombradas por propósito
```

Buenas prácticas de código dentro del archivo:

- Constantes con nombre para valores mágicos (límites, tiempos de espera simulados, textos repetidos).
- Una función por responsabilidad: validar, renderizar, simular respuesta, mostrar mensaje.
- Nombres que expliquen el propósito (`validarMonto`, `mostrarConfirmacion`), no el tipo (evitar `data`, `temp`).
- Retornos anticipados en validaciones en lugar de anidar `if`.
- Comentarios solo donde haya algo que aclarar.

## 6. Cobertura de flujos

Como mínimo, el prototipo debe permitir recorrer:

- **Camino feliz** completo hasta el estado de éxito.
- **Al menos un error de validación** por cada criterio que tenga una condición de fallo.
- **Estado vacío o sin resultados**, si la historia lista o busca datos.
- **Reinicio** del flujo.

## 7. Verificación

Antes de dar el trabajo por terminado:

1. Abre el archivo en el navegador y recorre el camino feliz y los errores.
2. Confirma que no hay referencias externas: busca `http://`, `https://`, `<link` y `<script src` en el archivo; solo se permiten enlaces de texto informativos.
3. Confirma que cada criterio de aceptación aparece mapeado en el panel de trazabilidad.
4. Confirma que no hay secretos, tokens ni datos reales.

## 8. Entrega

Responde al usuario con:

- Ruta del archivo generado (con enlace Markdown).
- Pantallas y flujos incluidos.
- Supuestos tomados y preguntas abiertas.
- Cómo abrirlo: doble clic sobre `index.html`.
- Qué criterios quedaron cubiertos y cuáles requieren decisión de negocio.

No crees archivos de planificación ni notas adicionales salvo que el usuario los pida. No hagas commit ni push salvo que el usuario lo solicite.
