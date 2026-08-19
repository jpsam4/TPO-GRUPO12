package uade.prog3.tpo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uade.prog3.tpo.config.ClienteQueryApi;
import uade.prog3.tpo.config.TransporteNeo4j;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de la eleccion de transporte hacia Neo4j.
 *
 * No usan la red salvo el ultimo, que apunta a un puerto cerrado de la propia
 * maquina justamente para comprobar que la deteccion da "no disponible" rapido
 * y sin colgarse.
 */
class TransporteNeo4jTest {

    @Test
    @DisplayName("La URI de Bolt se traduce bien a la de la Query API")
    void traduceLaUri() {
        // Aura: HTTPS por el 443 (implicito), y la base va en el path
        assertEquals("https://3ae8699b.databases.neo4j.io/db/3ae8699b/query/v2",
                ClienteQueryApi.endpointDesde("neo4j+s://3ae8699b.databases.neo4j.io", "3ae8699b"));

        // Neo4j local: el puerto HTTP es el 7474, NO el 7687 de Bolt
        assertEquals("http://localhost:7474/db/neo4j/query/v2",
                ClienteQueryApi.endpointDesde("bolt://localhost:7687", "neo4j"));

        // bolt+s tambien es seguro
        assertTrue(ClienteQueryApi.endpointDesde("bolt+s://ejemplo.com", "neo4j").startsWith("https://"));
    }

    @Test
    @DisplayName("Si no hay nadie escuchando, Bolt se descarta rapido y no se cuelga")
    void detectaQueBoltNoEsta() {
        // Puerto alto de localhost, casi seguro cerrado: tiene que dar false
        // enseguida. Si esto tarda, el arranque de la aplicacion se va a colgar.
        long t0 = System.currentTimeMillis();
        boolean disponible = TransporteNeo4j.boltDisponible("bolt://127.0.0.1:59999");
        long ms = System.currentTimeMillis() - t0;

        assertFalse(disponible, "no hay ningun Neo4j en ese puerto");
        assertTrue(ms < 10_000, "la deteccion tardo demasiado: " + ms + " ms");
    }

    @Test
    @DisplayName("Un host que no existe tampoco cuelga la deteccion")
    void hostInexistente() {
        assertFalse(TransporteNeo4j.boltDisponible("neo4j+s://no-existe.invalid"));
    }
}
