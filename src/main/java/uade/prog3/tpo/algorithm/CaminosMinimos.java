package uade.prog3.tpo.algorithm;

import org.springframework.stereotype.Component;
import uade.prog3.tpo.service.Grafo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * UNIDAD: Grafos II (clase 5) y PD sobre grafos (clase 8)
 * PUNTAJE: forma parte de los 3 puntos de "Dijkstra, Prim, Kruskal"
 *          y de 1 punto de programacion dinamica si implementan Floyd.
 */
@Component
public class CaminosMinimos {

    /** Resultado de una consulta de camino minimo. */
    public record Camino(List<String> vertices, double costoTotal) { }

    /**
     * Entrada de la cola de prioridad de Dijkstra: un vertice y la distancia
     * acumulada con la que fue encolado.
     */
    private record EntradaCola(int vertice, double distancia) implements Comparable<EntradaCola> {
        @Override
        public int compareTo(EntradaCola otra) {
            int cmp = Double.compare(this.distancia, otra.distancia);
            return (cmp != 0) ? cmp : Integer.compare(this.vertice, otra.vertice);
        }
    }

    /**
     * Dijkstra: camino de costo minimo desde un origen hasta un destino.
     *
     * En cada paso elige el vertice todavia no cerrado con menor distancia
     * acumulada desde el origen (eleccion greedy), lo marca como definitivo y
     * relaja todas sus aristas salientes: si pasar por u mejora la distancia
     * conocida hasta un vecino v (dist[u] + costo(u,v) < dist[v]), actualiza
     * dist[v], anota que a v se llega desde u (predecesor[v] = u) y encola la
     * nueva distancia en el heap.
     *
     * Como PriorityQueue de Java no tiene decreaseKey en O(log V), al mejorar
     * dist[v] se inserta una entrada nueva y la vieja queda obsoleta en el
     * heap; cuando mas adelante sale del heap, el vertice ya figura en
     * visitado[] y se descarta en O(1) (borrado perezoso).
     *
     * Complejidad temporal: O((V + E) log V) con heap binario (PriorityQueue) y
     *   lista de adyacencia. Cada vertice se cierra a lo sumo una vez y cada
     *   arista produce a lo sumo una insercion en el heap de tamano <= E
     *   (y log E <= 2 log V = O(log V)).
     * Complejidad espacial: O(V + E). Arreglos dist[], predecesor[] y visitado[]
     *   de tamano V, mas la cola de prioridad con a lo sumo E entradas.
     *
     * Para el coloquio:
     *   - Es greedy porque al cerrar el vertice u de menor dist[u], asume que
     *     ningun camino futuro pasando por vertices aun abiertos puede mejorar
     *     dist[u].
     *   - Eso solo vale si todos los costos son >= 0: si hubiera una arista
     *     negativa, un camino mas largo ahora podria restar despues y mejorar
     *     un vertice que Dijkstra ya habia cerrado y no vuelve a revisar.
     */
    public Camino dijkstra(Grafo grafo, String origenId, String destinoId) {
        int origen = grafo.indiceDe(origenId);     // si no existe: IllegalArgumentException -> 400
        int destino = grafo.indiceDe(destinoId);
        int n = grafo.cantidadVertices();

        // Validacion previa: ningun camino puede tener costo negativo.
        for (int u = 0; u < n; u++) {
            for (Grafo.Arista a : grafo.vecinos(u)) {
                if (a.costo() < 0) {
                    throw new IllegalArgumentException(
                            "Dijkstra no admite aristas de costo negativo: "
                                    + grafo.idDe(u) + " -> " + grafo.idDe(a.destino())
                                    + " (" + a.costo() + ")");
                }
            }
        }

        double[] dist = new double[n];
        int[] predecesor = new int[n];
        dijkstraDesde(grafo, origen, dist, predecesor, new long[3]);

        if (dist[destino] == Grafo.INFINITO) {
            return new Camino(List.of(), Grafo.INFINITO);
        }

        // Reconstruccion del camino desde el destino hacia atras con predecesor[].
        ArrayDeque<String> camino = new ArrayDeque<>();
        for (int v = destino; v != -1; v = predecesor[v]) {
            camino.addFirst(grafo.idDe(v));
        }
        return new Camino(new ArrayList<>(camino), dist[destino]);
    }

