package uade.prog3.tpo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uade.prog3.tpo.algorithm.Recorridos;
import uade.prog3.tpo.model.Item;
import uade.prog3.tpo.model.Nodo;
import uade.prog3.tpo.repository.AlmacenNeo4j;
import uade.prog3.tpo.seed.DatosValdoria;
import uade.prog3.tpo.service.Grafo;
import uade.prog3.tpo.service.GrafoService;

import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de BFS y DFS (Hito 4).
 *
 * No tocan Neo4j: el grafo de Valdoria se arma con GrafoService igual que en
 * la aplicacion, pero leyendo de un almacen en memoria que devuelve
 * DatosValdoria, que es lo mismo que la carga inicial escribe en la base.
 */
class RecorridosTest {

    private final Recorridos recorridos = new Recorridos();

    /** Almacen de mentira: devuelve los datos de Valdoria sin ir a la base. */
    private static class AlmacenEnMemoria implements AlmacenNeo4j {
        public List<Nodo> todosLosNodos() { return DatosValdoria.nodos(); }
        public long contarNodos() { return DatosValdoria.ZONAS.size(); }
        public void guardarNodos(List<Nodo> nodos) { }
        public List<Item> todosLosItems() { return DatosValdoria.items(); }
        public void guardarItems(List<Item> items) { }
        public void borrarTodo() { }
        public String descripcionDelTransporte() { return "memoria"; }
    }

    private Grafo valdoria() {
        return new GrafoService(new AlmacenEnMemoria()).cargar(false);
    }

    /**
     * Grafo chico para verificar a mano:
     *
     *   A - B - D
     *   |   |
     *   C - E      F (aislado)
     */
    private Grafo chico() {
        Grafo g = new Grafo(false);
        g.agregarArista("A", "B", 1);
        g.agregarArista("A", "C", 1);
        g.agregarArista("B", "D", 1);
        g.agregarArista("B", "E", 1);
        g.agregarArista("C", "E", 1);
        g.agregarVertice("F");
        g.ordenarVecinosPorId();
        return g;
    }

    // --- Grafo chico -----------------------------------------------------------

    @Test
    @DisplayName("BFS visita por niveles: primero los vecinos del origen, despues los de ellos")
    void bfsPorNiveles() {
        assertEquals(List.of("A", "B", "C", "D", "E"), recorridos.bfs(chico(), "A"));
    }

    @Test
    @DisplayName("DFS se mete por el primer vecino hasta el fondo antes de volver")
    void dfsEnProfundidad() {
        // A -> B -> D (sin salida, vuelve) -> E -> C
        assertEquals(List.of("A", "B", "D", "E", "C"), recorridos.dfs(chico(), "A"));
    }

    @Test
    @DisplayName("Un vertice sin caminos no aparece en el recorrido y ninguno se repite")
    void noVisitaInalcanzablesNiRepite() {
        List<String> bfs = recorridos.bfs(chico(), "A");
        List<String> dfs = recorridos.dfs(chico(), "A");
        assertFalse(bfs.contains("F"));
        assertFalse(dfs.contains("F"));
        assertEquals(bfs.size(), new HashSet<>(bfs).size());
        assertEquals(dfs.size(), new HashSet<>(dfs).size());
    }

    @Test
    @DisplayName("Desde un vertice aislado, el recorrido es solo ese vertice")
    void origenAislado() {
        assertEquals(List.of("F"), recorridos.bfs(chico(), "F"));
        assertEquals(List.of("F"), recorridos.dfs(chico(), "F"));
    }

    @Test
    @DisplayName("Un origen que no existe tira IllegalArgumentException (el endpoint responde 400)")
    void origenInexistente() {
        assertThrows(IllegalArgumentException.class, () -> recorridos.bfs(chico(), "Z"));
        assertThrows(IllegalArgumentException.class, () -> recorridos.dfs(chico(), "Z"));
    }

    // --- Valdoria --------------------------------------------------------------

    @Test
    @DisplayName("Valdoria cargado como no dirigido: 38 zonas y 57 caminos")
    void valdoriaTamanio() {
        Grafo g = valdoria();
        assertEquals(38, g.cantidadVertices());
        assertEquals(57, g.cantidadAristas());
    }

    @Test
    @DisplayName("BFS desde la Aldea Brezo (VB1) recorre las 38 zonas en el orden esperado")
    void bfsValdoria() {
        List<String> esperado = List.of(
                "VB1", "VB2", "VB3", "VB4", "VB5", "VB6", "VB7", "VB8",
                "CN1", "CN3", "CN2", "CN4", "CN5", "CN6", "CN8", "CN7", "CN9",
                "DA1", "DA2", "DA3", "DA4", "DA5", "DA7", "DA6", "DA10", "DA8", "DA9",
                "CC1", "CC2", "CC3", "CC4", "CC5", "CC6", "CC8", "CC7", "CC11", "CC9", "CC10");
        assertEquals(esperado, recorridos.bfs(valdoria(), "VB1"));
    }

    @Test
    @DisplayName("DFS desde la Aldea Brezo (VB1) recorre las 38 zonas en el orden esperado")
    void dfsValdoria() {
        // Llega a la Guarida de Ignea (CC11) y sigue; DA8 y DA9 quedan para el
        // final porque solo se alcanzan volviendo hasta las Dunas.
        List<String> esperado = List.of(
                "VB1", "VB2", "VB3", "VB4", "VB5", "VB6", "VB7", "VB8",
                "CN1", "CN3", "CN2", "CN5", "CN6", "CN4", "CN7", "CN9", "CN8",
                "DA1", "DA2", "DA4", "DA5", "DA3", "DA6", "DA7", "DA10",
                "CC1", "CC2", "CC4", "CC3", "CC6", "CC5", "CC8", "CC11", "CC10", "CC9", "CC7",
                "DA8", "DA9");
        assertEquals(esperado, recorridos.dfs(valdoria(), "VB1"));
    }

    @Test
    @DisplayName("El reino es conexo: desde cualquier zona BFS y DFS llegan a las 38")
    void valdoriaEsConexo() {
        Grafo g = valdoria();
        for (String id : g.ids()) {
            assertEquals(38, new HashSet<>(recorridos.bfs(g, id)).size(), "BFS desde " + id);
            assertEquals(38, new HashSet<>(recorridos.dfs(g, id)).size(), "DFS desde " + id);
        }
    }

    @Test
    @DisplayName("Los caminos se recorren en los dos sentidos: desde la guarida final se vuelve al inicio")
    void valdoriaNoDirigido() {
        List<String> desdeElFinal = recorridos.bfs(valdoria(), "CC11");
        assertEquals("CC11", desdeElFinal.get(0));
        assertTrue(desdeElFinal.contains("VB1"));
    }
}
