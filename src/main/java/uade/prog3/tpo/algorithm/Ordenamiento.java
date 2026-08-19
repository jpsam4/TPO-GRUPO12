package uade.prog3.tpo.algorithm;

import org.springframework.stereotype.Component;
import uade.prog3.tpo.model.Item;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * UNIDAD: Divide y Conquista (clase 2)     PUNTAJE: 1 punto
 *
 * Implementacion propia de QuickSort y MergeSort. No se usa Collections.sort
 * ni Arrays.sort en ningun lado: el punto se asigna por implementar el
 * algoritmo, no por llamarlo.
 *
 * Las dos rutinas publicas trabajan igual:
 *   1. copian la lista de entrada a un arreglo (la entrada NUNCA se modifica)
 *   2. ordenan el arreglo en el lugar
 *   3. devuelven una lista nueva
 *
 * Se ordena sobre un arreglo y no sobre la List directamente porque el acceso
 * por indice de un arreglo es O(1) garantizado. Sobre una List generica no lo
 * es: con una LinkedList cada get(i) seria O(n) y ambos algoritmos se
 * degradarian a O(n^2 log n) sin que se note en el codigo.
 */
@Component
public class Ordenamiento {

    /**
     * Por debajo de este tamano conviene ordenar por insercion en lugar de
     * seguir partiendo. Para tramos chicos la insercion hace menos trabajo real
     * que el manejo de la recursion, aunque su orden sea peor: O(n^2) con n<=12
     * es menos operaciones que O(n log n) con la constante de la recursion.
     * No cambia la complejidad asintotica de ninguno de los dos algoritmos.
     */
    private static final int UMBRAL_INSERCION = 12;

    // ------------------------------------------------------------------
    // QuickSort
    // ------------------------------------------------------------------

    /**
     * QuickSort sobre una lista de items.
     *
     * Estrategia de pivote: MEDIANA DE TRES (primero, medio, ultimo).
     * Se eligio esa y no "el primer elemento" porque el peor caso de QuickSort
     * aparece cuando el pivote queda siempre en un extremo de la particion, y
     * con pivote fijo eso pasa justo con la entrada mas comun en la practica:
     * una lista ya ordenada o ordenada al reves. Tomando la mediana de tres,
     * sobre esas entradas el pivote cae exactamente en el centro y el
     * comportamiento pasa a ser el mejor caso en lugar del peor.
     *
     * Particion de TRES VIAS (esquema de la bandera holandesa): deja
     *     [menores | iguales al pivote | mayores]
     * y los iguales quedan fuera de la recursion. Importa en este dominio
     * porque los criterios de orden son peso, valor y ratio: sobre un conjunto
     * real de items hay muchos empates, y con particion de dos vias las claves
     * repetidas se vuelven a comparar en cada nivel. Con tres vias una lista
     * entera de claves iguales se resuelve en O(n).
     *
     * Complejidad temporal: O(n log n) en promedio, O(n^2) en el peor caso.
     *   El peor caso ocurre cuando en cada nivel el pivote es el minimo o el
     *   maximo del tramo: la particion divide en 0 y n-1, hay n niveles y cada
     *   uno cuesta O(n). Con mediana de tres hace falta una entrada armada a
     *   proposito contra el metodo de eleccion del pivote; no se da por azar
     *   ni con datos ordenados.
     * Complejidad espacial: O(log n).
     *   Se ordena en el lugar. La pila de recursion se acota recursando siempre
     *   sobre el tramo MAS CHICO e iterando sobre el mas grande: asi cada marco
     *   apilado corresponde a un tramo de a lo sumo la mitad, y no puede haber
     *   mas de log2(n) marcos ni siquiera en el peor caso.
     */
    public List<Item> quickSort(List<Item> items, Comparator<Item> criterio) {
        Item[] a = copiar(items, criterio);
        if (a.length > 1) {
            quickSort(a, 0, a.length - 1, criterio);
        }
        return aLista(a);
    }

