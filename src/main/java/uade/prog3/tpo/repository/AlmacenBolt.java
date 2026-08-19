package uade.prog3.tpo.repository;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import uade.prog3.tpo.model.Item;
import uade.prog3.tpo.model.Nodo;

import java.util.List;

/**
 * Implementacion normal: delega en los repositorios de Spring Data Neo4j,
 * que hablan Bolt por el puerto 7687.
 *
 * Es la que se usa siempre que el 7687 este accesible: fuera de la facultad,
 * contra un Neo4j local, y en el despliegue de Render.
 */
@Component
@Profile("bolt")
public class AlmacenBolt implements AlmacenNeo4j {

    private final NodoRepository nodoRepository;
    private final ItemRepository itemRepository;

    public AlmacenBolt(NodoRepository nodoRepository, ItemRepository itemRepository) {
        this.nodoRepository = nodoRepository;
        this.itemRepository = itemRepository;
    }

    @Override
    public List<Nodo> todosLosNodos() {
        return nodoRepository.findAll();
    }

    @Override
    public long contarNodos() {
        return nodoRepository.count();
    }

    @Override
    public void guardarNodos(List<Nodo> nodos) {
        nodoRepository.saveAll(nodos);
    }

    @Override
    public List<Item> todosLosItems() {
        return itemRepository.findAll();
    }

    @Override
    public void guardarItems(List<Item> items) {
        itemRepository.saveAll(items);
    }

    @Override
    public String descripcionDelTransporte() {
        return "Bolt (Spring Data Neo4j, puerto 7687)";
    }
}
