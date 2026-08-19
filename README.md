# TPO Programación III — Scaffold 2026

Punto de partida del Trabajo Práctico Obligatorio. **No contiene ningún algoritmo
implementado**: eso es exactamente lo que hay que hacer.

Lo que sí trae resuelto, para que no pierdan tiempo en infraestructura:

- modelo de grafo **ponderado** en Neo4j, listo para Dijkstra, Prim y Kruskal
- carga del grafo de la base a una estructura en memoria (`Grafo`)
- todos los endpoints ya cableados, devolviendo `501 Not Implemented`
- datos de ejemplo que se cargan solos al arrancar
- manejo de errores, configuración por variables de entorno y tests de base

## Cómo se corre esto

El camino principal es **el desplegado**: la aplicación vive en Render y la base
en Neo4j Aura, así todo el equipo mira lo mismo sin instalar nada. Correrlo
local es opcional y sirve para desarrollar.

| | Dónde corre la app | Dónde está la base | Para qué |
|---|---|---|---|
| **Desplegado (principal)** | Render | Aura | lo que ve el equipo y el docente |
| Local (opcional) | tu máquina | Aura | desarrollar y probar cambios |
| Local aislado | tu máquina | Neo4j en Docker | trabajar sin tocar la base compartida |

### Camino principal: Render

El servicio se redespliega solo con cada push a `main` (`autoDeploy` en
`render.yaml`). No hay que hacer nada más que mergear.

```
https://tpo-grupo12.onrender.com/api/grafo/resumen
```

> El plan free se duerme a los ~15 minutos sin uso: la primera visita después
> de un rato tarda cerca de un minuto en responder. Es normal, no está roto.

Para crear el servicio la primera vez, ver
[docs/DESPLIEGUE.md](docs/DESPLIEGUE.md).

### Camino opcional: correrlo local

```bash
cp .env.example .env      # completar con los datos de la instancia de Aura
./mvnw spring-boot:run    # el .env se lee solo, no hace falta exportar nada
```

```bash
curl "http://localhost:8080/api/grafo/resumen"
# -> {"vertices":8,"aristas":12,"dirigido":true,"ids":["A","B",...]}
```

Si eso responde, la infraestructura está bien y lo único que queda es escribir
algoritmos.

### Camino aislado: base local en Docker

Para experimentar sin ensuciar la base compartida:

```bash
docker compose up -d
# y en .env: NEO4J_URI=bolt://localhost:7687  NEO4J_USER=neo4j  NEO4J_DATABASE=neo4j
```

## Base de datos en la nube (Neo4j Aura)

El proyecto apunta a una instancia de **Neo4j Aura**, no a una base local: todo
lo que la aplicación escriba queda en la nube y lo ve todo el grupo.

| | |
|---|---|
| Instancia | `TPOGrupo12` (id `3ae8699b`) |
| URI | `neo4j+s://3ae8699b.databases.neo4j.io` |
| Base | `3ae8699b` |
| Consola | https://console.neo4j.io |

> **Ojo con esta instancia:** el usuario y el nombre de la base **no** son
> `neo4j`, son el id de la instancia. Por eso hace falta la propiedad
> `spring.data.neo4j.database`, que se agregó a `application.properties`: sin
> ella el driver busca una base llamada `neo4j` y falla con
> `DatabaseNotFound`.

Las credenciales salen del archivo `Neo4j-3ae8699b-Created-*.txt` que Aura hace
descargar al crear la instancia. Van en `.env`, que está en `.gitignore` y
**nunca** se commitea:

```bash
cp .env.example .env      # completar con los valores de la instancia
set -a && . ./.env && set +a
./mvnw spring-boot:run
```

### Conexión desde la facultad: se resuelve sola

La red de UADE **bloquea TLS en todos los puertos que no sean los conocidos**
(443, 993, 465). Bolt, el protocolo del driver de Neo4j, usa el 7687, así que
desde la facultad el driver nunca llega a Aura.

No hace falta hacer nada: la aplicación lo detecta al arrancar y usa el otro
protocolo que Neo4j ya ofrece, la **Query API sobre HTTPS por el 443**, que sí
está permitido.

```
[Neo4j] Probando si Bolt llega a 3ae8699b.databases.neo4j.io ...
[Neo4j] Bolt NO responde (782 ms). Se cambia a la Query API sobre HTTPS (443).
[Neo4j] Hablando con Neo4j por la Query API: https://3ae8699b.databases.neo4j.io/db/3ae8699b/query/v2
Started TpoApplication in 2.3 seconds
```