    /** Ordena a[desde..hasta] (ambos inclusive). */
    private void quickSort(Item[] a, int desde, int hasta, Comparator<Item> criterio) {
        // Bucle en lugar de una segunda llamada recursiva: es la eliminacion de
        // la recursion de cola que mantiene la pila en O(log n).
        while (desde < hasta) {

            if (hasta - desde + 1 <= UMBRAL_INSERCION) {
                insercion(a, desde, hasta, criterio);
                return;
            }

            Item pivote = a[medianaDeTres(a, desde, hasta, criterio)];

            // Particion de tres vias. Invariante mientras se recorre:
            //   a[desde .. menor-1]  <  pivote
            //   a[menor .. i-1]      == pivote
            //   a[i     .. mayor]    todavia sin clasificar
            //   a[mayor+1 .. hasta]  >  pivote
            int menor = desde, i = desde, mayor = hasta;
            while (i <= mayor) {
                int cmp = criterio.compare(a[i], pivote);
                if (cmp < 0) {
                    intercambiar(a, menor++, i++);
                } else if (cmp > 0) {
                    // Lo que viene de la derecha todavia no se miro: no se avanza i.
                    intercambiar(a, i, mayor--);
                } else {
                    i++;
                }
            }
            // El bloque a[menor..mayor] ya esta en su posicion final: son todos
            // iguales al pivote y no se vuelven a tocar.

            if (menor - desde < hasta - mayor) {
                quickSort(a, desde, menor - 1, criterio);   // recursion: tramo chico
                desde = mayor + 1;                          // iteracion: tramo grande
            } else {
                quickSort(a, mayor + 1, hasta, criterio);
                hasta = menor - 1;
            }
        }
    }

    /**
     * Deja a[desde] <= a[medio] <= a[hasta] y devuelve la posicion de la
     * mediana. Ordenar los tres de paso no es gratis pero tampoco se
     * desperdicia: ubica dos elementos del lado que les corresponde.
     */
    private int medianaDeTres(Item[] a, int desde, int hasta, Comparator<Item> criterio) {
        int medio = desde + ((hasta - desde) >>> 1);   // >>> 1 evita el desborde de (desde+hasta)/2
        if (criterio.compare(a[medio], a[desde]) < 0) intercambiar(a, medio, desde);
        if (criterio.compare(a[hasta], a[desde]) < 0) intercambiar(a, hasta, desde);
        if (criterio.compare(a[hasta], a[medio]) < 0) intercambiar(a, hasta, medio);
        return medio;
    }

    private void intercambiar(Item[] a, int i, int j) {
        Item tmp = a[i];
        a[i] = a[j];
        a[j] = tmp;
    }

    // ------------------------------------------------------------------
    // MergeSort
    // ------------------------------------------------------------------

    /**
     * MergeSort sobre una lista de items.
     *
     * Es ESTABLE: dos items que empatan en el criterio quedan en el mismo orden
     * relativo que tenian en la entrada. Eso se sostiene en un unico punto, la
     * mezcla, donde ante un empate se toma siempre el elemento de la mitad
     * izquierda (ver mezclar). Sirve, por ejemplo, para ordenar por peso y que
     * entre items del mismo peso se conserve el orden por id que traia la base.
     *
     * Recurrencia: T(n) = 2 T(n/2) + O(n).
     *   Con la regla practica a=2, b=2, k=1: como a == b^k, cae en el caso
     *   T(n) = O(n^k log n) = O(n log n). Vale SIEMPRE, no en promedio: la
     *   division es por posicion y no depende de los datos, asi que no existe
     *   una entrada que lo empeore. Esa es la diferencia de fondo con QuickSort,
     *   que parte segun el valor del pivote y por eso tiene peor caso O(n^2).
     *
     * Complejidad espacial: O(n) por el arreglo auxiliar. Es el precio que se
     *   paga por la estabilidad y por el peor caso garantizado.
     *
     * Dos detalles de implementacion que evitan trabajo al pedo:
     *   - el auxiliar se reserva UNA sola vez, no uno por llamada. Reservarlo
     *     en cada mezcla no cambia el orden pero multiplica las reservas de
     *     memoria por n y es el error mas comun en este algoritmo.
     *   - se alternan los roles de los dos arreglos entre niveles (el destino
     *     de un nivel es la fuente del siguiente), lo que ahorra copiar el
     *     tramo entero de vuelta despues de cada mezcla.
     */
    public List<Item> mergeSort(List<Item> items, Comparator<Item> criterio) {
        Item[] destino = copiar(items, criterio);
        if (destino.length > 1) {
            Item[] auxiliar = destino.clone();
            mergeSort(auxiliar, destino, 0, destino.length - 1, criterio);
        }
        return aLista(destino);
    }

