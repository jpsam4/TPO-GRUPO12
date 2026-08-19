package uade.prog3.tpo.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Cliente de la Query API de Neo4j: ejecuta Cypher sobre HTTPS.
 *
 * POR QUE EXISTE
 * --------------
 * La red de la facultad deja pasar TLS solamente por los puertos "conocidos"
 * (443, 993, 465...). En cualquier otro puerto el firewall ve el ClientHello y
 * corta la conexion. El protocolo Bolt de Neo4j usa el 7687, asi que desde la
 * facultad el driver de Spring nunca llega a Aura.
 *
 * La Query API es un protocolo OFICIAL de Neo4j que hace lo mismo que Bolt pero
 * sobre HTTPS por el 443, que si esta permitido. No es un truco ni una forma de
 * saltear el firewall: es hablar con la base por el otro protocolo que la base
 * ya ofrece, el que usa el puerto que la red habilita.
 *
 * Referencia: https://neo4j.com/docs/query-api/current/
 */
public class ClienteQueryApi {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final HttpClient http;
    private final URI endpoint;
    private final String autorizacion;

    public ClienteQueryApi(String uriNeo4j, String usuario, String password, String base) {
        this.endpoint = URI.create(endpointDesde(uriNeo4j, base));
        this.autorizacion = "Basic " + Base64.getEncoder()
                .encodeToString((usuario + ":" + password).getBytes(StandardCharsets.UTF_8));
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    /**
     * Traduce la URI de Bolt a la de la Query API.
     *
     *   neo4j+s://xxx.databases.neo4j.io  ->  https://xxx.databases.neo4j.io/db/<base>/query/v2
     *   bolt://localhost:7687             ->  http://localhost:7474/db/<base>/query/v2
     */
    public static String endpointDesde(String uriNeo4j, String base) {
        String sinEsquema = uriNeo4j.contains("://") ? uriNeo4j.split("://", 2)[1] : uriNeo4j;
        boolean seguro = uriNeo4j.startsWith("neo4j+s") || uriNeo4j.startsWith("bolt+s")
                || uriNeo4j.startsWith("neo4j+ssc") || uriNeo4j.startsWith("bolt+ssc");
        String host = sinEsquema.split("/")[0].split(":")[0];
        // En Aura el HTTPS va por el 443 (implicito). En una instalacion local
        // el puerto HTTP de Neo4j es el 7474, no el 7687 de Bolt.
        String autoridad = seguro ? host : host + ":7474";
        return (seguro ? "https://" : "http://") + autoridad + "/db/" + base + "/query/v2";
    }

    /** Ejecuta una sentencia Cypher y devuelve las filas ya desarmadas. */
    public Resultado ejecutar(String cypher, Map<String, Object> parametros) {
        try {
            ObjectNode cuerpo = JSON.createObjectNode();
            cuerpo.put("statement", cypher);
            cuerpo.set("parameters", JSON.valueToTree(parametros == null ? Map.of() : parametros));

            HttpRequest pedido = HttpRequest.newBuilder(endpoint)
                    .timeout(Duration.ofSeconds(60))
                    .header("Authorization", autorizacion)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(cuerpo.toString(), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> respuesta = http.send(pedido, HttpResponse.BodyHandlers.ofString());
            JsonNode raiz = JSON.readTree(respuesta.body());

            if (raiz.has("errors")) {
                throw new ErrorDeNeo4j("Neo4j rechazo la consulta: " + raiz.get("errors").toString());
            }
            if (respuesta.statusCode() >= 300) {
                throw new ErrorDeNeo4j("La Query API respondio HTTP " + respuesta.statusCode()
                        + ": " + respuesta.body());
            }
            return new Resultado(raiz.path("data").path("fields"), raiz.path("data").path("values"));

        } catch (ErrorDeNeo4j e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ErrorDeNeo4j("Consulta interrumpida", e);
        } catch (Exception e) {
            throw new ErrorDeNeo4j("No se pudo hablar con Neo4j por HTTPS (" + endpoint + ")", e);
        }
    }

    public String endpoint() {
        return endpoint.toString();
    }

    /** Filas devueltas por la Query API, con acceso por nombre de columna. */
    public static class Resultado {
        private final JsonNode campos;
        private final JsonNode valores;

        Resultado(JsonNode campos, JsonNode valores) {
            this.campos = campos;
            this.valores = valores;
        }

        public int cantidadFilas() {
            return valores.isArray() ? valores.size() : 0;
        }

        public JsonNode fila(int i) {
            return valores.get(i);
        }

        /** Indice de la columna con ese nombre, o -1. */
        public int columna(String nombre) {
            for (int i = 0; i < campos.size(); i++) {
                if (nombre.equals(campos.get(i).asText())) return i;
            }
            return -1;
        }

        public JsonNode valor(int fila, String columna) {
            int c = columna(columna);
            if (c < 0) throw new ErrorDeNeo4j("La consulta no devolvio la columna '" + columna + "'");
            return valores.get(fila).get(c);
        }

        public List<JsonNode> filas() {
            return JSON.convertValue(valores, JSON.getTypeFactory()
                    .constructCollectionType(List.class, JsonNode.class));
        }
    }

    /** Falla al hablar con Neo4j por HTTPS. */
    public static class ErrorDeNeo4j extends RuntimeException {
        public ErrorDeNeo4j(String mensaje) { super(mensaje); }
        public ErrorDeNeo4j(String mensaje, Throwable causa) { super(mensaje, causa); }
    }
}
