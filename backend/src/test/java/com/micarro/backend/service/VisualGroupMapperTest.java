package com.micarro.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.junit.jupiter.api.Test;

class VisualGroupMapperTest {

    private final MainCategoryMapper mainCategoryMapper = new MainCategoryMapper();
    private final VisualGroupMapper visualGroupMapper = new VisualGroupMapper();

    @Test
    void everyKnownCategoryHasExactlyOneVisualGroup() {

        Set<String> assigned = new HashSet<>();

        for (String mainCategory : visualGroupMapper.mainCategories()) {
            for (String group : visualGroupMapper.groupsOf(mainCategory)) {
                for (String category : visualGroupMapper.categoriesOf(mainCategory, group)) {
                    assertThat(assigned.add(normalize(category)))
                            .as("categoría duplicada: %s", category)
                            .isTrue();
                }
            }
        }

        assertThat(assigned).hasSize(mainCategoryMapper.knownCategories().size());
    }

    @Test
    void everyKnownCategoryIsAssignedToAGroup() {

        Set<String> assigned = new HashSet<>();

        for (String mainCategory : visualGroupMapper.mainCategories()) {
            for (String group : visualGroupMapper.groupsOf(mainCategory)) {
                for (String category : visualGroupMapper.categoriesOf(mainCategory, group)) {
                    assigned.add(normalize(category));
                }
            }
        }

        assertThat(assigned).containsAll(mainCategoryMapper.knownCategories());
    }

    @Test
    void noUnknownCategoryIsInvented() {

        for (String category : visualGroupMapper.categories()) {
            assertThat(mainCategoryMapper.knownCategories())
                    .as("categoría no conocida: %s", category)
                    .contains(category);
        }
    }

    @Test
    void coverageMatchesMainCategoryMapper() {

        assertThat(visualGroupMapper.categories().size())
                .isEqualTo(mainCategoryMapper.knownCategories().size());
    }

    @Test
    void mapsKnownCategoriesToTheirGroup() {

        assertThat(visualGroupMapper.visualGroup("Manzana y pera"))
                .isEqualTo("Fruta");
        assertThat(visualGroupMapper.visualGroup("Champú"))
                .isEqualTo("Cabello");
        assertThat(visualGroupMapper.visualGroup("Atún"))
                .isEqualTo("Conservas de pescado");
        assertThat(visualGroupMapper.visualGroup("Pasta de dientes"))
                .isEqualTo("Higiene bucal");
    }

    @Test
    void unknownCategoryHasNoGroup() {

        assertThat(visualGroupMapper.visualGroup("Categoría inexistente"))
                .isNull();
        assertThat(visualGroupMapper.visualGroup(null)).isNull();
    }

    @Test
    void approvedGroupNamesArePresent() {

        assertThat(visualGroupMapper.groupsOf(MainCategoryMapper.BEBIDAS))
                .contains("Batidos y bebidas vegetales", "Vinos y aperitivos");
        assertThat(visualGroupMapper.groupsOf(MainCategoryMapper.PESCADO_MARISCO))
                .contains("Ahumados y preparados");
        assertThat(visualGroupMapper.groupsOf(MainCategoryMapper.PLATOS_PREPARADOS))
                .contains("Platos preparados");
        assertThat(visualGroupMapper.groupsOf(MainCategoryMapper.HOGAR_OTROS))
                .contains("Varios");
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}