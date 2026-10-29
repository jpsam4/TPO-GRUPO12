package uade.prog3.tpo.algorithm;

import org.springframework.stereotype.Component;
import uade.prog3.tpo.service.Grafo;

import java.util.ArrayList;
import java.util.List;

/**
 * UNIDAD: Backtracking (clases 9 y 10)      PUNTAJE: 1 punto
 *
 * Enumera todos los caminos simples entre dos zonas del reino que respeten un
 * presupuesto maximo de energia (costoMaximo) y un tope de caminos recorridos
 * (saltosMaximos).
 *
 * Esquema del backtracking:
 *   1. Construccion incremental: el camino se extiende de a un vecino por vez.
 *   2. Criterio de viabilidad (poda):
 *        - no repetir zonas ya presentes en la rama actual (!visitado[sig])
 *        - no superar la energia disponible (costoActual + costoArista <= costoMaximo)
 *        - no superar el limite de tramos (saltosActuales + 1 <= saltosMaximos)
 *      Como los costos de las aristas son >= 0, el costo acumulado es monotono
 *      creciente: en cuanto una rama supera costoMaximo, ninguna extension
 *      futura puede volverla valida y se corta el subarbol entero sin perder
 *      soluciones.
 *   3. Retroceso (backtrack): al volver de explorar un vecino se lo saca del
 *      camino parcial y se desmarca visitado[sig] = false para que otras ramas
 *      puedan pasar por esa zona.
 */
@Component
public class Backtracking {

    /** Un camino simple encontrado, con su costo acumulado. */
    public record Ruta(List<String> vertices, double costoTotal) { }

    /**
     * Resultado detallado de una exploracion del arbol de busqueda.
     *
     * @param rutas           caminos simples validos de origen a destino
     * @param nodosExplorados estados visitados en el arbol de busqueda (llamadas recursivas)
     * @param podasPorCosto   ramas cortadas porque costoParcial > costoMaximo
     * @param podasPorSaltos  ramas cortadas porque saltosParciales > saltosMaximos
     */
    public record Exploracion(List<Ruta> rutas,
                              long nodosExplorados,
                              long podasPorCosto,
                              long podasPorSaltos) { }

    /**
     * Comparacion pedida en el Hito 8: nodos explorados con poda de viabilidad
     * frente a explorar todos los caminos simples sin podar (filtrando recien
     * al llegar al destino).
     */
    public record ComparacionPoda(List<Ruta> rutas,
                                  long nodosExploradosConPoda,
                                  long nodosExploradosSinPoda,
                                  long podasPorCosto,
                                  long podasPorSaltos,
                                  boolean mismasRutas) { }

    /**
     * Enumera TODOS los caminos simples de origen a destino que no superen
     * {@code costoMaximo} ni {@code saltosMaximos}, podando las ramas inviables
     * apenas violan cualquiera de las dos restricciones.
     *
     * Complejidad temporal: exponencial en el peor caso, O(b^d) acotada por
     *   saltosMaximos = d (con factor de ramificacion b <= V - 1), o O(V!) si
     *   no hay cota efectiva de saltos sobre un grafo completo, porque pueden
     *   existir (V - 2)! caminos simples entre dos vertices. En la practica la
     *   poda por costo y saltos corta subarboles enteros en cuanto se excede el
     *   presupuesto.
     * Complejidad espacial: O(V) de memoria auxiliar para la busqueda (arreglo
     *   boolean[] visitado, camino parcial y pila de recursion de profundidad
     *   <= min(V, saltosMaximos + 1)), sin contar la lista de rutas devueltas.
     */
    public List<Ruta> rutasSimples(Grafo grafo, String origenId, String destinoId,
                                   double costoMaximo, int saltosMaximos) {
        return explorar(grafo, origenId, destinoId, costoMaximo, saltosMaximos).rutas();
    }

    /**
     * Igual que {@link #rutasSimples}, pero devolviendo ademas cuantos nodos
     * del arbol de busqueda se exploraron y cuantas ramas fueron podadas.
     */
    public Exploracion explorar(Grafo grafo, String origenId, String destinoId,
                                double costoMaximo, int saltosMaximos) {
        int origen = validarParametros(grafo, origenId, destinoId, costoMaximo, saltosMaximos);
        int destino = grafo.indiceDe(destinoId);
        int n = grafo.cantidadVertices();

        boolean[] visitado = new boolean[n];
        List<String> camino = new ArrayList<>(n);
        List<Ruta> rutas = new ArrayList<>();
        // contadores[0] = nodosExplorados, [1] = podasPorCosto, [2] = podasPorSaltos
        long[] contadores = new long[3];

        visitado[origen] = true;
        camino.add(grafo.idDe(origen));

        explorarConPoda(grafo, origen, destino, 0.0, 0,
                costoMaximo, saltosMaximos, visitado, camino, rutas, contadores);

        return new Exploracion(rutas, contadores[0], contadores[1], contadores[2]);
    }

