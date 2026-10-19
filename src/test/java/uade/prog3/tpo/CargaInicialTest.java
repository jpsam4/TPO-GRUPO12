package uade.prog3.tpo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uade.prog3.tpo.model.Item;
import uade.prog3.tpo.model.Nodo;
import uade.prog3.tpo.repository.AlmacenNeo4j;
import uade.prog3.tpo.seed.CargaInicial;
import uade.prog3.tpo.seed.DatosValdoria;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de los datos de Valdoria y de la carga inicial (Hito 4: el dominio
 * propio cargado reemplazando la semilla).
 *
 * La base se simula con un almacen en memoria, asi el test corre sin Neo4j.
 */
class CargaInicialTest {

    /** Base de mentira en memoria. Cuenta cuantas veces se borra y se guarda. */
    private static class BaseEnMemoria implements AlmacenNeo4j {
        final List<Nodo> nodos = new ArrayList<>();
        final List<Item> items = new ArrayList<>();
        int borrados = 0;
        int guardados = 0;

        public List<Nodo> todosLosNodos() { return nodos; }
        public long contarNodos() { return nodos.size(); }
        public void guardarNodos(List<Nodo> n) { nodos.addAll(n); guardados++; }
        public List<Item> todosLosItems() { return items; }
        public void guardarItems(List<Item> i) { items.addAll(i); }
        public void borrarTodo() { nodos.clear(); items.clear(); borrados++; }
        public String descripcionDelTransporte() { return "memoria"; }
    }

    /** La semilla del scaffold: nodos A..H. */
    private static void cargarSemillaDelScaffold(BaseEnMemoria base) {
        Nodo a = new Nodo("A", "Nodo A", "CENTRO", 0);
        Nodo b = new Nodo("B", "Nodo B", "PUNTO", 12);
        a.conectar(b, 4);
        base.nodos.addAll(List.of(a, b));
        base.items.add(new Item("I1", "Item 1", 5, 10, "A"));
    }

    private static Set<String> ids(List<Nodo> nodos) {
        Set<String> ids = new HashSet<>();
        for (Nodo n : nodos) {
            ids.add(n.getId());
        }
        return ids;
    }

    // --- Los datos ---------------------------------------------------------------

    @Test
    @DisplayName("Valdoria tiene 38 zonas, 57 caminos y 25 objetos que pesan 108 kg")
    void totales() {
        assertEquals(38, DatosValdoria.ZONAS.size());
        assertEquals(57, DatosValdoria.CAMINOS.size());
        assertEquals(25, DatosValdoria.OBJETOS.size());
        int pesoTotal = 0;
        for (DatosValdoria.Objeto o : DatosValdoria.OBJETOS) {
            pesoTotal += o.peso();
        }
        assertEquals(108, pesoTotal);
    }

    @Test
    @DisplayName("Ids unicos, costos positivos y cada camino y objeto apunta a una zona que existe")
    void datosConsistentes() {
        Set<String> zonas = new HashSet<>();
        for (DatosValdoria.Zona z : DatosValdoria.ZONAS) {
            assertTrue(zonas.add(z.id()), "zona repetida: " + z.id());
        }
        Set<String> caminos = new HashSet<>();
        for (DatosValdoria.Camino c : DatosValdoria.CAMINOS) {
            assertTrue(zonas.contains(c.desde()) && zonas.contains(c.hasta()), c.toString());
            assertTrue(c.costo() > 0, "costo no positivo: " + c);
            String clave = c.desde().compareTo(c.hasta()) < 0 ? c.desde() + c.hasta() : c.hasta() + c.desde();
            assertTrue(caminos.add(clave), "camino repetido: " + c);
        }
        Set<String> objetos = new HashSet<>();
        for (DatosValdoria.Objeto o : DatosValdoria.OBJETOS) {
            assertTrue(objetos.add(o.id()), "objeto repetido: " + o.id());
            assertTrue(zonas.contains(o.zona()), o.toString());
        }
    }

    @Test
    @DisplayName("Cada region tiene al menos 8 zonas y una fuente que cura 6")
    void regiones() {
        for (String codigo : List.of("VB", "CN", "DA", "CC")) {
            int zonas = 0;
            int fuentes = 0;
            for (DatosValdoria.Zona z : DatosValdoria.ZONAS) {
                if (z.id().startsWith(codigo)) {
                    zonas++;
                    if (z.curacion() == 6) {
                        fuentes++;
                    }
                }
            }
            assertTrue(zonas >= 8, codigo + " tiene " + zonas + " zonas");
            assertEquals(1, fuentes, codigo);
        }
    }

    // --- La carga ------------------------------------------------------------------

    @Test
    @DisplayName("Con la semilla A..H en la base, la borra y carga Valdoria")
    void reemplazaLaSemilla() {
        BaseEnMemoria base = new BaseEnMemoria();
        cargarSemillaDelScaffold(base);

        new CargaInicial(base).run();

        assertEquals(1, base.borrados);
        assertEquals(38, base.nodos.size());
        assertEquals(25, base.items.size());
        assertFalse(ids(base.nodos).contains("A"));
        assertTrue(ids(base.nodos).contains("VB1"));
    }

    @Test
    @DisplayName("Con la base vacia, carga Valdoria sin borrar nada")
    void baseVacia() {
        BaseEnMemoria base = new BaseEnMemoria();

        new CargaInicial(base).run();

        assertEquals(0, base.borrados);
        assertEquals(38, base.nodos.size());
    }

    @Test
    @DisplayName("Si la base ya tiene Valdoria, no la vuelve a escribir")
    void noRecargaSiYaEsta() {
        BaseEnMemoria base = new BaseEnMemoria();
        new CargaInicial(base).run();

        new CargaInicial(base).run();

        assertEquals(1, base.guardados);
        assertEquals(0, base.borrados);
    }

    @Test
    @DisplayName("Si en la base cambio un costo, la detecta y recarga")
    void recargaSiCambioUnCosto() {
        BaseEnMemoria base = new BaseEnMemoria();
        new CargaInicial(base).run();
        base.nodos.get(0).getConexiones().get(0).setCosto(99);

        new CargaInicial(base).run();

        assertEquals(1, base.borrados);
        assertEquals(2, base.guardados);
        assertEquals(3.0, base.nodos.get(0).getConexiones().get(0).getCosto());
    }
}
