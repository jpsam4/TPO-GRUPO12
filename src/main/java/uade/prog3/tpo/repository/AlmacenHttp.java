package uade.prog3.tpo.repository;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import uade.prog3.tpo.config.ClienteQueryApi;
import uade.prog3.tpo.model.Conexion;
import uade.prog3.tpo.model.Item;
import uade.prog3.tpo.model.Nodo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementacion alternativa: habla con Neo4j por la Query API sobre HTTPS,
 * puerto 443, en vez de Bolt sobre el 7687.
 *
 * Se activa sola cuando el 7687 no esta accesible (tipicamente desde la red de
 * UADE, que corta el TLS en todos los puertos que no sean los conocidos).
 * Ver TransporteNeo4j para la deteccion.
 *
 * Escribe y lee exactamente el mismo modelo que Spring Data Neo4j:
 *   (:Nodo {id, nombre, tipo, valor}) -[:CONECTA {costo}]-> (:Nodo)
 *   (:Item {id, nombre, peso, valor, nodoId})
 * asi que las dos implementaciones son intercambiables y ven los mismos datos.
 */
@Component
@Profile("http")
public class AlmacenHttp implements AlmacenNeo4j {

    private static final Logger log = LoggerFactory.getLogger(AlmacenHttp.class);

    private final ClienteQueryApi cliente;

    public AlmacenHttp(@Value("${spring.neo4j.uri}") String uri,
                       @Value("${spring.neo4j.authentication.username}") String usuario,
                       @Value("${spring.neo4j.authentication.password}") String password,
                       @Value("${spring.data.neo4j.database:neo4j}") String base) {
        this.cliente = new ClienteQueryApi(uri, usuario, password, base);
        log.info("Hablando con Neo4j por la Query API: {}", cliente.endpoint());
    }

    @Override
    public List<Nodo> todosLosNodos() {
        // Una sola consulta trae los nodos con sus conexiones salientes.
        // OPTIONAL MATCH para no perder los nodos aislados, y se filtran los
        // destinos nulos que genera el optional.
        ClienteQueryApi.Resultado r = cliente.ejecutar("""
                MATCH (n:Nodo)
                OPTIONAL MATCH (n)-[c:CONECTA]->(d:Nodo)
                WITH n, collect({destino: d.id, costo: c.costo}) AS salientes
                RETURN n.id AS id, n.nombre AS nombre, n.tipo AS tipo, n.valor AS valor,
                       [x IN salientes WHERE x.destino IS NOT NULL] AS conexiones
                ORDER BY id
                """, Map.of());

        // Primera pasada: crear todos los Nodo sin conexiones.
        Map<String, Nodo> porId = new LinkedHashMap<>();
        for (int i = 0; i < r.cantidadFilas(); i++) {
            String id = texto(r.valor(i, "id"));
            porId.put(id, new Nodo(id, texto(r.valor(i, "nombre")),
                    texto(r.valor(i, "tipo")), numero(r.valor(i, "valor"))));
        }

        // Segunda pasada: enlazar. Hace falta que todos existan antes, porque
        // una conexion apunta al objeto Nodo destino, no a su id.
        for (int i = 0; i < r.cantidadFilas(); i++) {
            Nodo origen = porId.get(texto(r.valor(i, "id")));
            for (JsonNode c : r.valor(i, "conexiones")) {
                Nodo destino = porId.get(c.path("destino").asText());
                if (destino != null) {
                    origen.getConexiones().add(new Conexion(destino, c.path("costo").asDouble()));
                }
            }
        }
        return new ArrayList<>(porId.values());
    }

    @Override
    public long contarNodos() {
        ClienteQueryApi.Resultado r = cliente.ejecutar("MATCH (n:Nodo) RETURN count(n) AS total", Map.of());
        return r.cantidadFilas() == 0 ? 0 : (long) numero(r.valor(0, "total"));
    }

    @Override
    public void guardarNodos(List<Nodo> nodos) {
        List<Map<String, Object>> filasNodo = new ArrayList<>();
        List<Map<String, Object>> filasArista = new ArrayList<>();

        for (Nodo n : nodos) {
            filasNodo.add(Map.of("id", n.getId(), "nombre", valorODefecto(n.getNombre()),
                    "tipo", valorODefecto(n.getTipo()), "valor", n.getValor()));
            for (Conexion c : n.getConexiones()) {
                filasArista.add(Map.of("origen", n.getId(),
                        "destino", c.getDestino().getId(), "costo", c.getCosto()));
            }
        }

        // MERGE y no CREATE: guardar dos veces no duplica, igual que saveAll.
        cliente.ejecutar("""
                UNWIND $filas AS f
                MERGE (n:Nodo {id: f.id})
                SET n.nombre = f.nombre, n.tipo = f.tipo, n.valor = toFloat(f.valor)
                """, Map.of("filas", filasNodo));

        if (!filasArista.isEmpty()) {
            cliente.ejecutar("""
                    UNWIND $filas AS f
                    MATCH (o:Nodo {id: f.origen}), (d:Nodo {id: f.destino})
                    MERGE (o)-[c:CONECTA]->(d)
                    SET c.costo = toFloat(f.costo)
                    """, Map.of("filas", filasArista));
        }
        log.info("Guardados {} nodos y {} aristas por HTTPS", filasNodo.size(), filasArista.size());
    }

    @Override
    public List<Item> todosLosItems() {
        ClienteQueryApi.Resultado r = cliente.ejecutar("""
                MATCH (i:Item)
                RETURN i.id AS id, i.nombre AS nombre, i.peso AS peso,
                       i.valor AS valor, i.nodoId AS nodoId
                ORDER BY id
                """, Map.of());

        List<Item> items = new ArrayList<>(r.cantidadFilas());
        for (int i = 0; i < r.cantidadFilas(); i++) {
            items.add(new Item(texto(r.valor(i, "id")), texto(r.valor(i, "nombre")),
                    numero(r.valor(i, "peso")), numero(r.valor(i, "valor")),
                    texto(r.valor(i, "nodoId"))));
        }
        return items;
    }

    @Override
    public void guardarItems(List<Item> items) {
        List<Map<String, Object>> filas = new ArrayList<>();
        for (Item i : items) {
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("id", i.getId());
            fila.put("nombre", valorODefecto(i.getNombre()));
            fila.put("peso", i.getPeso());
            fila.put("valor", i.getValor());
            fila.put("nodoId", i.getNodoId());   // puede ser null: LinkedHashMap lo acepta
            filas.add(fila);
        }
        cliente.ejecutar("""
                UNWIND $filas AS f
                MERGE (i:Item {id: f.id})
                SET i.nombre = f.nombre, i.peso = toFloat(f.peso),
                    i.valor = toFloat(f.valor), i.nodoId = f.nodoId
                """, Map.of("filas", filas));
        log.info("Guardados {} items por HTTPS", filas.size());
    }

    @Override
    public String descripcionDelTransporte() {
        return "Query API sobre HTTPS (" + cliente.endpoint() + ")";
    }

    // --- Lectura defensiva del JSON: la Query API manda null como JSON null ---

    private static String texto(JsonNode n) {
        return n == null || n.isNull() ? null : n.asText();
    }

    private static double numero(JsonNode n) {
        return n == null || n.isNull() ? 0d : n.asDouble();
    }

    private static String valorODefecto(String s) {
        return s == null ? "" : s;
    }
}
