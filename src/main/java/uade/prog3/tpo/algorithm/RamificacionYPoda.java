package uade.prog3.tpo.algorithm;

import org.springframework.stereotype.Component;
import uade.prog3.tpo.model.Item;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * UNIDAD: Branch and Bound (clase 12)       PUNTAJE: 1 punto
 *
 * Es backtracking mas una cota. La diferencia con la clase Backtracking
 * es el criterio de poda: alli se podaba por VIABILIDAD, aca se poda por
 * OPTIMALIDAD.
 *
 * Problema en Valdoria: repartir el botin (los objetos del reino) entre k
 * personajes de forma que el que mas carga, cargue lo menos posible
 * (minimizar el makespan). Con los 25 objetos (108 kg) y 3 personajes, el
 * optimo es 36 kg cada uno.
 */
@Component
public class RamificacionYPoda {

    /**
     * Resultado del reparto.
     *
     * @param porContenedor   que objetos lleva cada personaje
     * @param cargaMaxima     kg del personaje mas cargado (lo que se minimiza)
     * @param cargas          kg de cada personaje
     * @param cota            la cota inferior global: ningun reparto puede bajar de aca
     * @param nodosExplorados nodos del arbol que se visitaron
     * @param nodosPodados    ramas descartadas por la cota sin explorarlas
     * @param nodosSinPoda    nodos que tiene el arbol completo: 1 + k + k^2 + ... + k^n
     */
    public record Asignacion(Map<String, List<Item>> porContenedor, double cargaMaxima,
                             List<Double> cargas, double cota,
                             long nodosExplorados, long nodosPodados, double nodosSinPoda) { }

    /** Mas pesados primero; a igual peso, por id para que el resultado sea siempre el mismo. */
    private static final Comparator<Item> POR_PESO_DESCENDENTE =
            Comparator.comparingDouble(Item::getPeso).reversed().thenComparing(Item::getId);

    private final Ordenamiento ordenamiento;

    public RamificacionYPoda(Ordenamiento ordenamiento) {
        this.ordenamiento = ordenamiento;
    }

    /** Estado de la busqueda: se comparte entre todas las llamadas recursivas. */
    private static final class Busqueda {
        Item[] items;
        int k;
        double cota;               // cota inferior global (techo de total / k si los pesos son enteros)
        double[] cargas;           // carga actual de cada personaje en el nodo
        int[] asignacion;          // asignacion[i] = personaje del objeto i en el nodo
        double mejor = Double.POSITIVE_INFINITY;
        int[] mejorAsignacion;
        long explorados;
        long podados;
    }

    /**
     * Branch and Bound para repartir los objetos entre k personajes
     * minimizando la carga del mas cargado.
     *
     * RAMIFICAR: el nodo de nivel i decide a que personaje va el objeto i.
     *   Cada nodo tiene hasta k hijos. Los objetos se procesan del mas pesado
     *   al mas liviano (con el MergeSort propio): asi las cargas grandes
     *   aparecen arriba en el arbol y la cota poda antes.
     *
     * ACOTAR: para cada hijo se calcula una cota INFERIOR de la mejor carga
     *   maxima alcanzable desde ahi:
     *
     *       cota(hijo) = max( carga maxima despues de asignar el objeto ,
     *                         peso total / k )
     *
     *   Es OPTIMISTA (nunca sobreestima): la carga maxima solo puede subir al
     *   seguir asignando, y aunque el resto se repartiera perfecto, alguno
     *   tiene que cargar al menos el promedio total / k. Si los pesos son
     *   enteros, la carga maxima tambien es entera y se puede usar el techo
     *   de total / k. Si la cota sobreestimara, se podarian ramas que
     *   contienen el optimo y el resultado dejaria de ser optimo.
     *
     * PODAR: si cota(hijo) >= mejor carga maxima ya encontrada, esa rama no
     *   puede mejorarla: se descarta sin explorarla. Ademas:
     *   - si ya se encontro un reparto que iguala la cota global, es optimo
     *     y se corta la busqueda;
     *   - los personajes son identicos: mandar el objeto a dos personajes con
     *     la misma carga genera subarboles equivalentes, asi que se prueba
     *     solo el primero (poda por simetria).
     *
     * Complejidad temporal: O(k^n) en el peor caso (el arbol completo tiene
     *   1 + k + ... + k^n nodos). Con la cota se explora una fraccion minima:
     *   con 25 objetos y 3 personajes, 546 nodos de 1.270.932.914.164.
     * Complejidad espacial: O(n + k): la recursion tiene profundidad n y se
     *   guardan las cargas de los k personajes y la mejor asignacion.
     */
    public Asignacion repartir(List<Item> items, int cantidadContenedores) {
        if (cantidadContenedores < 1) {
            throw new IllegalArgumentException("La cantidad de contenedores debe ser al menos 1");
        }
        Busqueda b = new Busqueda();
        b.items = ordenamiento.mergeSort(items, POR_PESO_DESCENDENTE).toArray(new Item[0]);
        b.k = cantidadContenedores;

        double total = 0;
        boolean enteros = true;
        for (Item it : b.items) {
            if (it.getPeso() < 0) {
                throw new IllegalArgumentException("Peso negativo en " + it.getId());
            }
            total += it.getPeso();
            enteros &= it.getPeso() == Math.floor(it.getPeso());
        }
        b.cota = enteros ? Math.ceil(total / b.k) : total / b.k;
        b.cargas = new double[b.k];
        b.asignacion = new int[b.items.length];
        b.mejorAsignacion = new int[b.items.length];

        explorar(b, 0, 0.0);

        return armarResultado(b, total);
    }

