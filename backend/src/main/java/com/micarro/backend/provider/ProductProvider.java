package com.micarro.backend.provider;

import com.micarro.backend.entity.Product;
import java.util.List;

public interface ProductProvider {
    List<Product> getProducts();
}