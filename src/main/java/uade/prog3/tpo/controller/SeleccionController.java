package uade.prog3.tpo.controller;

import org.springframework.web.bind.annotation.*;
import uade.prog3.tpo.algorithm.Ordenamiento;
import uade.prog3.tpo.algorithm.RamificacionYPoda;
import uade.prog3.tpo.algorithm.Seleccion;
import uade.prog3.tpo.model.Item;
import uade.prog3.tpo.repository.AlmacenNeo4j;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Endpoints de seleccion, ordenamiento y reparto.
 * Igual que en GrafoController: la logica va en el paquete algorithm.
 */
@RestController
@RequestMapping("/api/seleccion")
public class SeleccionController {

    private final AlmacenNeo4j almacen;
    private final Seleccion seleccion;
    private final Ordenamiento ordenamiento;
    private final RamificacionYPoda ramificacionYPoda;

    public SeleccionController(AlmacenNeo4j almacen,
                               Seleccion seleccion,
                               Ordenamiento ordenamiento,
                               RamificacionYPoda ramificacionYPoda) {
        this.almacen = almacen;
        this.seleccion = seleccion;
        this.ordenamiento = ordenamiento;
        this.ramificacionYPoda = ramificacionYPoda;
    }

    private static final Set<String> REGIONES = Set.of("VB", "CN", "DA", "CC");

    private List<Item> todos() {
        return almacen.todosLosItems();
    }

    /**
     * Los objetos de una region (VB, CN, DA o CC), o todos los del reino si
     * no se indica. Es solo un filtro de datos: el algoritmo no cambia.
     */
    private List<Item> deLaRegion(String region) {
        List<Item> items = todos();
        if (region == null || region.isBlank()) {
            return items;
        }
        String codigo = region.trim().toUpperCase();
        if (!REGIONES.contains(codigo)) {
            throw new IllegalArgumentException("Region invalida: " + region + ". Use VB, CN, DA o CC.");
        }
        List<Item> filtrados = new ArrayList<>();
        for (Item i : items) {
            if (i.getNodoId() != null && i.getNodoId().startsWith(codigo)) {
                filtrados.add(i);
            }
        }
        return filtrados;
    }

    /** GET /api/seleccion/greedy?capacidad=10&region=VB   (region es opcional) */
    @GetMapping("/greedy")
    public Seleccion.Resultado greedy(@RequestParam double capacidad,
                                      @RequestParam(required = false) String region) {
        return seleccion.greedy(deLaRegion(region), capacidad);
    }

    /** GET /api/seleccion/dinamica?capacidad=10&region=VB   (region es opcional) */
    @GetMapping("/dinamica")
    public Seleccion.Resultado dinamica(@RequestParam int capacidad,
                                        @RequestParam(required = false) String region) {
        return seleccion.programacionDinamica(deLaRegion(region), capacidad);
    }

    /**
     * GET /api/seleccion/quicksort?criterio=valor
     * criterio admite: valor | peso | ratio
     */
    @GetMapping("/quicksort")
    public List<Item> quickSort(@RequestParam(defaultValue = "ratio") String criterio) {
        return ordenamiento.quickSort(todos(), comparador(criterio));
    }

    /** GET /api/seleccion/mergesort?criterio=peso */
    @GetMapping("/mergesort")
    public List<Item> mergeSort(@RequestParam(defaultValue = "ratio") String criterio) {
        return ordenamiento.mergeSort(todos(), comparador(criterio));
    }

    /** GET /api/seleccion/repartir?contenedores=3 */
    @GetMapping("/repartir")
    public RamificacionYPoda.Asignacion repartir(@RequestParam int contenedores) {
        if (contenedores < 1) {
            throw new IllegalArgumentException("La cantidad de contenedores debe ser al menos 1");
        }
        return ramificacionYPoda.repartir(todos(), contenedores);
    }

    private Comparator<Item> comparador(String criterio) {
        return switch (criterio) {
            case "valor" -> Comparator.comparingDouble(Item::getValor);
            case "peso"  -> Comparator.comparingDouble(Item::getPeso);
            case "ratio" -> Comparator.comparingDouble(Item::ratioValorPeso);
            default -> throw new IllegalArgumentException(
                    "Criterio invalido: " + criterio + ". Use valor, peso o ratio.");
        };
    }
}
