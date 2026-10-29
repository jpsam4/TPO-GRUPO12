package uade.prog3.tpo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uade.prog3.tpo.algorithm.Ordenamiento;
import uade.prog3.tpo.algorithm.Seleccion;
import uade.prog3.tpo.model.Item;
import uade.prog3.tpo.seed.DatosValdoria;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de greedy y mochila 0/1 con programacion dinamica (Hito 6).
 *
 * El juego de datos donde greedy NO da el optimo son los objetos de cada
 * region de Valdoria con la mochila de 10 kg: pasa en las cuatro.
 */
class SeleccionTest {

    private final Seleccion seleccion = new Seleccion(new Ordenamiento());

    /** Los objetos de una region de Valdoria (VB, CN, DA o CC). */
    private static List<Item> region(String codigo) {
        List<Item> items = new ArrayList<>();
        for (Item i : DatosValdoria.items()) {
            if (i.getNodoId().startsWith(codigo)) {
                items.add(i);
            }
        }
        return items;
    }

    private static List<String> nombres(Seleccion.Resultado r) {
        List<String> nombres = new ArrayList<>();
        for (Item i : r.elegidos()) {
            nombres.add(i.getNombre());
        }
        return nombres;
    }

    /** Optimo por fuerza bruta (2^n subconjuntos), para comparar con la PD. */
    private static double optimoPorFuerzaBruta(List<Item> items, int capacidad) {
        double mejor = 0;
        int n = items.size();
        for (int mascara = 0; mascara < (1 << n); mascara++) {
            double peso = 0;
            double valor = 0;
            for (int i = 0; i < n; i++) {
                if ((mascara & (1 << i)) != 0) {
                    peso += items.get(i).getPeso();
                    valor += items.get(i).getValor();
                }
            }
            if (peso <= capacidad && valor > mejor) {
                mejor = valor;
            }
        }
        return mejor;
    }

    // --- El caso del hito: greedy pierde -------------------------------------------

    @Test
    @DisplayName("Valle de Brezo, mochila de 10 kg: greedy junta 30 de poder, el optimo es 35")
    void valleDeBrezoGreedyPierde() {
        Seleccion.Resultado g = seleccion.greedy(region("VB"), 10);
        Seleccion.Resultado dp = seleccion.programacionDinamica(region("VB"), 10);

        // Greedy toma por ratio: Arco (4.00), Hacha (3.67), Gema (3.00); la Armadura (3.17) ya no entra.
        assertEquals(List.of("Arco", "Hacha", "Gema"), nombres(g));
        assertEquals(30.0, g.valorTotal());
        assertEquals(8.0, g.pesoTotal());

        assertEquals(List.of("Armadura", "Arco"), nombres(dp));
        assertEquals(35.0, dp.valorTotal());
        assertEquals(10.0, dp.pesoTotal());
    }

    @Test
    @DisplayName("En las cuatro regiones, con 10 kg, greedy queda por debajo del optimo")
    void greedyPierdeEnLasCuatroRegiones() {
        String[] codigos = {"VB", "CN", "DA", "CC"};
        double[] greedyEsperado = {30, 56, 69, 79};
        double[] optimoEsperado = {35, 61, 75, 90};
        for (int k = 0; k < codigos.length; k++) {
            List<Item> items = region(codigos[k]);
            assertEquals(greedyEsperado[k], seleccion.greedy(items, 10).valorTotal(), codigos[k]);
            assertEquals(optimoEsperado[k], seleccion.programacionDinamica(items, 10).valorTotal(), codigos[k]);
        }
    }

    // --- Programacion dinamica -----------------------------------------------------

