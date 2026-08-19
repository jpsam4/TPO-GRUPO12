package uade.prog3.tpo.config;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Decide COMO se va a hablar con Neo4j, antes de que arranque Spring.
 *
 * EL PROBLEMA
 * -----------
 * La red de UADE permite TLS solamente en los puertos "conocidos" (443, 993,
 * 465). En cualquier otro puerto el firewall detecta el handshake TLS y corta
 * la conexion. Se verifico que pasa con cualquier destino, no solo con Aura:
 * tambien se cae contra Cloudflare en el 8443 y contra 1.1.1.1 en el 853.
 * Como Bolt usa el 7687, desde la facultad el driver de Neo4j no conecta nunca.
 *
 * LA SOLUCION
 * -----------
 * Neo4j ofrece DOS protocolos para lo mismo: Bolt (7687) y la Query API
 * (HTTPS, 443). El 443 si esta permitido. Asi que en vez de pelear con el
 * firewall se usa el otro protocolo, que es oficial y llega igual a la base.
 *
 * Esta clase prueba Bolt de verdad (abre el socket, hace el handshake y espera
 * la respuesta del servidor) y elige el perfil que corresponde:
 *
 *   perfil "bolt" -> Spring Data Neo4j (lo normal, fuera de la facultad)
 *   perfil "http" -> Query API sobre HTTPS (lo que funciona en la facultad)
 *
 * Se puede forzar con la variable NEO4J_TRANSPORTE = auto | bolt | http.
 */
public final class TransporteNeo4j {

    /** Primeros 4 bytes de todo handshake Bolt. */
    private static final byte[] PREAMBULO = {0x60, 0x60, (byte) 0xB0, 0x17};

    /** Versiones de Bolt que se le proponen al servidor, de mayor a menor. */
    private static final int[] VERSIONES = {0x00000405, 0x00000004, 0x00000003, 0x00000002};

    /** Corto a proposito: si Bolt no contesta rapido, no vale la pena esperarlo. */
    private static final int ESPERA_MS = 4000;

    private TransporteNeo4j() {
    }

    /**
     * Elige el transporte y deja el perfil de Spring configurado.
     * Se llama desde main(), antes de SpringApplication.run().
     */
    public static void elegirYConfigurar() {
        if (System.getProperty("spring.profiles.active") != null
                || System.getenv("SPRING_PROFILES_ACTIVE") != null) {
            return;   // alguien ya lo eligio a mano: no se toca
        }

        Map<String, String> config = leerConfiguracion();
        aplicarComoPropiedadesDelSistema(config);
        String uri = config.getOrDefault("NEO4J_URI", "bolt://localhost:7687");
        String preferencia = config.getOrDefault("NEO4J_TRANSPORTE", "auto").trim().toLowerCase();

        String perfil;
        if ("bolt".equals(preferencia) || "http".equals(preferencia)) {
            perfil = preferencia;
            System.out.println("[Neo4j] Transporte forzado por configuracion: " + perfil);
        } else {
            System.out.println("[Neo4j] Probando si Bolt llega a " + soloHostYPuerto(uri) + " ...");
            long t0 = System.currentTimeMillis();
            boolean boltAndaBien = boltDisponible(uri);
            long ms = System.currentTimeMillis() - t0;

            perfil = boltAndaBien ? "bolt" : "http";
            if (boltAndaBien) {
                System.out.println("[Neo4j] Bolt responde (" + ms + " ms). Se usa Spring Data Neo4j.");
            } else {
                System.out.println("[Neo4j] Bolt NO responde (" + ms + " ms). "
                        + "Suele ser el firewall de la facultad, que corta TLS fuera de los puertos "
                        + "conocidos. Se cambia a la Query API sobre HTTPS (443).");
            }
        }

        System.setProperty("spring.profiles.active", perfil);
    }

    /**
     * Prueba Bolt de punta a punta: TCP, TLS si corresponde, handshake, y que
     * el servidor conteste una version. No alcanza con que abra el TCP: el
     * firewall de la facultad DEJA abrir el TCP y recien corta cuando ve el
     * TLS, asi que un test que solo abra el socket da un falso positivo.
     */
    public static boolean boltDisponible(String uri) {
        String sinEsquema = uri.contains("://") ? uri.split("://", 2)[1] : uri;
        String autoridad = sinEsquema.split("/")[0];
        String host = autoridad.contains(":") ? autoridad.substring(0, autoridad.lastIndexOf(':')) : autoridad;
        int puerto = autoridad.contains(":")
                ? Integer.parseInt(autoridad.substring(autoridad.lastIndexOf(':') + 1))
                : 7687;
        boolean seguro = uri.startsWith("neo4j+s") || uri.startsWith("bolt+s")
                || uri.startsWith("neo4j+ssc") || uri.startsWith("bolt+ssc");

        Socket socket = null;
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(host, puerto), ESPERA_MS);
            socket.setSoTimeout(ESPERA_MS);

            if (seguro) {
                // Aca es donde se cae en la facultad: el TCP ya esta abierto y
                // el firewall manda RST apenas ve el ClientHello.
                SSLSocketFactory fabrica = (SSLSocketFactory) SSLSocketFactory.getDefault();
                SSLSocket tls = (SSLSocket) fabrica.createSocket(socket, host, puerto, true);
                tls.setSoTimeout(ESPERA_MS);
                tls.startHandshake();   // aca salta el RST del firewall, si lo hay
                socket = tls;
            }

