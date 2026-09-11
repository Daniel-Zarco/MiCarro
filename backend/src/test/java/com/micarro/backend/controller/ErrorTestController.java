package com.micarro.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador de test para provocar errores controlados en el
 * GlobalExceptionHandler. Solo existe en el classpath de tests.
 */
@RestController
@RequestMapping("/api/test-errors")
public class ErrorTestController {

    @GetMapping("/boom")
    public void boom() {
        throw new IllegalStateException("detalle interno sensible");
    }
}
