package com.micarro.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class MainCategoryMapperTest {

    private final MainCategoryMapper mapper = new MainCategoryMapper();

    /*
     * Las 410 subcategorías conocidas del catálogo con su mainCategory.
     * Las claves usan la capitalización real de Product.category; el mapper
     * normaliza a minúsculas internamente.
     */
    private static final Map<String, String> EXPECTED = buildExpected();

    @Test
    void knownSubcategoriesMapToTheirMainCategory() {

        for (Map.Entry<String, String> entry : EXPECTED.entrySet()) {
            assertThat(mapper.map(entry.getKey()))
                    .as("subcategoría '%s'", entry.getKey())
                    .isEqualTo(entry.getValue());
        }
    }

    @Test
    void knownSubcategoriesNeverFallToFallbackUnlessAssigned() {

        for (Map.Entry<String, String> entry : EXPECTED.entrySet()) {
            if (entry.getValue().equals(MainCategoryMapper.HOGAR_OTROS)) {
                continue;
            }
            assertThat(mapper.map(entry.getKey()))
                    .as("subcategoría '%s' no debe caer al fallback", entry.getKey())
                    .isNotEqualTo(MainCategoryMapper.HOGAR_OTROS);
        }
    }

    @Test
    void knownSubcategoriesMapOnlyToDefinedMainCategories() {

        for (String result : EXPECTED.values()) {
            assertThat(allMainCategories()).contains(result);
        }
    }

    @Test
    void knownSubcategoriesCoverage() {

        assertThat(EXPECTED.size()).isEqualTo(410);
    }

    @Test
    void mapsUnknownSubcategoriesWithKeywordRules() {

        assertThat(mapper.map("Manzana Golden"))
                .isEqualTo(MainCategoryMapper.FRUTAS_VERDURAS);
        assertThat(mapper.map("Leche sin lactosa"))
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

        assertThat(mapper.map("  MANZANA Y PERA  "))
                .isEqualTo(MainCategoryMapper.FRUTAS_VERDURAS);
        assertThat(mapper.map("aceite de oliva"))
                .isEqualTo(MainCategoryMapper.ACEITES_SALSAS_ESPECIAS);
    }

    private static java.util.List<String> allMainCategories() {
        return java.util.List.of(
                MainCategoryMapper.FRUTAS_VERDURAS,
                MainCategoryMapper.CARNE,
                MainCategoryMapper.PESCADO_MARISCO,
                MainCategoryMapper.CHARCUTERIA,
                MainCategoryMapper.LECHE_HUEVOS_LACTEOS,
                MainCategoryMapper.PANADERIA,
                MainCategoryMapper.ARROZ_PASTA_LEGUMBRES,
                MainCategoryMapper.CONSERVAS,
                MainCategoryMapper.ACEITES_SALSAS_ESPECIAS,
                MainCategoryMapper.DESAYUNO_DULCES,
                MainCategoryMapper.SNACKS_FRUTOS_SECOS,
                MainCategoryMapper.BEBIDAS,
                MainCategoryMapper.CONGELADOS,
                MainCategoryMapper.PLATOS_PREPARADOS,
                MainCategoryMapper.LIMPIEZA_HOGAR,
                MainCategoryMapper.HIGIENE_CUIDADO,
                MainCategoryMapper.BEBE,
                MainCategoryMapper.MASCOTAS,
                MainCategoryMapper.HOGAR_OTROS
        );
    }

    private static Map<String, String> buildExpected() {

        Map<String, String> map = new HashMap<>();

        map.put("Calabacín y pimiento", MainCategoryMapper.FRUTAS_VERDURAS);
        map.put("Cebolla y ajo", MainCategoryMapper.FRUTAS_VERDURAS);
        map.put("Cítricos", MainCategoryMapper.FRUTAS_VERDURAS);
        map.put("Fruta", MainCategoryMapper.FRUTAS_VERDURAS);
        map.put("Fruta tropical", MainCategoryMapper.FRUTAS_VERDURAS);
        map.put("Lechuga", MainCategoryMapper.FRUTAS_VERDURAS);
        map.put("Limón", MainCategoryMapper.FRUTAS_VERDURAS);
        map.put("Manzana y pera", MainCategoryMapper.FRUTAS_VERDURAS);
        map.put("Melocotón", MainCategoryMapper.FRUTAS_VERDURAS);
        map.put("Melón y sandía", MainCategoryMapper.FRUTAS_VERDURAS);
        map.put("Naranja", MainCategoryMapper.FRUTAS_VERDURAS);
        map.put("Otras frutas", MainCategoryMapper.FRUTAS_VERDURAS);
        map.put("Otras verduras y hortalizas", MainCategoryMapper.FRUTAS_VERDURAS);
        map.put("Patata", MainCategoryMapper.FRUTAS_VERDURAS);
        map.put("Patatas", MainCategoryMapper.FRUTAS_VERDURAS);
        map.put("Pepino y zanahoria", MainCategoryMapper.FRUTAS_VERDURAS);
        map.put("Piña", MainCategoryMapper.FRUTAS_VERDURAS);
        map.put("Plátano y uva", MainCategoryMapper.FRUTAS_VERDURAS);
        map.put("Repollo y col", MainCategoryMapper.FRUTAS_VERDURAS);
        map.put("Setas y champiñones", MainCategoryMapper.FRUTAS_VERDURAS);
        map.put("Tomate", MainCategoryMapper.FRUTAS_VERDURAS);
        map.put("Verdura", MainCategoryMapper.FRUTAS_VERDURAS);

        map.put("Carne", MainCategoryMapper.CARNE);
        map.put("Cerdo", MainCategoryMapper.CARNE);
        map.put("Conejo", MainCategoryMapper.CARNE);
        map.put("Cordero", MainCategoryMapper.CARNE);
        map.put("Hamburguesas", MainCategoryMapper.CARNE);
        map.put("Pavo y otras aves", MainCategoryMapper.CARNE);
        map.put("Pavo y otros", MainCategoryMapper.CARNE);
        map.put("Picadas y otros", MainCategoryMapper.CARNE);
        map.put("Pollo", MainCategoryMapper.CARNE);
        map.put("Vacuno", MainCategoryMapper.CARNE);

        map.put("Ahumados", MainCategoryMapper.PESCADO_MARISCO);
        map.put("Bacalao", MainCategoryMapper.PESCADO_MARISCO);
        map.put("Boquerón", MainCategoryMapper.PESCADO_MARISCO);
        map.put("Corvina", MainCategoryMapper.PESCADO_MARISCO);
        map.put("Dorada", MainCategoryMapper.PESCADO_MARISCO);
        map.put("Lubina", MainCategoryMapper.PESCADO_MARISCO);
        map.put("Marisco", MainCategoryMapper.PESCADO_MARISCO);
        map.put("Marisco de concha", MainCategoryMapper.PESCADO_MARISCO);
        map.put("Merluza", MainCategoryMapper.PESCADO_MARISCO);
        map.put("Pescado", MainCategoryMapper.PESCADO_MARISCO);
        map.put("Rodaballo", MainCategoryMapper.PESCADO_MARISCO);
        map.put("Salmón", MainCategoryMapper.PESCADO_MARISCO);
        map.put("Sardina", MainCategoryMapper.PESCADO_MARISCO);
        map.put("Sepia, pulpo y calamar", MainCategoryMapper.PESCADO_MARISCO);
        map.put("Surimi y otros", MainCategoryMapper.PESCADO_MARISCO);
        map.put("Trucha", MainCategoryMapper.PESCADO_MARISCO);

        map.put("Bacón", MainCategoryMapper.CHARCUTERIA);
        map.put("Chopped", MainCategoryMapper.CHARCUTERIA);
        map.put("Chorizo", MainCategoryMapper.CHARCUTERIA);
        map.put("Embutido", MainCategoryMapper.CHARCUTERIA);
        map.put("Jamón cocido", MainCategoryMapper.CHARCUTERIA);
        map.put("Jamón serrano", MainCategoryMapper.CHARCUTERIA);
        map.put("Lomo y otros", MainCategoryMapper.CHARCUTERIA);
        map.put("Mortadela", MainCategoryMapper.CHARCUTERIA);
        map.put("Salchichas", MainCategoryMapper.CHARCUTERIA);
        map.put("Salchichón", MainCategoryMapper.CHARCUTERIA);
        map.put("Sobrasada", MainCategoryMapper.CHARCUTERIA);

        map.put("Bífidus de sabores", MainCategoryMapper.LECHE_HUEVOS_LACTEOS);
        map.put("Bífidus naturales", MainCategoryMapper.LECHE_HUEVOS_LACTEOS);
        map.put("Flan", MainCategoryMapper.LECHE_HUEVOS_LACTEOS);
        map.put("Fruta + leche", MainCategoryMapper.LECHE_HUEVOS_LACTEOS);
        map.put("Fruta variada y otros sabores", MainCategoryMapper.LECHE_HUEVOS_LACTEOS);
        map.put("Gelatina", MainCategoryMapper.LECHE_HUEVOS_LACTEOS);
        map.put("Huevos", MainCategoryMapper.LECHE_HUEVOS_LACTEOS);
        map.put("L-Casei", MainCategoryMapper.LECHE_HUEVOS_LACTEOS);
        map.put("Leche", MainCategoryMapper.LECHE_HUEVOS_LACTEOS);
        map.put("Leche condensada y otros", MainCategoryMapper.LECHE_HUEVOS_LACTEOS);
        map.put("Leche desnatada", MainCategoryMapper.LECHE_HUEVOS_LACTEOS);
        map.put("Leche en polvo", MainCategoryMapper.LECHE_HUEVOS_LACTEOS);
        map.put("Leche entera", MainCategoryMapper.LECHE_HUEVOS_LACTEOS);
        map.put("Leche semidesnatada", MainCategoryMapper.LECHE_HUEVOS_LACTEOS);
        map.put("Mantequilla", MainCategoryMapper.LECHE_HUEVOS_LACTEOS);
        map.put("Margarina", MainCategoryMapper.LECHE_HUEVOS_LACTEOS);
        map.put("Nata", MainCategoryMapper.LECHE_HUEVOS_LACTEOS);
        map.put("Natillas", MainCategoryMapper.LECHE_HUEVOS_LACTEOS);
        map.put("Otros postres", MainCategoryMapper.LECHE_HUEVOS_LACTEOS);
        map.put("Otros sabores", MainCategoryMapper.LECHE_HUEVOS_LACTEOS);
        map.put("Postres de soja", MainCategoryMapper.LECHE_HUEVOS_LACTEOS);
        map.put("Queso curado", MainCategoryMapper.LECHE_HUEVOS_LACTEOS);
        map.put("Queso en porciones", MainCategoryMapper.LECHE_HUEVOS_LACTEOS);
        map.put("Queso especialidades", MainCategoryMapper.LECHE_HUEVOS_LACTEOS);
        map.put("Queso fresco", MainCategoryMapper.LECHE_HUEVOS_LACTEOS);
        map.put("Queso lonchas", MainCategoryMapper.LECHE_HUEVOS_LACTEOS);
        map.put("Queso rallado", MainCategoryMapper.LECHE_HUEVOS_LACTEOS);
        map.put("Queso semicurado", MainCategoryMapper.LECHE_HUEVOS_LACTEOS);
        map.put("Queso tierno", MainCategoryMapper.LECHE_HUEVOS_LACTEOS);
        map.put("Queso untable", MainCategoryMapper.LECHE_HUEVOS_LACTEOS);
        map.put("Yogures de sabores", MainCategoryMapper.LECHE_HUEVOS_LACTEOS);
        map.put("Yogures desnatados", MainCategoryMapper.LECHE_HUEVOS_LACTEOS);
        map.put("Yogures griegos", MainCategoryMapper.LECHE_HUEVOS_LACTEOS);
        map.put("Yogures líquidos", MainCategoryMapper.LECHE_HUEVOS_LACTEOS);
        map.put("Yogures naturales", MainCategoryMapper.LECHE_HUEVOS_LACTEOS);

        map.put("Barra de pan", MainCategoryMapper.PANADERIA);
        map.put("Base de pizza", MainCategoryMapper.PANADERIA);
        map.put("Bollería dulce", MainCategoryMapper.PANADERIA);
        map.put("Bollería envasada", MainCategoryMapper.PANADERIA);
        map.put("Bollería salada", MainCategoryMapper.PANADERIA);
        map.put("Churros", MainCategoryMapper.PANADERIA);
        map.put("Masas", MainCategoryMapper.PANADERIA);
        map.put("Otros panes", MainCategoryMapper.PANADERIA);
        map.put("Pan de bocadillo", MainCategoryMapper.PANADERIA);
        map.put("Pan de hamburguesa y wrap", MainCategoryMapper.PANADERIA);
        map.put("Pan de molde", MainCategoryMapper.PANADERIA);
        map.put("Pan rallado", MainCategoryMapper.PANADERIA);
        map.put("Pan rebanado", MainCategoryMapper.PANADERIA);
        map.put("Pan tostado", MainCategoryMapper.PANADERIA);
        map.put("Picos", MainCategoryMapper.PANADERIA);
        map.put("Roscas, quiche y baguettes", MainCategoryMapper.PANADERIA);
        map.put("Rosquilletas", MainCategoryMapper.PANADERIA);

        map.put("Alubias", MainCategoryMapper.ARROZ_PASTA_LEGUMBRES);
        map.put("Arroz", MainCategoryMapper.ARROZ_PASTA_LEGUMBRES);
        map.put("Fideos", MainCategoryMapper.ARROZ_PASTA_LEGUMBRES);
        map.put("Fideos orientales", MainCategoryMapper.ARROZ_PASTA_LEGUMBRES);
        map.put("Garbanzos", MainCategoryMapper.ARROZ_PASTA_LEGUMBRES);
        map.put("Harina", MainCategoryMapper.ARROZ_PASTA_LEGUMBRES);
        map.put("Lentejas y otros", MainCategoryMapper.ARROZ_PASTA_LEGUMBRES);
        map.put("Levadura y preparado repostería", MainCategoryMapper.ARROZ_PASTA_LEGUMBRES);
        map.put("Macarrones, pajaritas y hélices", MainCategoryMapper.ARROZ_PASTA_LEGUMBRES);
        map.put("Pasta", MainCategoryMapper.ARROZ_PASTA_LEGUMBRES);
        map.put("Pasta rellena", MainCategoryMapper.ARROZ_PASTA_LEGUMBRES);
        map.put("Spaghetti y tallarines", MainCategoryMapper.ARROZ_PASTA_LEGUMBRES);

        map.put("Atún", MainCategoryMapper.CONSERVAS);
        map.put("Berberechos y almejas", MainCategoryMapper.CONSERVAS);
        map.put("Bonito", MainCategoryMapper.CONSERVAS);
        map.put("Caballa y melva", MainCategoryMapper.CONSERVAS);
        map.put("Conservas fruta", MainCategoryMapper.CONSERVAS);
        map.put("Conservas verdura", MainCategoryMapper.CONSERVAS);
        map.put("Mejillones", MainCategoryMapper.CONSERVAS);
        map.put("Otras conservas de pescado", MainCategoryMapper.CONSERVAS);
        map.put("Paté", MainCategoryMapper.CONSERVAS);
        map.put("Pepinillos y otros encurtidos", MainCategoryMapper.CONSERVAS);
        map.put("Salazones", MainCategoryMapper.CONSERVAS);
        map.put("Sardinas", MainCategoryMapper.CONSERVAS);

        map.put("Aceite de oliva", MainCategoryMapper.ACEITES_SALSAS_ESPECIAS);
        map.put("Allioli", MainCategoryMapper.ACEITES_SALSAS_ESPECIAS);
        map.put("Colorante y pimentón", MainCategoryMapper.ACEITES_SALSAS_ESPECIAS);
        map.put("Hierbas aromáticas", MainCategoryMapper.ACEITES_SALSAS_ESPECIAS);
        map.put("Ketchup", MainCategoryMapper.ACEITES_SALSAS_ESPECIAS);
        map.put("Mayonesa", MainCategoryMapper.ACEITES_SALSAS_ESPECIAS);
        map.put("Mostaza", MainCategoryMapper.ACEITES_SALSAS_ESPECIAS);
        map.put("Otras especias", MainCategoryMapper.ACEITES_SALSAS_ESPECIAS);
        map.put("Otras salsas", MainCategoryMapper.ACEITES_SALSAS_ESPECIAS);
        map.put("Otros aceites", MainCategoryMapper.ACEITES_SALSAS_ESPECIAS);
        map.put("Pimienta", MainCategoryMapper.ACEITES_SALSAS_ESPECIAS);
        map.put("Sal y bicarbonato", MainCategoryMapper.ACEITES_SALSAS_ESPECIAS);
        map.put("Salsas orientales", MainCategoryMapper.ACEITES_SALSAS_ESPECIAS);
        map.put("Salsas para carnes", MainCategoryMapper.ACEITES_SALSAS_ESPECIAS);
        map.put("Salsas para pasta", MainCategoryMapper.ACEITES_SALSAS_ESPECIAS);
        map.put("Sazonadores", MainCategoryMapper.ACEITES_SALSAS_ESPECIAS);
        map.put("Vinagre y otros aderezos", MainCategoryMapper.ACEITES_SALSAS_ESPECIAS);

        map.put("Azúcar", MainCategoryMapper.DESAYUNO_DULCES);
        map.put("Barritas de cereales", MainCategoryMapper.DESAYUNO_DULCES);
        map.put("Bombones", MainCategoryMapper.DESAYUNO_DULCES);
        map.put("Cacao soluble", MainCategoryMapper.DESAYUNO_DULCES);
        map.put("Café en grano", MainCategoryMapper.DESAYUNO_DULCES);
        map.put("Café molido", MainCategoryMapper.DESAYUNO_DULCES);
        map.put("Café soluble", MainCategoryMapper.DESAYUNO_DULCES);
        map.put("Cápsulas compatibles Dolce gusto", MainCategoryMapper.DESAYUNO_DULCES);
        map.put("Cápsulas compatibles Nespresso", MainCategoryMapper.DESAYUNO_DULCES);
        map.put("Cápsulas compatibles Tassimo", MainCategoryMapper.DESAYUNO_DULCES);
        map.put("Caramelos", MainCategoryMapper.DESAYUNO_DULCES);
        map.put("Cereales", MainCategoryMapper.DESAYUNO_DULCES);
        map.put("Cereales integrales y muesli", MainCategoryMapper.DESAYUNO_DULCES);
        map.put("Chicles", MainCategoryMapper.DESAYUNO_DULCES);
        map.put("Chocolate a la taza", MainCategoryMapper.DESAYUNO_DULCES);
        map.put("Chocolate blanco", MainCategoryMapper.DESAYUNO_DULCES);
        map.put("Chocolate con leche", MainCategoryMapper.DESAYUNO_DULCES);
        map.put("Chocolate negro", MainCategoryMapper.DESAYUNO_DULCES);
        map.put("Chocolatinas", MainCategoryMapper.DESAYUNO_DULCES);
        map.put("Con chocolate y rellenas", MainCategoryMapper.DESAYUNO_DULCES);
        map.put("Confitura y otros", MainCategoryMapper.DESAYUNO_DULCES);
        map.put("Cremas de untar", MainCategoryMapper.DESAYUNO_DULCES);
        map.put("Edulcorante y otros", MainCategoryMapper.DESAYUNO_DULCES);
        map.put("Galletas desayuno", MainCategoryMapper.DESAYUNO_DULCES);
        map.put("Galletas integrales y digestive", MainCategoryMapper.DESAYUNO_DULCES);
        map.put("Galletas surtidas", MainCategoryMapper.DESAYUNO_DULCES);
        map.put("Golosinas", MainCategoryMapper.DESAYUNO_DULCES);
        map.put("Hierbas", MainCategoryMapper.DESAYUNO_DULCES);
        map.put("Infusiones", MainCategoryMapper.DESAYUNO_DULCES);
        map.put("Mermelada", MainCategoryMapper.DESAYUNO_DULCES);
        map.put("Miel", MainCategoryMapper.DESAYUNO_DULCES);
        map.put("Pastelitos surtidos", MainCategoryMapper.DESAYUNO_DULCES);
        map.put("Tartas", MainCategoryMapper.DESAYUNO_DULCES);
        map.put("Tartas infantiles", MainCategoryMapper.DESAYUNO_DULCES);
        map.put("Té", MainCategoryMapper.DESAYUNO_DULCES);
        map.put("Turrones", MainCategoryMapper.DESAYUNO_DULCES);

        map.put("Aceitunas negras", MainCategoryMapper.SNACKS_FRUTOS_SECOS);
        map.put("Aceitunas verdes", MainCategoryMapper.SNACKS_FRUTOS_SECOS);
        map.put("Cóctel y banderillas", MainCategoryMapper.SNACKS_FRUTOS_SECOS);
        map.put("Crakers y tartaletas", MainCategoryMapper.SNACKS_FRUTOS_SECOS);
        map.put("Fruta desecada", MainCategoryMapper.SNACKS_FRUTOS_SECOS);
        map.put("Frutos secos", MainCategoryMapper.SNACKS_FRUTOS_SECOS);
        map.put("Patatas fritas", MainCategoryMapper.SNACKS_FRUTOS_SECOS);
        map.put("Picatostes", MainCategoryMapper.SNACKS_FRUTOS_SECOS);
        map.put("Snacks", MainCategoryMapper.SNACKS_FRUTOS_SECOS);
        map.put("Tortitas", MainCategoryMapper.SNACKS_FRUTOS_SECOS);

        map.put("Agua con gas", MainCategoryMapper.BEBIDAS);
        map.put("Agua sin gas", MainCategoryMapper.BEBIDAS);
        map.put("Anís", MainCategoryMapper.BEBIDAS);
        map.put("Batidos", MainCategoryMapper.BEBIDAS);
        map.put("Bebidas frías", MainCategoryMapper.BEBIDAS);
        map.put("Bebidas vegetales", MainCategoryMapper.BEBIDAS);
        map.put("Brandy", MainCategoryMapper.BEBIDAS);
        map.put("Castilla la Mancha", MainCategoryMapper.BEBIDAS);
        map.put("Cava brut", MainCategoryMapper.BEBIDAS);
        map.put("Cava semi seco", MainCategoryMapper.BEBIDAS);
        map.put("Cerveza botella y botellín", MainCategoryMapper.BEBIDAS);
        map.put("Cerveza lata", MainCategoryMapper.BEBIDAS);
        map.put("Cocktails", MainCategoryMapper.BEBIDAS);
        map.put("Cola clásica", MainCategoryMapper.BEBIDAS);
        map.put("Cola sin cafeína", MainCategoryMapper.BEBIDAS);
        map.put("Cola zero", MainCategoryMapper.BEBIDAS);
        map.put("Combinado de cerveza", MainCategoryMapper.BEBIDAS);
        map.put("Energético", MainCategoryMapper.BEBIDAS);
        map.put("Gaseosa", MainCategoryMapper.BEBIDAS);
        map.put("Ginebra", MainCategoryMapper.BEBIDAS);
        map.put("Isotónico", MainCategoryMapper.BEBIDAS);
        map.put("Licores sin alcohol", MainCategoryMapper.BEBIDAS);
        map.put("Lima limón", MainCategoryMapper.BEBIDAS);
        map.put("Otros licores", MainCategoryMapper.BEBIDAS);
        map.put("Otros refrescos sin gas", MainCategoryMapper.BEBIDAS);
        map.put("Otros vinos tintos", MainCategoryMapper.BEBIDAS);
        map.put("Ribera del Duero", MainCategoryMapper.BEBIDAS);
        map.put("Rioja", MainCategoryMapper.BEBIDAS);
        map.put("Rioja y otras denominaciones", MainCategoryMapper.BEBIDAS);
        map.put("Ron", MainCategoryMapper.BEBIDAS);
        map.put("Rueda", MainCategoryMapper.BEBIDAS);
        map.put("Sidra", MainCategoryMapper.BEBIDAS);
        map.put("Smoothie", MainCategoryMapper.BEBIDAS);
        map.put("Tinto de verano y sangría", MainCategoryMapper.BEBIDAS);
        map.put("Tónica y bitter", MainCategoryMapper.BEBIDAS);
        map.put("Vermouth y aperitivos", MainCategoryMapper.BEBIDAS);
        map.put("Vino blanco de mesa", MainCategoryMapper.BEBIDAS);
        map.put("Vino lambrusco y espumoso", MainCategoryMapper.BEBIDAS);
        map.put("Vino rosado", MainCategoryMapper.BEBIDAS);
        map.put("Vinos dulces y mosto", MainCategoryMapper.BEBIDAS);
        map.put("Vinos semidulces", MainCategoryMapper.BEBIDAS);
        map.put("Vino tinto de mesa", MainCategoryMapper.BEBIDAS);
        map.put("Vodka", MainCategoryMapper.BEBIDAS);
        map.put("Whisky", MainCategoryMapper.BEBIDAS);

        map.put("Barras de helado y barquillos", MainCategoryMapper.CONGELADOS);
        map.put("Carne rebozada", MainCategoryMapper.CONGELADOS);
        map.put("Cucuruchos", MainCategoryMapper.CONGELADOS);
        map.put("Empanados y elaborados", MainCategoryMapper.CONGELADOS);
        map.put("Granizados y helados de hielo", MainCategoryMapper.CONGELADOS);
        map.put("Hielo", MainCategoryMapper.CONGELADOS);
        map.put("Otros Helados", MainCategoryMapper.CONGELADOS);
        map.put("Pescado congelado", MainCategoryMapper.CONGELADOS);
        map.put("Pescado rebozado congelado", MainCategoryMapper.CONGELADOS);
        map.put("Pizzas congeladas", MainCategoryMapper.CONGELADOS);
        map.put("Sepia, pulpo y calamar congelado", MainCategoryMapper.CONGELADOS);
        map.put("Tarrinas", MainCategoryMapper.CONGELADOS);
        map.put("Verdura rebozada y otros", MainCategoryMapper.CONGELADOS);
        map.put("Verduras al vapor", MainCategoryMapper.CONGELADOS);

        map.put("Caldo en pastillas", MainCategoryMapper.PLATOS_PREPARADOS);
        map.put("Caldo líquido", MainCategoryMapper.PLATOS_PREPARADOS);
        map.put("Cremas y puré", MainCategoryMapper.PLATOS_PREPARADOS);
        map.put("Ensalada preparada", MainCategoryMapper.PLATOS_PREPARADOS);
        map.put("Ensaladilla", MainCategoryMapper.PLATOS_PREPARADOS);
        map.put("Gazpacho y salmorejo", MainCategoryMapper.PLATOS_PREPARADOS);
        map.put("Hummus y otros", MainCategoryMapper.PLATOS_PREPARADOS);
        map.put("Lasaña y canelones", MainCategoryMapper.PLATOS_PREPARADOS);
        map.put("Pizzas", MainCategoryMapper.PLATOS_PREPARADOS);
        map.put("Pizzas refrigeradas", MainCategoryMapper.PLATOS_PREPARADOS);
        map.put("Platos calientes", MainCategoryMapper.PLATOS_PREPARADOS);
        map.put("Platos de cuchara", MainCategoryMapper.PLATOS_PREPARADOS);
        map.put("Platos fríos", MainCategoryMapper.PLATOS_PREPARADOS);
        map.put("Sándwich", MainCategoryMapper.PLATOS_PREPARADOS);
        map.put("Sándwiches", MainCategoryMapper.PLATOS_PREPARADOS);
        map.put("Sopa", MainCategoryMapper.PLATOS_PREPARADOS);
        map.put("Tortilla", MainCategoryMapper.PLATOS_PREPARADOS);

        map.put("Absorbeolores y antihumedad", MainCategoryMapper.LIMPIEZA_HOGAR);
        map.put("Activador y antical lavadora", MainCategoryMapper.LIMPIEZA_HOGAR);
        map.put("Ambientador automático", MainCategoryMapper.LIMPIEZA_HOGAR);
        map.put("Ambientador coche", MainCategoryMapper.LIMPIEZA_HOGAR);
        map.put("Ambientador decorativo y otros", MainCategoryMapper.LIMPIEZA_HOGAR);
        map.put("Ambientador eléctrico", MainCategoryMapper.LIMPIEZA_HOGAR);
        map.put("Ambientador spray", MainCategoryMapper.LIMPIEZA_HOGAR);
        map.put("Amoníaco y salfumán", MainCategoryMapper.LIMPIEZA_HOGAR);
        map.put("Bayeta", MainCategoryMapper.LIMPIEZA_HOGAR);
        map.put("Bolsas de basura", MainCategoryMapper.LIMPIEZA_HOGAR);
        map.put("Cubos y barreños", MainCategoryMapper.LIMPIEZA_HOGAR);
        map.put("Detergente en polvo y monodosis", MainCategoryMapper.LIMPIEZA_HOGAR);
        map.put("Detergente lavado a mano", MainCategoryMapper.LIMPIEZA_HOGAR);
        map.put("Detergente líquido y gel", MainCategoryMapper.LIMPIEZA_HOGAR);
        map.put("Estropajo", MainCategoryMapper.LIMPIEZA_HOGAR);
        map.put("Fregonas, escobas y mopas", MainCategoryMapper.LIMPIEZA_HOGAR);
        map.put("Friegasuelos", MainCategoryMapper.LIMPIEZA_HOGAR);
        map.put("Guantes", MainCategoryMapper.LIMPIEZA_HOGAR);
        map.put("Insecticida eléctrico y otros", MainCategoryMapper.LIMPIEZA_HOGAR);
        map.put("Insecticida spray", MainCategoryMapper.LIMPIEZA_HOGAR);
        map.put("Lejía", MainCategoryMapper.LIMPIEZA_HOGAR);
        map.put("Limpiacristales", MainCategoryMapper.LIMPIEZA_HOGAR);
        map.put("Limpiahogar", MainCategoryMapper.LIMPIEZA_HOGAR);
        map.put("Limpiavajilla a mano", MainCategoryMapper.LIMPIEZA_HOGAR);
        map.put("Limpiavajilla a máquina", MainCategoryMapper.LIMPIEZA_HOGAR);
        map.put("Limpieza baño y WC", MainCategoryMapper.LIMPIEZA_HOGAR);
        map.put("Limpieza cocina", MainCategoryMapper.LIMPIEZA_HOGAR);
        map.put("Limpieza de calzado", MainCategoryMapper.LIMPIEZA_HOGAR);
        map.put("Limpieza muebles", MainCategoryMapper.LIMPIEZA_HOGAR);
        map.put("Monodosis", MainCategoryMapper.LIMPIEZA_HOGAR);
        map.put("Multiusos y otros", MainCategoryMapper.LIMPIEZA_HOGAR);
        map.put("Otros utensilios de limpieza", MainCategoryMapper.LIMPIEZA_HOGAR);
        map.put("Quitamanchas", MainCategoryMapper.LIMPIEZA_HOGAR);
        map.put("Rollo cocina", MainCategoryMapper.LIMPIEZA_HOGAR);
        map.put("Suavizante", MainCategoryMapper.LIMPIEZA_HOGAR);

        map.put("Aceite corporal", MainCategoryMapper.HIGIENE_CUIDADO);
        map.put("Aceite y crema", MainCategoryMapper.HIGIENE_CUIDADO);
        map.put("Acondicionador", MainCategoryMapper.HIGIENE_CUIDADO);
        map.put("After shave", MainCategoryMapper.HIGIENE_CUIDADO);
        map.put("Aseo y cuidado", MainCategoryMapper.HIGIENE_CUIDADO);
        map.put("Bandas, cera y crema", MainCategoryMapper.HIGIENE_CUIDADO);
        map.put("Botiquín", MainCategoryMapper.HIGIENE_CUIDADO);
        map.put("Braguita y otros", MainCategoryMapper.HIGIENE_CUIDADO);
        map.put("Cejas", MainCategoryMapper.HIGIENE_CUIDADO);
        map.put("Cepillo de dientes", MainCategoryMapper.HIGIENE_CUIDADO);
        map.put("Champú", MainCategoryMapper.HIGIENE_CUIDADO);
        map.put("Champú anticaspa", MainCategoryMapper.HIGIENE_CUIDADO);
        map.put("Champú y jabón", MainCategoryMapper.HIGIENE_CUIDADO);
        map.put("Colesterol y otros", MainCategoryMapper.HIGIENE_CUIDADO);
        map.put("Colonia", MainCategoryMapper.HIGIENE_CUIDADO);
        map.put("Coloración castaño, marrón y rubio oscuro", MainCategoryMapper.HIGIENE_CUIDADO);
        map.put("Coloración hombre", MainCategoryMapper.HIGIENE_CUIDADO);
        map.put("Coloración negro y castaño oscuro", MainCategoryMapper.HIGIENE_CUIDADO);
        map.put("Coloración rojo, cobre y violín", MainCategoryMapper.HIGIENE_CUIDADO);
        map.put("Coloración rubio", MainCategoryMapper.HIGIENE_CUIDADO);
        map.put("Coloración superaclarante y platino", MainCategoryMapper.HIGIENE_CUIDADO);
        map.put("Colorete", MainCategoryMapper.HIGIENE_CUIDADO);
        map.put("Colutorio e hilo dental", MainCategoryMapper.HIGIENE_CUIDADO);
        map.put("Compresas", MainCategoryMapper.HIGIENE_CUIDADO);
        map.put("Contorno de ojos", MainCategoryMapper.HIGIENE_CUIDADO);
        map.put("Correctores y prebase", MainCategoryMapper.HIGIENE_CUIDADO);
        map.put("Crema corporal", MainCategoryMapper.HIGIENE_CUIDADO);
        map.put("Crema de cara", MainCategoryMapper.HIGIENE_CUIDADO);
        map.put("Crema manos", MainCategoryMapper.HIGIENE_CUIDADO);
        map.put("Crema pies", MainCategoryMapper.HIGIENE_CUIDADO);
        map.put("Cremas", MainCategoryMapper.HIGIENE_CUIDADO);
        map.put("Crema y gel de cara", MainCategoryMapper.HIGIENE_CUIDADO);
        map.put("Cuchilla", MainCategoryMapper.HIGIENE_CUIDADO);
        map.put("Cuidado", MainCategoryMapper.HIGIENE_CUIDADO);
        map.put("Cuidado de uñas y complementos", MainCategoryMapper.HIGIENE_CUIDADO);
        map.put("Desodorante roll on y stick", MainCategoryMapper.HIGIENE_CUIDADO);
        map.put("Desodorante Spray", MainCategoryMapper.HIGIENE_CUIDADO);
        map.put("Esponjas", MainCategoryMapper.HIGIENE_CUIDADO);
        map.put("Espuma de afeitar", MainCategoryMapper.HIGIENE_CUIDADO);
        map.put("Espuma y laca", MainCategoryMapper.HIGIENE_CUIDADO);
        map.put("Fitoterapia", MainCategoryMapper.HIGIENE_CUIDADO);
        map.put("Gel", MainCategoryMapper.HIGIENE_CUIDADO);
        map.put("Gomina y cera", MainCategoryMapper.HIGIENE_CUIDADO);
        map.put("Jabón de manos", MainCategoryMapper.HIGIENE_CUIDADO);
        map.put("Laca de uñas", MainCategoryMapper.HIGIENE_CUIDADO);
        map.put("Limpieza de cara", MainCategoryMapper.HIGIENE_CUIDADO);
        map.put("Lotes hombre", MainCategoryMapper.HIGIENE_CUIDADO);
        map.put("Lotes mujer", MainCategoryMapper.HIGIENE_CUIDADO);
        map.put("Maquillaje compacto", MainCategoryMapper.HIGIENE_CUIDADO);
        map.put("Maquillaje fluido", MainCategoryMapper.HIGIENE_CUIDADO);
        map.put("Maquinillas de afeitar", MainCategoryMapper.HIGIENE_CUIDADO);
        map.put("Máscara de pestañas", MainCategoryMapper.HIGIENE_CUIDADO);
        map.put("Mascarilla", MainCategoryMapper.HIGIENE_CUIDADO);
        map.put("Otros desodorantes", MainCategoryMapper.HIGIENE_CUIDADO);
        map.put("Pañales para adulto", MainCategoryMapper.HIGIENE_CUIDADO);
        map.put("Perfilador de ojos", MainCategoryMapper.HIGIENE_CUIDADO);
        map.put("Perfilador labios", MainCategoryMapper.HIGIENE_CUIDADO);
        map.put("Perfume y colonia hombre", MainCategoryMapper.HIGIENE_CUIDADO);
        map.put("Perfume y colonia mujer", MainCategoryMapper.HIGIENE_CUIDADO);
        map.put("Pinceles y brochas", MainCategoryMapper.HIGIENE_CUIDADO);
        map.put("Pintalabios cremoso y brillos", MainCategoryMapper.HIGIENE_CUIDADO);
        map.put("Pintalabios mate", MainCategoryMapper.HIGIENE_CUIDADO);
        map.put("Polvos", MainCategoryMapper.HIGIENE_CUIDADO);
        map.put("Preservativos", MainCategoryMapper.HIGIENE_CUIDADO);
        map.put("Protector solar y aftersun", MainCategoryMapper.HIGIENE_CUIDADO);
        map.put("Protegeslips", MainCategoryMapper.HIGIENE_CUIDADO);
        map.put("Recambios maquinilla de afeitar", MainCategoryMapper.HIGIENE_CUIDADO);
        map.put("Retoca raíces y otros", MainCategoryMapper.HIGIENE_CUIDADO);
        map.put("Sérum y ampollas", MainCategoryMapper.HIGIENE_CUIDADO);
        map.put("Sérum y otros", MainCategoryMapper.HIGIENE_CUIDADO);
        map.put("Sombra de ojos", MainCategoryMapper.HIGIENE_CUIDADO);
        map.put("Tampones", MainCategoryMapper.HIGIENE_CUIDADO);
        map.put("Toallitas", MainCategoryMapper.HIGIENE_CUIDADO);
        map.put("Toallitas y gel", MainCategoryMapper.HIGIENE_CUIDADO);

        map.put("Biberón", MainCategoryMapper.BEBE);
        map.put("Champú infantil", MainCategoryMapper.BEBE);
        map.put("Chupete", MainCategoryMapper.BEBE);
        map.put("Colonia infantil", MainCategoryMapper.BEBE);
        map.put("Leche Infantil", MainCategoryMapper.BEBE);
        map.put("Pañal talla de 0 a 3", MainCategoryMapper.BEBE);
        map.put("Pañal talla de 4 a XL", MainCategoryMapper.BEBE);
        map.put("Papillas", MainCategoryMapper.BEBE);
        map.put("Tarritos salados", MainCategoryMapper.BEBE);
        map.put("Yogures y postres infantiles", MainCategoryMapper.BEBE);

        map.put("Accesorios", MainCategoryMapper.MASCOTAS);
        map.put("Alimentación húmeda", MainCategoryMapper.MASCOTAS);
        map.put("Alimentación seca", MainCategoryMapper.MASCOTAS);
        map.put("Pájaro", MainCategoryMapper.MASCOTAS);

        map.put("Arreglos", MainCategoryMapper.HOGAR_OTROS);
        map.put("Cubiertos, vajilla y mantel", MainCategoryMapper.HOGAR_OTROS);
        map.put("Decoración", MainCategoryMapper.HOGAR_OTROS);
        map.put("Encendedores, velas y carbón", MainCategoryMapper.HOGAR_OTROS);
        map.put("Herméticos y moldes", MainCategoryMapper.HOGAR_OTROS);
        map.put("Otros", MainCategoryMapper.HOGAR_OTROS);
        map.put("Papel higiénico", MainCategoryMapper.HOGAR_OTROS);
        map.put("Papel y bolsas de conservación", MainCategoryMapper.HOGAR_OTROS);
        map.put("Pañuelos", MainCategoryMapper.HOGAR_OTROS);
        map.put("Pilas", MainCategoryMapper.HOGAR_OTROS);
        map.put("Planchado", MainCategoryMapper.HOGAR_OTROS);
        map.put("Servilletas", MainCategoryMapper.HOGAR_OTROS);
        map.put("Tratamiento piscina", MainCategoryMapper.HOGAR_OTROS);
        map.put("Velas", MainCategoryMapper.HOGAR_OTROS);

        return Map.copyOf(map);
    }
}