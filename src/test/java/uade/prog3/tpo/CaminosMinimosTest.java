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
 * Tests de Dijkstra (Hito 5: caminos minimos con reconstruccion de camino).
 *
 * No tocan Neo4j: trabajan sobre el grafo de Valdoria cargado en memoria desde
 * DatosValdoria y sobre el grafo de referencia de la guia (A..H).
 */
class CaminosMinimosTest {

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

    /**
     * Grafo de 8 vertices y 12 aristas de GUIA-TPO.md:
     *   dijkstra(A, C) = 7 por A->B->C (no 9 por el directo)
     *   dijkstra(A, H) = 20 por A->B->C->D->F->H (4+3+2+8+3)
     */
    private Grafo grafoGuia() {
        Grafo g = new Grafo(false);
        g.agregarArista("A", "B", 4);
        g.agregarArista("A", "C", 9);
        g.agregarArista("B", "C", 3);
        g.agregarArista("B", "E", 7);
        g.agregarArista("C", "D", 2);
        g.agregarArista("C", "F", 12);
        g.agregarArista("D", "E", 5);
        g.agregarArista("D", "F", 8);
        g.agregarArista("E", "F", 6);
        g.agregarArista("E", "G", 9);
        g.agregarArista("F", "H", 3);
        g.agregarArista("G", "H", 4);
        g.ordenarVecinosPorId();
        return g;
    }

    // --- Verificacion contra los casos de GUIA-TPO.md --------------------------

    @Test
    @DisplayName("Guia TPO: dijkstra(A, C) elige A->B->C de costo 7 en lugar del directo de costo 9")
    void guiaCaminoAConRelajacion() {
        Camino c = caminosMinimos.dijkstra(grafoGuia(), "A", "C");
        assertEquals(7.0, c.costoTotal());
        assertEquals(List.of("A", "B", "C"), c.vertices());
    }

    @Test
    @DisplayName("Guia TPO: dijkstra(A, H) da 20 por A->B->C->D->F->H")
    void guiaCaminoAH() {
        Camino c = caminosMinimos.dijkstra(grafoGuia(), "A", "H");
        assertEquals(20.0, c.costoTotal());
        assertEquals(List.of("A", "B", "C", "D", "F", "H"), c.vertices());
    }

    // --- Verificacion a mano sobre el reino de Valdoria ------------------------

    @Test
    @DisplayName("Valle de Brezo a mano: de VB1 (Aldea Brezo) a VB8 (Cubil de Musgo) cuesta 20 por VB1-VB2-VB3-VB5-VB6-VB8")
    void valleDeBrezoAMano() {
        // Calculo a mano sobre la Region 1:
        //   VB1->VB2 (3) -> VB3 (+4=7) -> VB5 (+3=10) -> VB6 (+6=16) -> VB8 (+4=20).
        //   Relaja el camino antes descubierto por VB7: VB5(10)->VB7(+5=15)->VB8(+6=21),
        //   y descarta VB3(7)->VB4(+6=13)->VB6(+4=17).
        Camino c = caminosMinimos.dijkstra(valdoria(), "VB1", "VB8");
        assertEquals(20.0, c.costoTotal());
        assertEquals(List.of("VB1", "VB2", "VB3", "VB5", "VB6", "VB8"), c.vertices());
    }

    @Test
    @DisplayName("Dunas de Ambar: de DA4 a DA9 Dijkstra prefiere 3 caminos de costo 11 antes que 2 caminos de costo 13 (BFS)")
    void dunasDeAmbarMenosEnergiaQueMenosSaltos() {
        // Caso citado en ENDPOINTS.md:
        //   En 2 saltos: DA4 -> DA7 (9) -> DA9 (4) = 13 de energia.
        //   En 3 saltos: DA4 -> DA5 (3) -> DA6 (3) -> DA9 (5) = 11 de energia.
        Camino c = caminosMinimos.dijkstra(valdoria(), "DA4", "DA9");
        assertEquals(11.0, c.costoTotal());
        assertEquals(List.of("DA4", "DA5", "DA6", "DA9"), c.vertices());
    }

