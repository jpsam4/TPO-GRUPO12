# Documentación de endpoints

> Plantilla. Cada grupo la completa con SUS endpoints, sus ejemplos reales
> y su análisis de complejidad. Un endpoint sin esta ficha no suma puntos.

---

### GET /api/grafo/resumen

Cuántas zonas y caminos tiene el grafo cargado. Sirve para verificar que en
Neo4j está Valdoria y no la semilla del scaffold.

**Parámetros:** `dirigido` (opcional, por defecto `false`)

**Ejemplo:** `GET /api/grafo/resumen`

**Respuesta (recortada):**
```json
{"vertices": 38, "aristas": 57, "dirigido": false, "ids": ["VB1", "VB2", "...", "CC11"]}
```
`ids` trae las 38 zonas, en el orden en que las devuelve Neo4j.

**Complejidad:** O(V + E) para pasar el grafo de Neo4j a memoria (más
O(E log E) para dejar los vecinos ordenados por id).

---

### GET /api/grafo/dfs

Recorrido en profundidad desde una zona: entra por el primer camino hacia una
zona sin visitar y sigue hasta el fondo; cuando no hay más zonas nuevas,
vuelve a la anterior y prueba el próximo camino.

**Parámetros:** `origen` (id de la zona de partida, por ejemplo `VB1`)

**Ejemplo:** `GET /api/grafo/dfs?origen=VB1`

**Respuesta:**
```json
["VB1","VB2","VB3","VB4","VB5","VB6","VB7","VB8",
 "CN1","CN3","CN2","CN5","CN6","CN4","CN7","CN9","CN8",
 "DA1","DA2","DA4","DA5","DA3","DA6","DA7","DA10",
 "CC1","CC2","CC4","CC3","CC6","CC5","CC8","CC11","CC10","CC9","CC7",
 "DA8","DA9"]
```
DA8 y DA9 aparecen al final: DFS llega hasta las Cumbres por DA10 y recién al
volver encuentra esas dos zonas de las Dunas.

**Errores:** un `origen` que no existe devuelve `400 Bad Request`.

**Complejidad temporal:** O(V + E). Cada zona se visita una sola vez (queda
marcada) y su lista de vecinos se recorre una vez, en la única llamada que la
visita. La suma de los largos de las listas es 2E en un grafo no dirigido.

**Complejidad espacial:** O(V): arreglo de visitados, lista resultado y pila de
llamadas (en el peor caso, un camino de V zonas).

**Estructura usada:** lista de adyacencia + recursión (pila de llamadas) +
arreglo `boolean[]` de visitados indexado por vértice (marcar y consultar en O(1)).

---

### GET /api/grafo/bfs

Recorrido en anchura desde una zona: primero todas las zonas a un camino del
origen, después las que están a dos, y así.

**Parámetros:** `origen` (id de la zona de partida, por ejemplo `VB1`)

**Ejemplo:** `GET /api/grafo/bfs?origen=VB1`

**Respuesta:**
```json
["VB1","VB2","VB3","VB4","VB5","VB6","VB7","VB8",
 "CN1","CN3","CN2","CN4","CN5","CN6","CN8","CN7","CN9",
 "DA1","DA2","DA3","DA4","DA5","DA7","DA6","DA10","DA8","DA9",
 "CC1","CC2","CC3","CC4","CC5","CC6","CC8","CC7","CC11","CC9","CC10"]
```

**Errores:** un `origen` que no existe devuelve `400 Bad Request`.

**Complejidad temporal:** O(V + E). Cada zona entra y sale de la cola una sola
vez (O(1) con `ArrayDeque`) y, al salir, se recorre su lista de vecinos una vez.

**Complejidad espacial:** O(V): arreglo de visitados, cola y lista resultado.

**Estructura usada:** lista de adyacencia + cola FIFO (`ArrayDeque`) + arreglo
`boolean[]` de visitados. Las zonas se marcan **al encolarlas**, no al
sacarlas: así ninguna entra dos veces en la cola.

**BFS no es camino mínimo en energía.** Da el camino con menos caminos
recorridos, no el de menos energía: de DA4 a DA9 llega en 2 caminos
(DA4–DA7–DA9, 13 puntos de energía), pero el más barato usa 3
(DA4–DA5–DA6–DA9, 11 puntos). Ese lo encuentra Dijkstra.

---

