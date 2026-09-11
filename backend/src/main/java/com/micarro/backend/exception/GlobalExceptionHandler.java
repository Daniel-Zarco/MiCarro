package com.micarro.backend.exception;

import java.time.Instant;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.micarro.backend.dto.ApiErrorResponse;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /*
     * Errores de validación (@Valid): devuelve los campos inválidos con un
     * mensaje claro, p. ej. "email: ...; password: ...".
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError -> describe(fieldError))
                .collect(Collectors.joining("; "));

        if (message.isBlank()) {
            message = "Datos de la petición inválidos";
        }

        return build(HttpStatus.BAD_REQUEST, message, pathOf(request));
    }

    /*
     * Cuerpo de la petición ilegible (JSON inválido, tipos incorrectos...).
     */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            org.springframework.http.converter.HttpMessageNotReadableException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        return build(
                HttpStatus.BAD_REQUEST,
                "El cuerpo de la petición no es válido",
                pathOf(request)
        );
    }

    /*
     * Respeta el status y el motivo de las ResponseStatusException lanzadas
     * por los servicios (409 email duplicado, 401 credenciales, 404, ...).
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Object> handleResponseStatus(
            ResponseStatusException exception,
            WebRequest request) {

        HttpStatusCode status = exception.getStatusCode();

        String reason = exception.getReason();

        String message = (reason != null && !reason.isBlank())
                ? reason
                : defaultMessage(status);

        return build(status, message, pathOf(request));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Object> handleAccessDenied(
            AccessDeniedException exception,
            WebRequest request) {

        return build(
                HttpStatus.FORBIDDEN,
                "No tienes permisos para acceder a este recurso",
                pathOf(request)
        );
    }

    /*
     * Cualquier excepción no controlada: 500 genérico, sin detalles internos.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGeneric(
            Exception exception,
            WebRequest request) {

        log.error("Error no controlado en {}", pathOf(request), exception);

        return build(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ha ocurrido un error interno",
                pathOf(request)
        );
    }

    /*
     * Resto de excepciones de Spring MVC (404 rutas inexistentes, 405 método
     * no permitido, 415 tipo no soportado...). Mantiene su status.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception exception,
            Object body,
            HttpHeaders headers,
            HttpStatusCode statusCode,
            WebRequest request) {

        return build(statusCode, defaultMessage(statusCode), pathOf(request));
    }

    private String describe(FieldError fieldError) {
        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
    }

    private ResponseEntity<Object> build(
            HttpStatusCode status,
            String message,
            String path) {

        ApiErrorResponse apiError = new ApiErrorResponse(
                Instant.now(),
                status.value(),
                reasonPhrase(status),
                message,
                path
        );

        return ResponseEntity.status(status).body(apiError);
    }

    private String reasonPhrase(HttpStatusCode status) {

        HttpStatus resolved = HttpStatus.resolve(status.value());

        return resolved != null ? resolved.getReasonPhrase() : "Error";
    }

    private String defaultMessage(HttpStatusCode status) {

        HttpStatus resolved = HttpStatus.resolve(status.value());

        if (resolved == null) {
            return "Se ha producido un error";
        }

        return switch (resolved) {
            case BAD_REQUEST -> "Petición incorrecta";
            case UNAUTHORIZED -> "No autenticado";
            case FORBIDDEN -> "Acceso denegado";
            case NOT_FOUND -> "Recurso no encontrado";
            case METHOD_NOT_ALLOWED -> "Método no permitido";
            case UNSUPPORTED_MEDIA_TYPE -> "Tipo de contenido no soportado";
            default -> resolved.getReasonPhrase();
        };
    }

    private String pathOf(WebRequest request) {

        if (request instanceof ServletWebRequest servletWebRequest) {
            return servletWebRequest.getRequest().getRequestURI();
        }

        return request.getDescription(false).replace("uri=", "");
    }
}