    private void explorar(Busqueda b, int i, double cargaMaxima) {
        b.explorados++;
        if (i == b.items.length) {                          // hoja: reparto completo
            if (cargaMaxima < b.mejor) {
                b.mejor = cargaMaxima;
                System.arraycopy(b.asignacion, 0, b.mejorAsignacion, 0, b.asignacion.length);
            }
            return;
        }
        double peso = b.items[i].getPeso();
        for (int c = 0; c < b.k; c++) {
            if (b.mejor <= b.cota) {
                return;                                     // ya se alcanzo la cota global: es optimo
            }
            if (mismaCargaQueUnoAnterior(b, c)) {
                continue;                                   // poda por simetria
            }
            double nuevaMaxima = Math.max(cargaMaxima, b.cargas[c] + peso);
            double cotaHijo = Math.max(nuevaMaxima, b.cota);
            if (cotaHijo >= b.mejor) {
                b.podados++;                                // poda por cota
                continue;
            }
            b.cargas[c] += peso;                            // ramificar
            b.asignacion[i] = c;
            explorar(b, i + 1, nuevaMaxima);
            b.cargas[c] -= peso;                            // deshacer
        }
    }

    private static boolean mismaCargaQueUnoAnterior(Busqueda b, int c) {
        for (int anterior = 0; anterior < c; anterior++) {
            if (b.cargas[anterior] == b.cargas[c]) {
                return true;
            }
        }
        return false;
    }

    private static Asignacion armarResultado(Busqueda b, double total) {
        Map<String, List<Item>> porContenedor = new LinkedHashMap<>();
        double[] cargas = new double[b.k];
        for (int c = 0; c < b.k; c++) {
            porContenedor.put("Personaje " + (c + 1), new ArrayList<>());
        }
        for (int i = 0; i < b.items.length; i++) {
            int c = b.mejorAsignacion[i];
            porContenedor.get("Personaje " + (c + 1)).add(b.items[i]);
            cargas[c] += b.items[i].getPeso();
        }
        List<Double> listaCargas = new ArrayList<>();
        double maxima = 0;
        for (double carga : cargas) {
            listaCargas.add(carga);
            maxima = Math.max(maxima, carga);
        }
        return new Asignacion(porContenedor, maxima, listaCargas, b.cota,
                b.explorados, b.podados, nodosDelArbolCompleto(b.k, b.items.length));
    }

    /** 1 + k + k^2 + ... + k^n: los nodos que recorreria el backtracking sin ninguna poda. */
    private static double nodosDelArbolCompleto(int k, int n) {
        double total = 0;
        double nivel = 1;
        for (int i = 0; i <= n; i++) {
            total += nivel;
            nivel *= k;
        }
        return total;
    }
}
