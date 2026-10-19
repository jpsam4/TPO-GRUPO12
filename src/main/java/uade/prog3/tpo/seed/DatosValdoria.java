package uade.prog3.tpo.seed;

import uade.prog3.tpo.model.Item;
import uade.prog3.tpo.model.Nodo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Los datos del reino de Valdoria: las cuatro regiones fijas del README.
 *
 * Es la UNICA fuente de los datos del dominio. La usan la carga inicial (que
 * los escribe en Neo4j) y los tests (que arman el grafo sin base), asi que lo
 * que se verifica en los tests es exactamente lo que esta cargado.
 *
 *   Nodo  -> una zona del reino. valor = energia que cura al entrar (las fuentes curan 6).
 *   Arista -> un camino de ida y vuelta. costo = puntos de energia que gasta recorrerlo.
 *   Item  -> un objeto para la mochila. peso en kg, valor en puntos de poder.
 *
 * Cada camino se guarda UNA sola vez (relacion CONECTA de "desde" a "hasta").
 * El grafo es no dirigido: el sentido contrario lo agrega Grafo al cargarlo
 * con dirigido=false.
 *
 * Totales: 38 zonas, 57 caminos (incluidos los 3 pasos entre regiones), 25 objetos.
 */
public final class DatosValdoria {

    private DatosValdoria() {
    }

    /** Una zona del reino. curacion = energia que devuelve al entrar. */
    public record Zona(String id, String nombre, String tipo, int curacion) { }

    /** Un camino de ida y vuelta entre dos zonas. costo en puntos de energia. */
    public record Camino(String desde, String hasta, int costo) { }

    /** Un objeto que se puede cargar en la mochila. peso en kg, poder en puntos. */
    public record Objeto(String id, String nombre, int peso, int poder, String zona) { }

    public static final List<Zona> ZONAS = List.of(
            // --- Valle de Brezo (VB) ---
            new Zona("VB1", "Aldea Brezo", "ALDEA", 0),
            new Zona("VB2", "Fuente Clara", "FUENTE", 6),
            new Zona("VB3", "Santuario del Valle", "SANTUARIO", 0),
            new Zona("VB4", "Puerto Sal", "PUERTO", 0),
            new Zona("VB5", "Molino Viejo", "MOLINO", 0),
            new Zona("VB6", "Torre Vigía", "TORRE", 0),
            new Zona("VB7", "Castillo Alto", "CASTILLO", 0),
            new Zona("VB8", "Cubil de Musgo", "GUARIDA", 0),
            // --- Ciénagas de Niebla (CN) ---
            new Zona("CN1", "Campamento de la Ciénaga", "CAMPAMENTO", 0),
            new Zona("CN2", "Santuario de la Niebla", "SANTUARIO", 0),
            new Zona("CN3", "Charca Azul", "FUENTE", 6),
            new Zona("CN4", "Choza de Barro", "CABANA", 0),
            new Zona("CN5", "Ruinas Hundidas", "RUINAS", 0),
            new Zona("CN6", "Atalaya", "TORRE", 0),
            new Zona("CN7", "Roble Antiguo", "ARBOLEDA", 0),
            new Zona("CN8", "Palafito", "CABANA", 0),
            new Zona("CN9", "Gruta de Ciénaga", "GUARIDA", 0),
            // --- Dunas de Ámbar (DA) ---
            new Zona("DA1", "Vivac del Viajero", "CAMPAMENTO", 0),
            new Zona("DA2", "Obelisco", "OBELISCO", 0),
            new Zona("DA3", "Oasis", "FUENTE", 6),
            new Zona("DA4", "Bastión", "CASTILLO", 0),
            new Zona("DA5", "Santuario de Ámbar", "SANTUARIO", 0),
            new Zona("DA6", "Arcos Rotos", "RUINAS", 0),
            new Zona("DA7", "Minas Hondas", "MINAS", 0),
            new Zona("DA8", "Torre Rota", "TORRE", 0),
            new Zona("DA9", "Aguja de Piedra", "OBELISCO", 0),
            new Zona("DA10", "Cueva de Duna", "GUARIDA", 0),
            // --- Cumbres de Ceniza (CC) ---
            new Zona("CC1", "Fogón", "CAMPAMENTO", 0),
            new Zona("CC2", "Templo Caído", "RUINAS", 0),
            new Zona("CC3", "Muralla Vieja", "RUINAS", 0),
            new Zona("CC4", "Monolito", "OBELISCO", 0),
            new Zona("CC5", "Santuario de Ceniza", "SANTUARIO", 0),
            new Zona("CC6", "Cráter Rojo", "VOLCAN", 0),
            new Zona("CC7", "Cantera", "MINAS", 0),
            new Zona("CC8", "Termas", "FUENTE", 6),
            new Zona("CC9", "Torre del Mago", "TORRE", 0),
            new Zona("CC10", "Socavón", "MINAS", 0),
            new Zona("CC11", "Guarida de Ignea", "GUARIDA", 0)
    );

