package uade.prog3.tpo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uade.prog3.tpo.model.Conexion;
import uade.prog3.tpo.model.Nodo;
import uade.prog3.tpo.repository.AlmacenNeo4j;

import java.util.List;

/**
 * Carga el grafo desde Neo4j a memoria.
 *
 * INFRAESTRUCTURA: no forma parte de lo que hay que implementar.
 *
 * Decision de diseno importante para el trabajo: los algoritmos NO consultan
 * la base. Se carga el grafo una vez, se opera en memoria, y se devuelve el
 * resultado. Asi la complejidad temporal que se mide y se justifica en el
 * coloquio es la del algoritmo, no la de la red ni la del motor de base.
 */
@Service
public class GrafoService {

    private static final Logger log = LoggerFactory.getLogger(GrafoService.class);

    private final AlmacenNeo4j almacen;

    public GrafoService(AlmacenNeo4j almacen) {
        this.almacen = almacen;
    }

    /**
     * @param dirigido true si las relaciones CONECTA tienen sentido unico.
     *                 Para el arbol de recubrimiento minimo (Prim y Kruskal)
     *                 el grafo DEBE cargarse como NO dirigido.
     */
    public Grafo cargar(boolean dirigido) {
        Grafo grafo = new Grafo(dirigido);
        List<Nodo> nodos = almacen.todosLosNodos();

        // 1) primero todos los vertices, para que queden en el grafo
        //    incluso los que no tienen ninguna arista
        for (Nodo n : nodos) {
            grafo.agregarVertice(n.getId());
        }

        // 2) despues las aristas, con su costo
        for (Nodo n : nodos) {
            for (Conexion c : n.getConexiones()) {
                grafo.agregarArista(n.getId(), c.getDestino().getId(), c.getCosto());
            }
        }

        // 3) vecinos en orden fijo, para que los recorridos sean reproducibles
        grafo.ordenarVecinosPorId();

        log.info("Grafo cargado: {} vertices, {} aristas, dirigido={}",
                grafo.cantidadVertices(), grafo.cantidadAristas(), dirigido);
        return grafo;
    }

    /**
     * Grafo de COSTO NETO, para Floyd-Warshall: recorrer el camino u -> v
     * cuesta su energia menos lo que cura la zona v al llegar (Nodo.valor;
     * las fuentes curan 6). Ejemplo: de la Aldea Brezo a la Fuente Clara el
     * camino cuesta 3, pero la fuente cura 6, asi que el neto es 3 - 6 = -3.
     *
     * El grafo queda DIRIGIDO aunque los caminos sean de ida y vuelta, porque
     * el neto depende de hacia donde se va: VB1 -> VB2 cuesta -3 y VB2 -> VB1
     * cuesta 3. Por eso cada camino se carga como dos aristas.
     *
     * @param curacionFuentes si no es null, reemplaza lo que cura cada zona que
     *                        cura algo. Sirve para mostrar un ciclo negativo:
     *                        con 7, ir y volver entre VB1 y VB2 cuesta 3-7 + 3 = -1.
     */
    public Grafo cargarConCuracion(Double curacionFuentes) {
        Grafo grafo = new Grafo(true);
        List<Nodo> nodos = almacen.todosLosNodos();
        for (Nodo n : nodos) {
            grafo.agregarVertice(n.getId());
        }
        for (Nodo n : nodos) {
            for (Conexion c : n.getConexiones()) {
                Nodo d = c.getDestino();
                grafo.agregarArista(n.getId(), d.getId(), c.getCosto() - curacion(d, curacionFuentes));
                grafo.agregarArista(d.getId(), n.getId(), c.getCosto() - curacion(n, curacionFuentes));
            }
        }
        grafo.ordenarVecinosPorId();
        log.info("Grafo de costo neto cargado: {} vertices, {} aristas dirigidas",
                grafo.cantidadVertices(), grafo.cantidadAristas());
        return grafo;
    }

    private static double curacion(Nodo n, Double curacionFuentes) {
        if (curacionFuentes != null && n.getValor() > 0) {
            return curacionFuentes;
        }
        return n.getValor();
    }
}
