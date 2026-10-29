package uade.prog3.tpo.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import uade.prog3.tpo.algorithm.PendienteDeImplementar;

import java.util.Map;

/**
 * Traduce excepciones a respuestas HTTP con un cuerpo entendible y sin stack traces.
 *
 *   - PendienteDeImplementar                  -> 501 Not Implemented
 *   - IllegalArgumentException                -> 400 Bad Request
 *   - Parametro faltante o de tipo invalido   -> 400 Bad Request
 *   - IllegalStateException (ciclo negativo)  -> 422 Unprocessable Entity
 */
@RestControllerAdvice
public class ManejadorDeErrores {

    @ExceptionHandler(PendienteDeImplementar.class)
    public ResponseEntity<Map<String, String>> pendiente(PendienteDeImplementar e) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(Map.of("estado", "PENDIENTE", "detalle", e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> datosInvalidos(IllegalArgumentException e) {
        return ResponseEntity.badRequest()
                .body(Map.of("estado", "ERROR", "detalle", e.getMessage()));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, String>> parametroFaltante(MissingServletRequestParameterException e) {
        return ResponseEntity.badRequest()
                .body(Map.of("estado", "ERROR", "detalle", "Falta el parametro requerido: " + e.getParameterName()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> tipoDeParametroInvalido(MethodArgumentTypeMismatchException e) {
        return ResponseEntity.badRequest()
                .body(Map.of("estado", "ERROR", "detalle", "Valor invalido para el parametro '" + e.getName() + "': " + e.getValue()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> estadoInvalido(IllegalStateException e) {
        return ResponseEntity.unprocessableEntity()
                .body(Map.of("estado", "ERROR", "detalle", e.getMessage()));
    }
}
