package uade.prog3.tpo.algorithm;

import org.springframework.stereotype.Component;
import uade.prog3.tpo.service.Grafo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.PriorityQueue;

/**
 * UNIDAD: Grafos II (clase 5)
 * PUNTAJE: forma parte de los 3 puntos de "Dijkstra, Prim, Kruskal"
 *
 * ATENCION: el arbol de recubrimiento minimo solo tiene sentido sobre un
 * grafo NO DIRIGIDO y CONEXO. Al cargar el grafo hay que pedirlo con
 * grafoService.cargar(false).
 */
@Component
public class ArbolRecubrimiento {

    /** Una arista del arbol resultante, con ids de negocio. */
    public record AristaMst(String origen, String destino, double costo) { }

    /** Arbol de recubrimiento minimo completo. */
    public record Mst(List<AristaMst> aristas, double costoTotal) { }

    /**
     * Candidato en la cola de prioridad de Prim: un vertice fuera del arbol y
     * el costo de la arista mas barata que lo conecta al arbol actual (key[v]).
     */
    private record CandidatoPrim(int vertice, double costo) implements Comparable<CandidatoPrim> {
        @Override
        public int compareTo(CandidatoPrim otro) {
            int cmp = Double.compare(this.costo, otro.costo);
            return (cmp != 0) ? cmp : Integer.compare(this.vertice, otro.vertice);
        }
    }

    /** Arista no dirigida con indices internos para Kruskal. */
    private record AristaInterna(int u, int v, double costo) {
        int compararCon(AristaInterna otra) {
            int cmp = Double.compare(this.costo, otra.costo);
            if (cmp != 0) return cmp;
            cmp = Integer.compare(this.u, otra.u);
            return (cmp != 0) ? cmp : Integer.compare(this.v, otra.v);
        }
    }

    /**
     * Prim: hace crecer un unico arbol desde un vertice inicial, tomando en
     * cada paso la arista de menor costo que cruza desde los vertices ya
     * incorporados (inMST[u] == true) hacia los que todavia estan afuera.
     *
     * Implementacion elegida: cola de prioridad con heap binario (PriorityQueue)
     * apoyada en los tres arreglos clasicos:
     *   - key[v]:    menor costo de una arista que conecta a v con el arbol actual
     *   - parent[v]: vertice del arbol desde el cual se alcanza ese key[v]
     *   - inMST[v]:  true cuando v ya fue incorporado al arbol
     *
     * Por que heap y no busqueda lineal:
     *   El reino de Valdoria es un grafo ralo (38 zonas y 57 caminos, E ~ 1.5 V,
     *   muy lejos del grafo denso de V*(V-1)/2 = 703 aristas). Con heap binario
     *   el costo es O((V + E) log V) — o O(E log V) por ser conexo — frente al
     *   O(V^2) de recorrer linealmente key[] en cada uno de los V pasos.
     *
     * Complejidad temporal: O(E log V) sobre un grafo conexo con lista de
     *   adyacencia y heap binario.
     * Complejidad espacial: O(V + E) por los arreglos key[], parent[], inMST[]
     *   de tamano V y la cola de prioridad.
     */
    public Mst prim(Grafo grafo, String origenId) {
        validarNoDirigido(grafo);
        int origen = grafo.indiceDe(origenId);     // si no existe: IllegalArgumentException -> 400
        int n = grafo.cantidadVertices();

        double[] key = new double[n];
        int[] parent = new int[n];
        boolean[] inMST = new boolean[n];
        Arrays.fill(key, Grafo.INFINITO);
        Arrays.fill(parent, -1);

        PriorityQueue<CandidatoPrim> cola = new PriorityQueue<>();
        key[origen] = 0.0;
        cola.add(new CandidatoPrim(origen, 0.0));

        List<AristaMst> aristas = new ArrayList<>(Math.max(0, n - 1));
        double costoTotal = 0.0;
        int incorporados = 0;

        while (!cola.isEmpty()) {
            CandidatoPrim actual = cola.poll();
            int u = actual.vertice();

            if (inMST[u]) {
                continue;   // entrada vieja desplazada por una arista mas barata hacia u
            }
            inMST[u] = true;
            incorporados++;

            if (parent[u] != -1) {
                aristas.add(new AristaMst(grafo.idDe(parent[u]), grafo.idDe(u), key[u]));
                costoTotal += key[u];
            }

            for (Grafo.Arista a : grafo.vecinos(u)) {
                int v = a.destino();
                if (!inMST[v] && a.costo() < key[v]) {
                    key[v] = a.costo();
                    parent[v] = u;
                    cola.add(new CandidatoPrim(v, key[v]));
                }
            }
        }

        if (incorporados < n) {
            throw new IllegalArgumentException(
                    "El grafo no es conexo: Prim alcanzo " + incorporados
                            + " de " + n + " vertices desde " + origenId);
        }

        return new Mst(aristas, costoTotal);
    }

