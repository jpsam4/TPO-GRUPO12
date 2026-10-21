package uade.prog3.tpo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uade.prog3.tpo.algorithm.ArbolRecubrimiento;
import uade.prog3.tpo.algorithm.ArbolRecubrimiento.AristaMst;
import uade.prog3.tpo.algorithm.ArbolRecubrimiento.Mst;
import uade.prog3.tpo.model.Item;
import uade.prog3.tpo.model.Nodo;
import uade.prog3.tpo.repository.AlmacenNeo4j;
import uade.prog3.tpo.seed.DatosValdoria;
import uade.prog3.tpo.service.Grafo;
import uade.prog3.tpo.service.GrafoService;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de Prim y Kruskal con Union-Find (Hito 5: arbol de recubrimiento minimo).
 *
 * Verifica los resultados sobre:
 *   1) el grafo de ejemplo de GUIA-TPO.md (8 vertices, costo 27, 7 aristas)
 *   2) la Region 1 de Valdoria calculada a mano (Valle de Brezo: 8 zonas, costo 25, 7 aristas)
 *   3) el reino completo de Valdoria (38 zonas, costo 161, 37 aristas)
 */
class ArbolRecubrimientoTest {

    private final ArbolRecubrimiento arbol = new ArbolRecubrimiento();

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
     *   MST de 7 aristas: C-D(2), B-C(3), F-H(3), A-B(4), G-H(4), D-E(5), E-F(6) -> costo 27.
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

    /** Subgrafo de la Region 1 (Valle de Brezo: VB1..VB8, 8 zonas y 11 caminos). */
    private Grafo valleDeBrezo() {
        Grafo g = new Grafo(false);
        for (DatosValdoria.Camino c : DatosValdoria.CAMINOS) {
            if (c.desde().startsWith("VB") && c.hasta().startsWith("VB")) {
                g.agregarArista(c.desde(), c.hasta(), c.costo());
            }
        }
        g.ordenarVecinosPorId();
        return g;
    }

    // --- Verificacion contra GUIA-TPO.md ---------------------------------------

    @Test
    @DisplayName("Guia TPO: Prim y Kruskal sobre A..H dan 7 aristas y costo total 27")
    void guiaMstCosto27() {
        Grafo g = grafoGuia();
        Mst prim = arbol.prim(g, "A");
        Mst kruskal = arbol.kruskal(g);

        assertEquals(7, prim.aristas().size());
        assertEquals(27.0, prim.costoTotal());
        assertTrue(esArbolGeneradorValido(g, prim));

        assertEquals(7, kruskal.aristas().size());
        assertEquals(27.0, kruskal.costoTotal());
        assertTrue(esArbolGeneradorValido(g, kruskal));

        Set<String> esperadas = Set.of(
                par("C", "D", 2), par("B", "C", 3), par("F", "H", 3),
                par("A", "B", 4), par("G", "H", 4), par("D", "E", 5), par("E", "F", 6)
        );
        assertEquals(esperadas, normalizar(prim.aristas()));
        assertEquals(esperadas, normalizar(kruskal.aristas()));
    }

    // --- Verificacion a mano sobre Valle de Brezo ------------------------------

    @Test
    @DisplayName("Valle de Brezo a mano: Prim y Kruskal eligen las mismas 7 aristas y suman 25")
    void valleDeBrezoAMano() {
        // Calculo a mano sobre VB1..VB8 (8 zonas, 11 caminos):
        //   Costo 3: VB1-VB2(3), VB3-VB5(3), VB6-VB7(3)          -> subtotal 9
        //   Costo 4: VB2-VB3(4), VB4-VB5(4), VB4-VB6(4), VB6-VB8(4) -> subtotal 16
        //   Descartadas por formar ciclo: VB5-VB7(5), VB3-VB4(6), VB5-VB6(6), VB7-VB8(6)
        //   Total = 7 aristas, costo 25.
        Grafo vb = valleDeBrezo();
        Mst prim = arbol.prim(vb, "VB1");
        Mst kruskal = arbol.kruskal(vb);

        Set<String> esperadas = Set.of(
                par("VB1", "VB2", 3),
                par("VB3", "VB5", 3),
                par("VB6", "VB7", 3),
                par("VB2", "VB3", 4),
                par("VB4", "VB5", 4),
                par("VB4", "VB6", 4),
                par("VB6", "VB8", 4)
        );

        assertEquals(7, prim.aristas().size());
        assertEquals(25.0, prim.costoTotal());
        assertEquals(esperadas, normalizar(prim.aristas()));

        assertEquals(7, kruskal.aristas().size());
        assertEquals(25.0, kruskal.costoTotal());
        assertEquals(esperadas, normalizar(kruskal.aristas()));
    }

    // --- Reino completo de Valdoria (38 zonas, 57 caminos) ---------------------

