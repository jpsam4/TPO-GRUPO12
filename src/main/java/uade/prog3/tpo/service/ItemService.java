package uade.prog3.tpo.service;

import org.springframework.stereotype.Service;
import uade.prog3.tpo.model.Item;
import uade.prog3.tpo.repository.AlmacenNeo4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Carga los objetos del reino desde Neo4j a memoria.
 *
 * INFRAESTRUCTURA, igual que GrafoService: los algoritmos de seleccion,
 * ordenamiento y reparto reciben la lista ya cargada y nunca consultan la base.
 * Asi el controller solo traduce HTTP y delega; no tiene bucles propios.
 */
@Service
public class ItemService {

    /** Codigos de region de Valdoria (prefijo del id de cada zona). */
    public static final Set<String> REGIONES = Set.of("VB", "CN", "DA", "CC");

    private final AlmacenNeo4j almacen;

    public ItemService(AlmacenNeo4j almacen) {
        this.almacen = almacen;
    }

    /**
     * Los 25 objetos del reino, en el orden del reino (ver {@link OrdenDelReino}).
     * Una sola consulta a la base.
     */
    public List<Item> todos() {
        return OrdenDelReino.objetos(almacen.todosLosItems());
    }

    /**
     * Los objetos de una region (VB, CN, DA o CC), o todos los del reino si no
     * se indica. Es un filtro de datos O(n), no parte de ningun algoritmo.
     *
     * @throws IllegalArgumentException si la region no existe (el endpoint responde 400)
     */
    public List<Item> deLaRegion(String region) {
        List<Item> items = todos();
        if (region == null || region.isBlank()) {
            return items;
        }
        String codigo = region.trim().toUpperCase();
        if (!REGIONES.contains(codigo)) {
            throw new IllegalArgumentException("Region invalida: " + region + ". Use VB, CN, DA o CC.");
        }
        List<Item> filtrados = new ArrayList<>();
        for (Item i : items) {
            if (i.getNodoId() != null && i.getNodoId().startsWith(codigo)) {
                filtrados.add(i);
            }
        }
        return filtrados;
    }
}
