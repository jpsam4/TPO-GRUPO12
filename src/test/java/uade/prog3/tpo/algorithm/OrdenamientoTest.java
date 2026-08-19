package uade.prog3.tpo.algorithm;

import org.junit.jupiter.api.Test;
import uade.prog3.tpo.model.Item;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OrdenamientoTest {

    private final Ordenamiento ordenamiento = new Ordenamiento();

    // Comparador por valor, ascendente. Es el criterio que le pasamos
    // a los dos algoritmos en todos los tests.
    private final Comparator<Item> porValor = Comparator.comparingDouble(Item::getValor);

    private List<Item> itemsDesordenados() {
        List<Item> items = new ArrayList<>();
        items.add(new Item("i1", "Item1", 2, 50, "A"));
        items.add(new Item("i2", "Item2", 3, 10, "B"));
        items.add(new Item("i3", "Item3", 1, 80, "A"));
        items.add(new Item("i4", "Item4", 5, 30, "C"));
        items.add(new Item("i5", "Item5", 4, 60, "B"));
        return items;
    }

    // ---------- QuickSort ----------

    @Test
    void quickSortOrdenaAscendentePorValor() {
        List<Item> resultado = ordenamiento.quickSort(itemsDesordenados(), porValor);

        List<Double> valores = resultado.stream().map(Item::getValor).toList();
        assertEquals(List.of(10.0, 30.0, 50.0, 60.0, 80.0), valores);
    }

    @Test
    void quickSortNoModificaLaListaOriginal() {
        List<Item> original = itemsDesordenados();
        List<Item> copiaParaComparar = new ArrayList<>(original);

        ordenamiento.quickSort(original, porValor);

        // La lista pasada como parametro debe seguir en el mismo orden que antes
        for (int i = 0; i < original.size(); i++) {
            assertEquals(copiaParaComparar.get(i).getId(), original.get(i).getId());
        }
    }

    @Test
    void quickSortConListaVaciaDevuelveListaVacia() {
        List<Item> resultado = ordenamiento.quickSort(new ArrayList<>(), porValor);
        assertTrue(resultado.isEmpty());
    }

    @Test
    void quickSortConUnSoloElementoLoDevuelveIgual() {
        List<Item> unSolo = List.of(new Item("i1", "Unico", 1, 99, "A"));
        List<Item> resultado = ordenamiento.quickSort(unSolo, porValor);
        assertEquals(1, resultado.size());
        assertEquals("i1", resultado.get(0).getId());
    }

    // ---------- MergeSort ----------

    @Test
    void mergeSortOrdenaAscendentePorValor() {
        List<Item> resultado = ordenamiento.mergeSort(itemsDesordenados(), porValor);

        List<Double> valores = resultado.stream().map(Item::getValor).toList();
        assertEquals(List.of(10.0, 30.0, 50.0, 60.0, 80.0), valores);
    }

    @Test
    void mergeSortNoModificaLaListaOriginal() {
        List<Item> original = itemsDesordenados();
        List<Item> copiaParaComparar = new ArrayList<>(original);

        ordenamiento.mergeSort(original, porValor);

        for (int i = 0; i < original.size(); i++) {
            assertEquals(copiaParaComparar.get(i).getId(), original.get(i).getId());
        }
    }

    @Test
    void mergeSortEsEstableAnteValoresEmpatados() {
        // Dos items con el mismo valor (50): tienen que mantener su orden relativo
        // original (i1 antes que i2) despues de ordenar.
        List<Item> items = new ArrayList<>();
        items.add(new Item("i1", "Primero", 2, 50, "A"));
        items.add(new Item("i2", "Segundo", 3, 50, "B"));
        items.add(new Item("i3", "Tercero", 1, 20, "A"));

        List<Item> resultado = ordenamiento.mergeSort(items, porValor);

        // Orden esperado: i3 (20), i1 (50), i2 (50) -> i1 antes que i2 por estabilidad
        assertEquals("i3", resultado.get(0).getId());
        assertEquals("i1", resultado.get(1).getId());
        assertEquals("i2", resultado.get(2).getId());
    }

    @Test
    void mergeSortConListaVaciaDevuelveListaVacia() {
        List<Item> resultado = ordenamiento.mergeSort(new ArrayList<>(), porValor);
        assertTrue(resultado.isEmpty());
    }
}