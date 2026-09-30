package com.micarro.backend.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/*
 * Petición para guardar un plan. El backend NO confía en datos enviados por el
 * cliente (nombre/precio/subtotal de cada item): solo acepta el nombre y, por
 * cada producto, su id y cantidad; el snapshot se construye desde el catálogo.
 */
public class SavedPlanRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 80, message = "El nombre no puede superar los 80 caracteres")
    private String name;

    @NotNull(message = "Los items son obligatorios")
    @NotEmpty(message = "Debe incluir al menos un producto")
    private List<@Valid SavedPlanItemRequest> items;

    public SavedPlanRequest() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<SavedPlanItemRequest> getItems() {
        return items;
    }

    public void setItems(List<SavedPlanItemRequest> items) {
        this.items = items;
    }
}