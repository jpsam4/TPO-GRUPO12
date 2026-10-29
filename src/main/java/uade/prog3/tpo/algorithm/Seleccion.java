package uade.prog3.tpo.algorithm;

import org.springframework.stereotype.Component;
import uade.prog3.tpo.model.Item;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * UNIDAD: Greedy (clase 3) y Programacion Dinamica (clase 6)
 * PUNTAJE: 1 punto greedy + 1 punto programacion dinamica
 *
 * Los dos metodos resuelven EL MISMO problema: llenar la mochila del heroe.
 *
 *   Funcion objetivo: MAXIMIZAR el poder total (suma de Item.valor) de los
 *   objetos elegidos, sujeto a que la suma de sus pesos (kg) no supere la
 *   capacidad de la mochila. Cada objeto se lleva entero o no se lleva (0/1).
 *
 * Greedy es rapido pero no garantiza el optimo; programacion dinamica si.
 * Caso donde greedy pierde (Valle de Brezo, mochila de 10 kg):
 *   greedy -> Arco + Hacha + Gema = 30 de poder (8 kg, sobran 2 kg)
 *   optimo -> Armadura + Arco     = 35 de poder (10 kg)
 */
@Component
public class Seleccion {

    /** Resultado de una seleccion de items bajo restriccion de capacidad. */
    public record Resultado(List<Item> elegidos, double valorTotal, double pesoTotal, String tecnica) { }

    /** Mayor ratio valor/peso primero; a igual ratio, por id para que el resultado no dependa del orden de la base. */
    private static final Comparator<Item> POR_RATIO_DESCENDENTE =
            Comparator.comparingDouble(Item::ratioValorPeso).reversed()
                    .thenComparing(Item::getId);

    private final Ordenamiento ordenamiento;

    public Seleccion(Ordenamiento ordenamiento) {
        this.ordenamiento = ordenamiento;
    }

    /**
     * GREEDY por ratio poder/peso.
     *
     * Ordena los objetos de mayor a menor ratio valor/peso (con el MergeSort
     * propio del Hito 2) y los recorre una sola vez: si el objeto entra en lo
     * que queda de capacidad, lo toma; si no entra, lo saltea y sigue con el
     * proximo (uno mas liviano todavia puede entrar). Nunca revisa una
     * decision ya tomada: eso es lo que lo hace greedy.
     *
     * Complejidad temporal: O(n log n), dominada por el ordenamiento; la
     *   pasada que elige es O(n).
     * Complejidad espacial: O(n): la copia ordenada y la lista de elegidos.
     *
     * NO garantiza el optimo en la mochila 0/1: llenar con lo de mejor ratio
     * puede dejar capacidad sin usar que un objeto de peor ratio hubiera
     * aprovechado mejor (ver el ejemplo del Valle de Brezo arriba).
     */
    public Resultado greedy(List<Item> items, double capacidad) {
        if (capacidad < 0) {
            throw new IllegalArgumentException("La capacidad no puede ser negativa: " + capacidad);
        }
        List<Item> ordenados = ordenamiento.mergeSort(items, POR_RATIO_DESCENDENTE);

        List<Item> elegidos = new ArrayList<>();
        double peso = 0;
        double valor = 0;
        for (Item item : ordenados) {
            if (peso + item.getPeso() <= capacidad) {
                elegidos.add(item);
                peso += item.getPeso();
                valor += item.getValor();
            }
        }
        return new Resultado(elegidos, valor, peso, "greedy por ratio valor/peso");
    }

    /**
     * PROGRAMACION DINAMICA: mochila 0/1 con tabla dp, bottom-up.
     *
     *   dp[i][j] = maximo poder usando solo los primeros i objetos
     *              con una mochila de capacidad j.
     *
     *   dp[0][j] = 0                                  (sin objetos)
     *   dp[i][j] = dp[i-1][j]                          si el objeto i pesa mas que j
     *   dp[i][j] = max(dp[i-1][j],                     no llevarlo
     *                  dp[i-1][j - peso_i] + valor_i)  llevarlo
     *
     * La respuesta es dp[n][W]. Para RECUPERAR los objetos se recorre la tabla
     * hacia atras desde dp[n][W]: si dp[i][j] != dp[i-1][j], el objeto i se
     * llevo (sin el no se llega a ese valor), y se sigue desde j - peso_i.
     *
     * Complejidad temporal: O(n * W): se llena cada celda una vez en O(1),
     *   y la recuperacion es O(n).
     * Complejidad espacial: O(n * W) por la tabla completa, que hace falta
     *   para poder recuperar los objetos.
     *
     * Es PSEUDOPOLINOMICA: W es un valor, no un tamanio de entrada. Escribir W
     * ocupa log W bits, asi que O(n * W) es exponencial en esos bits: duplicar
     * la capacidad duplica el trabajo aunque la entrada casi no crezca.
     *
     * Los pesos tienen que ser enteros, porque se usan como indice de columna.
     */
    public Resultado programacionDinamica(List<Item> items, int capacidad) {
        if (capacidad < 0) {
            throw new IllegalArgumentException("La capacidad no puede ser negativa: " + capacidad);
        }
        int n = items.size();
        int[] peso = new int[n];
        for (int i = 0; i < n; i++) {
            double p = items.get(i).getPeso();
            if (p < 0 || p != Math.floor(p)) {
                throw new IllegalArgumentException("La mochila 0/1 necesita pesos enteros no negativos: "
                        + items.get(i).getId() + " pesa " + p);
            }
            peso[i] = (int) p;
        }

        double[][] dp = tabla(items, peso, capacidad);

        // Recuperar el camino: de dp[n][W] hacia arriba.
        List<Item> alReves = new ArrayList<>();
        int j = capacidad;
        for (int i = n; i >= 1; i--) {
            if (dp[i][j] != dp[i - 1][j]) {
                alReves.add(items.get(i - 1));
                j -= peso[i - 1];
            }
        }
        List<Item> elegidos = new ArrayList<>(alReves.size());
        for (int k = alReves.size() - 1; k >= 0; k--) {
            elegidos.add(alReves.get(k));
        }

        return new Resultado(elegidos, dp[n][capacidad], capacidad - j,
                "programacion dinamica (mochila 0/1)");
    }

    /** Llena la tabla dp de (n+1) x (W+1), fila por fila. */
    private double[][] tabla(List<Item> items, int[] peso, int capacidad) {
        int n = items.size();
        double[][] dp = new double[n + 1][capacidad + 1];   // fila 0 queda en 0
        for (int i = 1; i <= n; i++) {
            int p = peso[i - 1];
            double v = items.get(i - 1).getValor();
            for (int j = 0; j <= capacidad; j++) {
                dp[i][j] = dp[i - 1][j];                       // no llevarlo
                if (p <= j && dp[i - 1][j - p] + v > dp[i][j]) {
                    dp[i][j] = dp[i - 1][j - p] + v;           // llevarlo
                }
            }
        }
        return dp;
    }
}
