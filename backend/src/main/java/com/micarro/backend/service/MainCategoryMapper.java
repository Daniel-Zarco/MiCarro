package com.micarro.backend.service;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Component;

/*
 * Agrupa las subcategorías del catálogo (Product.category) en las categorías
 * principales que ve el usuario (Product.mainCategory).
 *
 * Para las subcategorías conocidas usa un mapeo explícito; solo recurre a
 * reglas por palabras clave y a un fallback para subcategorías nuevas que
 * todavía no tienen entrada explícita.
 */
@Component
public class MainCategoryMapper {

    public static final String FRUTAS_VERDURAS = "Frutas y verduras";
    public static final String CARNE = "Carne";
    public static final String PESCADO_MARISCO = "Pescado y marisco";
    public static final String CHARCUTERIA = "Charcutería";
    public static final String LECHE_HUEVOS_LACTEOS = "Leche, huevos y lácteos";
    public static final String PANADERIA = "Panadería y bollería";
    public static final String ARROZ_PASTA_LEGUMBRES = "Arroz, pasta y legumbres";
    public static final String CONSERVAS = "Conservas";
    public static final String ACEITES_SALSAS_ESPECIAS = "Aceites, salsas y especias";
    public static final String DESAYUNO_DULCES = "Desayuno y dulces";
    public static final String SNACKS_FRUTOS_SECOS = "Snacks y frutos secos";
    public static final String BEBIDAS = "Bebidas";
    public static final String CONGELADOS = "Congelados";
    public static final String PLATOS_PREPARADOS = "Platos preparados";
    public static final String LIMPIEZA_HOGAR = "Limpieza del hogar";
    public static final String HIGIENE_CUIDADO = "Higiene y cuidado personal";
    public static final String BEBE = "Bebé";
    public static final String MASCOTAS = "Mascotas";
    public static final String HOGAR_OTROS = "Hogar y otros";

    private static final String DEFAULT_MAIN_CATEGORY = HOGAR_OTROS;

