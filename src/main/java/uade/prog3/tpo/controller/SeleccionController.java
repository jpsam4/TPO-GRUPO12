package uade.prog3.tpo.controller;

import org.springframework.web.bind.annotation.*;
import uade.prog3.tpo.algorithm.Ordenamiento;
import uade.prog3.tpo.algorithm.RamificacionYPoda;
import uade.prog3.tpo.algorithm.Seleccion;
import uade.prog3.tpo.model.Item;
import uade.prog3.tpo.service.ItemService;

import java.util.Comparator;
import java.util.List;

/**
 * Endpoints de seleccion, ordenamiento y reparto.
 * Igual que en GrafoController: el controller solo traduce parametros HTTP,
 * pide los objetos a ItemService y delega en el paquete algorithm.
 */
@RestController
@RequestMapping("/api/seleccion")
public class SeleccionController {

    private final ItemService itemService;
    private final Seleccion seleccion;
    private final Ordenamiento ordenamiento;
    private final RamificacionYPoda ramificacionYPoda;

    public SeleccionController(ItemService itemService,
                               Seleccion seleccion,
                               Ordenamiento ordenamiento,
                               RamificacionYPoda ramificacionYPoda) {
        this.itemService = itemService;
        this.seleccion = seleccion;
        this.ordenamiento = ordenamiento;
        this.ramificacionYPoda = ramificacionYPoda;
    }

    /** GET /api/seleccion/greedy?capacidad=10&region=VB   (region es opcional) */
    @GetMapping("/greedy")
    public Seleccion.Resultado greedy(@RequestParam double capacidad,
                                      @RequestParam(required = false) String region) {
        return seleccion.greedy(itemService.deLaRegion(region), capacidad);
    }

    /** GET /api/seleccion/dinamica?capacidad=10&region=VB   (region es opcional) */
    @GetMapping("/dinamica")
    public Seleccion.Resultado dinamica(@RequestParam int capacidad,
                                        @RequestParam(required = false) String region) {
        return seleccion.programacionDinamica(itemService.deLaRegion(region), capacidad);
    }

    /**
     * GET /api/seleccion/quicksort?criterio=valor
     * criterio admite: valor | peso | ratio
     */
    @GetMapping("/quicksort")
    public List<Item> quickSort(@RequestParam(defaultValue = "ratio") String criterio) {
        return ordenamiento.quickSort(itemService.todos(), comparador(criterio));
    }

    /** GET /api/seleccion/mergesort?criterio=peso */
    @GetMapping("/mergesort")
    public List<Item> mergeSort(@RequestParam(defaultValue = "ratio") String criterio) {
        return ordenamiento.mergeSort(itemService.todos(), comparador(criterio));
    }

    /** GET /api/seleccion/repartir?contenedores=3   (la validacion la hace RamificacionYPoda) */
    @GetMapping("/repartir")
    public RamificacionYPoda.Asignacion repartir(@RequestParam int contenedores) {
        return ramificacionYPoda.repartir(itemService.todos(), contenedores);
    }

    /** Traduce el parametro de texto al criterio de comparacion. No ordena nada. */
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