    @Test
    @DisplayName("La PD da el mismo valor que probar todos los subconjuntos, en cada region y capacidad")
    void dinamicaCoincideConFuerzaBruta() {
        for (String codigo : List.of("VB", "CN", "DA", "CC")) {
            List<Item> items = region(codigo);
            for (int capacidad = 0; capacidad <= 30; capacidad++) {
                assertEquals(optimoPorFuerzaBruta(items, capacidad),
                        seleccion.programacionDinamica(items, capacidad).valorTotal(),
                        codigo + " con capacidad " + capacidad);
            }
        }
    }

    @Test
    @DisplayName("Los objetos recuperados de la tabla suman exactamente el valor y el peso informados")
    void caminoRecuperadoEsConsistente() {
        Random azar = new Random(12);
        for (int prueba = 0; prueba < 200; prueba++) {
            List<Item> items = new ArrayList<>();
            int n = 1 + azar.nextInt(12);
            for (int i = 0; i < n; i++) {
                items.add(new Item("X" + i, "Objeto " + i, 1 + azar.nextInt(9), 1 + azar.nextInt(50), null));
            }
            int capacidad = azar.nextInt(30);
            Seleccion.Resultado r = seleccion.programacionDinamica(items, capacidad);

            double peso = 0;
            double valor = 0;
            for (Item i : r.elegidos()) {
                peso += i.getPeso();
                valor += i.getValor();
            }
            assertEquals(r.valorTotal(), valor);
            assertEquals(r.pesoTotal(), peso);
            assertTrue(peso <= capacidad);
            assertEquals(optimoPorFuerzaBruta(items, capacidad), r.valorTotal());
        }
    }

    @Test
    @DisplayName("Greedy nunca supera al optimo ni a la capacidad")
    void greedyNuncaSuperaAlOptimo() {
        List<Item> reino = DatosValdoria.items();
        for (int capacidad = 0; capacidad <= 40; capacidad++) {
            Seleccion.Resultado g = seleccion.greedy(reino, capacidad);
            assertTrue(g.pesoTotal() <= capacidad);
            assertTrue(g.valorTotal() <= seleccion.programacionDinamica(reino, capacidad).valorTotal());
        }
    }

    // --- Casos borde ---------------------------------------------------------------

    @Test
    @DisplayName("Capacidad 0 o sin objetos: no se elige nada")
    void casosVacios() {
        assertTrue(seleccion.greedy(region("VB"), 0).elegidos().isEmpty());
        assertTrue(seleccion.programacionDinamica(region("VB"), 0).elegidos().isEmpty());
        assertEquals(0.0, seleccion.greedy(List.of(), 10).valorTotal());
        assertEquals(0.0, seleccion.programacionDinamica(List.of(), 10).valorTotal());
    }

    @Test
    @DisplayName("Si todo entra, los dos se llevan todo")
    void todoEntra() {
        List<Item> vb = region("VB");   // 19 kg en total
        assertEquals(63.0, seleccion.greedy(vb, 19).valorTotal());
        assertEquals(63.0, seleccion.programacionDinamica(vb, 19).valorTotal());
        assertEquals(5, seleccion.programacionDinamica(vb, 19).elegidos().size());
    }

    @Test
    @DisplayName("Capacidad negativa o pesos no enteros: IllegalArgumentException (el endpoint responde 400)")
    void datosInvalidos() {
        assertThrows(IllegalArgumentException.class, () -> seleccion.greedy(region("VB"), -1));
        assertThrows(IllegalArgumentException.class, () -> seleccion.programacionDinamica(region("VB"), -1));
        List<Item> conPesoRaro = List.of(new Item("X", "Pluma", 0.5, 3, null));
        assertThrows(IllegalArgumentException.class, () -> seleccion.programacionDinamica(conPesoRaro, 5));
    }

    @Test
    @DisplayName("La entrada no se modifica")
    void noModificaLaEntrada() {
        List<Item> vb = region("VB");
        List<Item> copia = new ArrayList<>(vb);
        seleccion.greedy(vb, 10);
        seleccion.programacionDinamica(vb, 10);
        assertEquals(copia, vb);
    }
}