    /**
     * Ordena el tramo [desde..hasta] tomando los datos de {@code fuente} y
     * dejando el resultado ordenado en {@code destino}.
     *
     * Precondicion: fuente y destino contienen los mismos elementos en ese
     * tramo. Se cumple al entrar (uno es copia del otro) y se mantiene en cada
     * nivel, que es lo que permite alternar los roles sin copiar.
     */
    private void mergeSort(Item[] fuente, Item[] destino, int desde, int hasta, Comparator<Item> criterio) {
        if (hasta - desde + 1 <= UMBRAL_INSERCION) {
            // destino ya tiene los mismos elementos, se ordena ahi directamente.
            // La insercion tambien es estable, asi que no rompe la estabilidad.
            insercion(destino, desde, hasta, criterio);
            return;
        }

        int medio = desde + ((hasta - desde) >>> 1);

        // Roles invertidos a proposito: lo ordenado queda en fuente, listo para
        // que la mezcla de este nivel lo lea y escriba en destino.
        mergeSort(destino, fuente, desde, medio, criterio);
        mergeSort(destino, fuente, medio + 1, hasta, criterio);

        // Si el ultimo de la izquierda ya es <= al primero de la derecha, las
        // dos mitades juntas YA estan ordenadas y la mezcla seria copiar al
        // pedo comparando. Con esto una entrada ya ordenada cuesta O(n log n)
        // de recorrido pero sin ninguna comparacion de mezcla.
        if (criterio.compare(fuente[medio], fuente[medio + 1]) <= 0) {
            System.arraycopy(fuente, desde, destino, desde, hasta - desde + 1);
            return;
        }

        mezclar(fuente, destino, desde, medio, hasta, criterio);
    }

    /**
     * Mezcla los tramos ordenados fuente[desde..medio] y fuente[medio+1..hasta]
     * dejando el resultado en destino[desde..hasta]. O(n) y una sola pasada.
     */
    private void mezclar(Item[] fuente, Item[] destino,
                         int desde, int medio, int hasta, Comparator<Item> criterio) {
        int i = desde;       // cabezal de la mitad izquierda
        int j = medio + 1;   // cabezal de la mitad derecha

        for (int k = desde; k <= hasta; k++) {
            if (i > medio) {
                destino[k] = fuente[j++];                       // se agoto la izquierda
            } else if (j > hasta) {
                destino[k] = fuente[i++];                       // se agoto la derecha
            } else if (criterio.compare(fuente[j], fuente[i]) < 0) {
                destino[k] = fuente[j++];                       // la derecha es ESTRICTAMENTE menor
            } else {
                destino[k] = fuente[i++];                       // empate -> izquierda: ACA vive la estabilidad
            }
        }
    }

    // ------------------------------------------------------------------
    // Auxiliares comunes
    // ------------------------------------------------------------------

    /**
     * Ordenamiento por insercion de a[desde..hasta]. Se usa como caso base de
     * los dos algoritmos. Es estable porque solo corre elementos cuando son
     * ESTRICTAMENTE mayores que el que se esta ubicando: ante un empate se
     * frena y el que ya estaba queda adelante.
     */
    private void insercion(Item[] a, int desde, int hasta, Comparator<Item> criterio) {
        for (int i = desde + 1; i <= hasta; i++) {
            Item actual = a[i];
            int j = i - 1;
            while (j >= desde && criterio.compare(a[j], actual) > 0) {
                a[j + 1] = a[j];
                j--;
            }
            a[j + 1] = actual;
        }
    }

    /** Copia la lista de entrada a un arreglo. La lista original no se toca. */
    private Item[] copiar(List<Item> items, Comparator<Item> criterio) {
        if (criterio == null) {
            throw new IllegalArgumentException("El criterio de comparacion no puede ser null");
        }
        if (items == null || items.isEmpty()) {
            return new Item[0];
        }
        Item[] a = new Item[items.size()];
        int i = 0;
        for (Item item : items) {
            a[i++] = item;
        }
        return a;
    }

    private List<Item> aLista(Item[] a) {
        List<Item> salida = new ArrayList<>(a.length);
        for (Item item : a) {
            salida.add(item);
        }
        return salida;
    }
}
