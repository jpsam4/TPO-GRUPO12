package uade.prog3.tpo.service;

import uade.prog3.tpo.model.Item;
import uade.prog3.tpo.model.Nodo;

import java.util.ArrayList;
import java.util.List;

/**
 * Orden fijo de las zonas y los objetos al cargarlos de la base.
 *
 * Neo4j no garantiza en que orden devuelve las filas, y Bolt y la Query API
 * no las devuelven igual. Varios resultados dependen de ese orden aunque el
 * costo no cambie: el listado de aristas de Kruskal cuando hay empates, los
 * contadores de Floyd y de Dijkstra V veces, y el orden de los empates de los
 * ordenamientos. Ordenando al cargar, los ejemplos de docs/ENDPOINTS.md (que
 * salen de los tests, con el orden de DatosValdoria) son exactamente lo que
 * devuelve el servidor desplegado.
 *
 * El orden es el del reino: Valle de Brezo, Cienagas de Niebla, Dunas de
 * Ambar y Cumbres de Ceniza, y dentro de cada region por numero (VB2 antes que
 * VB10). Los ids que no siguen ese formato (la semilla A..H del scaffold) van
 * al final, en orden de texto.
 *
 * Es parte de la carga, no de ningun algoritmo. Se ordena por insercion
 * (estable, implementada aca): O(n^2) con n = 38 zonas o 25 objetos son unas
 * pocas centenas de comparaciones, despreciable frente a la consulta a la base.
 */
public final class OrdenDelReino {

    private static final String[] REGIONES = {"VB", "CN", "DA", "CC"};

    private OrdenDelReino() {
    }

    public static List<Nodo> zonas(List<Nodo> nodos) {
        List<Nodo> salida = new ArrayList<>(nodos);
        for (int i = 1; i < salida.size(); i++) {
            Nodo actual = salida.get(i);
            int j = i - 1;
            while (j >= 0 && comparar(salida.get(j).getId(), actual.getId()) > 0) {
                salida.set(j + 1, salida.get(j));
                j--;
            }
            salida.set(j + 1, actual);
        }
        return salida;
    }

    public static List<Item> objetos(List<Item> items) {
        List<Item> salida = new ArrayList<>(items);
        for (int i = 1; i < salida.size(); i++) {
            Item actual = salida.get(i);
            int j = i - 1;
            while (j >= 0 && comparar(sinPrefijoDeObjeto(salida.get(j).getId()),
                    sinPrefijoDeObjeto(actual.getId())) > 0) {
                salida.set(j + 1, salida.get(j));
                j--;
            }
            salida.set(j + 1, actual);
        }
        return salida;
    }

    /** Compara dos ids de zona segun el orden del reino. */
    static int comparar(String a, String b) {
        int ra = region(a);
        int rb = region(b);
        if (ra != rb) {
            return Integer.compare(ra, rb);
        }
        if (ra < REGIONES.length) {
            int na = numero(a);
            int nb = numero(b);
            if (na != nb) {
                return Integer.compare(na, nb);
            }
        }
        return a.compareTo(b);
    }

    private static int region(String id) {
        for (int r = 0; r < REGIONES.length; r++) {
            if (id.startsWith(REGIONES[r]) && numero(id) >= 0) {
                return r;
            }
        }
        return REGIONES.length;
    }

    /** El numero despues de las dos letras de la region, o -1 si no hay. */
    private static int numero(String id) {
        if (id.length() <= 2) {
            return -1;
        }
        int n = 0;
        for (int i = 2; i < id.length(); i++) {
            char c = id.charAt(i);
            if (c < '0' || c > '9') {
                return -1;
            }
            n = n * 10 + (c - '0');
        }
        return n;
    }

    /** Los objetos se llaman como su region con una O adelante: OVB1, OCN3... */
    private static String sinPrefijoDeObjeto(String id) {
        return id.length() > 1 && id.charAt(0) == 'O' ? id.substring(1) : id;
    }
}
