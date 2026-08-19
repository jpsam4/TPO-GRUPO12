# Documentación de endpoints

> Plantilla. Cada grupo la completa con SUS endpoints, sus ejemplos reales
> y su análisis de complejidad. Un endpoint sin esta ficha no suma puntos.

---

### GET /api/grafo/dfs

Recorrido en profundidad desde un nodo.

**Parámetros:** `origen` (id del nodo de partida)

**Ejemplo:** `GET /api/grafo/dfs?origen=A`

**Respuesta:**
```json
["A", "B", "C", "D", "E", "F", "H", "G"]
```

**Complejidad:** O(V + E) — cada vértice se visita una vez y cada arista se
recorre una vez, con lista de adyacencia.

**Estructura usada:** recursión (pila de llamadas) + arreglo de visitados.

---

### GET /api/grafo/bfs

_(completar)_

---

### GET /api/grafo/dijkstra

_(completar)_

---

### GET /api/grafo/prim

_(completar)_

---

### GET /api/grafo/kruskal

_(completar)_

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
