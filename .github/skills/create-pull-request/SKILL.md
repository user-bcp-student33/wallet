---
name: create-pull-request
description: 'Crea un Pull Request cuyo título es exactamente el nombre de la rama actual, solo si las pruebas unitarias del backend y del web pasan. Úsalo cuando el usuario pida abrir, crear o preparar un PR desde la rama de trabajo.'
---

# Crear Pull Request con título igual a la rama

Crea un PR en GitHub con `gh` cuyo **título sea el nombre exacto de la rama actual** (por ejemplo `feature/skill-prototype`). Requisito obligatorio: **todas las pruebas unitarias deben pasar** antes de crear el PR.

## Reglas

- No crees el PR si alguna prueba falla, no se puede ejecutar o hay cambios sin confirmar.
- No uses `--no-verify`, no omitas pruebas y no marques resultados que no hayas observado.
- No ejecutes `git push --force` ni modifiques ramas remotas existentes.
- Ejecuta todos los comandos con **Git Bash** (`bash`), desde la raíz del repositorio, no con PowerShell.
- No registres como aprobada la sección de revisión humana; déjala pendiente.
- Usa la plantilla `.github/PULL_REQUEST_TEMPLATE.md` tal como está (encabezados, orden y comentarios).

## 1. Validar el estado del repositorio

Ejecuta desde la raíz del repositorio, en Git Bash:

```bash
git branch --show-current
git status --porcelain
gh auth status
```

Detén el proceso y explica el motivo si:

- `git branch --show-current` está vacío (HEAD desacoplado) o es la rama base del repositorio.
- `git status --porcelain` muestra cambios. Pide al usuario que confirme con la skill `git-commit` o con un commit explícito.
- `gh auth status` no está autenticado.

Obtén la rama base del repositorio (no la asumas):

```bash
gh repo view --json defaultBranchRef --jq .defaultBranchRef.name
```

Comprueba que no exista ya un PR para esta rama:

```bash
gh pr list --head "<rama>" --state all --json number,url,state
```

Si existe un PR abierto, informa su URL y termina sin crear otro.

## 2. Ejecutar las pruebas unitarias (puerta obligatoria)

Ejecuta **ambas** suites completas, sin filtrar por componente modificado. Desde la raíz:

**Backend (Maven, JDK 21):**

```bash
(cd backend && ./mvnw -B test)
```

**Web (Node 22.12+ y npm):**

```bash
(cd web && npm ci && npm test)
```

El código de salida de cada comando queda en `$?` (0 = aprobado). Verifica `$?` inmediatamente después de cada suite.

Reglas de ejecución:

- Usa `mode: sync` con `initial_wait` de 120 segundos o más; las suites pueden tardar varios minutos.
- Revisa el código de salida de **cada** comando. Si una suite falla, no continúes con la siguiente para crear el PR.
- Si falla por herramienta ausente (`java`, `node`, `npm`), informa qué falta. No instales software sin pedir confirmación.
- Guarda el comando exacto y el resultado (`passed` o `failed`) de cada suite para la sección de comprobación.

Si alguna prueba falla: **detente**. Muestra las pruebas fallidas y el resumen del error, y no crees el PR.

## 3. Publicar la rama

Solo si todas las pruebas pasaron:

```bash
git push -u origin "<rama>"
```

Si la rama ya tiene upstream y está sincronizada, omite este paso.

## 4. Redactar el cuerpo con la plantilla

Lee `.github/PULL_REQUEST_TEMPLATE.md` y reemplaza sus textos de instrucción con datos reales:

- **Cambio:** issue relacionado (si existe), IDs H-xx, componentes afectados, comportamiento anterior y resultado esperado. Obtén los componentes de `git diff --name-only <base>...HEAD`.
- **Comprobación:** cada comando de la sección 2 con su resultado real. Lista como pendiente cualquier verificación que no se haya ejecutado.
- **Revisión humana:** deja la decisión como pendiente para la persona revisora.

Enlaza solo archivos que existan. Verifica cada ruta con `test -e <ruta>` antes de citarla; si no existe, indícalo como no disponible.

Escribe el cuerpo en un archivo temporal **fuera del repositorio**, por ejemplo con `BODY_FILE=$(mktemp)` (queda en `/tmp`), nunca dentro del árbol de trabajo.

## 5. Crear el PR

```bash
gh pr create --base "<base>" --head "<rama>" --title "<rama>" --body-file "$BODY_FILE"
```

Reglas del título:

- Debe ser exactamente el nombre de la rama, sin prefijos, sufijos ni cambios de mayúsculas.
- Si la rama contiene `/`, mantenlo tal cual (`feature/skill-prototype`).

## 6. Verificar y limpiar

```bash
gh pr view "<rama>" --json number,title,url,baseRefName,headRefName
```

Confirma que `title` coincide con `headRefName`. Elimina el archivo temporal con `rm -f "$BODY_FILE"`.

## Respuesta final

Informa en pocas líneas:

- URL del PR y su título.
- Resultado de cada suite de pruebas con su comando.
- Confirmación de que la revisión humana quedó pendiente.
