package com.micarro.backend.controller;

import com.micarro.backend.dto.PageResponse;
import com.micarro.backend.dto.ProductResponse;
import com.micarro.backend.entity.Product;
import com.micarro.backend.service.ProductService;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private static final String NOT_FOUND_MESSAGE = "Producto no encontrado";

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ProductResponse createProduct(@RequestBody Product product) {
        return productService.createProduct(product);
    }

    @GetMapping
    public PageResponse<ProductResponse> getProducts(
            @RequestParam(required = false) String search,
            Pageable pageable) {

        return productService.getProducts(search, pageable);
    }

    @GetMapping("/recent")
    public PageResponse<ProductResponse> getRecentProducts(Pageable pageable) {
        return productService.getRecentProducts(pageable);
    }

    @GetMapping("/{id}")
    public ProductResponse getProductById(@PathVariable Long id) {
        return productService.getProductById(id)
                .orElseThrow(() -> notFound());
    }

    @PutMapping("/{id}")
    public ProductResponse updateProduct(
            @PathVariable Long id,
            @RequestBody Product productDetails) {

        return productService.updateProduct(id, productDetails)
                .orElseThrow(() -> notFound());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProduct(@PathVariable Long id) {

        if (!productService.deleteProduct(id)) {
            throw notFound();
        }
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                NOT_FOUND_MESSAGE
        );
    }
}
