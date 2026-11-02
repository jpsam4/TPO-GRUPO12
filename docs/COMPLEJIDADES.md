# Informe de complejidades — Reino de Valdoria (Grupo 12)

Hito 10. Para cada algoritmo del trabajo: su recurrencia (cuando corresponde),
su complejidad temporal y espacial, y la estructura de datos que la sostiene.

**Tamaños en Valdoria:** V = 38 zonas, E = 57 caminos, n = 25 objetos (108 kg),
W = capacidad de la mochila (10 kg en el juego), k = personajes del reparto.

**Decisión que sostiene todas las complejidades:** los algoritmos no consultan
Neo4j. El grafo se carga una vez a memoria (`GrafoService`, O(V + E), más
O(E log E) para dejar los vecinos ordenados por id) y se opera sobre una
**lista de adyacencia**. Así, la complejidad que se mide es la del algoritmo y
no la de la red ni la de la base.

## Tabla resumen

| Algoritmo | Técnica | Recurrencia | Temporal | Espacial | Estructura que la sostiene |
|---|---|---|---|---|---|
| QuickSort | Divide y vencerás | Promedio: T(n) = 2T(n/2) + O(n). Peor: T(n) = T(n−1) + O(n) | O(n log n) promedio, O(n²) peor | O(log n) | Arreglo `Item[]` (acceso O(1) por índice), partición de 3 vías y mediana de tres |
| MergeSort | Divide y vencerás | T(n) = 2T(n/2) + O(n) | O(n log n) siempre | O(n) | Arreglo `Item[]` + un arreglo auxiliar que alterna roles con el destino |
| DFS | Grafos (recorrido) | — (cada vértice se visita una vez) | O(V + E) | O(V) | Lista de adyacencia + `boolean[] visitado` + pila de recursión |
| BFS | Grafos (recorrido) | — | O(V + E) | O(V) | Lista de adyacencia + cola FIFO `ArrayDeque` + `boolean[] visitado` |
| Dijkstra | Greedy sobre grafos | — | O((V + E) log V) | O(V + E) | Lista de adyacencia + heap binario (`PriorityQueue`) con borrado perezoso + `dist[]` y `predecesor[]` |
| Prim | Greedy (árbol mínimo) | — | O(E log V) | O(V + E) | Lista de adyacencia + heap binario (`PriorityQueue`) + arreglos `key[]`, `parent[]` e `inMST[]` |
| Kruskal | Greedy (árbol mínimo) | — | O(E log E) = O(E log V) | O(V + E) | Lista de aristas ordenada con MergeSort + Union-Find con compresión de caminos y unión por rango |
| Greedy de mochila | Greedy | — | O(n log n) | O(n) | Arreglo ordenado por ratio con MergeSort + una pasada lineal |
| Mochila 0/1 | Programación dinámica | dp[i][j] = max(dp[i−1][j], dp[i−1][j−pᵢ] + vᵢ) | O(n · W) (pseudopolinómica) | O(n · W) | Matriz `double[n+1][W+1]` |
| Floyd-Warshall | Programación dinámica | dₖ[i][j] = min(dₖ₋₁[i][j], dₖ₋₁[i][k] + dₖ₋₁[k][j]) | O(V³) | O(V²) | Matriz de adyacencia `double[V][V]` + matriz `int[V][V]` de siguientes |
| Backtracking de rutas | Backtracking | T(d) = b · T(d−1) + O(1) | O(b^d), hasta O(V!) sin límite de saltos | O(V) | Lista de adyacencia + `boolean[] visitado` + camino parcial como pila + recursión |
| Branch & Bound de reparto | Ramificación y poda | T(n) = k · T(n−1) + O(k) | O(kⁿ) peor caso | O(n + k) | Arreglo `Item[]` ordenado por peso + `double[k]` de cargas + `int[n]` de asignación + recursión |

## Justificación de cada una

### QuickSort — O(n log n) promedio, O(n²) peor
Cada nivel de recursión recorre O(n) elementos para particionar. Si el pivote
parte el tramo en mitades comparables hay O(log n) niveles:
T(n) = 2T(n/2) + O(n) = O(n log n) (teorema maestro, caso 2). Si el pivote
fuera siempre el mínimo o el máximo, la partición queda de 0 y n−1:
T(n) = T(n−1) + O(n) = O(n²). La **mediana de tres** evita ese caso con datos
ordenados o al revés, y la **partición de 3 vías** saca los iguales de la
recursión (importa porque al ordenar por peso hay muchos empates). Espacio
O(log n): se recursa siempre sobre el tramo más chico.

### MergeSort — O(n log n) siempre
Divide en dos mitades, ordena cada una y las mezcla en O(n):
T(n) = 2T(n/2) + O(n) = O(n log n), sin peor caso distinto, porque la división
no depende de los datos. Espacio O(n) por el arreglo auxiliar. Es **estable**,
por eso lo usan greedy, Kruskal y Branch & Bound.

