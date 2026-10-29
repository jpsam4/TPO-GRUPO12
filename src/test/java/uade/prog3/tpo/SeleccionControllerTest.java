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
 * Verifica que los endpoints de SeleccionController (ordenamiento, greedy,
 * mochila 0/1 y manejo de errores / pendientes) esten cableados a los beans
 * reales sobre los 25 objetos de Valdoria.
 *
 * Test de slice web: no se conecta a Neo4j; el almacen esta mockeado.
 */
@WebMvcTest(SeleccionController.class)
@Import({Ordenamiento.class, Seleccion.class, RamificacionYPoda.class})
class SeleccionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AlmacenNeo4j almacen;

    @BeforeEach
    void conLosObjetosDeValdoria() {
        when(almacen.todosLosItems()).thenReturn(DatosValdoria.items());
    }

    @Test
    @DisplayName("GET /api/seleccion/quicksort y /mergesort ordenan los 25 objetos de Valdoria")
    void ordenamientoSobreValdoria() throws Exception {
        mockMvc.perform(get("/api/seleccion/quicksort").param("criterio", "ratio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(25))
                .andExpect(jsonPath("$[0].id").value("OVB3"))
                .andExpect(jsonPath("$[24].id").value("OCC2"));

        mockMvc.perform(get("/api/seleccion/mergesort").param("criterio", "peso"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(25))
                .andExpect(jsonPath("$[0].id").value("OVB4"))
                .andExpect(jsonPath("$[1].id").value("OCC4"))
                .andExpect(jsonPath("$[24].id").value("OCC7"));
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
    @DisplayName("Una region inexistente, capacidad negativa o parametro faltante devuelven 400 JSON")
    void datosInvalidosDevuelven400() throws Exception {
        mockMvc.perform(get("/api/seleccion/greedy").param("capacidad", "10").param("region", "XX"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.estado").value("ERROR"));
        mockMvc.perform(get("/api/seleccion/dinamica").param("capacidad", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.estado").value("ERROR"));
        mockMvc.perform(get("/api/seleccion/dinamica"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.estado").value("ERROR"));
        mockMvc.perform(get("/api/seleccion/repartir").param("contenedores", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.estado").value("ERROR"));
    }

    @Test
    @DisplayName("Endpoint pendiente (/repartir, Hito 11) responde 501 JSON limpio, no stack trace")
    void repartirPendienteDevuelve501() throws Exception {
        mockMvc.perform(get("/api/seleccion/repartir").param("contenedores", "3"))
                .andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"));
    }
}