    /**
     * Kruskal: ordena TODAS las aristas por costo ascendente y las recorre
     * agregando cada arista cuyos extremos pertenezcan a componentes conexas
     * distintas (es decir, que no forme ciclo), hasta reunir V-1 aristas.
     *
     * Para saber en tiempo casi constante si dos vertices ya estan conectados
     * se implementa a mano una estructura Union-Find (conjuntos disjuntos) con:
     *   - compresion de caminos en find(u): aplana el arbol al buscar la raiz
     *   - union por rango en union(u, v): cuelga el arbol mas bajo del mas alto
     *
     * El ordenamiento de las E aristas tambien se resuelve a mano (MergeSort
     * propio sobre el arreglo de aristas), sin Collections.sort ni Arrays.sort.
     *
     * Complejidad temporal: O(E log E) = O(E log V). Ordenar las E aristas
     *   cuesta O(E log E) y domina a las 2E operaciones find y V-1 operaciones
     *   union sobre el Union-Find, que con compresion de caminos y union por
     *   rango cuestan O(E * alpha(V)), practicamene lineal.
     * Complejidad espacial: O(V + E). Arreglos padre[] y rango[] de tamano V,
     *   mas el arreglo de E aristas y su auxiliar para el ordenamiento.
     *
     * Para el coloquio (Prim vs Kruskal segun la densidad):
     *   - Kruskal conviene en grafos ralos (E ~ V) o cuando las aristas ya
     *     vienen como lista/ordenadas: ordenar pocas aristas es muy simple.
     *   - Prim conviene en grafos densos (E ~ V^2): con matriz/busqueda lineal
     *     cuesta O(V^2) sin pagar el log V del heap ni ordenar V^2 aristas en
     *     O(V^2 log V) como haria Kruskal.
     */
    public Mst kruskal(Grafo grafo) {
        validarNoDirigido(grafo);
        int n = grafo.cantidadVertices();
        if (n == 0) {
            return new Mst(List.of(), 0.0);
        }

        // 1) Recolectar cada arista no dirigida una sola vez (u < v).
        List<AristaInterna> listaAristas = new ArrayList<>();
        for (int u = 0; u < n; u++) {
            for (Grafo.Arista a : grafo.vecinos(u)) {
                int v = a.destino();
                if (u < v) {
                    listaAristas.add(new AristaInterna(u, v, a.costo()));
                }
            }
        }

        // 2) Ordenar las aristas por costo ascendente (MergeSort propio, O(E log E)).
        AristaInterna[] aristas = listaAristas.toArray(new AristaInterna[0]);
        if (aristas.length > 1) {
            AristaInterna[] aux = aristas.clone();
            ordenarAristas(aux, aristas, 0, aristas.length - 1);
        }

        // 3) Inicializar Union-Find: cada vertice arranca en su propio conjunto.
        int[] padre = new int[n];
        int[] rango = new int[n];
        for (int i = 0; i < n; i++) {
            padre[i] = i;
        }

        // 4) Recorrer las aristas en orden y agregar las que no formen ciclo.
        List<AristaMst> elegidas = new ArrayList<>(Math.max(0, n - 1));
        double costoTotal = 0.0;

        for (AristaInterna a : aristas) {
            if (find(padre, a.u()) != find(padre, a.v())) {
                union(padre, rango, a.u(), a.v());
                elegidas.add(new AristaMst(grafo.idDe(a.u()), grafo.idDe(a.v()), a.costo()));
                costoTotal += a.costo();
                if (elegidas.size() == n - 1) {
                    break;   // un arbol sobre V vertices tiene exactamente V-1 aristas
                }
            }
        }

        if (elegidas.size() < n - 1) {
            throw new IllegalArgumentException(
                    "El grafo no es conexo: Kruskal solo pudo unir "
                            + (elegidas.size() + 1) + " de " + n + " vertices");
        }

        return new Mst(elegidas, costoTotal);
    }

    // ------------------------------------------------------------------
    // Union-Find (conjuntos disjuntos) implementado a mano
    // ------------------------------------------------------------------

    /**
     * Devuelve el representante (raiz) del conjunto al que pertenece x.
     * Aplica compresion de caminos: al volver de la recursion, engancha cada
     * nodo visitado directamente de la raiz.
     */
    private int find(int[] padre, int x) {
        if (padre[x] != x) {
            padre[x] = find(padre, padre[x]);
        }
        return padre[x];
    }

    /**
     * Une los conjuntos que contienen a x e y usando union por rango:
     * la raiz de menor rango pasa a ser hija de la de mayor rango.
     */
    private void union(int[] padre, int[] rango, int x, int y) {
        int raizX = find(padre, x);
        int raizY = find(padre, y);
        if (raizX == raizY) {
            return;
        }
        if (rango[raizX] < rango[raizY]) {
            padre[raizX] = raizY;
        } else if (rango[raizX] > rango[raizY]) {
            padre[raizY] = raizX;
        } else {
            padre[raizY] = raizX;
            rango[raizX]++;
        }
    }

    // ------------------------------------------------------------------
    // Ordenamiento propio de aristas para Kruskal (MergeSort O(E log E))
    // ------------------------------------------------------------------

    private void ordenarAristas(AristaInterna[] fuente, AristaInterna[] destino, int desde, int hasta) {
        if (desde >= hasta) {
            return;
        }
        int medio = desde + ((hasta - desde) >>> 1);
        ordenarAristas(destino, fuente, desde, medio);
        ordenarAristas(destino, fuente, medio + 1, hasta);

        int i = desde;
        int j = medio + 1;
        for (int k = desde; k <= hasta; k++) {
            if (i > medio) {
                destino[k] = fuente[j++];
            } else if (j > hasta) {
                destino[k] = fuente[i++];
            } else if (fuente[j].compararCon(fuente[i]) < 0) {
                destino[k] = fuente[j++];
            } else {
                destino[k] = fuente[i++];
            }
        }
    }

    private void validarNoDirigido(Grafo grafo) {
        if (grafo.esDirigido()) {
            throw new IllegalArgumentException(
                    "El arbol de recubrimiento minimo requiere un grafo no dirigido (cargar con dirigido=false)");
        }
    }
}
