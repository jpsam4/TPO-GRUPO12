package uade.prog3.tpo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uade.prog3.tpo.algorithm.ArbolRecubrimiento;
import uade.prog3.tpo.algorithm.Backtracking;
import uade.prog3.tpo.algorithm.CaminosMinimos;
import uade.prog3.tpo.algorithm.Recorridos;
import uade.prog3.tpo.controller.GrafoController;
import uade.prog3.tpo.repository.AlmacenNeo4j;
import uade.prog3.tpo.seed.DatosValdoria;
import uade.prog3.tpo.service.GrafoService;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests HTTP de todos los endpoints de GrafoController (Hito 9).
 *
 * Verifica de punta a punta la capa MVC, la serializacion JSON y el manejo de
 * errores (400, 422 y 501 sin stack traces) contra los beans reales de
 * GrafoService, Recorridos, CaminosMinimos, ArbolRecubrimiento y Backtracking,
 * mockeando unicamente AlmacenNeo4j con los datos de Valdoria.
 */
@WebMvcTest(GrafoController.class)
@Import({GrafoService.class, Recorridos.class, CaminosMinimos.class, ArbolRecubrimiento.class, Backtracking.class})
class GrafoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AlmacenNeo4j almacen;

    @BeforeEach
    void conLasZonasDeValdoria() {
        when(almacen.todosLosNodos()).thenReturn(DatosValdoria.nodos());
    }

    @Test
    @DisplayName("GET /api/grafo/resumen devuelve 38 vertices y 57 aristas no dirigidas")
    void resumenValdoria() throws Exception {
        mockMvc.perform(get("/api/grafo/resumen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vertices").value(38))
                .andExpect(jsonPath("$.aristas").value(57))
                .andExpect(jsonPath("$.dirigido").value(false))
                .andExpect(jsonPath("$.ids.length()").value(38));
    }

    @Test
    @DisplayName("GET /api/grafo/dfs y /bfs recorren las 38 zonas de Valdoria desde VB1")
    void dfsYBfsValdoria() throws Exception {
        mockMvc.perform(get("/api/grafo/dfs").param("origen", "VB1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(38))
                .andExpect(jsonPath("$[0]").value("VB1"))
                .andExpect(jsonPath("$[37]").value("DA9"));

        mockMvc.perform(get("/api/grafo/bfs").param("origen", "VB1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(38))
                .andExpect(jsonPath("$[0]").value("VB1"))
                .andExpect(jsonPath("$[37]").value("CC10"));
    }

    @Test
    @DisplayName("GET /api/grafo/dijkstra?origen=VB1&destino=CC11 devuelve las 22 zonas y costo 103")
    void dijkstraValdoria() throws Exception {
        mockMvc.perform(get("/api/grafo/dijkstra")
                        .param("origen", "VB1")
                        .param("destino", "CC11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.costoTotal").value(103.0))
                .andExpect(jsonPath("$.vertices.length()").value(22))
                .andExpect(jsonPath("$.vertices[0]").value("VB1"))
                .andExpect(jsonPath("$.vertices[21]").value("CC11"));
    }

    @Test
    @DisplayName("GET /api/grafo/floyd, /floyd/camino y /floyd/comparacion responden con el costo neto y la comparacion")
    void floydValdoria() throws Exception {
        mockMvc.perform(get("/api/grafo/floyd"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['VB1->VB2']").value(-3.0))
                .andExpect(jsonPath("$['VB2->VB1']").value(3.0))
                .andExpect(jsonPath("$['VB1->CC11']").value(79.0));

        mockMvc.perform(get("/api/grafo/floyd/camino")
                        .param("origen", "VB1")
                        .param("destino", "CC11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.costoTotal").value(79.0))
                .andExpect(jsonPath("$.vertices.length()").value(22));

        mockMvc.perform(get("/api/grafo/floyd/comparacion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.floydComparaciones").value(54872))
                .andExpect(jsonPath("$.dijkstraNodosExpandidos").value(1444))
                .andExpect(jsonPath("$.mismasDistancias").value(true));
    }

    @Test
    @DisplayName("GET /api/grafo/floyd?curacion=7 detecta el ciclo negativo y responde 422 JSON sin stack trace")
    void floydCicloNegativoDevuelve422() throws Exception {
        mockMvc.perform(get("/api/grafo/floyd").param("curacion", "7"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.estado").value("ERROR"))
                .andExpect(jsonPath("$.detalle").exists());
    }

    @Test
    @DisplayName("GET /api/grafo/prim y /kruskal devuelven 37 aristas y costo total 161")
    void primYKruskalValdoria() throws Exception {
        mockMvc.perform(get("/api/grafo/prim").param("origen", "VB1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.costoTotal").value(161.0))
                .andExpect(jsonPath("$.aristas.length()").value(37));

        mockMvc.perform(get("/api/grafo/kruskal"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.costoTotal").value(161.0))
                .andExpect(jsonPath("$.aristas.length()").value(37));
    }

    @Test
    @DisplayName("GET /api/grafo/rutas y /rutas/comparacion devuelven las 3 rutas y 19 vs 37 nodos en Valle de Brezo")
    void backtrackingRutasYComparacion() throws Exception {
        mockMvc.perform(get("/api/grafo/rutas")
                        .param("origen", "VB1")
                        .param("destino", "VB8")
                        .param("costoMaximo", "22")
                        .param("saltosMaximos", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].costoTotal").value(21.0))
                .andExpect(jsonPath("$[1].costoTotal").value(20.0))
                .andExpect(jsonPath("$[2].costoTotal").value(21.0));

        mockMvc.perform(get("/api/grafo/rutas/comparacion")
                        .param("origen", "VB1")
                        .param("destino", "VB8")
                        .param("costoMaximo", "22")
                        .param("saltosMaximos", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mismasRutas").value(true))
                .andExpect(jsonPath("$.rutas.length()").value(3))
                .andExpect(jsonPath("$.nodosExploradosConPoda").value(19))
                .andExpect(jsonPath("$.nodosExploradosSinPoda").value(37))
                .andExpect(jsonPath("$.podasPorCosto").value(7))
                .andExpect(jsonPath("$.podasPorSaltos").value(4));
    }

    @Test
    @DisplayName("Endpoint opcional sin implementar (/ucs) responde 501 JSON limpio, no 500")
    void ucsPendienteDevuelve501() throws Exception {
        mockMvc.perform(get("/api/grafo/ucs")
                        .param("origen", "VB1")
                        .param("destino", "CC11"))
                .andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"));
    }

    @Test
    @DisplayName("Zonas inexistentes, parametros faltantes o mal tipados responden 400 JSON limpio")
    void erroresManejadosCon400() throws Exception {
        mockMvc.perform(get("/api/grafo/dfs").param("origen", "INEXISTENTE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.estado").value("ERROR"));

        mockMvc.perform(get("/api/grafo/dfs"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.estado").value("ERROR"));

        mockMvc.perform(get("/api/grafo/rutas")
                        .param("origen", "VB1")
                        .param("destino", "VB8")
                        .param("saltosMaximos", "no-es-numero"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.estado").value("ERROR"));
    }
}
