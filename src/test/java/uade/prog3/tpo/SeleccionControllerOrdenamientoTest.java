package uade.prog3.tpo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uade.prog3.tpo.algorithm.Ordenamiento;
import uade.prog3.tpo.algorithm.RamificacionYPoda;
import uade.prog3.tpo.algorithm.Seleccion;
import uade.prog3.tpo.controller.SeleccionController;
import uade.prog3.tpo.model.Item;
import uade.prog3.tpo.repository.AlmacenNeo4j;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Verifica que los endpoints de ordenamiento esten realmente cableados al
 * algoritmo: entra un GET y sale la lista ordenada en JSON.
 *
 * Es un test de slice web: levanta SOLO la capa MVC y el bean Ordenamiento.
 * NO se conecta a Neo4j — el almacen esta mockeado y devuelve los mismos
 * items que carga CargaInicial. Asi el test corre en cualquier lado, sin base
 * y sin red, que es lo que se necesita para que ./mvnw test pase siempre.
 */
@WebMvcTest(SeleccionController.class)
@Import(Ordenamiento.class)
class SeleccionControllerOrdenamientoTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AlmacenNeo4j almacen;

    // El controller los pide por constructor, pero estos endpoints no los usan.
    @MockitoBean
    private Seleccion seleccion;

    @MockitoBean
    private RamificacionYPoda ramificacionYPoda;

    private void conItemsDeLaSemilla() {
        when(almacen.todosLosItems()).thenReturn(List.of(
                new Item("I1", "Item 1", 5, 10, "A"),
                new Item("I2", "Item 2", 4, 40, "B"),
                new Item("I3", "Item 3", 6, 30, "C"),
                new Item("I4", "Item 4", 3, 50, "D"),
                new Item("I5", "Item 5", 7, 55, "E")
        ));
    }

    @Test
    @DisplayName("GET /api/seleccion/quicksort?criterio=ratio devuelve los items ordenados por ratio")
    void quicksortPorRatio() throws Exception {
        conItemsDeLaSemilla();
        mockMvc.perform(get("/api/seleccion/quicksort").param("criterio", "ratio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0].id").value("I1"))   // ratio 2.00
                .andExpect(jsonPath("$[1].id").value("I3"))   // ratio 5.00
                .andExpect(jsonPath("$[2].id").value("I5"))   // ratio 7.86
                .andExpect(jsonPath("$[3].id").value("I2"))   // ratio 10.00
                .andExpect(jsonPath("$[4].id").value("I4"));  // ratio 16.67
    }

    @Test
    @DisplayName("GET /api/seleccion/mergesort?criterio=peso devuelve los items ordenados por peso")
    void mergesortPorPeso() throws Exception {
        conItemsDeLaSemilla();
        mockMvc.perform(get("/api/seleccion/mergesort").param("criterio", "peso"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0].peso").value(3.0))
                .andExpect(jsonPath("$[4].peso").value(7.0))
                .andExpect(jsonPath("$[0].id").value("I4"))
                .andExpect(jsonPath("$[4].id").value("I5"));
    }

    @Test
    @DisplayName("Un criterio invalido devuelve 400, no un stack trace")
    void criterioInvalidoDevuelve400() throws Exception {
        conItemsDeLaSemilla();
        mockMvc.perform(get("/api/seleccion/quicksort").param("criterio", "inventado"))
                .andExpect(status().isBadRequest());
    }
}
