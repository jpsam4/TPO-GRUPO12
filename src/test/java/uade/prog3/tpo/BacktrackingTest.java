package uade.prog3.tpo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uade.prog3.tpo.algorithm.Backtracking;
import uade.prog3.tpo.algorithm.Backtracking.ComparacionPoda;
import uade.prog3.tpo.algorithm.Backtracking.Exploracion;
import uade.prog3.tpo.algorithm.Backtracking.Ruta;
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
 * Tests de Backtracking de rutas simples con poda por costo y saltos (Hito 8).
 *
 * Verifica:
 *   1) un grafo chico calculado a mano (retroceso y conteo exacto de nodos y podas)
 *   2) todas las rutas simples del Valle de Brezo (VB1 -> VB8: 13 rutas y 37 nodos sin poda)
 *   3) comparacion de nodos explorados con poda vs sin poda en una region, dos regiones y el reino entero
 *   4) invariantes de camino simple (sin vertices repetidos, aristas reales, respeta cotas)
 *   5) casos borde y validaciones de entrada
 */
class BacktrackingTest {

    private final Backtracking backtracking = new Backtracking();

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
     * Grafo chico para verificar a mano el retroceso y la poda:
     *
     *   A --(4)-- B --(3)-- C
     *   |                   |
     *   +-------(10)--------+
     */
    private Grafo triangulo() {
        Grafo g = new Grafo(false);
        g.agregarArista("A", "B", 4);
        g.agregarArista("B", "C", 3);
        g.agregarArista("A", "C", 10);
        g.ordenarVecinosPorId();
        return g;
    }

    // --- Grafo chico, a mano ---------------------------------------------------

    @Test
    @DisplayName("Triangulo a mano: poda la arista directa A->C (10 > 8) y encuentra A->B->C (7)")
    void trianguloPodaPorCosto() {
        ComparacionPoda cmp = backtracking.compararConYSinPoda(triangulo(), "A", "C", 8, 2);

        assertEquals(1, cmp.rutas().size());
        assertEquals(List.of("A", "B", "C"), cmp.rutas().get(0).vertices());
        assertEquals(7.0, cmp.rutas().get(0).costoTotal());
        assertTrue(cmp.mismasRutas());

        // Con poda: entra a A, B, C (3 nodos) y corta A->C antes de entrar (1 poda por costo).
        // Sin poda: entra tambien al estado C por A->C (4 nodos) y lo descarta al final.
        assertEquals(3, cmp.nodosExploradosConPoda());
        assertEquals(4, cmp.nodosExploradosSinPoda());
        assertEquals(1, cmp.podasPorCosto());
        assertEquals(0, cmp.podasPorSaltos());
    }

    @Test
    @DisplayName("El retroceso desmarca visitados: un vertice usado en una rama queda libre para la otra")
    void desmarcaVisitadosAlRetroceder() {
        // Sin restricciones ajustadas, B y C tienen que poder visitarse en ambos ordenes:
        //   Ruta 1: A -> B -> C (costo 7)
        //   Ruta 2: A -> C      (costo 10)
        // Si no se desmarcara C al volver de A->B->C, la rama A->C nunca se encontraria.
        List<Ruta> rutas = backtracking.rutasSimples(triangulo(), "A", "C", 20, 5);
        assertEquals(2, rutas.size());
        assertEquals(List.of("A", "B", "C"), rutas.get(0).vertices());
        assertEquals(List.of("A", "C"), rutas.get(1).vertices());
    }

    // --- Valle de Brezo (VB1 -> VB8) calculado a mano --------------------------

    @Test
    @DisplayName("Valle de Brezo sin restricciones apretadas: existen exactamente 13 caminos simples de VB1 a VB8 en 37 nodos")
    void valleDeBrezoTodasLasRutasSimples() {
        Exploracion sinPoda = backtracking.explorarSinPoda(valdoria(), "VB1", "VB8", 1000, 38);
        Exploracion conPodaAmplia = backtracking.explorar(valdoria(), "VB1", "VB8", 1000, 38);

        assertEquals(13, sinPoda.rutas().size());
        assertEquals(37, sinPoda.nodosExplorados());
        assertEquals(sinPoda.rutas(), conPodaAmplia.rutas());
    }

    @Test
    @DisplayName("Valle de Brezo con costoMaximo=22 y saltosMaximos=5: 3 rutas, 19 nodos con poda contra 37 sin poda")
    void valleDeBrezoConYSinPodaAMano() {
        Grafo g = valdoria();
        ComparacionPoda cmp = backtracking.compararConYSinPoda(g, "VB1", "VB8", 22, 5);

        assertTrue(cmp.mismasRutas());
        assertEquals(3, cmp.rutas().size());
        assertEquals(List.of("VB1", "VB2", "VB3", "VB4", "VB6", "VB8"), cmp.rutas().get(0).vertices());
        assertEquals(21.0, cmp.rutas().get(0).costoTotal());
        assertEquals(List.of("VB1", "VB2", "VB3", "VB5", "VB6", "VB8"), cmp.rutas().get(1).vertices());
        assertEquals(20.0, cmp.rutas().get(1).costoTotal());
        assertEquals(List.of("VB1", "VB2", "VB3", "VB5", "VB7", "VB8"), cmp.rutas().get(2).vertices());
        assertEquals(21.0, cmp.rutas().get(2).costoTotal());

        assertEquals(19, cmp.nodosExploradosConPoda());
        assertEquals(37, cmp.nodosExploradosSinPoda());
        assertEquals(7, cmp.podasPorCosto());
        assertEquals(4, cmp.podasPorSaltos());

        for (Ruta r : cmp.rutas()) {
            verificarRutaSimpleValida(g, r, "VB1", "VB8", 22, 5);
        }
    }