    /**
     * Exploracion SIN poda por costo ni por saltos: recorre todos los caminos
     * simples desde el origen (solo evita repetir vertices y frena al tocar el
     * destino) y recien en el destino verifica si el camino cumple el costo y
     * los saltos maximos. Sirve para medir cuantos estados ahorra la poda.
     */
    public Exploracion explorarSinPoda(Grafo grafo, String origenId, String destinoId,
                                       double costoMaximo, int saltosMaximos) {
        int origen = validarParametros(grafo, origenId, destinoId, costoMaximo, saltosMaximos);
        int destino = grafo.indiceDe(destinoId);
        int n = grafo.cantidadVertices();

        boolean[] visitado = new boolean[n];
        List<String> camino = new ArrayList<>(n);
        List<Ruta> rutas = new ArrayList<>();
        long[] contadores = new long[1];

        visitado[origen] = true;
        camino.add(grafo.idDe(origen));

        explorarExhaustivo(grafo, origen, destino, 0.0, 0,
                costoMaximo, saltosMaximos, visitado, camino, rutas, contadores);

        return new Exploracion(rutas, contadores[0], 0L, 0L);
    }

    /**
     * Corre la busqueda con poda y sin poda sobre la misma consulta, verifica
     * que la poda no haya perdido ni agregado ninguna ruta valida y reporta
     * los nodos explorados en cada caso.
     */
    public ComparacionPoda compararConYSinPoda(Grafo grafo, String origenId, String destinoId,
                                               double costoMaximo, int saltosMaximos) {
        Exploracion conPoda = explorar(grafo, origenId, destinoId, costoMaximo, saltosMaximos);
        Exploracion sinPoda = explorarSinPoda(grafo, origenId, destinoId, costoMaximo, saltosMaximos);
        return new ComparacionPoda(
                conPoda.rutas(),
                conPoda.nodosExplorados(),
                sinPoda.nodosExplorados(),
                conPoda.podasPorCosto(),
                conPoda.podasPorSaltos(),
                conPoda.rutas().equals(sinPoda.rutas()));
    }

    // ------------------------------------------------------------------
    // Recursion con poda y sin poda
    // ------------------------------------------------------------------

    private void explorarConPoda(Grafo grafo, int actual, int destino,
                                 double costoActual, int saltosActuales,
                                 double costoMaximo, int saltosMaximos,
                                 boolean[] visitado, List<String> camino,
                                 List<Ruta> rutas, long[] contadores) {
        contadores[0]++;

        if (actual == destino) {
            rutas.add(new Ruta(new ArrayList<>(camino), costoActual));
            return;
        }

        for (Grafo.Arista a : grafo.vecinos(actual)) {
            int sig = a.destino();
            if (visitado[sig]) {
                continue;   // camino simple: no repetir zonas en la misma ruta
            }

            double nuevoCosto = costoActual + a.costo();
            if (nuevoCosto > costoMaximo) {
                contadores[1]++;   // poda por costo: ya se paso del presupuesto
                continue;
            }

            int nuevosSaltos = saltosActuales + 1;
            if (nuevosSaltos > saltosMaximos) {
                contadores[2]++;   // poda por saltos: supera el tope de caminos
                continue;
            }

            // Hacer: avanzar al vecino
            visitado[sig] = true;
            camino.add(grafo.idDe(sig));

            explorarConPoda(grafo, sig, destino, nuevoCosto, nuevosSaltos,
                    costoMaximo, saltosMaximos, visitado, camino, rutas, contadores);

            // Deshacer (retroceso / backtrack): liberar la zona para otras ramas
            camino.remove(camino.size() - 1);
            visitado[sig] = false;
        }
    }

    private void explorarExhaustivo(Grafo grafo, int actual, int destino,
                                    double costoActual, int saltosActuales,
                                    double costoMaximo, int saltosMaximos,
                                    boolean[] visitado, List<String> camino,
                                    List<Ruta> rutas, long[] contadores) {
        contadores[0]++;

        if (actual == destino) {
            if (costoActual <= costoMaximo && saltosActuales <= saltosMaximos) {
                rutas.add(new Ruta(new ArrayList<>(camino), costoActual));
            }
            return;
        }

        for (Grafo.Arista a : grafo.vecinos(actual)) {
            int sig = a.destino();
            if (visitado[sig]) {
                continue;
            }

            visitado[sig] = true;
            camino.add(grafo.idDe(sig));

            explorarExhaustivo(grafo, sig, destino, costoActual + a.costo(), saltosActuales + 1,
                    costoMaximo, saltosMaximos, visitado, camino, rutas, contadores);

            camino.remove(camino.size() - 1);
            visitado[sig] = false;
        }
    }

    // ------------------------------------------------------------------
    // Validaciones
    // ------------------------------------------------------------------

    private int validarParametros(Grafo grafo, String origenId, String destinoId,
                                  double costoMaximo, int saltosMaximos) {
        int origen = grafo.indiceDe(origenId);     // si no existe: IllegalArgumentException -> 400
        grafo.indiceDe(destinoId);

        if (costoMaximo < 0) {
            throw new IllegalArgumentException("El costo maximo no puede ser negativo: " + costoMaximo);
        }
        if (saltosMaximos < 0) {
            throw new IllegalArgumentException("La cantidad maxima de saltos no puede ser negativa: " + saltosMaximos);
        }

        int n = grafo.cantidadVertices();
        for (int u = 0; u < n; u++) {
            for (Grafo.Arista a : grafo.vecinos(u)) {
                if (a.costo() < 0) {
                    throw new IllegalArgumentException(
                            "La poda por costo maximo exige aristas de costo no negativo: "
                                    + grafo.idDe(u) + " -> " + grafo.idDe(a.destino())
                                    + " (" + a.costo() + ")");
                }
            }
        }
        return origen;
    }
}
