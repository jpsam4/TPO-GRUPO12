package uade.prog3.tpo.repository;

import uade.prog3.tpo.model.Item;
import uade.prog3.tpo.model.Nodo;

import java.util.List;

/**
 * Todo lo que la aplicacion necesita de la base, en cinco operaciones.
 *
 * Existe para que el resto del codigo no dependa de COMO se llega a Neo4j.
 * Hay dos implementaciones y se elige sola al arrancar (ver TransporteNeo4j):
 *
 *   AlmacenBolt  -> Spring Data Neo4j sobre Bolt (puerto 7687). Es el camino
 *                   normal y el que se usa fuera de la facultad y en Render.
 *   AlmacenHttp  -> Query API sobre HTTPS (puerto 443). Es el camino que
 *                   funciona desde la red de UADE, que bloquea el 7687.
 *
 * La lista es tan corta a proposito: los algoritmos NO consultan la base, el
 * grafo se carga una vez a memoria. Por eso cambiar de transporte no toca
 * nada de lo que se evalua.
 */
public interface AlmacenNeo4j {

    /** Todos los nodos, con sus conexiones salientes ya cargadas. */
    List<Nodo> todosLosNodos();

    /** Cuantos nodos hay. Lo usa la carga inicial para no duplicar datos. */
    long contarNodos();

    /** Guarda los nodos y sus conexiones. */
    void guardarNodos(List<Nodo> nodos);

    /** Todos los items de seleccion. */
    List<Item> todosLosItems();

    void guardarItems(List<Item> items);

    /** Como se esta hablando con la base. Sale en el log y en /api/grafo/resumen. */
    String descripcionDelTransporte();
}