**Orden de los vecinos.** Neo4j no garantiza en qué orden devuelve los nodos y
las relaciones, así que al cargar el grafo las listas de vecinos se ordenan por
id, como texto. Por eso los recorridos dan siempre el mismo resultado, y
`CC10` va antes que `CC2`.

---

### GET /api/grafo/dijkstra

Camino de menor gasto de energía entre dos zonas del reino, con reconstrucción
completa de la secuencia de zonas.

**Parámetros:** `origen` (id de la zona de partida), `destino` (id de la zona de llegada)

**Ejemplo 1 (Región 1 — Valle de Brezo, verificado a mano):**
`GET /api/grafo/dijkstra?origen=VB1&destino=VB8`

**Respuesta:**
```json
{
  "vertices": ["VB1", "VB2", "VB3", "VB5", "VB6", "VB8"],
  "costoTotal": 20.0
}
```
Cálculo a mano sobre el Valle de Brezo: `VB1 -> VB2 (3) -> VB3 (+4 = 7) -> VB5 (+3 = 10) -> VB6 (+6 = 16) -> VB8 (+4 = 20)`.
Al expandir `VB7` (distancia 15) primero se descubre `VB8` con costo `15 + 6 = 21` (`predecesor = VB7`), y luego al expandir `VB6` (distancia 16) se **relaja** la arista `VB6 -> VB8` bajando `dist[VB8]` a `16 + 4 = 20` (`predecesor = VB6`).

**Ejemplo 2 (Reino completo — de la Aldea Brezo a la Guarida de Ignea):**
`GET /api/grafo/dijkstra?origen=VB1&destino=CC11`

**Respuesta:**
```json
{
  "vertices": [
    "VB1", "VB2", "VB3", "VB5", "VB6", "VB8",
    "CN1", "CN3", "CN6", "CN7", "CN9",
    "DA1", "DA3", "DA5", "DA6", "DA7", "DA10",
    "CC1", "CC2", "CC5", "CC8", "CC11"
  ],
  "costoTotal": 103.0
}
```
Cruza las 4 regiones pasando por 22 zonas: 20 en Valle de Brezo + 5 del paso + 23 en Ciénagas de Niebla + 5 del paso + 20 en Dunas de Ámbar + 5 del paso + 25 en Cumbres de Ceniza = **103 puntos de energía**.

**Errores:** un `origen` o `destino` inexistente o un grafo con aristas de costo negativo devuelve `400 Bad Request`.

**Complejidad temporal:** O((V + E) log V) con cola de prioridad basada en heap binario y lista de adyacencia. Cada zona se cierra a lo sumo una vez (O(V log V) en extracciones válidas) y cada camino se examina una vez por extremo, provocando a lo sumo una inserción en el heap de tamaño <= E (y como E <= V², log E = O(log V), sumando O(E log V) en relajaciones).

**Complejidad espacial:** O(V + E): arreglos `dist[]`, `predecesor[]` y `visitado[]` de tamaño V, más la cola de prioridad con a lo sumo E entradas pendientes (borrado perezoso de entradas desplazadas por una relajación).

**Estructura usada:** lista de adyacencia + `PriorityQueue` (heap binario) + arreglos `double[] dist`, `int[] predecesor` (para reconstruir el camino hacia atrás desde `destino` hasta `origen`) y `boolean[] visitado`.

**Por qué es greedy y por qué falla con pesos negativos:** en cada paso cierra definitivamente la zona abierta `u` de menor `dist[u]` asumiendo que ningún rodeo por zonas aún abiertas puede mejorarla. Eso requiere que sumar nuevas aristas nunca disminuya el costo acumulado; si hubiera una arista negativa, un camino más caro ahora podría restar después y mejorar un vértice que Dijkstra ya cerró.

---

### GET /api/grafo/floyd

Costo mínimo entre **todos los pares** de zonas con Floyd-Warshall, usando el
**costo neto**: la energía del camino menos lo que cura la zona a la que se
llega (cada fuente cura 6).

**Por qué costo neto:** es lo que de verdad le cuesta al personaje moverse, y
es lo que hace aparecer **costos negativos**, que Dijkstra no admite y Floyd
sí. Ir de la Aldea Brezo (VB1) a la Fuente Clara (VB2) gasta 3 pero cura 6, así
que el neto es **−3**. Volver de VB2 a VB1 cuesta **3**: el grafo neto queda
**dirigido** (57 caminos → 114 aristas), porque el neto depende de hacia dónde
se va.

