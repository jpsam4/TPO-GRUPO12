package uade.prog3.tpo.algorithm;

import org.springframework.stereotype.Component;
import uade.prog3.tpo.service.Grafo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * UNIDAD: Grafos I (clase 4)          PUNTAJE: 2 puntos
 *
 * Recorridos sobre el grafo del reino de Valdoria.
 *
 * Los dos trabajan sobre la lista de adyacencia de Grafo y visitan los vecinos
 * en el orden en que estan en la lista (ordenados por id al cargar el grafo).
 * Los dos marcan visitados en un arreglo boolean indexado por vertice: marcar
 * y consultar es O(1).
 */
@Component
public class Recorridos {

    /**
     * Recorrido en profundidad (DFS) desde un vertice, recursivo.
     *
     * Entra a una zona, la marca y se mete por el primer camino que lleve a una
     * zona sin visitar. Cuando una zona no tiene mas vecinos nuevos, vuelve
     * (backtrack) a la anterior y sigue con el proximo camino.
     *
     * Complejidad temporal: O(V + E). Cada vertice se visita una vez (despues
     *   queda marcado) y su lista de vecinos se recorre una sola vez, en la
     *   unica llamada que lo visita. La suma de los largos de todas las listas
     *   es 2E en un grafo no dirigido.
     * Complejidad espacial: O(V). Arreglo de visitados, lista resultado y la
     *   pila de llamadas, que en el peor caso (un camino largo) tiene V marcos.
     *
     * @param grafo grafo cargado en memoria
     * @param origenId id de la zona de partida (ej. "VB1")
     * @return ids de las zonas en el orden en que se visitan
     */
    public List<String> dfs(Grafo grafo, String origenId) {
        int origen = grafo.indiceDe(origenId);   // si no existe: IllegalArgumentException -> 400
        boolean[] visitado = new boolean[grafo.cantidadVertices()];
        List<String> orden = new ArrayList<>();
        dfsDesde(grafo, origen, visitado, orden);
        return orden;
    }

    private void dfsDesde(Grafo grafo, int v, boolean[] visitado, List<String> orden) {
        visitado[v] = true;
        orden.add(grafo.idDe(v));
        for (Grafo.Arista a : grafo.vecinos(v)) {
            if (!visitado[a.destino()]) {
                dfsDesde(grafo, a.destino(), visitado, orden);
            }
        }
    }

    /**
     * Recorrido en anchura (BFS) desde un vertice, con una cola FIFO.
     *
     * Visita primero todas las zonas a 1 camino del origen, despues las que
     * estan a 2, y asi. Cada zona se marca AL ENCOLARLA: si se marcara al
     * desencolar, una zona con varios vecinos ya visitados entraria varias
     * veces en la cola.
     *
     * Complejidad temporal: O(V + E). Cada vertice entra y sale de la cola una
     *   sola vez (O(1) cada operacion con ArrayDeque) y, al salir, se recorre
     *   su lista de vecinos una vez.
     * Complejidad espacial: O(V). Arreglo de visitados, cola y lista resultado.
     *
     * Para el coloquio: BFS da el camino con menos CAMINOS (saltos), pero no
     * el de menos ENERGIA. Ej.: de DA4 a DA9, BFS llega en 2 caminos
     * (DA4-DA7-DA9, 13 de energia), pero el mas barato usa 3 caminos
     * (DA4-DA5-DA6-DA9, 11 de energia). Ese lo encuentra Dijkstra.
     *
     * @param grafo grafo cargado en memoria
     * @param origenId id de la zona de partida (ej. "VB1")
     * @return ids de las zonas en el orden en que se visitan
     */
    public List<String> bfs(Grafo grafo, String origenId) {
        int origen = grafo.indiceDe(origenId);
        boolean[] visitado = new boolean[grafo.cantidadVertices()];
        List<String> orden = new ArrayList<>();

        ArrayDeque<Integer> cola = new ArrayDeque<>();
        visitado[origen] = true;
        cola.addLast(origen);

        while (!cola.isEmpty()) {
            int v = cola.pollFirst();
            orden.add(grafo.idDe(v));
            for (Grafo.Arista a : grafo.vecinos(v)) {
                if (!visitado[a.destino()]) {
                    visitado[a.destino()] = true;   // se marca al encolar
                    cola.addLast(a.destino());
                }
            }
        }
        return orden;
    }
}
