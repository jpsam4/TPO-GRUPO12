# Revisión previa a la entrega — Grupo 12 · Valdoria

Revisión del repositorio contra `docs/CONSIGNAS-TPO.md`, con el criterio de
corrección de la materia (sección 4: puntaje por algoritmo, admisibilidad y
penalizaciones). Hecha con Claude Code (ver «Uso de IA» en el README).

## 1. Puntaje por algoritmo (sección 4)

| Algoritmo | Pts | Nivel | Observaciones |
|---|:---:|---|---|
| BFS | 1 | Completo | Marca al encolar, `ArrayDeque`, O(V+E) justificado. |
| DFS | 1 | Completo | Recursivo, O(V+E). |
| Dijkstra | 1 | Completo | Heap con borrado perezoso, reconstrucción con `predecesor[]`, rechaza negativos. Verificado a mano (VB1→VB8 = 20). |
| Prim | 1 | Completo | Heap, `key/parent/inMST`, detecta grafo no conexo. MST de Valle de Brezo = 25 a mano. |
| Kruskal | 1 | Completo | Union-Find propio con compresión y rango; MergeSort propio de aristas. |
| Greedy | 1 | Completo | Documentado como NO óptimo, con contraejemplo (30 vs 35). |
| QuickSort / MergeSort | 1 | Completo | Mediana de tres, 3 vías, recursión sobre el tramo chico; MergeSort estable. |
| PD (mochila 0/1 y Floyd) | 1 | Completo | Tabla dp con recuperación; Floyd con `k` afuera y detección de ciclo negativo. |
| Backtracking | 1 | Completo | Poda por costo y saltos, deshace, compara con/sin poda. |
| Branch & Bound | 1 | Completo | Cota inferior optimista max(carga actual, ⌈total/k⌉) + poda por simetría; reporta podados. |
| **Total** | **10** | | Ningún algoritmo consulta la base dentro de su bucle. 108 tests al empezar la revisión, todos en verde. |

## 2. Hallazgos y correcciones

| # | Hallazgo | Riesgo según la consigna | Estado |
|---|---|---|---|
| 1 | `docs/RED-Y-CONEXION.md` mostraba la password local de Neo4j. | Admisibilidad: «ninguna credencial en el repositorio». | **Corregido**: se quitó del árbol y se reescribió el historial para que tampoco aparezca en commits anteriores (ver §3). |
| 2 | `SeleccionController` tenía un `for` que filtraba objetos por región. | −0,5: «lógica dentro de un controller». | **Corregido**: se movió a `service/ItemService`. |
| 3 | `ManejadorDeErrores` no tenía red de seguridad: un error no previsto (base caída) o una excepción con mensaje `null` (`Map.of` tira NPE) salía con la respuesta por defecto de Spring. | −0,5: «endpoint que devuelve un stack trace». | **Corregido**: handler genérico (500 con mensaje genérico, detalle sólo en el log), 404/405 en JSON y cuerpo null-safe. |
| 4 | `/api/seleccion/dinamica?capacidad=2000000000` reservaba una tabla de (n+1)·(W+1) doubles → `OutOfMemoryError`. | «Corre sin error». | **Corregido**: tope de 10⁷ celdas → 400 con la explicación de por qué es pseudopolinómica. |
| 5 | `greedy?capacidad=NaN` se aceptaba y devolvía una mochila vacía. | Robustez. | **Corregido**: 400. |
| 6 | Mensaje de Kruskal en grafo no conexo informaba mal la cantidad de vértices unidos. | Cosmético. | **Corregido**: informa aristas y componentes. |
| 7 | UCS opcional respondía 501. | Opcional. | **Implementado** con parada al extraer el destino y `/ucs/comparacion` (VB1→VB8: 8 zonas expandidas vs 38 de Dijkstra). |

Tests después de las correcciones: **118**, todos en verde (`./mvnw test`).
Nuevos: `UcsTest` (5), y casos en `GrafoControllerTest` (500 sin traza, 404,
405, UCS) y `SeleccionTest` (NaN, capacidad enorme).

## 3. Pendientes de la entrega (resueltos)

1. **Tag `entrega-final`** (Hito 12, obligatorio): **creado** sobre el commit que se corrige.
2. **Credencial en el historial**: **resuelto**. Se reescribió el historial para quitar la password local de Neo4j de todos los commits; era de una base local en Docker ya descartada, nunca de Aura.
3. **Participación**: **resuelto**. El `.mailmap` unifica los nombres y emails
   de cada integrante, así `git shortlog -sn` muestra un renglón por persona.

## 4. Preguntas probables del coloquio sobre este código

- ¿Por qué UCS para al **extraer** el destino y no al encolarlo? (DA4→DA9: entra con 13, sale con 11.)
- ¿Por qué la cota del B&B es optimista y qué pasaría con una pesimista?
- ¿Por qué Floyd admite aristas negativas y Dijkstra no? ¿Cómo se detecta el ciclo negativo?
- ¿Por qué la mochila 0/1 es pseudopolinómica y qué protege el tope de celdas?

## 5. Segunda pasada (code review de los cambios)

| Hallazgo | Corrección |
|---|---|
| El handler genérico convertía errores 5xx propios de Spring (ej. 503 por timeout) en 500 y los logueaba como bug. | Se respeta el código de cualquier `ErrorResponse`; los 5xx se registran como aviso. |
| Para los 4xx de Spring se perdía el detalle (ej. el 405 no decía qué método se usó). | Se devuelve `ProblemDetail.getDetail()` de Spring. |
| `/repartir?contenedores=0` consultaba la base antes de rechazar el parámetro (y con la base caída daba 500 en vez de 400). | `RamificacionYPoda.exigirContenedoresValidos` se llama antes de pedir los objetos. |
| UCS duplicaba el bucle de Dijkstra y la reconstrucción del camino. | `dijkstraDesde` recibe un destino opcional para la parada temprana y hay un único `reconstruirCamino`. |
| El tope de la tabla dp era una constante fija, ajena al heap real. | Configurable con `app.mochila.max-celdas` y recortado a 1/8 del heap. |
| Los tests usaban nombres completamente calificados. | Imports estáticos. |