**Parámetros:** `curacion` (opcional): reemplaza lo que curan las fuentes. Sirve
para mostrar la detección de ciclos negativos.

**Ejemplo:** `GET /api/grafo/floyd`

**Respuesta (recortada; son 38 × 38 = 1.444 pares):**
```json
{"VB1->VB1": 0.0, "VB1->VB2": -3.0, "VB2->VB1": 3.0, "VB1->CC11": 79.0, "...": "..."}
```

**Recurrencia:** después de la vuelta `k`, `dist[i][j]` es el costo mínimo de
`i` a `j` usando como intermedios solo los vértices `0..k`:

```
dist[i][j] = min(dist[i][j], dist[i][k] + dist[k][j])
```

El bucle de `k` va **por fuera** de los de `i` y `j`: al usar `k` como
intermedio, `dist[i][k]` y `dist[k][j]` ya tienen que estar resueltos con los
intermedios anteriores. Junto con `dist` se mantiene `siguiente[i][j]` (el
primer paso del camino de `i` a `j`) para poder reconstruir el camino.

**Detección de ciclos negativos:** al terminar se revisa la diagonal. Si algún
`dist[v][v] < 0`, desde `v` se puede salir y volver gastando energía negativa:
hay un ciclo negativo y no existe camino mínimo (dar vueltas lo abarata sin
límite). En ese caso el endpoint responde **`422`** con el ciclo encontrado
(se obtiene siguiendo `siguiente[][]` desde `v` hasta que se repite un vértice).

- **Datos reales:** no hay ciclos negativos. El peor caso es ir y volver entre
  VB1 y la Fuente Clara: −3 + 3 = 0.
- **Con `curacion=7`:** `GET /api/grafo/floyd?curacion=7` responde `422`:
  ```json
  {"estado":"ERROR","detalle":"Hay un ciclo de costo negativo: VB2 -> VB1 -> VB2. Dando vueltas la energia baja sin limite, asi que no existe camino minimo."}
  ```
  Ir a VB1 cuesta 3 y volver a la fuente, 3 − 7 = −4: cada vuelta resta 1.

**Complejidad temporal:** O(V³): tres bucles anidados de V vueltas con trabajo
O(1) adentro. En Valdoria son exactamente 38³ = **54.872** ternas `(k, i, j)`.
No depende de la cantidad de aristas.

**Complejidad espacial:** O(V²): las matrices `dist` y `siguiente`.

**Estructura usada:** matriz de adyacencia `double[V][V]` (de
`Grafo.matrizDeAdyacencia()`) + matriz `int[V][V]` de siguientes.

---

### GET /api/grafo/floyd/camino

El camino mínimo (costo neto) entre dos zonas, reconstruido con la matriz
`siguiente[][]` de Floyd.

**Parámetros:** `origen`, `destino` y `curacion` (opcional).

**Ejemplo:** `GET /api/grafo/floyd/camino?origen=VB1&destino=CC11`

**Respuesta:**
```json
{"vertices":["VB1","VB2","VB3","VB5","VB6","VB8","CN1","CN3","CN6","CN7","CN9",
             "DA1","DA3","DA5","DA6","DA7","DA10","CC1","CC2","CC5","CC8","CC11"],
 "costoTotal":79.0}
```
Es el mismo recorrido que da Dijkstra con energía pura (103), pero cuesta
**79**: pasa por las 4 fuentes y cada una devuelve 6 (103 − 4 × 6 = 79).

**Reconstrucción:** se arranca en el origen y se salta a
`siguiente[actual][destino]` hasta llegar. O(V) después de correr Floyd.

**Errores:** zona inexistente → `400`; ciclo negativo → `422`.

---

### GET /api/grafo/floyd/comparacion

Floyd-Warshall contra correr **Dijkstra desde cada una de las V zonas**, sobre
el mismo grafo. Se hace con la energía pura (sin curación), porque Dijkstra no
admite costos negativos.

**Ejemplo:** `GET /api/grafo/floyd/comparacion`

**Respuesta:**
```json
{"vertices":38, "aristas":57,
 "floydComparaciones":54872, "floydMejoras":1366,
 "dijkstraNodosExpandidos":1444, "dijkstraExtracciones":1509, "dijkstraRelajaciones":1471,
 "mismasDistancias":true}
```

