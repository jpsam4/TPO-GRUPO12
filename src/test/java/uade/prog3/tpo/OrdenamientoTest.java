package uade.prog3.tpo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uade.prog3.tpo.algorithm.Ordenamiento;
import uade.prog3.tpo.model.Item;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de QuickSort y MergeSort (hito 2).
 *
 * NO tocan Neo4j: son unitarios y corren en milisegundos.
 *
 * Los items son los mismos que carga CargaInicial, para poder verificar los
 * resultados a mano contra la semilla:
 *
 *   id   peso  valor   ratio = valor/peso
 *   I1     5     10      2.00
 *   I2     4     40     10.00
 *   I3     6     30      5.00
 *   I4     3     50     16.67
 *   I5     7     55      7.86
 *
 * A proposito no se compara contra Arrays.sort ni Collections.sort en ningun
 * test: se verifican las dos propiedades que definen un ordenamiento correcto,
 * que la salida este ordenada y que sea una permutacion de la entrada.
 */
class OrdenamientoTest {

    private final Ordenamiento ordenamiento = new Ordenamiento();

    private static final Comparator<Item> POR_PESO  = Comparator.comparingDouble(Item::getPeso);
    private static final Comparator<Item> POR_VALOR = Comparator.comparingDouble(Item::getValor);
    private static final Comparator<Item> POR_RATIO = Comparator.comparingDouble(Item::ratioValorPeso);

    /** Los 5 items de la carga inicial. */
    private List<Item> itemsSemilla() {
        List<Item> items = new ArrayList<>();
        items.add(new Item("I1", "Item 1", 5, 10, "A"));
        items.add(new Item("I2", "Item 2", 4, 40, "B"));
        items.add(new Item("I3", "Item 3", 6, 30, "C"));
        items.add(new Item("I4", "Item 4", 3, 50, "D"));
        items.add(new Item("I5", "Item 5", 7, 55, "E"));
        return items;
    }

    private List<String> ids(List<Item> items) {
        List<String> ids = new ArrayList<>(items.size());
        for (Item i : items) ids.add(i.getId());
        return ids;
    }

    // ------------------------------------------------------------------
    // QuickSort
    // ------------------------------------------------------------------

    @Test
    @DisplayName("QuickSort ordena los items de la semilla por peso")
    void quickSortOrdenaPorPeso() {
        // pesos 5,4,6,3,7 -> 3,4,5,6,7  =>  I4, I2, I1, I3, I5
        List<Item> r = ordenamiento.quickSort(itemsSemilla(), POR_PESO);
        assertEquals(List.of("I4", "I2", "I1", "I3", "I5"), ids(r));
    }

    @Test
    @DisplayName("QuickSort ordena por valor y por ratio")
    void quickSortOrdenaPorValorYRatio() {
        // valores 10,40,30,50,55 -> I1, I3, I2, I4, I5
        assertEquals(List.of("I1", "I3", "I2", "I4", "I5"),
                ids(ordenamiento.quickSort(itemsSemilla(), POR_VALOR)));
        // ratios 2.00, 10.00, 5.00, 16.67, 7.86 -> I1, I3, I5, I2, I4
        assertEquals(List.of("I1", "I3", "I5", "I2", "I4"),
                ids(ordenamiento.quickSort(itemsSemilla(), POR_RATIO)));
    }

    @Test
    @DisplayName("QuickSort no modifica la lista de entrada")
    void quickSortNoModificaLaEntrada() {
        List<Item> entrada = itemsSemilla();
        List<String> antes = ids(entrada);

        List<Item> salida = ordenamiento.quickSort(entrada, POR_PESO);

        assertEquals(antes, ids(entrada), "la lista original tiene que quedar intacta");
        assertNotSame(entrada, salida, "tiene que devolver una lista nueva");
    }

    @Test
    @DisplayName("QuickSort resuelve el caso de muchas claves repetidas (particion de 3 vias)")
    void quickSortConClavesRepetidas() {
        // Todos con el mismo peso: es el caso que la particion de dos vias
        // degrada a O(n^2) y que la de tres vias resuelve en una pasada.
        List<Item> items = new ArrayList<>();
        for (int i = 0; i < 500; i++) {
            items.add(new Item("R" + i, "repetido", 7, 7, null));
        }
        List<Item> r = ordenamiento.quickSort(items, POR_PESO);

        assertEquals(500, r.size());
        assertTrue(estaOrdenada(r, POR_PESO));
        assertTrue(esPermutacion(items, r));
    }

    // ------------------------------------------------------------------
    // MergeSort
    // ------------------------------------------------------------------

    @Test
    @DisplayName("MergeSort ordena los items de la semilla por peso")
    void mergeSortOrdenaPorPeso() {
        List<Item> r = ordenamiento.mergeSort(itemsSemilla(), POR_PESO);
        assertEquals(List.of("I4", "I2", "I1", "I3", "I5"), ids(r));
    }

    @Test
    @DisplayName("MergeSort ordena por valor y por ratio")
    void mergeSortOrdenaPorValorYRatio() {
        assertEquals(List.of("I1", "I3", "I2", "I4", "I5"),
                ids(ordenamiento.mergeSort(itemsSemilla(), POR_VALOR)));
        assertEquals(List.of("I1", "I3", "I5", "I2", "I4"),
                ids(ordenamiento.mergeSort(itemsSemilla(), POR_RATIO)));
    }

