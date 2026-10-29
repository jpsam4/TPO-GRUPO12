package uade.prog3.tpo;

import org.junit.jupiter.api.BeforeEach;
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
import uade.prog3.tpo.repository.AlmacenNeo4j;
import uade.prog3.tpo.seed.DatosValdoria;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Verifica que los endpoints de greedy y mochila 0/1 esten cableados al
 * algoritmo real, con el filtro opcional por region.
 *
 * Test de slice web: no se conecta a Neo4j; el almacen esta mockeado y
 * devuelve los objetos de Valdoria.
 */
@WebMvcTest(SeleccionController.class)
@Import({Ordenamiento.class, Seleccion.class})
class SeleccionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AlmacenNeo4j almacen;

    // El controller lo pide por constructor, pero estos endpoints no lo usan.
    @MockitoBean
    private RamificacionYPoda ramificacionYPoda;

    @BeforeEach
    void conLosObjetosDeValdoria() {
        when(almacen.todosLosItems()).thenReturn(DatosValdoria.items());
    }

    @Test
    @DisplayName("GET /api/seleccion/greedy?capacidad=10&region=VB devuelve 30 de poder")
    void greedyValleDeBrezo() throws Exception {
        mockMvc.perform(get("/api/seleccion/greedy").param("capacidad", "10").param("region", "VB"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valorTotal").value(30.0))
                .andExpect(jsonPath("$.pesoTotal").value(8.0))
                .andExpect(jsonPath("$.elegidos.length()").value(3));
    }

    @Test
    @DisplayName("GET /api/seleccion/dinamica?capacidad=10&region=VB devuelve el optimo: 35 de poder")
    void dinamicaValleDeBrezo() throws Exception {
        mockMvc.perform(get("/api/seleccion/dinamica").param("capacidad", "10").param("region", "VB"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valorTotal").value(35.0))
                .andExpect(jsonPath("$.pesoTotal").value(10.0))
                .andExpect(jsonPath("$.elegidos[0].nombre").value("Armadura"))
                .andExpect(jsonPath("$.elegidos[1].nombre").value("Arco"));
    }

    @Test
    @DisplayName("Sin region usa los 25 objetos del reino")
    void sinRegionUsaTodoElReino() throws Exception {
        mockMvc.perform(get("/api/seleccion/dinamica").param("capacidad", "108"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.elegidos.length()").value(25))
                .andExpect(jsonPath("$.pesoTotal").value(108.0));
    }

    @Test
    @DisplayName("Una region que no existe o una capacidad negativa devuelven 400")
    void datosInvalidosDevuelven400() throws Exception {
        mockMvc.perform(get("/api/seleccion/greedy").param("capacidad", "10").param("region", "XX"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/seleccion/dinamica").param("capacidad", "-1"))
                .andExpect(status().isBadRequest());
    }
}