| | Floyd-Warshall | Dijkstra × V (heap binario) |
|---|---|---|
| Complejidad | O(V³) | O(V · (V + E) log V) |
| Trabajo en Valdoria | 54.872 ternas evaluadas | 1.444 nodos expandidos (38 por corrida), 1.509 extracciones del heap, 1.471 relajaciones |
| Costos negativos | Sí (y detecta ciclos negativos) | No |
| Resultado | Las 1.444 distancias | Las mismas 1.444 (`mismasDistancias: true`) |

**Conclusión:** Valdoria es un grafo **ralo** (E = 57, muy lejos del máximo
V(V−1)/2 = 703). Ahí Dijkstra V veces hace mucho menos trabajo: unas 3.000
operaciones de heap contra 54.872 ternas de Floyd. Floyd conviene cuando el
grafo es **denso** (E ≈ V², donde V · (V + E) log V supera a V³), cuando hay
**costos negativos** (el costo neto con las fuentes) o cuando hacen falta todos
los pares con un código simple de tres bucles.

**Tests:** `FloydTest` (11 casos: grafo chico con arista negativa calculado a
mano, reconstrucción del camino, detección de un ciclo negativo, V³ ternas,
VB1→CC11 = 103 con energía y 79 con costo neto, VB1→VB2 = −3, sin ciclos con
los datos reales, ciclo VB2→VB1→VB2 con curación 7, y Floyd igual a Dijkstra
par por par).

---

### GET /api/grafo/prim

Árbol de recubrimiento mínimo (MST) del reino calculado con el algoritmo de Prim, haciendo crecer un único árbol desde la zona de origen y eligiendo en cada paso el camino más barato que cruza desde las zonas ya incorporadas hacia las que están afuera.

**Parámetros:** `origen` (id de la zona desde donde arranca el árbol, por ejemplo `VB1`)

**Ejemplo:** `GET /api/grafo/prim?origen=VB1`

**Respuesta (recortada a las primeras y últimas aristas):**
```json
{
  "aristas": [
    {"origen": "VB1", "destino": "VB2", "costo": 3.0},
    {"origen": "VB2", "destino": "VB3", "costo": 4.0},
    {"origen": "VB3", "destino": "VB5", "costo": 3.0},
    "... (37 aristas en total) ...",
    {"origen": "CC9", "destino": "CC11", "costo": 6.0},
    {"origen": "CC11", "destino": "CC10", "costo": 4.0}
  ],
  "costoTotal": 161.0
}
```
El árbol tiene exactamente **37 caminos** (V − 1 con V = 38) y un costo total de **161 puntos de energía**.

**Verificación a mano por regiones:** como las 4 regiones se unen únicamente por los 3 pasos puente de costo 5, el MST del reino es la suma de los MST de cada región más los 3 pasos:
- **Valle de Brezo (8 zonas, 7 aristas):** `VB1-VB2 (3)`, `VB3-VB5 (3)`, `VB6-VB7 (3)`, `VB2-VB3 (4)`, `VB4-VB5 (4)`, `VB4-VB6 (4)`, `VB6-VB8 (4)` → **25**
- **Ciénagas de Niebla (9 zonas, 8 aristas):** `CN2-CN3 (4)`, `CN6-CN7 (4)`, `CN1-CN3 (5)`, `CN3-CN4 (5)`, `CN4-CN6 (5)`, `CN5-CN6 (5)`, `CN8-CN9 (5)`, `CN7-CN9 (8)` → **41**
- **Dunas de Ámbar (10 zonas, 9 aristas):** `DA4-DA5 (3)`, `DA5-DA6 (3)`, `DA6-DA7 (3)`, `DA7-DA8 (3)`, `DA1-DA3 (4)`, `DA2-DA4 (4)`, `DA3-DA5 (4)`, `DA7-DA9 (4)`, `DA9-DA10 (4)` → **32**
- **Cumbres de Ceniza (11 zonas, 10 aristas):** `CC1-CC2 (4)`, `CC6-CC7 (4)`, `CC7-CC8 (4)`, `CC10-CC11 (4)`, `CC2-CC4 (5)`, `CC3-CC4 (5)`, `CC4-CC5 (5)`, `CC4-CC6 (5)`, `CC8-CC9 (6)`, `CC9-CC11 (6)` → **48**
- **3 pasos entre regiones:** `VB8-CN1 (5)` + `CN9-DA1 (5)` + `DA10-CC1 (5)` → **15**
- **Total:** `25 + 41 + 32 + 48 + 15 = 161.0`.

