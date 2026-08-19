package uade.prog3.tpo.algorithm;

import org.springframework.stereotype.Component;
import uade.prog3.tpo.model.Item;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * UNIDAD: Divide y Conquista (clase 2)     PUNTAJE: 1 punto
 *
 * IMPORTANTE: no vale llamar a Collections.sort ni a Arrays.sort.
 * El punto se asigna por implementar el algoritmo, no por usarlo.
 */
@Component
public class Ordenamiento {

    /**
     * QuickSort sobre una lista de items.
     *
     * Estrategia de pivote: se usa el ULTIMO elemento del sub-rango como pivote
     * (Lomuto partition scheme). Es la mas simple de implementar y explicar,
     * aunque degrada a O(n^2) si la lista ya viene ordenada o casi ordenada,
     * porque cada particion queda muy desbalanceada (un lado con 0 elementos
     * y el otro con n-1). Una alternativa mas robusta seria mediana de tres
     * (primero, medio, ultimo) para evitar ese peor caso en datos ya ordenados.
     *
     * Requisitos:
     *   - implementar la particion (partition) a mano
     *   - documentar que estrategia de pivote usaron y por que
     *   - NO modificar la lista de entrada: devolver una lista nueva
     *
     * Complejidad esperada: O(n log n) promedio, O(n^2) peor caso.
     * Peor caso: ocurre cuando el pivote elegido siempre termina siendo el
     * minimo o el maximo del sub-rango (ej: lista ya ordenada con pivote =
     * ultimo elemento), generando particiones de tamano 0 y n-1 en cada paso,
     * lo que da n niveles de recursion en vez de log n.
     */
    public List<Item> quickSort(List<Item> items, Comparator<Item> criterio) {
        List<Item> copia = new ArrayList<>(items); // no modificamos la entrada
        quickSortRec(copia, 0, copia.size() - 1, criterio);
        return copia;
    }

    private void quickSortRec(List<Item> lista, int desde, int hasta, Comparator<Item> criterio) {
        if (desde >= hasta) {
            return; // 0 o 1 elemento: ya esta ordenado, caso base
        }
        int posPivote = partition(lista, desde, hasta, criterio);
        quickSortRec(lista, desde, posPivote - 1, criterio);
        quickSortRec(lista, posPivote + 1, hasta, criterio);
    }

    /**
     * Particion estilo Lomuto: usa el ultimo elemento como pivote,
     * recorre el rango moviendo los elementos menores al pivote hacia
     * la izquierda, y al final coloca el pivote en su posicion final.
     * Devuelve el indice donde quedo el pivote.
     */
    private int partition(List<Item> lista, int desde, int hasta, Comparator<Item> criterio) {
        Item pivote = lista.get(hasta);
        int i = desde - 1; // frontera del bloque "menor o igual al pivote"

        for (int j = desde; j < hasta; j++) {
            if (criterio.compare(lista.get(j), pivote) <= 0) {
                i++;
                intercambiar(lista, i, j);
            }
        }
        intercambiar(lista, i + 1, hasta); // pivote a su posicion final
        return i + 1;
    }

    private void intercambiar(List<Item> lista, int a, int b) {
        Item temp = lista.get(a);
        lista.set(a, lista.get(b));
        lista.set(b, temp);
    }

    /**
     * MergeSort sobre una lista de items.
     *
     * Requisitos:
     *   - implementar la mezcla (merge) a mano
     *   - respetar la estabilidad del algoritmo
     *
     * Complejidad esperada: O(n log n) siempre.
     * Recurrencia: T(n) = 2T(n/2) + O(n)
     * Resolucion por teorema general (a=2, b=2, f(n)=O(n)):
     *   n^(log_b a) = n^(log_2 2) = n^1 = n
     *   f(n) = O(n) coincide con n^(log_b a) -> caso 2 del teorema general
     *   => T(n) = O(n^(log_b a) * log n) = O(n log n)
     *
     * Estabilidad: en el merge, cuando dos elementos son iguales segun el
     * criterio, siempre se toma primero el de la mitad IZQUIERDA. Eso
     * preserva el orden relativo original entre elementos "empatados".
     */
    public List<Item> mergeSort(List<Item> items, Comparator<Item> criterio) {
        if (items.size() <= 1) {
            return new ArrayList<>(items);
        }

        int medio = items.size() / 2;
        List<Item> izquierda = mergeSort(items.subList(0, medio), criterio);
        List<Item> derecha = mergeSort(items.subList(medio, items.size()), criterio);

        return merge(izquierda, derecha, criterio);
    }

    private List<Item> merge(List<Item> izquierda, List<Item> derecha, Comparator<Item> criterio) {
        List<Item> resultado = new ArrayList<>(izquierda.size() + derecha.size());
        int i = 0, j = 0;

        while (i < izquierda.size() && j < derecha.size()) {
            // <= en vez de < para asegurar estabilidad: ante empate, gana la izquierda
            if (criterio.compare(izquierda.get(i), derecha.get(j)) <= 0) {
                resultado.add(izquierda.get(i));
                i++;
            } else {
                resultado.add(derecha.get(j));
                j++;
            }
        }
        while (i < izquierda.size()) {
            resultado.add(izquierda.get(i));
            i++;
        }
        while (j < derecha.size()) {
            resultado.add(derecha.get(j));
            j++;
        }
        return resultado;
    }
}