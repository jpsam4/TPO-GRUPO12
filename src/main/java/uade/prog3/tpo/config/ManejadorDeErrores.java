package uade.prog3.tpo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import uade.prog3.tpo.algorithm.PendienteDeImplementar;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Traduce excepciones a respuestas HTTP con un cuerpo entendible y sin stack traces.
 *
 *   - PendienteDeImplementar                  -> 501 Not Implemented
 *   - IllegalArgumentException                -> 400 Bad Request
 *   - Parametro faltante o de tipo invalido   -> 400 Bad Request
 *   - Ruta inexistente                        -> 404 Not Found
 *   - IllegalStateException (ciclo negativo)  -> 422 Unprocessable Entity
 *   - Cualquier otra excepcion (base caida…)  -> 500 con mensaje generico; el
 *     detalle va al log del servidor, nunca al cliente.
 */
@RestControllerAdvice
public class ManejadorDeErrores {

    private static final Logger log = LoggerFactory.getLogger(ManejadorDeErrores.class);

    @ExceptionHandler(PendienteDeImplementar.class)
    public ResponseEntity<Map<String, String>> pendiente(PendienteDeImplementar e) {
        return respuesta(HttpStatus.NOT_IMPLEMENTED, "PENDIENTE", e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> datosInvalidos(IllegalArgumentException e) {
        return respuesta(HttpStatus.BAD_REQUEST, "ERROR", e.getMessage());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, String>> parametroFaltante(MissingServletRequestParameterException e) {
        return respuesta(HttpStatus.BAD_REQUEST, "ERROR", "Falta el parametro requerido: " + e.getParameterName());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> tipoDeParametroInvalido(MethodArgumentTypeMismatchException e) {
        return respuesta(HttpStatus.BAD_REQUEST, "ERROR",
                "Valor invalido para el parametro '" + e.getName() + "': " + e.getValue());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, String>> rutaInexistente(NoResourceFoundException e) {
        return respuesta(HttpStatus.NOT_FOUND, "ERROR", "No existe el endpoint: /" + e.getResourcePath());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> estadoInvalido(IllegalStateException e) {
        return respuesta(HttpStatus.UNPROCESSABLE_ENTITY, "ERROR", e.getMessage());
    }

    /** Red de seguridad: nada que no este previsto arriba llega al cliente con su traza. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> inesperado(Exception e) {
        // Las excepciones propias de Spring MVC (405, 415, 503 por timeout, etc.)
        // ya traen su codigo HTTP y un detalle legible: se respetan los dos en
        // lugar de convertirlas en un 500 generico.
        if (e instanceof ErrorResponse errorDeSpring) {
            HttpStatus status = HttpStatus.resolve(errorDeSpring.getStatusCode().value());
            if (status != null) {
                if (status.is5xxServerError()) {
                    log.warn("Spring respondio {}: {}", status.value(), e.getMessage());
                }
                return respuesta(status, "ERROR", errorDeSpring.getBody().getDetail());
            }
        }
        log.error("Error no previsto atendiendo la consulta", e);
        return respuesta(HttpStatus.INTERNAL_SERVER_ERROR, "ERROR",
                "Error interno del servidor. Revisar el log para el detalle.");
    }

    /**
     * Arma el cuerpo {estado, detalle}. Usa LinkedHashMap y no Map.of porque
     * Map.of tira NullPointerException si el mensaje de la excepcion es null,
     * y eso convertiria un error manejado en uno no manejado.
     */
    private static ResponseEntity<Map<String, String>> respuesta(HttpStatus status, String estado, String detalle) {
        Map<String, String> cuerpo = new LinkedHashMap<>();
        cuerpo.put("estado", estado);
        cuerpo.put("detalle", detalle != null ? detalle : status.getReasonPhrase());
        return ResponseEntity.status(status).body(cuerpo);
    }
}