**Errores:** un `origen` inexistente, un grafo dirigido o un grafo no conexo devuelve `400 Bad Request`.

**Complejidad temporal:** O(E log V) sobre un grafo conexo (implementación con heap binario, elegida porque Valdoria es un grafo ralo con E = 57 y V = 38, donde O(E log V) hace menos trabajo que el O(V²) de la búsqueda lineal).

**Complejidad espacial:** O(V + E) por los arreglos `key[]`, `parent[]`, `inMST[]` de tamaño V y la cola de prioridad.

**Estructura usada:** lista de adyacencia + `PriorityQueue` (heap binario) + arreglos `double[] key` (menor arista hacia el árbol), `int[] parent` (vértice del árbol que logra ese mínimo) y `boolean[] inMST`.

---

### GET /api/grafo/kruskal

Árbol de recubrimiento mínimo (MST) del reino calculado con el algoritmo de Kruskal: ordena todos los caminos de menor a mayor costo y los agrega en orden siempre que no formen ciclo, verificando con Union-Find si los dos extremos ya pertenecen a la misma componente conexa.

**Parámetros:** ninguno

**Ejemplo:** `GET /api/grafo/kruskal`

**Respuesta (recortada):**
```json
{
  "aristas": [
    {"origen": "VB1", "destino": "VB2", "costo": 3.0},
    {"origen": "VB3", "destino": "VB5", "costo": 3.0},
    {"origen": "VB6", "destino": "VB7", "costo": 3.0},
    {"origen": "DA4", "destino": "DA5", "costo": 3.0},
    {"origen": "DA5", "destino": "DA6", "costo": 3.0},
    {"origen": "DA6", "destino": "DA7", "costo": 3.0},
    {"origen": "DA7", "destino": "DA8", "costo": 3.0},
    "... (37 aristas en total, ordenadas por costo de 3.0 a 8.0) ...",
    {"origen": "CN7", "destino": "CN9", "costo": 8.0}
  ],
  "costoTotal": 161.0
}
```
Reúne las mismas **37 aristas** y el mismo **costo total 161.0** que Prim (y corta apenas llega a V − 1 aristas, sin evaluar `CN5-CN8` de costo 9).

**Errores:** un grafo dirigido o no conexo devuelve `400 Bad Request`.

**Complejidad temporal:** O(E log E) = O(E log V). Ordenar las E aristas con MergeSort propio cuesta O(E log E) y domina a las a lo sumo 2E consultas `find` y V − 1 uniones `union`, que con compresión de caminos y unión por rango cuestan O(E α(V)), prácticamente lineal.

**Complejidad espacial:** O(V + E): arreglos `padre[]` y `rango[]` de tamaño V para el Union-Find, más el arreglo de E aristas y su auxiliar para el ordenamiento.

**Estructura usada:** arreglo de aristas ordenado con MergeSort propio (sin `Collections.sort` ni `Arrays.sort`) + estructura **Union-Find** implementada a mano (`int[] padre` con compresión de caminos en `find` e `int[] rango` para unión por rango en `union`).

**Prim vs Kruskal según la densidad:** Kruskal conviene en grafos ralos (E ~ V, como Valdoria) porque ordenar pocas aristas es muy liviano; Prim con búsqueda lineal O(V²) conviene en grafos densos (E ~ V²), donde evita el factor log V del heap y el costo O(V² log V) de ordenar todas las aristas.

---

### GET /api/seleccion/quicksort

Ordena los items con QuickSort implementado a mano (sin `Collections.sort`).

**Parametros:** `criterio` — `valor` | `peso` | `ratio` (por defecto `ratio`)

**Ejemplo:** `GET /api/seleccion/quicksort?criterio=ratio`

**Respuesta:**
```json
[
  {"id":"I1","nombre":"Item 1","peso":5.0,"valor":10.0,"nodoId":"A"},
  {"id":"I3","nombre":"Item 3","peso":6.0,"valor":30.0,"nodoId":"C"},
  {"id":"I5","nombre":"Item 5","peso":7.0,"valor":55.0,"nodoId":"E"},
  {"id":"I2","nombre":"Item 2","peso":4.0,"valor":40.0,"nodoId":"B"},
  {"id":"I4","nombre":"Item 4","peso":3.0,"valor":50.0,"nodoId":"D"}
]
```
Los ratios valor/peso son 2.00, 5.00, 7.86, 10.00 y 16.67: la salida esta
ordenada de menor a mayor.