    @Test
    @DisplayName("Reino completo: de VB1 (Aldea Brezo) a CC11 (Guarida de Ignea) cuesta 103 y cruza 22 zonas")
    void reinoCompletoAldeaAGuaridaFinal() {
        Grafo g = valdoria();
        Camino c = caminosMinimos.dijkstra(g, "VB1", "CC11");

        List<String> esperado = List.of(
                // Valle de Brezo (20) + paso a CN (5) = 25
                "VB1", "VB2", "VB3", "VB5", "VB6", "VB8",
                // Cienagas de Niebla (23) + paso a DA (5) = 53
                "CN1", "CN3", "CN6", "CN7", "CN9",
                // Dunas de Ambar (20) + paso a CC (5) = 78
                "DA1", "DA3", "DA5", "DA6", "DA7", "DA10",
                // Cumbres de Ceniza (25) = 103
                "CC1", "CC2", "CC5", "CC8", "CC11"
        );
        assertEquals(22, c.vertices().size());
        assertEquals(esperado, c.vertices());
        assertEquals(103.0, c.costoTotal());
        assertEquals(c.costoTotal(), sumarAristasDelCamino(g, c.vertices()));
    }

    @Test
    @DisplayName("Como el grafo es no dirigido, volver de CC11 a VB1 cuesta los mismos 103 puntos de energia")
    void caminoInversoMismoCosto() {
        Grafo g = valdoria();
        Camino ida = caminosMinimos.dijkstra(g, "VB1", "CC11");
        Camino vuelta = caminosMinimos.dijkstra(g, "CC11", "VB1");

        assertEquals(ida.costoTotal(), vuelta.costoTotal());
        assertEquals("CC11", vuelta.vertices().get(0));
        assertEquals("VB1", vuelta.vertices().get(vuelta.vertices().size() - 1));
        assertEquals(vuelta.costoTotal(), sumarAristasDelCamino(g, vuelta.vertices()));
    }

    // --- Casos borde y validaciones --------------------------------------------

    @Test
    @DisplayName("Origen igual a destino devuelve costo 0 y un unico vertice")
    void origenIgualADestino() {
        Camino c = caminosMinimos.dijkstra(valdoria(), "VB1", "VB1");
        assertEquals(0.0, c.costoTotal());
        assertEquals(List.of("VB1"), c.vertices());
    }

    @Test
    @DisplayName("Si el destino es inalcanzable, devuelve lista vacia y costo INFINITO")
    void destinoInalcanzable() {
        Grafo g = new Grafo(false);
        g.agregarArista("A", "B", 3);
        g.agregarVertice("ISLA");

        Camino c = caminosMinimos.dijkstra(g, "A", "ISLA");
        assertTrue(c.vertices().isEmpty());
        assertEquals(Grafo.INFINITO, c.costoTotal());
    }

    @Test
    @DisplayName("Una arista de costo negativo falla con IllegalArgumentException")
    void rechazaAristaNegativa() {
        Grafo g = new Grafo(true);
        g.agregarArista("A", "B", 4);
        g.agregarArista("B", "C", -2);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> caminosMinimos.dijkstra(g, "A", "C"));
        assertTrue(ex.getMessage().toLowerCase().contains("negativo"));
    }

    @Test
    @DisplayName("Origen o destino inexistente lanza IllegalArgumentException")
    void nodoInexistenteFalla() {
        Grafo g = valdoria();
        assertThrows(IllegalArgumentException.class, () -> caminosMinimos.dijkstra(g, "NO_EXISTE", "VB1"));
        assertThrows(IllegalArgumentException.class, () -> caminosMinimos.dijkstra(g, "VB1", "NO_EXISTE"));
    }

    // --- Auxiliar --------------------------------------------------------------

    private double sumarAristasDelCamino(Grafo g, List<String> vertices) {
        double suma = 0.0;
        for (int i = 0; i < vertices.size() - 1; i++) {
            int u = g.indiceDe(vertices.get(i));
            int v = g.indiceDe(vertices.get(i + 1));
            double costoArista = Grafo.INFINITO;
            for (Grafo.Arista a : g.vecinos(u)) {
                if (a.destino() == v) {
                    costoArista = Math.min(costoArista, a.costo());
                }
            }
            assertNotEquals(Grafo.INFINITO, costoArista,
                    "No existe arista entre " + vertices.get(i) + " y " + vertices.get(i + 1));
            suma += costoArista;
        }
        return suma;
    }
}

