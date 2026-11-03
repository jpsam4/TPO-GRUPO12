package uade.prog3.tpo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uade.prog3.tpo.algorithm.CaminosMinimos;
import uade.prog3.tpo.algorithm.CaminosMinimos.Camino;
import uade.prog3.tpo.model.Item;
import uade.prog3.tpo.model.Nodo;
import uade.prog3.tpo.repository.AlmacenNeo4j;
import uade.prog3.tpo.seed.DatosValdoria;
import uade.prog3.tpo.service.Grafo;
import uade.prog3.tpo.service.GrafoService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de UCS (Uniform Cost Search, opcional): mismo resultado que Dijkstra
 * pero expandiendo solo hasta sacar el destino del heap.
 */
class UcsTest {

    private final CaminosMinimos caminosMinimos = new CaminosMinimos();

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

    @Test
    @DisplayName("UCS da el mismo costo que Dijkstra para TODOS los pares de zonas de Valdoria")
    void mismoCostoQueDijkstraEnTodosLosPares() {
        Grafo g = valdoria();
        for (String o : g.ids()) {
            for (String d : g.ids()) {
                assertEquals(caminosMinimos.dijkstra(g, o, d).costoTotal(),
                        caminosMinimos.ucs(g, o, d).costoTotal(), o + " -> " + d);
            }
        }
    }

    @Test
    @DisplayName("Valle de Brezo: VB1 -> VB8 cuesta 20 y UCS expande 8 zonas contra las 38 de Dijkstra")
    void expandeMenosQueDijkstra() {
        CaminosMinimos.ComparacionUcs c = caminosMinimos.compararUcsConDijkstra(valdoria(), "VB1", "VB8");
        assertEquals(List.of("VB1", "VB2", "VB3", "VB5", "VB6", "VB8"), c.camino().vertices());
        assertEquals(20.0, c.camino().costoTotal());
        assertEquals(8, c.ucsNodosExpandidos());
        assertEquals(38, c.dijkstraNodosExpandidos());
        assertTrue(c.mismoCosto());
    }

    @Test
    @DisplayName("Parar al ENCOLAR el destino seria un error: DA4 -> DA9 entra al heap con 13 pero el minimo es 11")
    void noParaAlEncolar() {
        Camino c = caminosMinimos.ucs(valdoria(), "DA4", "DA9");
        assertEquals(11.0, c.costoTotal());
        assertEquals(List.of("DA4", "DA5", "DA6", "DA9"), c.vertices());
    }

    @Test
    @DisplayName("Origen igual a destino: camino de una zona, costo 0, una sola expansion")
    void origenIgualDestino() {
        CaminosMinimos.ComparacionUcs c = caminosMinimos.compararUcsConDijkstra(valdoria(), "CN3", "CN3");
        assertEquals(List.of("CN3"), c.camino().vertices());
        assertEquals(0.0, c.camino().costoTotal());
        assertEquals(1, c.ucsNodosExpandidos());
    }

    @Test
    @DisplayName("Sin camino devuelve lista vacia e INFINITO; con arista negativa o zona inexistente, IllegalArgumentException")
    void casosBorde() {
        Grafo g = new Grafo(false);
        g.agregarVertice("A");
        g.agregarVertice("B");
        Camino c = caminosMinimos.ucs(g, "A", "B");
        assertTrue(c.vertices().isEmpty());
        assertEquals(Grafo.INFINITO, c.costoTotal());

        Grafo negativo = new Grafo(false);
        negativo.agregarArista("A", "B", -1);
        assertThrows(IllegalArgumentException.class, () -> caminosMinimos.ucs(negativo, "A", "B"));
        assertThrows(IllegalArgumentException.class, () -> caminosMinimos.ucs(valdoria(), "VB1", "ZZ9"));
    }
}