**Errores:** un `criterio` que no sea `valor`, `peso` o `ratio` devuelve
`400 Bad Request` con el detalle, no un stack trace.

**Complejidad temporal:** O(n log n) en promedio, O(n^2) en el peor caso.
En promedio el pivote parte el tramo en dos mitades comparables, hay O(log n)
niveles y cada nivel recorre O(n) elementos. El peor caso es que el pivote sea
el minimo o el maximo en cada nivel: la particion queda de 0 y n-1, hay n
niveles de O(n) cada uno. Con **mediana de tres** ese caso no se da con datos
ya ordenados ni al reves (que es como aparece el peor caso con pivote fijo);
hace falta una entrada construida a proposito contra el metodo de eleccion.

**Complejidad espacial:** O(log n). Ordena en el lugar sobre un arreglo; lo
unico que crece es la pila de recursion, y se acota recursando siempre sobre
el subtramo mas chico e iterando sobre el mas grande (eliminacion de la
recursion de cola), asi cada marco apilado cubre a lo sumo la mitad del tramo.

**Estructura usada:** arreglo `Item[]` (acceso O(1) por indice, que sobre una
`List` generica no esta garantizado) + particion de **tres vias** (bandera
holandesa), que deja `[menores | iguales | mayores]` y saca los iguales de la
recursion. Importa en este dominio porque al ordenar por `peso` o `valor` hay
muchos empates: con particion de dos vias las claves repetidas se vuelven a
comparar en cada nivel y el algoritmo se degrada; con tres vias un tramo
entero de claves iguales se resuelve en O(n).

**No es estable.** Los intercambios de la particion mueven elementos a
distancia y pueden alterar el orden relativo de los empates. Si hace falta
estabilidad, se usa el endpoint de MergeSort.

---

### GET /api/seleccion/mergesort

Ordena los items con MergeSort implementado a mano (sin `Collections.sort`).

**Parametros:** `criterio` — `valor` | `peso` | `ratio` (por defecto `ratio`)

**Ejemplo:** `GET /api/seleccion/mergesort?criterio=peso`

**Respuesta:**
```json
[
  {"id":"I4","nombre":"Item 4","peso":3.0,"valor":50.0,"nodoId":"D"},
  {"id":"I2","nombre":"Item 2","peso":4.0,"valor":40.0,"nodoId":"B"},
  {"id":"I1","nombre":"Item 1","peso":5.0,"valor":10.0,"nodoId":"A"},
  {"id":"I3","nombre":"Item 3","peso":6.0,"valor":30.0,"nodoId":"C"},
  {"id":"I5","nombre":"Item 5","peso":7.0,"valor":55.0,"nodoId":"E"}
]
```

**Recurrencia:** T(n) = 2 T(n/2) + O(n).
Se parte en dos mitades (2 subproblemas de tamano n/2) y mezclarlas cuesta
O(n). Con la regla practica a = 2, b = 2, k = 1: como **a = b^k**, estamos en
el caso T(n) = O(n^k log n) = **O(n log n)**.

**Complejidad temporal:** O(n log n) **siempre**, no en promedio. La division
es por posicion (siempre por el medio) y no depende de los valores, asi que no
existe una entrada que la desbalancee. Esa es la diferencia de fondo con
QuickSort, que parte segun el valor del pivote y por eso arrastra un peor caso
O(n^2).

**Complejidad espacial:** O(n) por el arreglo auxiliar. Es el precio de la
estabilidad y del peor caso garantizado. El auxiliar se reserva **una sola
vez** al principio, no uno por llamada: reservarlo en cada mezcla no cambia el
orden asintotico pero multiplica las reservas de memoria por n.

**Estructura usada:** dos arreglos `Item[]` cuyos roles de fuente y destino se
**alternan entre niveles**, de modo que el resultado de cada mezcla ya queda
donde el nivel de arriba lo necesita y se evita copiar el tramo de vuelta
despues de cada mezcla.

**Es estable.** Ante un empate la mezcla toma siempre el elemento de la mitad
izquierda (`compare(derecha, izquierda) < 0` es estricto), asi que dos items
con la misma clave conservan el orden en que venian de la base. Verificado en
`OrdenamientoTest.mergeSortEsEstable`.

**Optimizaciones aplicadas a los dos algoritmos**

