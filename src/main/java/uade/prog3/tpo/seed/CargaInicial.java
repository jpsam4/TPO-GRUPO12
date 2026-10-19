package uade.prog3.tpo.seed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import uade.prog3.tpo.model.Conexion;
import uade.prog3.tpo.model.Item;
import uade.prog3.tpo.model.Nodo;
import uade.prog3.tpo.repository.AlmacenNeo4j;

import java.util.List;
import java.util.TreeSet;

/**
 * Carga el reino de Valdoria en Neo4j al arrancar la aplicacion.
 *
 * Reemplaza a la semilla generica del scaffold (nodos A..H): si la base tiene
 * cualquier otra cosa (la semilla, o una version vieja de nuestros datos), la
 * borra y carga Valdoria. Si ya tiene exactamente los datos de DatosValdoria,
 * no toca nada.
 *
 * Para comparar se arma una "firma" de la base y otra de DatosValdoria: todas
 * las zonas, todos los caminos (sin importar el sentido en que se guardaron) y
 * todos los objetos, ordenados. Si alguien cambia un costo en DatosValdoria,
 * la firma cambia y en el proximo arranque la base se actualiza sola.
 */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class CargaInicial implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CargaInicial.class);

    private final AlmacenNeo4j almacen;

    public CargaInicial(AlmacenNeo4j almacen) {
        this.almacen = almacen;
    }

    @Override
    public void run(String... args) {
        List<Nodo> nodosValdoria = DatosValdoria.nodos();
        List<Item> itemsValdoria = DatosValdoria.items();

        String esperada = firma(nodosValdoria, itemsValdoria);
        String actual = firma(almacen.todosLosNodos(), almacen.todosLosItems());
        if (esperada.equals(actual)) {
            log.info("La base ya tiene el reino de Valdoria. No se ejecuta la carga inicial.");
            return;
        }

        if (almacen.contarNodos() > 0) {
            log.info("La base tiene otros datos (la semilla del scaffold o una version vieja). Se borran.");
            almacen.borrarTodo();
        }

        almacen.guardarNodos(nodosValdoria);
        almacen.guardarItems(itemsValdoria);

        log.info("Carga inicial completada: Valdoria, {} zonas, {} caminos, {} objetos. Transporte: {}",
                nodosValdoria.size(), DatosValdoria.CAMINOS.size(), itemsValdoria.size(),
                almacen.descripcionDelTransporte());
    }

    /**
     * Descripcion ordenada de todo el contenido: una linea por zona, por camino
     * y por objeto. Los caminos se escriben con los extremos en orden
     * alfabetico, porque el grafo es no dirigido y da lo mismo hacia donde
     * apunta la relacion en la base.
     */
    static String firma(List<Nodo> nodos, List<Item> items) {
        TreeSet<String> lineas = new TreeSet<>();
        for (Nodo n : nodos) {
            lineas.add("Z|" + n.getId() + "|" + n.getNombre() + "|" + n.getTipo() + "|" + n.getValor());
            for (Conexion c : n.getConexiones()) {
                String a = n.getId();
                String b = c.getDestino().getId();
                String extremos = a.compareTo(b) <= 0 ? a + "|" + b : b + "|" + a;
                lineas.add("C|" + extremos + "|" + c.getCosto());
            }
        }
        for (Item i : items) {
            lineas.add("O|" + i.getId() + "|" + i.getNombre() + "|" + i.getPeso() + "|"
                    + i.getValor() + "|" + i.getNodoId());
        }
        return String.join("\n", lineas);
    }
}