    @Test
    @DisplayName("MergeSort no modifica la lista de entrada")
    void mergeSortNoModificaLaEntrada() {
        List<Item> entrada = itemsSemilla();
        List<String> antes = ids(entrada);

        List<Item> salida = ordenamiento.mergeSort(entrada, POR_PESO);

        assertEquals(antes, ids(entrada));
        assertNotSame(entrada, salida);
    }

    @Test
    @DisplayName("MergeSort es estable: los empates mantienen el orden de entrada")
    void mergeSortEsEstable() {
        // Todos pesan 5. Ordenando POR PESO, la salida tiene que respetar el
        // orden en que entraron, porque ninguno es menor que otro.
        List<Item> items = new ArrayList<>();
        for (int i = 0; i < 60; i++) {
            items.add(new Item("E" + i, "empate", 5, i, null));
        }
        List<Item> r = ordenamiento.mergeSort(items, POR_PESO);

        assertEquals(ids(items), ids(r), "MergeSort tiene que ser estable");
    }

    // ------------------------------------------------------------------
    // Casos borde y verificacion sobre entradas grandes
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Los dos algoritmos aguantan lista vacia y de un solo elemento")
    void casosBorde() {
        assertTrue(ordenamiento.quickSort(List.of(), POR_PESO).isEmpty());
        assertTrue(ordenamiento.mergeSort(List.of(), POR_PESO).isEmpty());

        List<Item> uno = List.of(new Item("U1", "unico", 1, 1, null));
        assertEquals(List.of("U1"), ids(ordenamiento.quickSort(uno, POR_PESO)));
        assertEquals(List.of("U1"), ids(ordenamiento.mergeSort(uno, POR_PESO)));
    }

    @Test
    @DisplayName("Criterio null da error de argumento, no NullPointerException")
    void criterioNullFalla() {
        List<Item> items = itemsSemilla();
        assertThrows(IllegalArgumentException.class, () -> ordenamiento.quickSort(items, null));
        assertThrows(IllegalArgumentException.class, () -> ordenamiento.mergeSort(items, null));
    }

    @Test
    @DisplayName("Sobre 2000 items al azar los dos ordenan bien y dan lo mismo")
    void entradaGrandeAlAzar() {
        Random azar = new Random(12);   // semilla fija: el test es reproducible
        List<Item> items = new ArrayList<>();
        for (int i = 0; i < 2000; i++) {
            items.add(new Item("X" + i, "x", 1 + azar.nextInt(50), azar.nextInt(500), null));
        }

        List<Item> porQuick = ordenamiento.quickSort(items, POR_VALOR);
        List<Item> porMerge = ordenamiento.mergeSort(items, POR_VALOR);

        assertTrue(estaOrdenada(porQuick, POR_VALOR), "QuickSort dejo la lista desordenada");
        assertTrue(estaOrdenada(porMerge, POR_VALOR), "MergeSort dejo la lista desordenada");
        assertTrue(esPermutacion(items, porQuick), "QuickSort perdio o duplico items");
        assertTrue(esPermutacion(items, porMerge), "MergeSort perdio o duplico items");
    }

    @Test
    @DisplayName("Entrada ya ordenada y entrada al reves: el peor caso clasico no aparece")
    void entradasOrdenadaYAlReves() {
        // Con pivote fijo estas dos entradas disparan el O(n^2) de QuickSort.
        // Con mediana de tres el pivote cae en el centro y no pasa: si este
        // test tarda mas que un parpadeo, la eleccion del pivote esta mal.
        int n = 3000;
        List<Item> ascendente = new ArrayList<>();
        List<Item> descendente = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            ascendente.add(new Item("A" + i, "a", i + 1, i, null));
            descendente.add(new Item("D" + i, "d", n - i, i, null));
        }

        assertTrue(estaOrdenada(ordenamiento.quickSort(ascendente, POR_PESO), POR_PESO));
        assertTrue(estaOrdenada(ordenamiento.quickSort(descendente, POR_PESO), POR_PESO));
        assertTrue(estaOrdenada(ordenamiento.mergeSort(ascendente, POR_PESO), POR_PESO));
        assertTrue(estaOrdenada(ordenamiento.mergeSort(descendente, POR_PESO), POR_PESO));
    }

    // ------------------------------------------------------------------
    // Verificadores
    // ------------------------------------------------------------------

    /** Una lista esta ordenada si ningun elemento es menor que el anterior. */
    private boolean estaOrdenada(List<Item> items, Comparator<Item> criterio) {
        for (int i = 1; i < items.size(); i++) {
            if (criterio.compare(items.get(i - 1), items.get(i)) > 0) return false;
        }
        return true;
    }

    /**
     * La salida tiene que tener exactamente los mismos objetos que la entrada,
     * sin perder ni duplicar ninguno. Se comparan por identidad de referencia.
     */
    private boolean esPermutacion(List<Item> entrada, List<Item> salida) {
        if (entrada.size() != salida.size()) return false;
        List<Item> pendientes = new ArrayList<>(entrada);
        for (Item i : salida) {
            boolean encontrado = false;
            for (int k = 0; k < pendientes.size(); k++) {
                if (pendientes.get(k) == i) {
                    pendientes.remove(k);
                    encontrado = true;
                    break;
                }
            }
            if (!encontrado) return false;
        }
        return pendientes.isEmpty();
    }
}