| Perfil | Cómo habla con la base | Cuándo se activa |
|---|---|---|
| `bolt` | Spring Data Neo4j, puerto 7687 | fuera de la facultad, Neo4j local, Render |
| `http` | Query API, HTTPS puerto 443 | cuando el 7687 está bloqueado |

Los dos escriben el mismo modelo de datos, así que se ven exactamente los
mismos nodos y aristas. Para forzar uno, poner `NEO4J_TRANSPORTE=bolt` o
`=http` en el `.env`.

El detalle completo del diagnóstico está en **[docs/RED-Y-CONEXION.md](docs/RED-Y-CONEXION.md)**.

### El `.env` se carga solo

No hace falta exportar nada: alcanza con tener el `.env` en la raíz. Además el
`.env` **le gana** a las variables de entorno del sistema, para que una
`NEO4J_URI` vieja apuntando a localhost no desvíe la conexión sin avisar. Si
existe ese conflicto, la aplicación lo dice al arrancar.

```bash
./mvnw spring-boot:run     # y listo
```

### Herramientas de diagnóstico

```bash
python scripts/probe-red.py              # qué deja pasar la red
python scripts/seed-aura.py --verificar  # cuenta nodos/aristas/items en Aura
python scripts/seed-aura.py --vaciar     # vacía la base (la app la recarga sola)
python scripts/seed-aura.py --reset      # vacía y vuelve a cargar la semilla
```

La instancia quedó cargada y verificada con el grafo de ejemplo: **8 nodos,
12 aristas y 5 items**.

## Estructura

```
model/          Nodo, Conexion (arista con costo), Item
repository/     acceso a Neo4j — repositorios BLOQUEANTES, sin Mono/Flux
service/        Grafo (lista de adyacencia en memoria) y GrafoService (carga)
algorithm/      >>> ACÁ VA TODO EL TRABAJO <<<  ahora son stubs con TODO
controller/     endpoints ya cableados — NO poner lógica algorítmica acá
seed/           datos de ejemplo
```

## Endpoints

| Método | Endpoint | Algoritmo | Puntos |
|---|---|---|---|
| GET | `/api/grafo/resumen` | — (verificación) | — |
| GET | `/api/grafo/dfs?origen=A` | DFS | 2 (con BFS) |
| GET | `/api/grafo/bfs?origen=A` | BFS | |
| GET | `/api/grafo/dijkstra?origen=A&destino=H` | Dijkstra | 3 (con Prim y Kruskal) |
| GET | `/api/grafo/prim?origen=A` | Prim | |
| GET | `/api/grafo/kruskal` | Kruskal | |
| GET | `/api/grafo/floyd` | Floyd-Warshall | 1 (con mochila 0/1) |
| GET | `/api/grafo/ucs?origen=A&destino=H` | UCS | opcional |
| GET | `/api/seleccion/greedy?capacidad=10` | Greedy | 1 |
| GET | `/api/seleccion/dinamica?capacidad=10` | Mochila 0/1 con PD | 1 |
| GET | `/api/seleccion/quicksort?criterio=ratio` | QuickSort | 1 (con MergeSort) |
| GET | `/api/seleccion/mergesort?criterio=peso` | MergeSort | |
| GET | `/api/grafo/rutas?origen=A&destino=H&costoMaximo=30` | Backtracking | 1 |
| GET | `/api/seleccion/repartir?contenedores=3` | Branch & Bound | 1 |

## El grafo de ejemplo

```
      4        3
  A ------ B ----- C
   \       |     / |  \
    \ 9    | 7  /2 |11 \
     \     |   /   |    \
      ---- C   D --+     E
                |  5    / \
             8  |      /10 \ 6
                F ----+     G
                 \ 3        | 4
                  H --------+
```

Aristas: A-B(4) A-C(9) B-C(3) B-D(7) C-D(2) C-E(11) D-E(5) D-F(8) E-F(6) E-G(10) F-H(3) G-H(4)

Está elegido a propósito para que el camino mínimo **no sea el directo**:
A→C cuesta 9 en línea recta, pero A→B→C cuesta 7. Si su Dijkstra devuelve 9,
la relajación de aristas está mal.

## Antes de entregar

- [ ] `./mvnw test` pasa
- [ ] no hay ninguna contraseña en el código ni en `application.properties`
- [ ] cada algoritmo implementado tiene al menos un test
- [ ] `docs/ENDPOINTS.md` documenta entrada y salida de cada endpoint
- [ ] el README explica **qué dominio eligieron y qué representa el costo**

Ver `GUIA-TPO.md` para el paso a paso.
