package uade.prog3.tpo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uade.prog3.tpo.algorithm.ArbolRecubrimiento;
import uade.prog3.tpo.algorithm.CaminosMinimos;
import uade.prog3.tpo.model.Item;
import uade.prog3.tpo.model.Nodo;
import uade.prog3.tpo.repository.AlmacenNeo4j;
import uade.prog3.tpo.seed.DatosValdoria;
import uade.prog3.tpo.service.Grafo;
import uade.prog3.tpo.service.GrafoService;
import uade.prog3.tpo.service.ItemService;
import uade.prog3.tpo.service.OrdenDelReino;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * El resultado de los endpoints no puede depender del orden en que Neo4j
 * devuelve las filas: Bolt (Render) y la Query API (facultad) no ordenan igual.
 *
 * Se simula una base que devuelve todo al reves y se verifica que el grafo
 * cargado, el listado de Kruskal y los contadores de Floyd/Dijkstra son
 * identicos a los de DatosValdoria, que son los que figuran en ENDPOINTS.md.
 */
class OrdenDelReinoTest {

    /** Base que devuelve las zonas, sus caminos y los objetos en orden inverso. */
    private static class BaseDesordenada implements AlmacenNeo4j {
        public List<Nodo> todosLosNodos() {
            List<Nodo> nodos = DatosValdoria.nodos();
            List<Nodo> alReves = new ArrayList<>();
            for (int i = nodos.size() - 1; i >= 0; i--) {
                Nodo n = nodos.get(i);
                List<uade.prog3.tpo.model.Conexion> conexiones = new ArrayList<>(n.getConexiones());
                List<uade.prog3.tpo.model.Conexion> invertidas = new ArrayList<>();
                for (int j = conexiones.size() - 1; j >= 0; j--) {
                    invertidas.add(conexiones.get(j));
                }
                n.setConexiones(invertidas);
                alReves.add(n);
            }
            return alReves;
        }
        public long contarNodos() { return DatosValdoria.ZONAS.size(); }
        public void guardarNodos(List<Nodo> nodos) { }
        public List<Item> todosLosItems() {
            List<Item> items = DatosValdoria.items();
            List<Item> alReves = new ArrayList<>();
            for (int i = items.size() - 1; i >= 0; i--) {
                alReves.add(items.get(i));
            }
            return alReves;
        }
        public void guardarItems(List<Item> items) { }
        public void borrarTodo() { }
        public String descripcionDelTransporte() { return "memoria desordenada"; }
    }

    private static class BaseOrdenada extends BaseDesordenada {
        @Override public List<Nodo> todosLosNodos() { return DatosValdoria.nodos(); }
        @Override public List<Item> todosLosItems() { return DatosValdoria.items(); }
    }

    @Test
    @DisplayName("Las zonas se cargan en el orden del reino aunque la base las devuelva al reves")
    void zonasEnOrdenDelReino() {
        Grafo g = new GrafoService(new BaseDesordenada()).cargar(false);
        List<String> esperado = new ArrayList<>();
        for (DatosValdoria.Zona z : DatosValdoria.ZONAS) {
            esperado.add(z.id());
        }
        assertEquals(esperado, g.ids());
    }

    @Test
    @DisplayName("Los objetos se devuelven en el orden del reino aunque la base los devuelva al reves")
    void objetosEnOrdenDelReino() {
        List<Item> items = new ItemService(new BaseDesordenada()).todos();
        for (int i = 0; i < items.size(); i++) {
            assertEquals(DatosValdoria.OBJETOS.get(i).id(), items.get(i).getId());
        }
    }

    @Test
    @DisplayName("Kruskal y los contadores de Floyd contra Dijkstra no cambian con el orden de la base")
    void resultadosIndependientesDelOrden() {
        Grafo ordenado = new GrafoService(new BaseOrdenada()).cargar(false);
        Grafo desordenado = new GrafoService(new BaseDesordenada()).cargar(false);

        ArbolRecubrimiento mst = new ArbolRecubrimiento();
        assertEquals(mst.kruskal(ordenado).aristas(), mst.kruskal(desordenado).aristas());

        CaminosMinimos cm = new CaminosMinimos();
        assertEquals(cm.compararFloydConDijkstra(ordenado), cm.compararFloydConDijkstra(desordenado));
    }

    @Test
    @DisplayName("VB2 va antes que VB10, y los ids que no son del reino van al final")
    void ordenNumericoYSemillaAlFinal() {
        List<Nodo> nodos = new ArrayList<>();
        for (String id : List.of("B", "VB10", "CC1", "A", "VB2", "DA3")) {
            nodos.add(new Nodo(id, id, "X", 0));
        }
        List<String> ids = new ArrayList<>();
        for (Nodo n : OrdenDelReino.zonas(nodos)) {
            ids.add(n.getId());
        }
        assertEquals(List.of("VB2", "VB10", "DA3", "CC1", "A", "B"), ids);
    }
}
