package com.micarro.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MainCategoryMapperTest {

    private final MainCategoryMapper mapper = new MainCategoryMapper();

    @Test
    void mapsKnownSubcategoriesExplicitly() {

        assertThat(mapper.map("Manzanas"))
                .isEqualTo(MainCategoryMapper.FRUTAS_VERDURAS);
        assertThat(mapper.map("Pollo"))
                .isEqualTo(MainCategoryMapper.CARNE);
        assertThat(mapper.map("Pescado fresco"))
                .isEqualTo(MainCategoryMapper.PESCADO_MARISCO);
        assertThat(mapper.map("Jamón serrano"))
                .isEqualTo(MainCategoryMapper.CHARCUTERIA);
        assertThat(mapper.map("Leche"))
                .isEqualTo(MainCategoryMapper.LECHE_HUEVOS_LACTEOS);
        assertThat(mapper.map("Pan de molde"))
                .isEqualTo(MainCategoryMapper.PANADERIA);
        assertThat(mapper.map("Arroz"))
                .isEqualTo(MainCategoryMapper.ARROZ_PASTA_LEGUMBRES);
        assertThat(mapper.map("Atún en conserva"))
                .isEqualTo(MainCategoryMapper.CONSERVAS);
        assertThat(mapper.map("Aceite de oliva"))
                .isEqualTo(MainCategoryMapper.ACEITES_SALSAS_ESPECIAS);
        assertThat(mapper.map("Galletas"))
                .isEqualTo(MainCategoryMapper.DESAYUNO_DULCES);
        assertThat(mapper.map("Frutos secos"))
                .isEqualTo(MainCategoryMapper.SNACKS_FRUTOS_SECOS);
        assertThat(mapper.map("Refrescos"))
                .isEqualTo(MainCategoryMapper.BEBIDAS);
        assertThat(mapper.map("Verduras congeladas"))
                .isEqualTo(MainCategoryMapper.CONGELADOS);
        assertThat(mapper.map("Pizzas"))
                .isEqualTo(MainCategoryMapper.PLATOS_PREPARADOS);
        assertThat(mapper.map("Detergente"))
                .isEqualTo(MainCategoryMapper.LIMPIEZA_HOGAR);
        assertThat(mapper.map("Champú"))
                .isEqualTo(MainCategoryMapper.HIGIENE_CUIDADO);
        assertThat(mapper.map("Pañales"))
                .isEqualTo(MainCategoryMapper.BEBE);
        assertThat(mapper.map("Pienso"))
                .isEqualTo(MainCategoryMapper.MASCOTAS);
        assertThat(mapper.map("Papel higiénico"))
                .isEqualTo(MainCategoryMapper.HOGAR_OTROS);
    }

    @Test
    void mapsUnknownSubcategoriesWithKeywordRules() {

        assertThat(mapper.map("Manzana Golden"))
                .isEqualTo(MainCategoryMapper.FRUTAS_VERDURAS);
        assertThat(mapper.map("Leche desnatada"))
                .isEqualTo(MainCategoryMapper.LECHE_HUEVOS_LACTEOS);
        assertThat(mapper.map("Pescado azul"))
                .isEqualTo(MainCategoryMapper.PESCADO_MARISCO);
        assertThat(mapper.map("Salsa de soja"))
                .isEqualTo(MainCategoryMapper.ACEITES_SALSAS_ESPECIAS);
        assertThat(mapper.map("Aperitivos salados"))
                .isEqualTo(MainCategoryMapper.SNACKS_FRUTOS_SECOS);
    }

    @Test
    void mapsUnknownSubcategoriesToHogarYOtros() {

        assertThat(mapper.map("Bricolaje y ferretería"))
                .isEqualTo(MainCategoryMapper.HOGAR_OTROS);
    }

    @Test
    void mapsNullAndBlankToHogarYOtros() {

        assertThat(mapper.map(null))
                .isEqualTo(MainCategoryMapper.HOGAR_OTROS);
        assertThat(mapper.map("  "))
                .isEqualTo(MainCategoryMapper.HOGAR_OTROS);
    }

    @Test
    void isCaseInsensitiveAndTrimsInput() {

        assertThat(mapper.map("  MANZANAS  "))
                .isEqualTo(MainCategoryMapper.FRUTAS_VERDURAS);
        assertThat(mapper.map("aceite de oliva"))
                .isEqualTo(MainCategoryMapper.ACEITES_SALSAS_ESPECIAS);
    }
}