    public static final List<Camino> CAMINOS = List.of(
            // --- Valle de Brezo (VB) ---
            new Camino("VB1", "VB2", 3),
            new Camino("VB2", "VB3", 4),
            new Camino("VB3", "VB4", 6),
            new Camino("VB3", "VB5", 3),
            new Camino("VB4", "VB5", 4),
            new Camino("VB4", "VB6", 4),
            new Camino("VB5", "VB6", 6),
            new Camino("VB5", "VB7", 5),
            new Camino("VB6", "VB7", 3),
            new Camino("VB6", "VB8", 4),
            new Camino("VB7", "VB8", 6),
            // --- Ciénagas de Niebla (CN) ---
            new Camino("CN1", "CN3", 5),
            new Camino("CN2", "CN3", 4),
            new Camino("CN2", "CN5", 6),
            new Camino("CN3", "CN4", 5),
            new Camino("CN3", "CN5", 6),
            new Camino("CN3", "CN6", 6),
            new Camino("CN4", "CN6", 5),
            new Camino("CN5", "CN6", 5),
            new Camino("CN5", "CN8", 9),
            new Camino("CN6", "CN7", 4),
            new Camino("CN7", "CN9", 8),
            new Camino("CN8", "CN9", 5),
            // --- Dunas de Ámbar (DA) ---
            new Camino("DA1", "DA2", 5),
            new Camino("DA1", "DA3", 4),
            new Camino("DA2", "DA4", 4),
            new Camino("DA3", "DA5", 4),
            new Camino("DA4", "DA5", 3),
            new Camino("DA4", "DA7", 9),
            new Camino("DA5", "DA6", 3),
            new Camino("DA6", "DA7", 3),
            new Camino("DA6", "DA9", 5),
            new Camino("DA7", "DA8", 3),
            new Camino("DA7", "DA9", 4),
            new Camino("DA7", "DA10", 6),
            new Camino("DA8", "DA10", 5),
            new Camino("DA9", "DA10", 4),
            // --- Cumbres de Ceniza (CC) ---
            new Camino("CC1", "CC2", 4),
            new Camino("CC1", "CC3", 6),
            new Camino("CC1", "CC4", 7),
            new Camino("CC2", "CC4", 5),
            new Camino("CC2", "CC5", 8),
            new Camino("CC3", "CC4", 5),
            new Camino("CC3", "CC6", 6),
            new Camino("CC4", "CC5", 5),
            new Camino("CC4", "CC6", 5),
            new Camino("CC5", "CC6", 7),
            new Camino("CC5", "CC8", 6),
            new Camino("CC6", "CC7", 4),
            new Camino("CC7", "CC8", 4),
            new Camino("CC8", "CC9", 6),
            new Camino("CC8", "CC11", 7),
            new Camino("CC9", "CC11", 6),
            new Camino("CC10", "CC11", 4),
            // --- Pasos entre regiones: la guarida de cada region con el inicio de la siguiente ---
            new Camino("VB8", "CN1", 5),
            new Camino("CN9", "DA1", 5),
            new Camino("DA10", "CC1", 5)
    );