    /**
     * Nucleo de Dijkstra desde un origen hacia TODOS los vertices. Llena
     * dist[] y predecesor[] (de tamano V). Lo usan dijkstra() y la comparacion
     * contra Floyd-Warshall.
     *
     * contadores[0] = vertices cerrados (nodos expandidos)
     * contadores[1] = extracciones del heap, incluidas las entradas obsoletas
     * contadores[2] = relajaciones que mejoraron una distancia
     */
    private void dijkstraDesde(Grafo grafo, int origen, double[] dist, int[] predecesor, long[] contadores) {
        int n = grafo.cantidadVertices();
        boolean[] visitado = new boolean[n];
        Arrays.fill(dist, Grafo.INFINITO);
        Arrays.fill(predecesor, -1);

        PriorityQueue<EntradaCola> cola = new PriorityQueue<>();
        dist[origen] = 0.0;
        cola.add(new EntradaCola(origen, 0.0));

        while (!cola.isEmpty()) {
            EntradaCola actual = cola.poll();
            contadores[1]++;
            int u = actual.vertice();

            if (visitado[u]) {
                continue;   // entrada vieja que quedo en el heap tras una relajacion
            }
            visitado[u] = true;
            contadores[0]++;

            for (Grafo.Arista a : grafo.vecinos(u)) {
                int v = a.destino();
                if (!visitado[v]) {
                    double nuevoCosto = dist[u] + a.costo();
                    if (nuevoCosto < dist[v]) {   // relajacion de la arista (u, v)
                        dist[v] = nuevoCosto;
                        predecesor[v] = u;
                        cola.add(new EntradaCola(v, nuevoCosto));
                        contadores[2]++;
                    }
                }
            }
        }
    }

    /**
     * Resultado completo de Floyd-Warshall.
     *
     * @param ids             id de cada vertice (el indice i de las matrices es ids.get(i))
     * @param dist            dist[i][j] = costo minimo de i a j (INFINITO si no hay camino)
     * @param siguiente       siguiente[i][j] = primer vertice despues de i en el camino minimo a j
     *                        (-1 si no hay camino); sirve para reconstruir el camino
     * @param comparaciones   ternas (k, i, j) evaluadas: siempre V^3
     * @param mejoras         cuantas de esas ternas mejoraron dist[i][j]
     * @param cicloNegativo   un ciclo de costo negativo (ids, cerrando en el primero),
     *                        o lista vacia si no hay
     */
    public record Floyd(List<String> ids, double[][] dist, int[][] siguiente,
                        long comparaciones, long mejoras, List<String> cicloNegativo) {
        public boolean hayCicloNegativo() {
            return !cicloNegativo.isEmpty();
        }
    }

    /**
     * Floyd-Warshall: costo minimo entre TODOS los pares de vertices.
     *
     * Programacion dinamica sobre la matriz de adyacencia. Despues de la vuelta
     * k, dist[i][j] es el costo minimo de i a j usando como intermedios solo
     * los vertices 0..k:
     *
     *   dist[i][j] = min(dist[i][j], dist[i][k] + dist[k][j])
     *
     * Por eso el bucle de k va POR FUERA: cuando se usa el vertice k como
     * intermedio, dist[i][k] y dist[k][j] ya tienen que estar resueltos con
     * los intermedios 0..k-1.
     *
     * Admite costos negativos (en Valdoria aparecen con el costo neto: llegar a
     * una fuente cura energia). Lo que no admite son ciclos de costo negativo:
     * si existen, dar vueltas abarata sin limite y no hay camino minimo.
     * Se detectan al terminar: hay un ciclo negativo si algun dist[i][i] < 0.
     *
     * Complejidad temporal: O(V^3): tres bucles anidados de V vueltas cada uno,
     *   con trabajo O(1) adentro. No depende de E.
     * Complejidad espacial: O(V^2): las matrices dist y siguiente.
     */
    public Floyd floyd(Grafo grafo) {
        int n = grafo.cantidadVertices();
        double[][] dist = grafo.matrizDeAdyacencia();
        int[][] siguiente = new int[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                siguiente[i][j] = (i == j || dist[i][j] < Grafo.INFINITO) ? j : -1;
            }
        }

