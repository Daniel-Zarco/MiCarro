package com.micarro.backend.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.micarro.backend.entity.Product;
import com.micarro.backend.provider.ProductProvider;
import com.micarro.backend.repository.ProductRepository;

@Service
public class ProductImportService {

    private final ProductProvider productProvider;
    private final ProductRepository productRepository;

    public ProductImportService(
            ProductProvider productProvider,
            ProductRepository productRepository) {

        this.productProvider = productProvider;
        this.productRepository = productRepository;
    }

    public List<Product> importProducts() {

        List<Product> products = productProvider.getProducts();

        // Eliminamos productos repetidos por externalId
        Map<String, Product> uniqueProducts = new LinkedHashMap<>();

        for (Product product : products) {
            if (product.getExternalId() != null) {
                uniqueProducts.put(product.getExternalId(), product);
            }
        }

        List<Product> productsToSave = new ArrayList<>();

        for (Product product : uniqueProducts.values()) {

            productRepository.findByExternalId(product.getExternalId())
                    .ifPresentOrElse(
                            existingProduct -> {
                                existingProduct.setName(product.getName());
                                existingProduct.setCategory(product.getCategory());
                                existingProduct.setImageUrl(product.getImageUrl());
                                existingProduct.setFormat(product.getFormat());
                                existingProduct.setPrice(product.getPrice());

                                productsToSave.add(existingProduct);
                            },
                            () -> productsToSave.add(product)
                    );
        }

        return productRepository.saveAll(productsToSave);
    }
}