package uade.prog3.tpo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uade.prog3.tpo.algorithm.CaminosMinimos;
import uade.prog3.tpo.model.Item;
import uade.prog3.tpo.model.Nodo;
import uade.prog3.tpo.repository.AlmacenNeo4j;
import uade.prog3.tpo.seed.DatosValdoria;
import uade.prog3.tpo.service.Grafo;
import uade.prog3.tpo.service.GrafoService;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de Floyd-Warshall (Hito 7): todos los pares, costos negativos,
 * deteccion de ciclos negativos y comparacion contra Dijkstra V veces.
 *
 * No tocan Neo4j: Valdoria se arma con GrafoService leyendo DatosValdoria.
 */
class FloydTest {

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

    private final GrafoService servicio = new GrafoService(new AlmacenEnMemoria());

    /** Energia pura, no dirigido (el mismo grafo que usa Dijkstra). */
    private Grafo energia() {
        return servicio.cargar(false);
    }

    /** Costo neto: energia menos lo que cura la zona de llegada. */
    private Grafo neto() {
        return servicio.cargarConCuracion(null);
    }

    /**
     * Grafo dirigido chico, con una arista negativa pero sin ciclo negativo:
     *
     *   A -> B (4)    A -> C (5)    B -> D (3)    C -> B (-2)    C -> D (6)
     *
     * A mano: A->B directo cuesta 4, pero A->C->B cuesta 5-2 = 3.
     *         A->D = A->C->B->D = 5-2+3 = 6.
     */
    private Grafo chicoConNegativa() {
        Grafo g = new Grafo(true);
        g.agregarArista("A", "B", 4);
        g.agregarArista("A", "C", 5);
        g.agregarArista("B", "D", 3);
        g.agregarArista("C", "B", -2);
        g.agregarArista("C", "D", 6);
        return g;
    }

    // --- Grafo chico, a mano --------------------------------------------------------

    @Test
    @DisplayName("Con una arista negativa (sin ciclo), Floyd encuentra A->C->B = 3 y A->D = 6")
    void aristaNegativaSinCiclo() {
        Map<String, Double> costos = caminosMinimos.floydWarshall(chicoConNegativa());
        assertEquals(3.0, (double) costos.get("A->B"));
        assertEquals(6.0, (double) costos.get("A->D"));
        assertEquals(1.0, (double) costos.get("C->D"));       // C->B->D = -2+3
        assertEquals(0.0, (double) costos.get("A->A"));
        assertEquals(Grafo.INFINITO, (double) costos.get("D->A"));   // dirigido: de D no se sale
    }

    @Test
    @DisplayName("La matriz siguiente[][] reconstruye A->C->B->D")
    void reconstruyeCamino() {
        CaminosMinimos.Camino c = caminosMinimos.caminoFloyd(chicoConNegativa(), "A", "D");
        assertEquals(List.of("A", "C", "B", "D"), c.vertices());
        assertEquals(6.0, c.costoTotal());
    }

    @Test
    @DisplayName("Un ciclo negativo se detecta en la diagonal y el endpoint no devuelve costos")
    void detectaCicloNegativo() {
        Grafo g = new Grafo(true);
        g.agregarArista("A", "B", 1);
        g.agregarArista("B", "C", -3);
        g.agregarArista("C", "B", 1);    // B->C->B cuesta -2
        g.agregarArista("C", "D", 2);

        CaminosMinimos.Floyd f = caminosMinimos.floyd(g);
        assertTrue(f.hayCicloNegativo());
        assertTrue(f.cicloNegativo().containsAll(List.of("B", "C")));
        assertEquals(f.cicloNegativo().get(0), f.cicloNegativo().get(f.cicloNegativo().size() - 1));
        assertThrows(IllegalStateException.class, () -> caminosMinimos.floydWarshall(g));
    }