| Optimizacion | Que evita | Efecto en la complejidad |
|---|---|---|
| Corte a insercion con tramos <= 12 | el costo de administrar la recursion en tramos chicos | no la cambia; baja la constante |
| QuickSort: mediana de tres | el peor caso O(n^2) con entrada ordenada o al reves | no cambia la cota, cambia cuando se alcanza |
| QuickSort: particion de tres vias | recomparar claves repetidas en cada nivel | O(n) para un tramo de claves iguales |
| QuickSort: recursion sobre el tramo chico | que la pila crezca O(n) en el peor caso | espacio O(log n) garantizado |
| MergeSort: auxiliar unico + roles alternados | reservar y copiar en cada mezcla | no la cambia; menos memoria y menos copias |
| MergeSort: atajo si ya quedo ordenado | mezclar dos mitades que ya estan en orden | entrada ordenada sin comparaciones de mezcla |

**Tests:** `OrdenamientoTest` (12 casos: orden por los tres criterios, entrada
no modificada, estabilidad, claves repetidas, casos borde, 2000 items al azar,
entrada ordenada y al reves) y `SeleccionControllerOrdenamientoTest` (3 casos
sobre los endpoints HTTP).

---

### GET /api/seleccion/greedy

Llena la mochila del héroe eligiendo primero los objetos con mejor relación
poder/peso.

**Función objetivo:** **maximizar el poder total** (suma de `valor`) de los
objetos elegidos, sin que la suma de sus pesos (kg) supere la capacidad. Cada
objeto se lleva entero o no se lleva (mochila 0/1).

**Parámetros:**
- `capacidad`: kg que entran en la mochila.
- `region` (opcional): `VB`, `CN`, `DA` o `CC`, para usar solo los objetos de
  esa región. Sin `region`, usa los 25 objetos del reino.

**Ejemplo:** `GET /api/seleccion/greedy?capacidad=10&region=VB`

**Respuesta (recortada):**
```json
{
  "elegidos": [
    {"id":"OVB5","nombre":"Arco","peso":4.0,"valor":16.0,"nodoId":"VB2"},
    {"id":"OVB2","nombre":"Hacha","peso":3.0,"valor":11.0,"nodoId":"VB1"},
    {"id":"OVB4","nombre":"Gema","peso":1.0,"valor":3.0,"nodoId":"VB4"}
  ],
  "valorTotal": 30.0, "pesoTotal": 8.0, "tecnica": "greedy por ratio valor/peso"
}
```

**Cómo decide:** ordena por ratio valor/peso de mayor a menor (con el MergeSort
propio del Hito 2; a igual ratio, por id) y recorre la lista una vez: si el
objeto entra en lo que queda, lo toma; si no, lo saltea y sigue. Nunca revisa
una decisión ya tomada.

**Errores:** `capacidad` negativa o `region` inexistente devuelven `400 Bad Request`.

**Complejidad temporal:** O(n log n), dominada por el ordenamiento; la pasada
que elige es O(n).

**Complejidad espacial:** O(n): la copia ordenada y la lista de elegidos.

**Estructura usada:** arreglo ordenado con MergeSort (estable) + una pasada lineal.

#### Juego de datos donde greedy NO da el óptimo

Los objetos del **Valle de Brezo** con la mochila de **10 kg** del juego:

| Objeto | Peso (kg) | Poder | Ratio poder/peso |
|---|---|---|---|
| Arco | 4 | 16 | 4,00 |
| Hacha | 3 | 11 | 3,67 |
| Armadura | 6 | 19 | 3,17 |
| Gema | 1 | 3 | 3,00 |
| Mapa | 5 | 14 | 2,80 |

- **Greedy:** toma Arco (4 kg) y Hacha (7 kg). La Armadura ya no entra (13 kg), toma la Gema (8 kg) y el Mapa tampoco entra. Total: **30 de poder, 8 kg** (sobran 2 kg).
- **Óptimo (programación dinámica):** Armadura + Arco. Total: **35 de poder, 10 kg**.

Greedy pierde porque llenar primero con lo de mejor ratio deja 2 kg sin usar,
que la Armadura (peor ratio, pero más poder) hubiera aprovechado. Pasa en las
cuatro regiones con 10 kg:

| Región | Greedy | Óptimo |
|---|---|---|
| Valle de Brezo (`VB`) | 30 | 35 |
| Ciénagas de Niebla (`CN`) | 56 | 61 |
| Dunas de Ámbar (`DA`) | 69 | 75 |
| Cumbres de Ceniza (`CC`) | 79 | 90 |