    public static final List<Objeto> OBJETOS = List.of(
            // --- Valle de Brezo (VB) ---
            new Objeto("OVB1", "Armadura", 6, 19, "VB2"),
            new Objeto("OVB2", "Hacha", 3, 11, "VB1"),
            new Objeto("OVB3", "Mapa", 5, 14, "VB5"),
            new Objeto("OVB4", "Gema", 1, 3, "VB4"),
            new Objeto("OVB5", "Arco", 4, 16, "VB2"),
            // --- Ciénagas de Niebla (CN) ---
            new Objeto("OCN1", "Arco antiguo", 7, 34, "CN1"),
            new Objeto("OCN2", "Gema antigua", 4, 27, "CN6"),
            new Objeto("OCN3", "Lanza antigua", 4, 29, "CN6"),
            new Objeto("OCN4", "Escudo antiguo", 4, 22, "CN8"),
            new Objeto("OCN5", "Antorcha antigua", 6, 32, "CN3"),
            new Objeto("OCN6", "Armadura antigua", 4, 17, "CN8"),
            // --- Dunas de Ámbar (DA) ---
            new Objeto("ODA1", "Mapa del desierto", 3, 19, "DA9"),
            new Objeto("ODA2", "Lanza del desierto", 2, 13, "DA8"),
            new Objeto("ODA3", "Arco del desierto", 3, 16, "DA3"),
            new Objeto("ODA4", "Gema del desierto", 7, 48, "DA4"),
            new Objeto("ODA5", "Antorcha del desierto", 5, 33, "DA8"),
            new Objeto("ODA6", "Hacha del desierto", 5, 41, "DA9"),
            new Objeto("ODA7", "Espada del desierto", 2, 15, "DA3"),
            // --- Cumbres de Ceniza (CC) ---
            new Objeto("OCC1", "Poción ígnea", 7, 48, "CC3"),
            new Objeto("OCC2", "Antorcha ígnea", 4, 46, "CC4"),
            new Objeto("OCC3", "Hacha ígnea", 7, 68, "CC2"),
            new Objeto("OCC4", "Lanza ígnea", 1, 11, "CC6"),
            new Objeto("OCC5", "Arco ígneo", 4, 26, "CC6"),
            new Objeto("OCC6", "Mapa ígneo", 3, 22, "CC9"),
            new Objeto("OCC7", "Gema ígnea", 7, 47, "CC8")
    );

    /**
     * Arma los nodos del reino con sus conexiones, listos para guardar en Neo4j.
     * Devuelve objetos nuevos en cada llamada.
     */
    public static List<Nodo> nodos() {
        Map<String, Nodo> porId = new LinkedHashMap<>();
        for (Zona z : ZONAS) {
            porId.put(z.id(), new Nodo(z.id(), z.nombre(), z.tipo(), z.curacion()));
        }
        for (Camino c : CAMINOS) {
            Nodo desde = porId.get(c.desde());
            Nodo hasta = porId.get(c.hasta());
            if (desde == null || hasta == null) {
                throw new IllegalStateException("Camino con una zona inexistente: " + c);
            }
            desde.conectar(hasta, c.costo());
        }
        return new ArrayList<>(porId.values());
    }

    /** Los objetos del reino como Items: peso en kg, valor en puntos de poder. */
    public static List<Item> items() {
        List<Item> items = new ArrayList<>(OBJETOS.size());
        for (Objeto o : OBJETOS) {
            items.add(new Item(o.id(), o.nombre(), o.peso(), o.poder(), o.zona()));
        }
        return items;
    }
}