### DFS y BFS — O(V + E)
Cada zona se marca una sola vez, así que entra una sola vez a la recursión
(DFS) o a la cola (BFS), y su lista de vecinos se recorre una vez. La suma de
los largos de todas las listas es 2E en un grafo no dirigido: O(V + E). Con
matriz de adyacencia sería O(V²): por eso la lista.

### Dijkstra — O((V + E) log V)
Cada arista se relaja a lo sumo una vez por extremo y cada relajación que
mejora inserta en el heap (O(log V)); cada extracción también cuesta O(log V).
Con búsqueda lineal del mínimo sería O(V²): la estructura (heap) es la que
sostiene la complejidad declarada. Exige costos ≥ 0.

### Prim — O(E log V) y Kruskal — O(E log E)
Prim: cada vez que una arista mejora `key[v]` de una zona todavía fuera del
árbol se inserta en el heap, con O(log V) por operación; como hay a lo sumo
una inserción por extremo de cada arista y el grafo es conexo, queda
O(E log V). Con búsqueda lineal de `key[]` sería O(V²), peor en un grafo ralo. Kruskal: ordenar las E aristas cuesta O(E log E) y domina a las
2E operaciones `find` y V−1 `union`, que con compresión de caminos y unión por
rango cuestan casi O(1) cada una (O(α(V))). Como E ≤ V², log E ≤ 2 log V, así
que O(E log E) = O(E log V).

### Greedy de mochila — O(n log n)
Ordenar por ratio poder/peso cuesta O(n log n); la pasada que elige es O(n).
**No garantiza el óptimo** en la mochila 0/1: en el Valle de Brezo con 10 kg
da 30 de poder y el óptimo es 35.

### Mochila 0/1 — O(n · W), pseudopolinómica
La tabla tiene (n+1)·(W+1) celdas y cada una se llena en O(1) con la
recurrencia; recuperar los objetos recorriendo la tabla hacia atrás es O(n).
Es **pseudopolinómica**: W es un valor, no un tamaño. Escribirlo ocupa log W
bits, así que O(n · W) es exponencial en el tamaño de la entrada.

### Floyd-Warshall — O(V³)
Tres bucles anidados de V vueltas con trabajo O(1) adentro: V³ ternas
(54.872 en Valdoria), sin importar cuántas aristas haya. El bucle de k va por
fuera. Admite costos negativos y detecta ciclos negativos en la diagonal.
**Contra Dijkstra V veces** (O(V · (V + E) log V)): en un grafo ralo como
Valdoria, Dijkstra × V hace mucho menos trabajo (1.444 nodos expandidos contra
54.872 ternas). Floyd conviene en grafos densos o con costos negativos.

### Backtracking de rutas — O(b^d)
Cada llamada prueba hasta b vecinos y baja un nivel: T(d) = b · T(d−1) + O(1)
= O(b^d), con d acotado por `saltosMaximos`. Sin límite de saltos, entre dos
vértices de un grafo completo hay (V−2)! caminos simples: O(V!). La poda por
costo (monótono, porque los costos son ≥ 0) y por saltos no cambia el peor
caso, pero corta subárboles enteros: en el reino completo (VB1 → CC11, costo ≤ 105, saltos ≤ 22)
pasa de 485.647 estados sin poda a 31.327 con poda. Espacio O(V): `visitado[]`, el camino
parcial y la pila de recursión.

### Branch & Bound de reparto — O(kⁿ)
Cada objeto puede ir a cualquiera de los k personajes:
T(n) = k · T(n−1) + O(k) = O(kⁿ); el árbol completo tiene 1 + k + … + kⁿ
nodos. La cota inferior optimista max(carga máxima actual, ⌈total / k⌉) poda
las ramas que no pueden mejorar la mejor solución: con 25 objetos y 3
personajes explora 546 nodos de 1.270.932.914.164. El peor caso sigue siendo
exponencial (el problema es NP-difícil). Espacio O(n + k): profundidad n de
recursión y las k cargas.

## Comparaciones que muestra el trabajo

| Comparación | Resultado en Valdoria | Dónde se ve |
|---|---|---|
| BFS contra Dijkstra | DA4 → DA9: BFS da 2 caminos (13 de energía), Dijkstra 3 caminos (11) | `ENDPOINTS.md`, BFS |
| Greedy contra programación dinámica | Valle de Brezo, 10 kg: 30 contra 35 (pasa en las 4 regiones) | `/api/seleccion/greedy` y `/dinamica` |
| Floyd contra Dijkstra × V | 54.872 ternas contra 1.444 nodos expandidos; mismas distancias | `/api/grafo/floyd/comparacion` |
| Backtracking con poda contra sin poda | 31.327 contra 485.647 estados (reino completo) | `/api/grafo/rutas/comparacion` |
| Branch & Bound contra árbol completo | 546 contra 1,27 billones de nodos | `/api/seleccion/repartir` |
