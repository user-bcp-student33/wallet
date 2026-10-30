---
name: sonarqube-analysis
description: 'Ejecuta análisis de calidad y seguridad con SonarQube sobre backend (Maven) y web (Angular), consulta el quality gate, la cobertura y los issues abiertos, y resume los hallazgos priorizados. Úsalo cuando el usuario pida analizar código con SonarQube, revisar el quality gate, listar issues de Sonar o preparar hallazgos antes de un PR.'
---

# Análisis de código con SonarQube

Ejecuta el análisis de SonarQube sobre los componentes del repositorio y reporta el resultado. Este skill **analiza y reporta**; no corrige código ni cambia el quality gate.

Componentes:

| Componente | Directorio | Herramienta de análisis | Cobertura |
|------------|------------|-------------------------|-----------|
| backend | `backend/` | `sonar:sonar` (plugin Maven) | `backend/target/site/jacoco/jacoco.xml` |
| web | `web/` | `sonar-scanner` (CLI) | `web/coverage/lcov.info` (ver nota) |

## Reglas

- **Nunca** escribas tokens en archivos, commits, comandos ni en la respuesta. Usa las variables de entorno `SONAR_HOST_URL` y `SONAR_TOKEN`.
- No imprimas el valor de `SONAR_TOKEN`; verifica solo que existe.
- No agregues `NOSONAR`, exclusiones (`sonar.exclusions`, `sonar.coverage.exclusions`) ni supresiones para ocultar hallazgos sin justificación explícita aprobada por el usuario.
- No cambies el quality gate ni desactives reglas del perfil de calidad.
- No crees ni modifiques `sonar-project.properties` ni otros archivos de configuración sin pedirlo.
- No hagas `git push`, no crees PR y no corrijas código como parte de este skill. Si el usuario quiere corregir hallazgos, propón la lista y pide confirmación.
- Ejecuta todos los comandos con **Git Bash** (`bash`), desde la raíz del repositorio, no con PowerShell.
- Ejecuta Maven desde `backend/` con `./mvnw` y `npm` desde `web/`.
- Usa `mode: sync` con `initial_wait` de 120 segundos o más para el análisis; puede tardar varios minutos.

## 1. Validar prerrequisitos

Comprueba sin mostrar secretos:

```bash
[ -n "$SONAR_HOST_URL" ] && echo "SONAR_HOST_URL: definido" || echo "SONAR_HOST_URL: NO definido"
[ -n "$SONAR_TOKEN" ] && echo "SONAR_TOKEN: definido" || echo "SONAR_TOKEN: NO definido"
java -version
node --version
sonar-scanner --version
```

Detén el proceso y pide al usuario configurar las variables si falta alguna. Si falta Java 21, Node 22.12+ o `sonar-scanner`, informa qué falta y no instales software sin confirmación.

Confirma que el servidor responde (este endpoint no requiere token):

```bash
curl -s "$SONAR_HOST_URL/api/system/status"
```

La respuesta debe contener `"status":"UP"`. Si no, detén el proceso.

## 2. Confirmar las claves de proyecto

Usa las claves `wallet-backend` y `wallet-web` salvo que el usuario indique otras. Confírmalas en el servidor antes de analizar:

```bash
curl -s -H "Authorization: Bearer $SONAR_TOKEN" "$SONAR_HOST_URL/api/projects/search?projects=wallet-backend,wallet-web"
```

Si un proyecto no existe, pregunta al usuario si debe crearse en SonarQube. No lo crees tú.

## 3. Analizar el backend

Desde `backend/`, genera primero la cobertura JaCoCo y luego envía el análisis. El plugin toma `SONAR_HOST_URL` y `SONAR_TOKEN` del entorno:

```bash
cd backend
./mvnw -B verify sonar:sonar -Dsonar.projectKey=wallet-backend -Dsonar.projectName=wallet-backend
```

Reglas:

- `verify` ejecuta las pruebas y genera `target/site/jacoco/jacoco.xml`. Si alguna prueba falla, **detente**, reporta el fallo y no envíes un análisis con cobertura incompleta.
- Comprueba `$?` (0 = análisis enviado).

