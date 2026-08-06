# Consignas oficiales del TPO — Referencia rápida

Transcripción de la consigna oficial (*TPO 2026 - Consignas y rúbrica*,
Programación III · 3.4.077 · Comisión Miércoles Mañana · Docente: Manuel
Adrián Cáceres) para tener el reglamento completo en el repo, no solo el
"cómo hacerlo" de `GUIA-TPO.md`.

> Para elegir dominio y el orden de implementación, ver `GUIA-TPO.md`.
> Este documento cubre fechas, calificación, admisibilidad y reglas del curso.

---

## 1. Cómo se evalúa la cursada

Tres instancias con nota. La nota de cursada es el **promedio simple** de
las tres, y **las tres deben superar los 4 puntos**.

| Instancia | Fecha | Formato | Qué mide |
|---|---|---|---|
| Primer Parcial | 16/09 | Escrito, individual | Complejidad, recurrencias, elección de técnica |
| TPO | 28/10 | Grupal, con hitos | Construcción del sistema y de los algoritmos |
| Segundo Parcial | 11/11 y 18/11 | Codificación en vivo, individual | Que sabe escribir los algoritmos, no solo describirlos |

**Encadenamiento:** cumplir los hitos habilita a aprobar el TPO → aprobar el
TPO habilita a rendir el Segundo Parcial. El Segundo Parcial cumple además
el requisito de defensa individual del Art. 12° de la normativa académica
(no hay coloquio separado).

---

## 2. Calendario de hitos

Cada hito se entrega por **Teams antes de la clase siguiente** (link al
commit correspondiente). No llevan nota individual: cumplido / no cumplido.

**Para aprobar el TPO hace falta cumplir al menos 9 de los 12 hitos**, y los
hitos **3, 4, 5 y 12 son obligatorios sin excepción**. Los demás admiten
hasta tres incumplimientos.

| # | Fecha | Hito | Qué hay que entregar | Obligatorio |
|---|---|---|---|---|
| 1 | 05/08 | Entorno corriendo | Clonar el scaffold, levantar Neo4j y que `GET /api/grafo/resumen` responda con 8 vértices y 12 aristas. Captura en Teams. | |
| 2 | 12/08 | Ordenamiento propio | QuickSort y MergeSort sobre los items del grafo semilla, sin `Collections.sort`. Un test por cada uno. | |
| 3 | 19/08 | Grupo y dominio | Grupo conformado (3 o 4). Página en el README: dominio, nodo, arista, costo y unidad. Diagrama con ≥8 nodos y sus pesos. | **Sí** |
| 4 | 26/08 | Dominio cargado y recorridos | Dominio propio en Neo4j reemplazando la semilla. BFS y DFS funcionando sobre él. | **Sí** |
| 5 | 02/09 | Caminos mínimos y MST | Dijkstra con reconstrucción de camino, Prim y Kruskal con Union-Find. Verificados a mano sobre el grafo del grupo. | **Sí** |
| 6 | 09/09 | Greedy contra PD | Mochila 0/1 con tabla dp y recuperación del camino. Documentar un juego de datos donde el greedy del hito 3 NO dé el óptimo. | |
| 7 | 23/09 | Todos contra todos | Floyd-Warshall con detección de ciclos negativos. Comparar nodos expandidos contra Dijkstra corrido V veces. | |
| 8 | 30/09 | Exploración con poda | Backtracking de rutas simples con restricción de costo y de saltos. Reportar nodos explorados con poda y sin poda. | |
| 9 | 07/10 | Tests y documentación | Un test por cada algoritmo implementado hasta acá. `docs/ENDPOINTS.md` completo con complejidad justificada. | |
| 10 | 14/10 | Informe de complejidades | Tabla con algoritmos, recurrencia (si corresponde), complejidad temporal/espacial y estructura de datos. | |
| 11 | 21/10 | Optimización con cota | Branch & Bound de reparto. Explicitar la cota y por qué es optimista. Reportar nodos podados. | |
| 12 | 28/10 | Entrega final | Repositorio con el tag `entrega-final`. Cumple los requisitos de admisibilidad (sección 4). Link en Teams. | **Sí** |

**Hito 1 vence hoy si estás leyendo esto el 05/08.**

**Nota (sección 3.1 de la consigna):** si el dominio propio del grupo
todavía no está listo, el hito se puede cumplir igual implementando el
algoritmo sobre el grafo de ejemplo (semilla) que trae el scaffold.

---

## 3. Calificación del TPO (10 puntos)

Los puntos se asignan **por algoritmo implementado**. Cada uno debe estar
accesible por endpoint y documentado en `docs/ENDPOINTS.md` con ejemplos de
entrada y salida.

