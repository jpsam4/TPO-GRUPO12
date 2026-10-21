package uade.prog3.tpo.algorithm;

import org.springframework.stereotype.Component;
import uade.prog3.tpo.service.Grafo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
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
        boolean[] visitado = new boolean[n];
        Arrays.fill(dist, Grafo.INFINITO);
        Arrays.fill(predecesor, -1);

        PriorityQueue<EntradaCola> cola = new PriorityQueue<>();
        dist[origen] = 0.0;
        cola.add(new EntradaCola(origen, 0.0));

        while (!cola.isEmpty()) {
            EntradaCola actual = cola.poll();
            int u = actual.vertice();

            if (visitado[u]) {
                continue;   // entrada vieja que quedo en el heap tras una relajacion
            }
            visitado[u] = true;

            for (Grafo.Arista a : grafo.vecinos(u)) {
                int v = a.destino();
                if (!visitado[v]) {
                    double nuevoCosto = dist[u] + a.costo();
                    if (nuevoCosto < dist[v]) {   // relajacion de la arista (u, v)
                        dist[v] = nuevoCosto;
                        predecesor[v] = u;
                        cola.add(new EntradaCola(v, nuevoCosto));
                    }
                }
            }
        }

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
     * Floyd-Warshall: costo minimo entre TODOS los pares de vertices.
     *
     * Requisitos:
     *   - trabajar sobre la matriz de adyacencia (grafo.matrizDeAdyacencia())
     *   - el bucle del vertice intermedio k va SIEMPRE por fuera de los de i y j
     *   - detectar ciclos de costo negativo revisando la diagonal al terminar
     *
     * Complejidad esperada: O(V^3) en tiempo, O(V^2) en espacio.
     *
     * Devuelve un mapa "origen->destino" con el costo minimo de cada par.
     */
    public Map<String, Double> floydWarshall(Grafo grafo) {
        throw new PendienteDeImplementar("Floyd-Warshall");
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