## 4. Analizar la web

Desde `web/`, genera la cobertura con Vitest y ejecuta el scanner. La CLI de `sonar-scanner` lee `SONAR_HOST_URL` y `SONAR_TOKEN` del entorno, así que no pases el token como argumento:

```bash
cd ../web
npm ci
npm test
sonar-scanner -Dsonar.projectKey=wallet-web -Dsonar.projectName=wallet-web -Dsonar.sources=src
```

Si `npm test` falla, **detente** y no envíes el análisis.

Nota de cobertura: `web/vitest.config.ts` exporta solo `text`, `json-summary` y `html`. Sin `lcov`, Sonar no recibe cobertura de la web. Si `web/coverage/lcov.info` no existe, ejecuta el análisis igualmente, informa que la cobertura de web no fue enviada y propón agregar el reporter `lcov` como cambio separado.

## 5. Consultar resultados

Espera a que termine el procesamiento en el servidor antes de consultar. Quality gate de cada proyecto:

```bash
for key in wallet-backend wallet-web; do
  curl -s -H "Authorization: Bearer $SONAR_TOKEN" "$SONAR_HOST_URL/api/qualitygates/project_status?projectKey=$key"
done
```

Métricas clave:

```bash
curl -s -H "Authorization: Bearer $SONAR_TOKEN" "$SONAR_HOST_URL/api/measures/component?component=wallet-backend&metricKeys=coverage,bugs,vulnerabilities,code_smells,duplicated_lines_density,security_hotspots"
```

Issues abiertos, priorizados por severidad:

```bash
curl -s -H "Authorization: Bearer $SONAR_TOKEN" "$SONAR_HOST_URL/api/issues/search?componentKeys=wallet-backend&resolved=false&severities=BLOCKER,CRITICAL,MAJOR&ps=100&s=SEVERITY"
```

Repite con `componentKeys=wallet-web`. Para hotspots de seguridad pendientes usa `api/hotspots/search?projectKey=<clave>&status=TO_REVIEW`.

Si hay más de 100 resultados, pagina con `p=2`, `p=3`, etc. y reporta el campo `total`, no solo los primeros 100.

## 6. Evaluar cobertura del código modificado

El proyecto exige **85% o más de cobertura en el código modificado**. Identifica los archivos cambiados:

```bash
git diff --name-only HEAD
```

Si no hay cambios locales, compara contra la rama base (confírmala con `gh repo view --json defaultBranchRef --jq .defaultBranchRef.name`) usando `git diff --name-only origin/<base>...HEAD`.

Consulta la cobertura por archivo y revisa solo los archivos modificados:

```bash
curl -s -H "Authorization: Bearer $SONAR_TOKEN" "$SONAR_HOST_URL/api/measures/component_tree?component=wallet-backend&metricKeys=coverage&qualifiers=FIL&ps=500"
```

## 7. Reporte final

Responde en español, en este orden:

1. **Quality gates** por proyecto (`OK` o `ERROR`) con la condición que falló, si aplica.
2. **Métricas clave** por proyecto: cobertura, bugs, vulnerabilidades, code smells, duplicación y hotspots pendientes.
3. **Tabla de hallazgos**, ordenada por severidad y luego por confianza:

| # | Severidad | Proyecto | Archivo | Línea | Regla | Descripción |
|---|-----------|----------|---------|-------|-------|-------------|
| 1 | BLOCKER | wallet-backend | `src/main/java/...` | 42 | java:SXXXX | Descripción breve |

Usa rutas relativas a cada componente en la tabla, no absolutas.

4. **Cobertura del código modificado** y si cumple el 85%.
5. **Pendientes**: lo que no pudo ejecutarse (por ejemplo, cobertura de web) y por qué.
6. **Siguiente paso**: ofrece elegir entre corregir BLOCKER/CRITICAL, corregir todos los hallazgos o guardar el resumen en un archivo que el usuario indique. No crees archivos de reporte sin pedirlo.

Al mencionar archivos del workspace, usa enlaces Markdown con la ruta absoluta.