    private static final Map<String, String> EXPLICIT = Map.ofEntries(
            // Frutas y verduras
            Map.entry("manzanas", FRUTAS_VERDURAS),
            Map.entry("peras", FRUTAS_VERDURAS),
            Map.entry("plátanos", FRUTAS_VERDURAS),
            Map.entry("frutas tropicales", FRUTAS_VERDURAS),
            Map.entry("cítricos", FRUTAS_VERDURAS),
            Map.entry("uvas", FRUTAS_VERDURAS),
            Map.entry("frutas de hueso", FRUTAS_VERDURAS),
            Map.entry("frutas de verano", FRUTAS_VERDURAS),
            Map.entry("frutas de otoño", FRUTAS_VERDURAS),
            Map.entry("frutas de invierno", FRUTAS_VERDURAS),
            Map.entry("verduras de hoja", FRUTAS_VERDURAS),
            Map.entry("verduras de temporada", FRUTAS_VERDURAS),
            Map.entry("tomates", FRUTAS_VERDURAS),
            Map.entry("patatas", FRUTAS_VERDURAS),
            Map.entry("cebollas y ajos", FRUTAS_VERDURAS),
            Map.entry("pimientos", FRUTAS_VERDURAS),
            Map.entry("calabacín y berenjena", FRUTAS_VERDURAS),
            Map.entry("setas", FRUTAS_VERDURAS),
            Map.entry("ensalada", FRUTAS_VERDURAS),
            Map.entry("brócoli y coliflor", FRUTAS_VERDURAS),
            Map.entry("zanahorias", FRUTAS_VERDURAS),
            Map.entry("hortalizas", FRUTAS_VERDURAS),
            Map.entry("espárragos", FRUTAS_VERDURAS),
            // Carne
            Map.entry("vacuno", CARNE),
            Map.entry("cerdo", CARNE),
            Map.entry("pollo", CARNE),
            Map.entry("pavo", CARNE),
            Map.entry("cordero", CARNE),
            Map.entry("carne picada", CARNE),
            Map.entry("conejo", CARNE),
            // Pescado y marisco
            Map.entry("pescado fresco", PESCADO_MARISCO),
            Map.entry("pescado congelado", PESCADO_MARISCO),
            Map.entry("marisco fresco", PESCADO_MARISCO),
            Map.entry("marisco congelado", PESCADO_MARISCO),
            Map.entry("salmón", PESCADO_MARISCO),
            Map.entry("gambas", PESCADO_MARISCO),
            Map.entry("mejillones", PESCADO_MARISCO),
            // Charcutería
            Map.entry("embutidos", CHARCUTERIA),
            Map.entry("jamón cocido", CHARCUTERIA),
            Map.entry("jamón serrano", CHARCUTERIA),
            Map.entry("chorizo", CHARCUTERIA),
            Map.entry("salchichón", CHARCUTERIA),
            Map.entry("lomo", CHARCUTERIA),
            Map.entry("panceta", CHARCUTERIA),
            // Leche, huevos y lácteos
            Map.entry("leche", LECHE_HUEVOS_LACTEOS),
            Map.entry("yogures", LECHE_HUEVOS_LACTEOS),
            Map.entry("huevos", LECHE_HUEVOS_LACTEOS),
            Map.entry("quesos", LECHE_HUEVOS_LACTEOS),
            Map.entry("mantequilla", LECHE_HUEVOS_LACTEOS),
            Map.entry("nata", LECHE_HUEVOS_LACTEOS),
            Map.entry("requesón", LECHE_HUEVOS_LACTEOS),
            // Panadería y bollería
            Map.entry("pan de molde", PANADERIA),
            Map.entry("pan fresco", PANADERIA),
            Map.entry("pan rallado", PANADERIA),
            Map.entry("bollería", PANADERIA),
            Map.entry("magdalenas", PANADERIA),
            Map.entry("croissants", PANADERIA),
            // Arroz, pasta y legumbres
            Map.entry("arroz", ARROZ_PASTA_LEGUMBRES),
            Map.entry("pasta", ARROZ_PASTA_LEGUMBRES),
            Map.entry("legumbres", ARROZ_PASTA_LEGUMBRES),
            Map.entry("garbanzos", ARROZ_PASTA_LEGUMBRES),
            Map.entry("lentejas", ARROZ_PASTA_LEGUMBRES),
            Map.entry("judías", ARROZ_PASTA_LEGUMBRES),
            // Conservas
            Map.entry("conservas vegetales", CONSERVAS),
            Map.entry("tomate frito", CONSERVAS),
            Map.entry("atún en conserva", CONSERVAS),
            Map.entry("sardinas en lata", CONSERVAS),
            Map.entry("conservas de pescado", CONSERVAS),
            // Aceites, salsas y especias
            Map.entry("aceite de oliva", ACEITES_SALSAS_ESPECIAS),
            Map.entry("aceite de girasol", ACEITES_SALSAS_ESPECIAS),
            Map.entry("vinagres", ACEITES_SALSAS_ESPECIAS),
            Map.entry("especias", ACEITES_SALSAS_ESPECIAS),
            Map.entry("sal", ACEITES_SALSAS_ESPECIAS),
            Map.entry("salsas", ACEITES_SALSAS_ESPECIAS),
            Map.entry("mostaza", ACEITES_SALSAS_ESPECIAS),
            Map.entry("mayonesa", ACEITES_SALSAS_ESPECIAS),
            Map.entry("ketchup", ACEITES_SALSAS_ESPECIAS),
            // Desayuno y dulces
            Map.entry("café", DESAYUNO_DULCES),
            Map.entry("infusiones", DESAYUNO_DULCES),
            Map.entry("azúcar", DESAYUNO_DULCES),
            Map.entry("miel", DESAYUNO_DULCES),
            Map.entry("cacao", DESAYUNO_DULCES),
            Map.entry("galletas", DESAYUNO_DULCES),
            Map.entry("chocolate", DESAYUNO_DULCES),
            Map.entry("cereales", DESAYUNO_DULCES),
            Map.entry("mermelada", DESAYUNO_DULCES),
            // Snacks y frutos secos
            Map.entry("patatas fritas", SNACKS_FRUTOS_SECOS),
            Map.entry("frutos secos", SNACKS_FRUTOS_SECOS),
            Map.entry("aceitunas", SNACKS_FRUTOS_SECOS),
            Map.entry("pipas", SNACKS_FRUTOS_SECOS),
            Map.entry("snacks", SNACKS_FRUTOS_SECOS),
            // Bebidas
            Map.entry("agua", BEBIDAS),
            Map.entry("refrescos", BEBIDAS),
            Map.entry("zumos", BEBIDAS),
            Map.entry("cerveza", BEBIDAS),
            Map.entry("vino", BEBIDAS),
            Map.entry("bebidas energéticas", BEBIDAS),
            Map.entry("batidos", BEBIDAS),
            // Congelados
            Map.entry("congelados", CONGELADOS),
            Map.entry("verduras congeladas", CONGELADOS),
            Map.entry("frutas congeladas", CONGELADOS),
            // Platos preparados
            Map.entry("platos preparados", PLATOS_PREPARADOS),
            Map.entry("pizzas", PLATOS_PREPARADOS),
            Map.entry("lasaña", PLATOS_PREPARADOS),
            Map.entry("sopas", PLATOS_PREPARADOS),
            Map.entry("croquetas", PLATOS_PREPARADOS),
            Map.entry("nuggets", PLATOS_PREPARADOS),
            // Limpieza del hogar
            Map.entry("detergente", LIMPIEZA_HOGAR),
            Map.entry("lavavajillas", LIMPIEZA_HOGAR),
            Map.entry("suavizante", LIMPIEZA_HOGAR),
            Map.entry("limpiadores", LIMPIEZA_HOGAR),
            Map.entry("lejía", LIMPIEZA_HOGAR),
            Map.entry("fregona", LIMPIEZA_HOGAR),
            Map.entry("estropajos", LIMPIEZA_HOGAR),
            // Higiene y cuidado personal
            Map.entry("champú", HIGIENE_CUIDADO),
            Map.entry("jabón", HIGIENE_CUIDADO),
            Map.entry("gel de ducha", HIGIENE_CUIDADO),
            Map.entry("desodorante", HIGIENE_CUIDADO),
            Map.entry("dentífrico", HIGIENE_CUIDADO),
            Map.entry("maquinillas", HIGIENE_CUIDADO),
            Map.entry("cremas", HIGIENE_CUIDADO),
            Map.entry("cosmética", HIGIENE_CUIDADO),
            Map.entry("perfumería", HIGIENE_CUIDADO),
            // Bebé
            Map.entry("pañales", BEBE),
            Map.entry("leche infantil", BEBE),
            Map.entry("potitos", BEBE),
            Map.entry("higiene bebé", BEBE),
            // Mascotas
            Map.entry("pienso", MASCOTAS),
            Map.entry("comida para gatos", MASCOTAS),
            Map.entry("arena para gatos", MASCOTAS),
            Map.entry("comida para perros", MASCOTAS),
            Map.entry("accesorios mascotas", MASCOTAS),
            // Hogar y otros
            Map.entry("papel higiénico", HOGAR_OTROS),
            Map.entry("papel de cocina", HOGAR_OTROS),
            Map.entry("bolsas de basura", HOGAR_OTROS),
            Map.entry("velas", HOGAR_OTROS),
            Map.entry("pilas", HOGAR_OTROS),
            Map.entry("utensilios de cocina", HOGAR_OTROS),
            Map.entry("textil hogar", HOGAR_OTROS),
            Map.entry("ferretería", HOGAR_OTROS)
    );

