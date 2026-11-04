package uade.prog3.tpo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uade.prog3.tpo.algorithm.Ordenamiento;
import uade.prog3.tpo.algorithm.RamificacionYPoda;
import uade.prog3.tpo.model.Item;
import uade.prog3.tpo.seed.DatosValdoria;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de Branch and Bound de reparto (Hito 11): repartir los objetos entre
 * k personajes minimizando la carga del mas cargado.
 */
class RamificacionYPodaTest {

    private final RamificacionYPoda bb = new RamificacionYPoda(new Ordenamiento());

    /** Optimo por fuerza bruta: prueba las k^n asignaciones (solo para casos chicos). */
    private static double optimoPorFuerzaBruta(List<Item> items, int k) {
        int n = items.size();
        long combinaciones = (long) Math.pow(k, n);
        double mejor = Double.POSITIVE_INFINITY;
        for (long codigo = 0; codigo < combinaciones; codigo++) {
            double[] cargas = new double[k];
            long resto = codigo;
            for (int i = 0; i < n; i++) {
                cargas[(int) (resto % k)] += items.get(i).getPeso();
                resto /= k;
            }
            double maxima = 0;
            for (double c : cargas) {
                maxima = Math.max(maxima, c);
            }
            mejor = Math.min(mejor, maxima);
        }
        return mejor;
    }

    private static void verificarQueRepartaTodo(RamificacionYPoda.Asignacion a, List<Item> items) {
        Set<String> vistos = new HashSet<>();
        double suma = 0;
        for (List<Item> lista : a.porContenedor().values()) {
            for (Item i : lista) {
                assertTrue(vistos.add(i.getId()), "objeto repetido: " + i.getId());
                suma += i.getPeso();
            }
        }
        assertEquals(items.size(), vistos.size());
        double total = 0;
        for (Item i : items) {
            total += i.getPeso();
        }
        assertEquals(total, suma);
    }

    @Test
    @DisplayName("Botin de Valdoria entre 3 personajes: 108 kg quedan en 36 kg cada uno")
    void valdoriaTresPersonajes() {
        List<Item> botin = DatosValdoria.items();
        RamificacionYPoda.Asignacion a = bb.repartir(botin, 3);

        assertEquals(36.0, a.cargaMaxima());
        assertEquals(36.0, a.cota());                       // techo de 108 / 3
        assertEquals(List.of(36.0, 36.0, 36.0), a.cargas());
        assertEquals(3, a.porContenedor().size());
        verificarQueRepartaTodo(a, botin);
    }

    @Test
    @DisplayName("La poda funciona: 546 nodos explorados de 1,27 billones del arbol completo")
    void podaReduceElArbol() {
        RamificacionYPoda.Asignacion a = bb.repartir(DatosValdoria.items(), 3);
        assertEquals(546, a.nodosExplorados());
        assertEquals(760, a.nodosPodados());
        assertEquals(1_270_932_914_164.0, a.nodosSinPoda());   // (3^26 - 1) / 2
        assertTrue(a.nodosExplorados() < a.nodosSinPoda() / 1_000_000_000);
    }

    @Test
    @DisplayName("Con 1 a 8 personajes la carga maxima es el techo de 108 / k (reparto perfecto)")
    void valdoriaVariosPersonajes() {
        double[] esperado = {108, 54, 36, 27, 22, 18, 16, 14};
        for (int k = 1; k <= 8; k++) {
            RamificacionYPoda.Asignacion a = bb.repartir(DatosValdoria.items(), k);
            assertEquals(esperado[k - 1], a.cargaMaxima(), "k = " + k);
            verificarQueRepartaTodo(a, DatosValdoria.items());
        }
    }

    @Test
    @DisplayName("En 300 casos al azar coincide con probar todas las asignaciones")
    void coincideConFuerzaBruta() {
        Random azar = new Random(11);
        for (int prueba = 0; prueba < 300; prueba++) {
            List<Item> items = new ArrayList<>();
            int n = 1 + azar.nextInt(8);
            for (int i = 0; i < n; i++) {
                items.add(new Item("X" + i, "Objeto " + i, 1 + azar.nextInt(12), 1, null));
            }
            int k = 1 + azar.nextInt(3);
            RamificacionYPoda.Asignacion a = bb.repartir(items, k);
            assertEquals(optimoPorFuerzaBruta(items, k), a.cargaMaxima(), "prueba " + prueba);
            assertTrue(a.cargaMaxima() >= a.cota(), "la cota es optimista: nunca supera al optimo");
            verificarQueRepartaTodo(a, items);
        }
    }

    @Test
    @DisplayName("Un caso donde no alcanza el promedio: pesos 5, 5, 5 en 2 personajes -> 10")
    void cotaNoAlcanzable() {
        List<Item> items = List.of(new Item("A", "A", 5, 1, null),
                new Item("B", "B", 5, 1, null), new Item("C", "C", 5, 1, null));
        RamificacionYPoda.Asignacion a = bb.repartir(items, 2);
        assertEquals(8.0, a.cota());          // techo de 15 / 2
        assertEquals(10.0, a.cargaMaxima());  // 5+5 y 5: no se puede menos
    }

    @Test
    @DisplayName("Mas personajes que objetos: la carga maxima es el objeto mas pesado")
    void masPersonajesQueObjetos() {
        RamificacionYPoda.Asignacion a = bb.repartir(DatosValdoria.items(), 30);
        assertEquals(7.0, a.cargaMaxima());
        assertEquals(30, a.porContenedor().size());
    }

    @Test
    @DisplayName("Sin objetos todos cargan 0; con 0 personajes o mas de 100 falla con IllegalArgumentException")
    void casosBorde() {
        RamificacionYPoda.Asignacion a = bb.repartir(List.of(), 3);
        assertEquals(0.0, a.cargaMaxima());
        assertThrows(IllegalArgumentException.class, () -> bb.repartir(DatosValdoria.items(), 0));
        assertThrows(IllegalArgumentException.class,
                () -> bb.repartir(DatosValdoria.items(), RamificacionYPoda.MAXIMO_CONTENEDORES + 1));
        assertEquals(7.0, bb.repartir(DatosValdoria.items(), RamificacionYPoda.MAXIMO_CONTENEDORES).cargaMaxima());
    }
}