    @Test
    @DisplayName("Floyd evalua exactamente V^3 ternas (k, i, j)")
    void cuentaVAlCubo() {
        assertEquals(64, caminosMinimos.floyd(chicoConNegativa()).comparaciones());     // 4^3
        assertEquals(38L * 38 * 38, caminosMinimos.floyd(energia()).comparaciones());   // 54.872
    }

    // --- Valdoria -------------------------------------------------------------------

    @Test
    @DisplayName("Energia pura: Floyd da VB1->CC11 = 103, igual que Dijkstra")
    void valdoriaEnergia() {
        assertEquals(103.0, (double) caminosMinimos.floydWarshall(energia()).get("VB1->CC11"));
    }

    @Test
    @DisplayName("Costo neto: VB1->CC11 = 79 (103 de energia menos lo que curan las 4 fuentes del camino)")
    void valdoriaNeto() {
        CaminosMinimos.Camino c = caminosMinimos.caminoFloyd(neto(), "VB1", "CC11");
        assertEquals(79.0, c.costoTotal());
        assertEquals(22, c.vertices().size());
        assertEquals("VB1", c.vertices().get(0));
        assertEquals("CC11", c.vertices().get(c.vertices().size() - 1));
    }

    @Test
    @DisplayName("Costo neto: ir de la Aldea Brezo a la Fuente Clara cuesta -3 y volver cuesta 3")
    void valdoriaNetoNegativo() {
        Map<String, Double> costos = caminosMinimos.floydWarshall(neto());
        assertEquals(-3.0, (double) costos.get("VB1->VB2"));
        assertEquals(3.0, (double) costos.get("VB2->VB1"));
    }

    @Test
    @DisplayName("Con los datos reales no hay ciclos negativos (ir y volver a una fuente cuesta >= 0)")
    void valdoriaSinCicloNegativo() {
        assertFalse(caminosMinimos.floyd(neto()).hayCicloNegativo());
    }

    @Test
    @DisplayName("Si las fuentes curaran 7, aparece el ciclo VB1-VB2 de costo -1 y Floyd lo detecta")
    void valdoriaConCuracion7() {
        Grafo g = servicio.cargarConCuracion(7.0);
        CaminosMinimos.Floyd f = caminosMinimos.floyd(g);
        assertTrue(f.hayCicloNegativo());
        assertTrue(f.cicloNegativo().containsAll(List.of("VB1", "VB2")));
        assertEquals(3, f.cicloNegativo().size());     // VB2 -> VB1 -> VB2 (o al reves)
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> caminosMinimos.caminoFloyd(g, "VB1", "CC11"));
        assertTrue(e.getMessage().contains("ciclo de costo negativo"));
    }

    // --- Comparacion contra Dijkstra V veces ----------------------------------------

    @Test
    @DisplayName("Floyd y Dijkstra desde cada zona dan la misma matriz de costos en Valdoria")
    void comparacionMismosResultados() {
        CaminosMinimos.Comparacion c = caminosMinimos.compararFloydConDijkstra(energia());
        assertTrue(c.mismasDistancias());
        assertEquals(38, c.vertices());
        assertEquals(57, c.aristas());
        assertEquals(54_872, c.floydComparaciones());
        assertEquals(38 * 38, c.dijkstraNodosExpandidos());   // cada corrida cierra las 38 zonas
        assertTrue(c.dijkstraExtracciones() >= c.dijkstraNodosExpandidos());
        assertTrue(c.dijkstraRelajaciones() < c.floydComparaciones());
    }

    @Test
    @DisplayName("Floyd coincide con Dijkstra par por par")
    void floydIgualDijkstraParPorPar() {
        Grafo g = energia();
        Map<String, Double> floyd = caminosMinimos.floydWarshall(g);
        for (String o : List.of("VB1", "CN5", "DA4", "CC11")) {
            for (String d : g.ids()) {
                assertEquals(caminosMinimos.dijkstra(g, o, d).costoTotal(), (double) floyd.get(o + "->" + d), o + "->" + d);
            }
        }
    }
}