        long comparaciones = 0;
        long mejoras = 0;
        for (int k = 0; k < n; k++) {               // vertice intermedio: SIEMPRE el de afuera
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    comparaciones++;
                    // Con INFINITO la suma da INFINITO y la comparacion es falsa: no hace falta un if aparte.
                    double porK = dist[i][k] + dist[k][j];
                    if (porK < dist[i][j]) {
                        dist[i][j] = porK;
                        siguiente[i][j] = siguiente[i][k];
                        mejoras++;
                    }
                }
            }
        }

        List<String> ids = new ArrayList<>(grafo.ids());
        return new Floyd(ids, dist, siguiente, comparaciones, mejoras, buscarCicloNegativo(grafo, dist, siguiente));
    }

    /**
     * Si algun dist[v][v] < 0, v esta en un ciclo negativo o llega a uno y
     * vuelve. Para mostrar el ciclo concreto se siguen los punteros siguiente[][]
     * desde v hacia v: como el camino "minimo" da vueltas al ciclo, en algun
     * momento se repite un vertice, y lo que hay entre las dos apariciones es
     * el ciclo. O(V).
     */
    private List<String> buscarCicloNegativo(Grafo grafo, double[][] dist, int[][] siguiente) {
        int n = dist.length;
        int peor = -1;
        for (int v = 0; v < n; v++) {
            if (dist[v][v] < 0 && (peor == -1 || dist[v][v] < dist[peor][peor])) {
                peor = v;
            }
        }
        if (peor == -1) {
            return List.of();
        }
        int[] posicion = new int[n];
        Arrays.fill(posicion, -1);
        List<Integer> recorrido = new ArrayList<>();
        int actual = peor;
        while (actual != -1 && posicion[actual] == -1) {
            posicion[actual] = recorrido.size();
            recorrido.add(actual);
            actual = siguiente[actual][peor];
        }
        List<String> ciclo = new ArrayList<>();
        if (actual == -1) {                 // no deberia pasar; se informa al menos el vertice
            ciclo.add(grafo.idDe(peor));
            ciclo.add(grafo.idDe(peor));
            return ciclo;
        }
        for (int p = posicion[actual]; p < recorrido.size(); p++) {
            ciclo.add(grafo.idDe(recorrido.get(p)));
        }
        ciclo.add(grafo.idDe(actual));      // se cierra en el primero
        return ciclo;
    }

    /**
     * Floyd-Warshall para el endpoint: costo minimo de todos los pares, como
     * mapa "origen->destino". Si hay un ciclo negativo no existe camino
     * minimo, asi que tira IllegalStateException (el endpoint responde 422).
     */
    public Map<String, Double> floydWarshall(Grafo grafo) {
        Floyd f = floyd(grafo);
        exigirSinCicloNegativo(f);
        Map<String, Double> costos = new LinkedHashMap<>();
        int n = f.ids().size();
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                costos.put(f.ids().get(i) + "->" + f.ids().get(j), f.dist()[i][j]);
            }
        }
        return costos;
    }

    /**
     * Camino minimo de origen a destino reconstruido con la matriz siguiente[][]
     * de Floyd: se arranca en el origen y se salta a siguiente[actual][destino]
     * hasta llegar. O(V^3) por correr Floyd + O(V) la reconstruccion.
     */
    public Camino caminoFloyd(Grafo grafo, String origenId, String destinoId) {
        int origen = grafo.indiceDe(origenId);
        int destino = grafo.indiceDe(destinoId);
        Floyd f = floyd(grafo);
        exigirSinCicloNegativo(f);
        if (f.siguiente()[origen][destino] == -1) {
            return new Camino(List.of(), Grafo.INFINITO);
        }
        List<String> vertices = new ArrayList<>();
        int actual = origen;
        vertices.add(grafo.idDe(actual));
        while (actual != destino) {
            actual = f.siguiente()[actual][destino];
            vertices.add(grafo.idDe(actual));
        }
        return new Camino(vertices, f.dist()[origen][destino]);
    }

    private static void exigirSinCicloNegativo(Floyd f) {
        if (f.hayCicloNegativo()) {
            throw new IllegalStateException("Hay un ciclo de costo negativo: "
                    + String.join(" -> ", f.cicloNegativo())
                    + ". Dando vueltas la energia baja sin limite, asi que no existe camino minimo.");
        }
    }

    /**
     * Comparacion pedida en el Hito 7: Floyd-Warshall contra correr Dijkstra
     * desde cada uno de los V vertices, sobre el mismo grafo.
     *
     * @param floydComparaciones          ternas (k, i, j) que evalua Floyd: V^3
     * @param dijkstraNodosExpandidos     vertices cerrados sumando las V corridas
     * @param dijkstraExtracciones        sacadas del heap, incluidas las obsoletas
     * @param dijkstraRelajaciones        relajaciones que mejoraron una distancia
     * @param mismasDistancias            si las dos matrices de costos coinciden
     */
    public record Comparacion(int vertices, int aristas,
                              long floydComparaciones, long floydMejoras,
                              long dijkstraNodosExpandidos, long dijkstraExtracciones,
                              long dijkstraRelajaciones, boolean mismasDistancias) { }

    /**
     * Corre Floyd una vez y Dijkstra V veces y cuenta el trabajo de cada uno.
     * Dijkstra exige costos no negativos, asi que la comparacion se hace sobre
     * el grafo de energia (sin la curacion de las fuentes).
     *
     *   Floyd:        O(V^3)                         -> V^3 ternas, siempre
     *   Dijkstra x V: O(V * (V + E) log V) con heap  -> conviene en grafos ralos
     */
    public Comparacion compararFloydConDijkstra(Grafo grafo) {
        int n = grafo.cantidadVertices();
        Floyd f = floyd(grafo);

        long[] contadores = new long[3];
        boolean iguales = true;
        double[] dist = new double[n];
        int[] predecesor = new int[n];
        for (int origen = 0; origen < n; origen++) {
            dijkstraDesde(grafo, origen, dist, predecesor, contadores);
            for (int j = 0; j < n; j++) {
                if (Math.abs(dist[j] - f.dist()[origen][j]) > 1e-9
                        && !(dist[j] == Grafo.INFINITO && f.dist()[origen][j] == Grafo.INFINITO)) {
                    iguales = false;
                }
            }
        }
        return new Comparacion(n, grafo.cantidadAristas(), f.comparaciones(), f.mejoras(),
                contadores[0], contadores[1], contadores[2], iguales);
    }

    /**
     * UCS (Uniform Cost Search): igual que Dijkstra pero con parada temprana
     * al expandir el destino.
     *
     * OPCIONAL. Solo suma si ya estan implementados Dijkstra y Floyd.
     * Si lo hacen, en el coloquio hay que poder mostrar la diferencia de
     * nodos expandidos respecto de Dijkstra sobre la misma consulta.
     */
    public Camino ucs(Grafo grafo, String origenId, String destinoId) {
        throw new PendienteDeImplementar("UCS");
    }
}