    @Test
    @DisplayName("Valdoria completo: Prim y Kruskal dan 37 aristas (V-1) y costo total 161")
    void valdoriaMstCompleto() {
        // Por regiones (unidas por los 3 pasos puente de costo 5):
        //   MST(VB) = 25 (7 aristas)
        //   MST(CN) = 41 (8 aristas)
        //   MST(DA) = 32 (9 aristas)
        //   MST(CC) = 48 (10 aristas)
        //   3 pasos = 15 (VB8-CN1, CN9-DA1, DA10-CC1)
        //   Total   = 25 + 41 + 32 + 48 + 15 = 161 (37 aristas).
        Grafo g = valdoria();
        Mst prim = arbol.prim(g, "VB1");
        Mst kruskal = arbol.kruskal(g);

        assertEquals(37, prim.aristas().size());
        assertEquals(161.0, prim.costoTotal());
        assertTrue(esArbolGeneradorValido(g, prim));

        assertEquals(37, kruskal.aristas().size());
        assertEquals(161.0, kruskal.costoTotal());
        assertTrue(esArbolGeneradorValido(g, kruskal));

        // En Valdoria el MST es unico: ambos eligen exactamente los mismos 37 caminos.
        assertEquals(normalizar(prim.aristas()), normalizar(kruskal.aristas()));
    }

    @Test
    @DisplayName("Prim da costo 161 y 37 aristas sin importar desde cual de las 38 zonas arranque")
    void primDesdeCualquierZonaDaLoMismo() {
        Grafo g = valdoria();
        for (String origen : g.ids()) {
            Mst mst = arbol.prim(g, origen);
            assertEquals(37, mst.aristas().size(), "Cantidad de aristas desde " + origen);
            assertEquals(161.0, mst.costoTotal(), "Costo total desde " + origen);
        }
    }

    // --- Casos borde y validaciones --------------------------------------------

    @Test
    @DisplayName("Si el grafo no es conexo, Prim y Kruskal fallan con IllegalArgumentException")
    void grafoNoConexoFalla() {
        Grafo g = new Grafo(false);
        g.agregarArista("A", "B", 3);
        g.agregarArista("C", "D", 4);

        assertThrows(IllegalArgumentException.class, () -> arbol.prim(g, "A"));
        assertThrows(IllegalArgumentException.class, () -> arbol.kruskal(g));
    }

    @Test
    @DisplayName("Si el grafo es dirigido, Prim y Kruskal fallan con IllegalArgumentException")
    void grafoDirigidoFalla() {
        Grafo g = new Grafo(true);
        g.agregarArista("A", "B", 3);
        g.agregarArista("B", "A", 3);

        assertThrows(IllegalArgumentException.class, () -> arbol.prim(g, "A"));
        assertThrows(IllegalArgumentException.class, () -> arbol.kruskal(g));
    }

    @Test
    @DisplayName("Un origen inexistente en Prim lanza IllegalArgumentException")
    void primOrigenInexistenteFalla() {
        assertThrows(IllegalArgumentException.class, () -> arbol.prim(valdoria(), "NO_EXISTE"));
    }

    // --- Verificadores auxiliares ----------------------------------------------

    /** Normaliza cada arista con sus extremos en orden alfabetico para comparar conjuntos. */
    private Set<String> normalizar(List<AristaMst> aristas) {
        Set<String> conjunto = new HashSet<>();
        for (AristaMst a : aristas) {
            conjunto.add(par(a.origen(), a.destino(), a.costo()));
        }
        return conjunto;
    }

    private String par(String u, String v, double costo) {
        return (u.compareTo(v) <= 0 ? u + "-" + v : v + "-" + u) + ":" + costo;
    }

    /**
     * Comprueba que el resultado sea efectivamente un arbol generador del grafo:
     * V-1 aristas, la suma de sus costos coincide con costoTotal y conecta los
     * V vertices en una sola componente conexa.
     */
    private boolean esArbolGeneradorValido(Grafo g, Mst mst) {
        int n = g.cantidadVertices();
        if (mst.aristas().size() != n - 1) {
            return false;
        }
        Map<String, Set<String>> adj = new HashMap<>();
        for (String id : g.ids()) {
            adj.put(id, new HashSet<>());
        }
        double suma = 0.0;
        for (AristaMst a : mst.aristas()) {
            adj.get(a.origen()).add(a.destino());
            adj.get(a.destino()).add(a.origen());
            suma += a.costo();
        }
        if (Double.compare(suma, mst.costoTotal()) != 0) {
            return false;
        }

        // BFS sobre las aristas del MST: con V-1 aristas, ser conexo equivale a ser arbol.
        Set<String> visitados = new HashSet<>();
        ArrayDeque<String> cola = new ArrayDeque<>();
        String inicio = g.ids().get(0);
        visitados.add(inicio);
        cola.addLast(inicio);
        while (!cola.isEmpty()) {
            String u = cola.pollFirst();
            for (String v : adj.get(u)) {
                if (visitados.add(v)) {
                    cola.addLast(v);
                }
            }
        }
        return visitados.size() == n;
    }
}