            OutputStream salida = socket.getOutputStream();
            salida.write(PREAMBULO);
            for (int v : VERSIONES) {
                salida.write(new byte[]{(byte) (v >>> 24), (byte) (v >>> 16), (byte) (v >>> 8), (byte) v});
            }
            salida.flush();

            InputStream entrada = socket.getInputStream();
            byte[] respuesta = new byte[4];
            int leidos = 0;
            while (leidos < 4) {
                int n = entrada.read(respuesta, leidos, 4 - leidos);
                if (n < 0) return false;    // cerro sin contestar
                leidos += n;
            }
            // El servidor devuelve la version acordada. Todo ceros = no hay acuerdo.
            return respuesta[0] != 0 || respuesta[1] != 0 || respuesta[2] != 0 || respuesta[3] != 0;

        } catch (Exception e) {
            return false;
        } finally {
            cerrar(socket);
        }
    }

    private static void cerrar(Socket s) {
        if (s != null) {
            try {
                s.close();
            } catch (IOException ignorado) {
                // no importa: la conexion era solo para probar
            }
        }
    }

    private static String soloHostYPuerto(String uri) {
        return uri.contains("://") ? uri.split("://", 2)[1] : uri;
    }

    /**
     * Busca el .env en el directorio actual y en los de mas arriba.
     *
     * Hace falta buscar hacia arriba porque segun como se arranque (mvnw
     * spring-boot:run, java -jar, el IDE, Docker) el directorio de trabajo del
     * proceso no siempre es la raiz del proyecto, y entonces un Path.of(".env")
     * pelado no encuentra nada y la aplicacion se conecta a localhost sin avisar.
     */
    private static Path buscarEnv() {
        Path actual = Path.of("").toAbsolutePath();
        for (int i = 0; i < 4 && actual != null; i++) {
            Path candidato = actual.resolve(".env");
            if (Files.isRegularFile(candidato)) return candidato;
            actual = actual.getParent();
        }
        return null;
    }

    /**
     * Lee la configuracion de .env y del ambiente. Las variables de entorno
     * ganan, para que Render y Docker puedan pisar lo del archivo.
     */
    private static Map<String, String> leerConfiguracion() {
        Map<String, String> config = new HashMap<>();
        Path env = buscarEnv();
        if (env == null) {
            System.out.println("[Neo4j] No se encontro un archivo .env (se busco desde "
                    + Path.of("").toAbsolutePath() + " hacia arriba). Se usa solo el ambiente.");
        } else {
            try {
                for (String linea : Files.readAllLines(env, StandardCharsets.UTF_8)) {
                    String l = linea.trim();
                    if (l.isEmpty() || l.startsWith("#") || !l.contains("=")) continue;
                    String[] partes = l.split("=", 2);
                    config.put(partes[0].trim(), partes[1].trim());
                }
                System.out.println("[Neo4j] Configuracion leida de " + env);
            } catch (IOException e) {
                System.out.println("[Neo4j] No se pudo leer " + env + " (" + e.getMessage()
                        + "), se usa el ambiente.");
            }
        }
        // Las variables que falten en el .env se completan con el ambiente.
        // Sirve para Render y Docker, que no tienen archivo .env.
        for (String clave : List.of("NEO4J_URI", "NEO4J_USER", "NEO4J_PASSWORD",
                                    "NEO4J_DATABASE", "NEO4J_TRANSPORTE")) {
            if (!config.containsKey(clave)) {
                String delAmbiente = System.getenv(clave);
                if (delAmbiente != null && !delAmbiente.isBlank()) {
                    config.put(clave, delAmbiente.trim());
                }
            }
        }
        return config;
    }

    /**
     * Pasa lo que dice el .env a propiedades del sistema, para que Spring lo
     * tome como fuente de maxima prioridad.
     *
     * POR QUE HACE FALTA
     * ------------------
     * Spring ordena las fuentes de configuracion asi:
     *     propiedades del sistema  >  variables de entorno  >  archivos
     * Con spring.config.import el .env entra como ARCHIVO, o sea al final. Si
     * la maquina tiene una variable de entorno NEO4J_URI vieja (por ejemplo
     * apuntando a localhost, de cuando se trabajaba con Neo4j local), esa
     * variable le gana al .env y la aplicacion se conecta a otro lado sin que
     * nada avise. Es exactamente lo que estaba pasando en esta maquina.
     *
     * Subiendolo a propiedad del sistema, el .env del proyecto manda. Donde no
     * hay .env (Render, Docker) no se toca nada y siguen mandando las
     * variables de entorno, que es lo que corresponde ahi.
     */
    private static void aplicarComoPropiedadesDelSistema(Map<String, String> config) {
        for (Map.Entry<String, String> e : config.entrySet()) {
            String clave = e.getKey();
            if (System.getProperty(clave) != null) continue;   // ya lo pisaron a mano: se respeta

            String delAmbiente = System.getenv(clave);
            if (delAmbiente != null && !delAmbiente.equals(e.getValue())) {
                System.out.println("[Neo4j] AVISO: la variable de entorno " + clave
                        + " dice algo distinto que el .env. Gana el .env."
                        + (clave.contains("PASSWORD") ? "" : " (ambiente=" + delAmbiente + ")"));
            }
            System.setProperty(clave, e.getValue());
        }
    }
}