    private record Rule(String keyword, String mainCategory) {
    }

    /*
     * Reglas de respaldo para subcategorías nuevas sin entrada explícita.
     * El orden importa: se evalúa la primera coincidencia.
     */
    private static final List<Rule> RULES = List.of(
            new Rule("frutos secos", SNACKS_FRUTOS_SECOS),
            new Rule("patatas fritas", SNACKS_FRUTOS_SECOS),
            new Rule("aperitivo", SNACKS_FRUTOS_SECOS),
            new Rule("snack", SNACKS_FRUTOS_SECOS),
            new Rule("aceituna", SNACKS_FRUTOS_SECOS),
            new Rule("tomate frito", CONSERVAS),
            new Rule("conserva", CONSERVAS),
            new Rule("en lata", CONSERVAS),
            new Rule("leche infantil", BEBE),
            new Rule("potito", BEBE),
            new Rule("pañal", BEBE),
            new Rule("bebé", BEBE),
            new Rule("leche", LECHE_HUEVOS_LACTEOS),
            new Rule("yogur", LECHE_HUEVOS_LACTEOS),
            new Rule("queso", LECHE_HUEVOS_LACTEOS),
            new Rule("huevo", LECHE_HUEVOS_LACTEOS),
            new Rule("mantequilla", LECHE_HUEVOS_LACTEOS),
            new Rule("nata", LECHE_HUEVOS_LACTEOS),
            new Rule("jamón", CHARCUTERIA),
            new Rule("embutido", CHARCUTERIA),
            new Rule("chorizo", CHARCUTERIA),
            new Rule("salchichón", CHARCUTERIA),
            new Rule("vacuno", CARNE),
            new Rule("cerdo", CARNE),
            new Rule("pollo", CARNE),
            new Rule("pavo", CARNE),
            new Rule("ternera", CARNE),
            new Rule("cordero", CARNE),
            new Rule("carne", CARNE),
            new Rule("pescado", PESCADO_MARISCO),
            new Rule("marisco", PESCADO_MARISCO),
            new Rule("gamba", PESCADO_MARISCO),
            new Rule("mejillón", PESCADO_MARISCO),
            new Rule("fruta", FRUTAS_VERDURAS),
            new Rule("manzana", FRUTAS_VERDURAS),
            new Rule("pera", FRUTAS_VERDURAS),
            new Rule("verdura", FRUTAS_VERDURAS),
            new Rule("hortaliza", FRUTAS_VERDURAS),
            new Rule("tomate", FRUTAS_VERDURAS),
            new Rule("patata", FRUTAS_VERDURAS),
            new Rule("cebolla", FRUTAS_VERDURAS),
            new Rule("lechuga", FRUTAS_VERDURAS),
            new Rule("brócoli", FRUTAS_VERDURAS),
            new Rule("coliflor", FRUTAS_VERDURAS),
            new Rule("zanahoria", FRUTAS_VERDURAS),
            new Rule("pimiento", FRUTAS_VERDURAS),
            new Rule("ensalada", FRUTAS_VERDURAS),
            new Rule("sal", ACEITES_SALSAS_ESPECIAS),
            new Rule("pan", PANADERIA),
            new Rule("bollería", PANADERIA),
            new Rule("magdalena", PANADERIA),
            new Rule("croissant", PANADERIA),
            new Rule("arroz", ARROZ_PASTA_LEGUMBRES),
            new Rule("pasta", ARROZ_PASTA_LEGUMBRES),
            new Rule("legumbre", ARROZ_PASTA_LEGUMBRES),
            new Rule("garbanzo", ARROZ_PASTA_LEGUMBRES),
            new Rule("lenteja", ARROZ_PASTA_LEGUMBRES),
            new Rule("aceite", ACEITES_SALSAS_ESPECIAS),
            new Rule("vinagre", ACEITES_SALSAS_ESPECIAS),
            new Rule("especia", ACEITES_SALSAS_ESPECIAS),
            new Rule("salsa", ACEITES_SALSAS_ESPECIAS),
            new Rule("mayonesa", ACEITES_SALSAS_ESPECIAS),
            new Rule("ketchup", ACEITES_SALSAS_ESPECIAS),
            new Rule("mostaza", ACEITES_SALSAS_ESPECIAS),
            new Rule("café", DESAYUNO_DULCES),
            new Rule("infusión", DESAYUNO_DULCES),
            new Rule("azúcar", DESAYUNO_DULCES),
            new Rule("miel", DESAYUNO_DULCES),
            new Rule("cacao", DESAYUNO_DULCES),
            new Rule("galleta", DESAYUNO_DULCES),
            new Rule("chocolate", DESAYUNO_DULCES),
            new Rule("cereal", DESAYUNO_DULCES),
            new Rule("mermelada", DESAYUNO_DULCES),
            new Rule("agua", BEBIDAS),
            new Rule("refresco", BEBIDAS),
            new Rule("zumo", BEBIDAS),
            new Rule("cerveza", BEBIDAS),
            new Rule("vino", BEBIDAS),
            new Rule("bebida", BEBIDAS),
            new Rule("congelado", CONGELADOS),
            new Rule("pizza", PLATOS_PREPARADOS),
            new Rule("lasaña", PLATOS_PREPARADOS),
            new Rule("croqueta", PLATOS_PREPARADOS),
            new Rule("nuggets", PLATOS_PREPARADOS),
            new Rule("preparado", PLATOS_PREPARADOS),
            new Rule("detergente", LIMPIEZA_HOGAR),
            new Rule("lavavajillas", LIMPIEZA_HOGAR),
            new Rule("suavizante", LIMPIEZA_HOGAR),
            new Rule("limpiador", LIMPIEZA_HOGAR),
            new Rule("lejía", LIMPIEZA_HOGAR),
            new Rule("champú", HIGIENE_CUIDADO),
            new Rule("desodorante", HIGIENE_CUIDADO),
            new Rule("dentífrico", HIGIENE_CUIDADO),
            new Rule("maquinilla", HIGIENE_CUIDADO),
            new Rule("crema", HIGIENE_CUIDADO),
            new Rule("cosmética", HIGIENE_CUIDADO),
            new Rule("perfume", HIGIENE_CUIDADO),
            new Rule("pienso", MASCOTAS),
            new Rule("perro", MASCOTAS),
            new Rule("gato", MASCOTAS),
            new Rule("mascota", MASCOTAS),
            new Rule("arena", MASCOTAS)
    );

    public String map(String category) {

        if (category == null || category.isBlank()) {
            return DEFAULT_MAIN_CATEGORY;
        }

        String normalized = category.trim().toLowerCase(Locale.ROOT);

        String explicit = EXPLICIT.get(normalized);

        if (explicit != null) {
            return explicit;
        }

        for (Rule rule : RULES) {
            if (normalized.contains(rule.keyword())) {
                return rule.mainCategory();
            }
        }

        return DEFAULT_MAIN_CATEGORY;
    }
}