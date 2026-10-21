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

_(completar — incluir un caso donde greedy NO dé el óptimo)_

---

### GET /api/seleccion/dinamica

_(completar — incluir la tabla dp del ejemplo)_

---

### GET /api/grafo/rutas

_(completar — incluir cantidad de nodos explorados con y sin poda)_

---

### GET /api/seleccion/repartir

_(completar — incluir cuál es la cota y por qué es optimista)_
