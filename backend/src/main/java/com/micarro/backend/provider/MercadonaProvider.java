package com.micarro.backend.provider;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.micarro.backend.entity.Product;
import com.micarro.backend.external.mercadona.MercadonaClient;
import com.micarro.backend.provider.dto.MercadonaCategoriesResponse;
import com.micarro.backend.provider.dto.MercadonaCategoryResponse;
import com.micarro.backend.provider.dto.MercadonaCategorySummaryDto;
import com.micarro.backend.provider.dto.MercadonaProductDto;
import com.micarro.backend.provider.dto.MercadonaSubcategoryDto;

@Component
public class MercadonaProvider implements ProductProvider {

    public static final String SOURCE = "MERCADONA";

    private final MercadonaClient mercadonaClient;

    public MercadonaProvider(MercadonaClient mercadonaClient) {
        this.mercadonaClient = mercadonaClient;
    }

    @Override
    public String getSource() {
        return SOURCE;
    }

    @Override
    public List<Product> getProducts() {

        List<Product> products = new ArrayList<>();

        MercadonaCategoriesResponse categoriesResponse =
                mercadonaClient.getCategories();

        for (MercadonaCategorySummaryDto mainCategory : categoriesResponse.getResults()) {

            if (mainCategory.getCategories() == null) {
                continue;
            }

            for (MercadonaCategorySummaryDto category : mainCategory.getCategories()) {

                MercadonaCategoryResponse response =
                        mercadonaClient.getCategory(category.getId());

                if (response.getCategories() == null) {
                    continue;
                }

                for (MercadonaSubcategoryDto subcategory : response.getCategories()) {

                    if (subcategory.getProducts() == null) {
                        continue;
                    }

                    for (MercadonaProductDto mercadonaProduct : subcategory.getProducts()) {

                        Product product = new Product();

                        product.setExternalId(mercadonaProduct.getId());
                        product.setName(mercadonaProduct.getDisplayName());
                        product.setCategory(subcategory.getName());
                        product.setImageUrl(mercadonaProduct.getThumbnail());
                        product.setFormat(mercadonaProduct.getPackaging());

                        if (mercadonaProduct.getPriceInstructions() != null
                                && mercadonaProduct.getPriceInstructions().getUnitPrice() != null) {

                            product.setPrice(
                                    new BigDecimal(
                                            mercadonaProduct
                                                    .getPriceInstructions()
                                                    .getUnitPrice()
                                                    .trim()
                                    )
                            );
                        }

                        products.add(product);
                    }
                }
            }
        }

        return products;
    }
}