| Algoritmo | Hito | Puntos |
|---|---|---|
| BFS y DFS | 4 | 2 (1 c/u) |
| Dijkstra, Prim y Kruskal | 5 | 3 (1 c/u) |
| Algoritmos Greedy | 3 | 1 |
| QuickSort y MergeSort | 2 | 1 |
| PD: mochila 0/1 o Floyd-Warshall | 6 y 7 | 1 |
| Backtracking | 8 | 1 |
| Branch & Bound | 11 | 1 |
| **Total** | | **10** |

### Qué significa "implementado"

| Nivel | Condiciones | Puntaje |
|---|---|---|
| **Completo** | Corre sin error · resultados correctos en casos de verificación · implementación propia (sin librerías) · la estructura de datos sostiene la complejidad declarada · tiene al menos un test · documentado con complejidad justificada | 100 % |
| **Parcial** | Corre y da resultados correctos, pero falta el test, o la complejidad declarada no es la real, o la estructura degrada el orden (ej.: Dijkstra con búsqueda lineal del mínimo presentado como O((V+E)·log V)) | 50 % |
| **No cuenta** | No corre · resultados incorrectos · usa una librería que resuelve el algoritmo | 0 % |

---

## 4. Requisitos de admisibilidad (entrega final)

Si la entrega final no cumple estos seis puntos, **se devuelve sin corregir**
y el grupo pasa a la instancia de regularización:

- [ ] Cumplió al menos 9 de los 12 hitos, incluidos los 4 obligatorios (3, 4, 5, 12).
- [ ] El proyecto compila y `./mvnw test` pasa sin errores.
- [ ] No hay ninguna credencial en el repositorio: ni en el código, ni en `application.properties`, ni en el historial de commits.
- [ ] El README declara el dominio elegido, qué representa un nodo, qué representa una arista y en qué unidad está expresado el costo.
- [ ] Cada algoritmo declarado tiene su ficha en `docs/ENDPOINTS.md`.
- [ ] El historial de commits muestra participación de todos los integrantes.

---

## 5. Penalizaciones

| Situación | Descuento |
|---|---|
| Lógica algorítmica dentro de un controller en lugar del paquete `algorithm` | −0,5 |
| Un algoritmo que consulta la base de datos dentro de su bucle principal | −0,5 por algoritmo |
| Endpoint que devuelve un stack trace en lugar de un error manejado | −0,5 |
| Hito entregado fuera de término | −0,25 por hito |

---

## 6. Habilitación al Segundo Parcial

**Regla:** para rendir el Segundo Parcial hay que tener el TPO aprobado con
4 o más puntos. La habilitación se comunica el **04/11 (clase 14)**, una
semana antes del parcial.

### Si un grupo no habilita

1. El 04/11 se comunica el estado de cada grupo y qué le falta.
2. El grupo tiene hasta el **18/11 (clase 16)** para regularizar: completar
   los hitos obligatorios faltantes y corregir lo que impide la admisibilidad.
3. Regularizado el TPO, sus integrantes rinden el Segundo Parcial el
   **25/11** (fecha de recuperatorio).
4. Si al 18/11 el TPO sigue sin aprobarse, el Segundo Parcial se considera
   desaprobado y se aplica el régimen de recuperatorio de la asignatura.

### Un integrante puede habilitar aunque su grupo no

Si un grupo se desarma o alguien deja de participar, quien sí cumplió sus
hitos puede pedir evaluación individual (el historial de commits es la
evidencia). **Hay que plantearlo antes del 21/10 (clase 12)**, no después.

---

## 7. Formato de la entrega

- Grupos de **3 o 4** integrantes, conformados en el hito 3 (19/08). No se
  aceptan grupos de 1, 2, ni de 5 o más.
- Repositorio en GitHub, con acceso de lectura para el docente si es privado.
- Un tag llamado **`entrega-final`** apuntando al commit que se corrige. Lo
  posterior a ese tag no se evalúa.
- Cada hito se entrega por Teams: link al commit correspondiente, antes de
  la clase siguiente.
- El README debe listar los integrantes con **nombre completo y usuario de
  GitHub**.

---

## 8. Uso de herramientas de inteligencia artificial

Permitido, no requiere autorización previa. Tres condiciones:

1. **Declararlo.** El README debe incluir una sección **"Uso de IA"**
   indicando qué herramientas se usaron y para qué. No declararlo es falta
   de honestidad académica.
2. **El Segundo Parcial es la verificación.** Se codifica en vivo, sin
   asistentes y sin acceso al propio repositorio. Quien no pueda escribir
   los algoritmos que entregó, no aprueba.
3. **La comprensión no se delega.** Generar el trabajo completo con una
   herramienta y presentarlo sin entenderlo se encuadra en el régimen
   disciplinario de deshonestidad académica.

> Usar IA para aprender más rápido está bien. Usarla para no aprender es lo
> que está en juego.
