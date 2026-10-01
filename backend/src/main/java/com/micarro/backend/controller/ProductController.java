package com.micarro.backend.controller;

import java.util.List;

import com.micarro.backend.dto.CategoryResponse;
import com.micarro.backend.dto.GroupResponse;
import com.micarro.backend.dto.PageResponse;
import com.micarro.backend.dto.ProductPriceChangeResponse;
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

    @GetMapping("/categories")
    public List<CategoryResponse> getCategories() {
        return productService.getCategories();
    }

    @GetMapping("/categories/{mainCategory}/groups")
    public List<GroupResponse> getGroups(@PathVariable String mainCategory) {
        return productService.getVisualGroups(mainCategory);
    }

    @GetMapping
    public PageResponse<ProductResponse> getProducts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String group,
            @RequestParam(defaultValue = "catalog") String sortBy,
            Pageable pageable) {

        return productService.getProducts(search, category, group, sortBy, pageable);
    }

    @GetMapping("/recent")
    public PageResponse<ProductResponse> getRecentProducts(Pageable pageable) {
        return productService.getRecentProducts(pageable);
    }

    @GetMapping("/new")
    public PageResponse<ProductPriceChangeResponse> getNewProducts(
            Pageable pageable) {
        return productService.getNewProducts(pageable);
    }

    @GetMapping("/price-drops")
    public PageResponse<ProductPriceChangeResponse> getPriceDrops(
            Pageable pageable) {
        return productService.getPriceDrops(pageable);
    }

    @GetMapping("/price-raises")
    public PageResponse<ProductPriceChangeResponse> getPriceRaises(
            Pageable pageable) {
        return productService.getPriceRaises(pageable);
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