    // --- Crecimiento exponencial y efecto de la poda en Valdoria ---------------

    @Test
    @DisplayName("Dos regiones (VB1 -> CN9) y reino completo (VB1 -> CC11): la poda ahorra mas del 93% de los estados sin perder rutas")
    void ahorroExponencialEnElReino() {
        Grafo g = valdoria();

        // Dos regiones: camino minimo VB1->CN9 cuesta 48 en 10 saltos.
        ComparacionPoda dosRegiones = backtracking.compararConYSinPoda(g, "VB1", "CN9", 50, 11);
        assertTrue(dosRegiones.mismasRutas());
        assertEquals(6, dosRegiones.rutas().size());
        assertEquals(254, dosRegiones.nodosExploradosConPoda());
        assertEquals(487, dosRegiones.nodosExploradosSinPoda());

        // Reino completo (38 zonas): camino minimo VB1->CC11 cuesta 103 en 21 saltos.
        // Con costoMaximo=105 y saltosMaximos=22 baja de 485.647 estados a 31.327.
        ComparacionPoda reino = backtracking.compararConYSinPoda(g, "VB1", "CC11", 105, 22);
        assertTrue(reino.mismasRutas());
        assertEquals(34, reino.rutas().size());
        assertEquals(31_327, reino.nodosExploradosConPoda());
        assertEquals(485_647, reino.nodosExploradosSinPoda());
        assertEquals(27_590, reino.podasPorCosto());
        assertEquals(2_193, reino.podasPorSaltos());

        for (Ruta r : reino.rutas()) {
            verificarRutaSimpleValida(g, r, "VB1", "CC11", 105, 22);
        }
    }

    @Test
    @DisplayName("Si el presupuesto es menor al camino minimo de Dijkstra (103), devuelve 0 rutas podando temprano")
    void presupuestoMenorAlCaminoMinimoDaVacio() {
        Exploracion exp = backtracking.explorar(valdoria(), "VB1", "CC11", 102, 30);
        assertTrue(exp.rutas().isEmpty());
        assertTrue(exp.podasPorCosto() > 0);
    }

    // --- Casos borde y validaciones --------------------------------------------

    @Test
    @DisplayName("Origen igual a destino devuelve una unica ruta de costo 0 y 0 saltos")
    void origenIgualADestino() {
        List<Ruta> rutas = backtracking.rutasSimples(valdoria(), "VB1", "VB1", 0, 0);
        assertEquals(1, rutas.size());
        assertEquals(List.of("VB1"), rutas.get(0).vertices());
        assertEquals(0.0, rutas.get(0).costoTotal());
    }

    @Test
    @DisplayName("Si el destino esta en otra componente conexa, devuelve lista vacia")
    void destinoInalcanzable() {
        Grafo g = new Grafo(false);
        g.agregarArista("A", "B", 3);
        g.agregarVertice("ISLA");

        assertTrue(backtracking.rutasSimples(g, "A", "ISLA", 100, 10).isEmpty());
    }

    @Test
    @DisplayName("Nodos inexistentes, topes negativos o aristas negativas lanzan IllegalArgumentException")
    void validacionesDeEntrada() {
        Grafo g = valdoria();
        assertThrows(IllegalArgumentException.class, () -> backtracking.rutasSimples(g, "NO_EXISTE", "VB8", 30, 5));
        assertThrows(IllegalArgumentException.class, () -> backtracking.rutasSimples(g, "VB1", "NO_EXISTE", 30, 5));
        assertThrows(IllegalArgumentException.class, () -> backtracking.rutasSimples(g, "VB1", "VB8", -1, 5));
        assertThrows(IllegalArgumentException.class, () -> backtracking.rutasSimples(g, "VB1", "VB8", 30, -1));

        Grafo conNegativa = new Grafo(true);
        conNegativa.agregarArista("A", "B", -3);
        assertThrows(IllegalArgumentException.class, () -> backtracking.rutasSimples(conNegativa, "A", "B", 10, 5));
    }

    // --- Verificador auxiliar --------------------------------------------------

    private void verificarRutaSimpleValida(Grafo g, Ruta r, String origen, String destino,
                                           double costoMaximo, int saltosMaximos) {
        List<String> v = r.vertices();
        assertEquals(origen, v.get(0));
        assertEquals(destino, v.get(v.size() - 1));
        assertEquals(v.size(), new HashSet<>(v).size(), "La ruta no es simple (repite vertices): " + v);
        assertTrue(v.size() - 1 <= saltosMaximos, "Excede saltosMaximos: " + v);
        assertTrue(r.costoTotal() <= costoMaximo, "Excede costoMaximo: " + r);

        double suma = 0.0;
        for (int i = 0; i < v.size() - 1; i++) {
            int desde = g.indiceDe(v.get(i));
            int hasta = g.indiceDe(v.get(i + 1));
            double costoArista = Grafo.INFINITO;
            for (Grafo.Arista a : g.vecinos(desde)) {
                if (a.destino() == hasta) {
                    costoArista = Math.min(costoArista, a.costo());
                }
            }
            assertNotEquals(Grafo.INFINITO, costoArista, "Arista inexistente: " + v.get(i) + "->" + v.get(i + 1));
            suma += costoArista;
        }
        assertEquals(suma, r.costoTotal(), 1e-9);
    }
}