También con el reino completo: con `capacidad=8`, greedy junta 75 de poder
(Antorcha ígnea, Lanza ígnea, Espada del desierto y Gema) y el óptimo es 79
(Hacha ígnea + Lanza ígnea).

---

### GET /api/seleccion/dinamica

Mochila 0/1 con programación dinámica: da siempre el **óptimo** y dice qué
objetos lo forman.

**Parámetros:** `capacidad` (entero, kg) y `region` (opcional, igual que en greedy).

**Ejemplo:** `GET /api/seleccion/dinamica?capacidad=10&region=VB`

**Respuesta (recortada):**
```json
{
  "elegidos": [
    {"id":"OVB1","nombre":"Armadura","peso":6.0,"valor":19.0,"nodoId":"VB2"},
    {"id":"OVB5","nombre":"Arco","peso":4.0,"valor":16.0,"nodoId":"VB2"}
  ],
  "valorTotal": 35.0, "pesoTotal": 10.0, "tecnica": "programacion dinamica (mochila 0/1)"
}
```

**Recurrencia:** `dp[i][j]` es el máximo poder usando solo los primeros `i`
objetos con capacidad `j`.

```
dp[0][j] = 0
dp[i][j] = dp[i-1][j]                                   si peso_i > j
dp[i][j] = max(dp[i-1][j], dp[i-1][j - peso_i] + valor_i)   si no
```

La tabla se llena de arriba hacia abajo (bottom-up) y la respuesta es `dp[n][W]`.

**Tabla dp del ejemplo** (Valle de Brezo, capacidad 10):

| Objetos considerados | j=0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 0 · ninguno | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 |
| 1 · Armadura (6 kg, 19) | 0 | 0 | 0 | 0 | 0 | 0 | 19 | 19 | 19 | 19 | 19 |
| 2 · Hacha (3 kg, 11) | 0 | 0 | 0 | 11 | 11 | 11 | 19 | 19 | 19 | 30 | 30 |
| 3 · Mapa (5 kg, 14) | 0 | 0 | 0 | 11 | 11 | 14 | 19 | 19 | 25 | 30 | 30 |
| 4 · Gema (1 kg, 3) | 0 | 3 | 3 | 11 | 14 | 14 | 19 | 22 | 25 | 30 | 33 |
| 5 · Arco (4 kg, 16) | 0 | 3 | 3 | 11 | 16 | 19 | 19 | 27 | 30 | 30 | **35** |

**Recuperación del camino** (de abajo hacia arriba, desde `dp[5][10]`):
- `dp[5][10]=35 ≠ dp[4][10]=33`: se lleva el **Arco**; quedan 10−4 = 6 kg.
- `dp[4][6] = dp[3][6] = dp[2][6] = dp[1][6] = 19`: Gema, Mapa y Hacha no se llevan.
- `dp[1][6]=19 ≠ dp[0][6]=0`: se lleva la **Armadura**; quedan 0 kg.

Resultado: Armadura + Arco = 35 de poder, 10 kg.

**Errores:** `capacidad` negativa, `region` inexistente o algún objeto con peso
no entero devuelven `400 Bad Request` (el peso se usa como índice de columna).

**Complejidad temporal:** O(n · W): cada una de las (n+1)·(W+1) celdas se
calcula en O(1); la recuperación es O(n).

**Complejidad espacial:** O(n · W) por la tabla completa, que hace falta para
recuperar los objetos (si solo se quisiera el valor alcanzaría con una fila).

**Es pseudopolinómica:** W es un *valor*, no un tamaño de entrada. Escribirlo
ocupa log W bits, así que O(n · W) es exponencial en el tamaño de la entrada:
duplicar la capacidad duplica el trabajo aunque la entrada casi no crezca.

**Estructura usada:** matriz `double[n+1][W+1]`.

**Tests:** `SeleccionTest` (9 casos: el ejemplo del Valle de Brezo, greedy
pierde en las cuatro regiones, PD contra fuerza bruta en cada región para
capacidades de 0 a 30, 200 casos al azar donde los objetos recuperados suman lo
informado, greedy nunca supera al óptimo, casos vacíos, datos inválidos y
entrada sin modificar) y `SeleccionControllerTest` (4 casos sobre los endpoints HTTP).

---

### GET /api/grafo/rutas

_(completar — incluir cantidad de nodos explorados con y sin poda)_

---

### GET /api/seleccion/repartir

_(completar — incluir cuál es la cota y por qué es optimista)_
