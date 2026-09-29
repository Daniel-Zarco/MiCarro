package com.micarro.backend.service;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Component;

/*
 * Agrupa las subcategorías del catálogo (Product.category) en las categorías
 * principales que ve el usuario (Product.mainCategory).
 *
 * Todas las subcategorías conocidas tienen un mapeo explícito. Las reglas por
 * palabras clave y el fallback solo se usan para futuras categorías que aún no
 * tienen entrada explícita.
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

    private static final Map<String, String> EXPLICIT = buildExplicit();

    private record Rule(String keyword, String mainCategory) {
    }

    /*
     * Reglas de respaldo para categorías futuras sin entrada explícita.
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
            new Rule("helado", CONGELADOS),
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

    private static Map<String, String> buildExplicit() {

        Map<String, String> map = new HashMap<>();

        // ===== Frutas y verduras =====
        map.put("calabacín y pimiento", FRUTAS_VERDURAS);
        map.put("cebolla y ajo", FRUTAS_VERDURAS);
        map.put("cítricos", FRUTAS_VERDURAS);
        map.put("fruta", FRUTAS_VERDURAS);
        map.put("fruta tropical", FRUTAS_VERDURAS);
        map.put("lechuga", FRUTAS_VERDURAS);
        map.put("limón", FRUTAS_VERDURAS);
        map.put("manzana y pera", FRUTAS_VERDURAS);
        map.put("melocotón", FRUTAS_VERDURAS);
        map.put("melón y sandía", FRUTAS_VERDURAS);
        map.put("naranja", FRUTAS_VERDURAS);
        map.put("otras frutas", FRUTAS_VERDURAS);
        map.put("otras verduras y hortalizas", FRUTAS_VERDURAS);
        map.put("patata", FRUTAS_VERDURAS);
        map.put("patatas", FRUTAS_VERDURAS);
        map.put("pepino y zanahoria", FRUTAS_VERDURAS);
        map.put("piña", FRUTAS_VERDURAS);
        map.put("plátano y uva", FRUTAS_VERDURAS);
        map.put("repollo y col", FRUTAS_VERDURAS);
        map.put("setas y champiñones", FRUTAS_VERDURAS);
        map.put("tomate", FRUTAS_VERDURAS);
        map.put("verdura", FRUTAS_VERDURAS);

        // ===== Carne =====
        map.put("carne", CARNE);
        map.put("cerdo", CARNE);
        map.put("conejo", CARNE);
        map.put("cordero", CARNE);
        map.put("hamburguesas", CARNE);
        map.put("pavo y otras aves", CARNE);
        map.put("pavo y otros", CARNE);
        map.put("picadas y otros", CARNE);
        map.put("pollo", CARNE);
        map.put("vacuno", CARNE);

        // ===== Pescado y marisco =====
        map.put("ahumados", PESCADO_MARISCO);
        map.put("bacalao", PESCADO_MARISCO);
        map.put("boquerón", PESCADO_MARISCO);
        map.put("corvina", PESCADO_MARISCO);
        map.put("dorada", PESCADO_MARISCO);
        map.put("lubina", PESCADO_MARISCO);
        map.put("marisco", PESCADO_MARISCO);
        map.put("marisco de concha", PESCADO_MARISCO);
        map.put("merluza", PESCADO_MARISCO);
        map.put("pescado", PESCADO_MARISCO);
        map.put("rodaballo", PESCADO_MARISCO);
        map.put("salmón", PESCADO_MARISCO);
        map.put("sardina", PESCADO_MARISCO);
        map.put("sepia, pulpo y calamar", PESCADO_MARISCO);
        map.put("surimi y otros", PESCADO_MARISCO);
        map.put("trucha", PESCADO_MARISCO);

        // ===== Charcutería =====
        map.put("bacón", CHARCUTERIA);
        map.put("chopped", CHARCUTERIA);
        map.put("chorizo", CHARCUTERIA);
        map.put("embutido", CHARCUTERIA);
        map.put("jamón cocido", CHARCUTERIA);
        map.put("jamón serrano", CHARCUTERIA);
        map.put("lomo y otros", CHARCUTERIA);
        map.put("mortadela", CHARCUTERIA);
        map.put("salchichas", CHARCUTERIA);
        map.put("salchichón", CHARCUTERIA);
        map.put("sobrasada", CHARCUTERIA);

        // ===== Leche, huevos y lácteos =====
        map.put("bífidus de sabores", LECHE_HUEVOS_LACTEOS);
        map.put("bífidus naturales", LECHE_HUEVOS_LACTEOS);
        map.put("flan", LECHE_HUEVOS_LACTEOS);
        map.put("fruta + leche", LECHE_HUEVOS_LACTEOS);
        map.put("fruta variada y otros sabores", LECHE_HUEVOS_LACTEOS);
        map.put("gelatina", LECHE_HUEVOS_LACTEOS);
        map.put("huevos", LECHE_HUEVOS_LACTEOS);
        map.put("l-casei", LECHE_HUEVOS_LACTEOS);
        map.put("leche", LECHE_HUEVOS_LACTEOS);
        map.put("leche condensada y otros", LECHE_HUEVOS_LACTEOS);
        map.put("leche desnatada", LECHE_HUEVOS_LACTEOS);
        map.put("leche en polvo", LECHE_HUEVOS_LACTEOS);
        map.put("leche entera", LECHE_HUEVOS_LACTEOS);
        map.put("leche semidesnatada", LECHE_HUEVOS_LACTEOS);
        map.put("mantequilla", LECHE_HUEVOS_LACTEOS);
        map.put("margarina", LECHE_HUEVOS_LACTEOS);
        map.put("nata", LECHE_HUEVOS_LACTEOS);
        map.put("natillas", LECHE_HUEVOS_LACTEOS);
        map.put("otros postres", LECHE_HUEVOS_LACTEOS);
        map.put("otros sabores", LECHE_HUEVOS_LACTEOS);
        map.put("postres de soja", LECHE_HUEVOS_LACTEOS);
        map.put("queso curado", LECHE_HUEVOS_LACTEOS);
        map.put("queso en porciones", LECHE_HUEVOS_LACTEOS);
        map.put("queso especialidades", LECHE_HUEVOS_LACTEOS);
        map.put("queso fresco", LECHE_HUEVOS_LACTEOS);
        map.put("queso lonchas", LECHE_HUEVOS_LACTEOS);
        map.put("queso rallado", LECHE_HUEVOS_LACTEOS);
        map.put("queso semicurado", LECHE_HUEVOS_LACTEOS);
        map.put("queso tierno", LECHE_HUEVOS_LACTEOS);
        map.put("queso untable", LECHE_HUEVOS_LACTEOS);
        map.put("yogures de sabores", LECHE_HUEVOS_LACTEOS);
        map.put("yogures desnatados", LECHE_HUEVOS_LACTEOS);
        map.put("yogures griegos", LECHE_HUEVOS_LACTEOS);
        map.put("yogures líquidos", LECHE_HUEVOS_LACTEOS);
        map.put("yogures naturales", LECHE_HUEVOS_LACTEOS);

        // ===== Panadería y bollería =====
        map.put("barra de pan", PANADERIA);
        map.put("base de pizza", PANADERIA);
        map.put("bollería dulce", PANADERIA);
        map.put("bollería envasada", PANADERIA);
        map.put("bollería salada", PANADERIA);
        map.put("churros", PANADERIA);
        map.put("masas", PANADERIA);
        map.put("otros panes", PANADERIA);
        map.put("pan de bocadillo", PANADERIA);
        map.put("pan de hamburguesa y wrap", PANADERIA);
        map.put("pan de molde", PANADERIA);
        map.put("pan rallado", PANADERIA);
        map.put("pan rebanado", PANADERIA);
        map.put("pan tostado", PANADERIA);
        map.put("picos", PANADERIA);
        map.put("roscas, quiche y baguettes", PANADERIA);
        map.put("rosquilletas", PANADERIA);

        // ===== Arroz, pasta y legumbres =====
        map.put("alubias", ARROZ_PASTA_LEGUMBRES);
        map.put("arroz", ARROZ_PASTA_LEGUMBRES);
        map.put("fideos", ARROZ_PASTA_LEGUMBRES);
        map.put("fideos orientales", ARROZ_PASTA_LEGUMBRES);
        map.put("garbanzos", ARROZ_PASTA_LEGUMBRES);
        map.put("harina", ARROZ_PASTA_LEGUMBRES);
        map.put("lentejas y otros", ARROZ_PASTA_LEGUMBRES);
        map.put("levadura y preparado repostería", ARROZ_PASTA_LEGUMBRES);
        map.put("macarrones, pajaritas y hélices", ARROZ_PASTA_LEGUMBRES);
        map.put("pasta", ARROZ_PASTA_LEGUMBRES);
        map.put("pasta rellena", ARROZ_PASTA_LEGUMBRES);
        map.put("spaghetti y tallarines", ARROZ_PASTA_LEGUMBRES);

        // ===== Conservas =====
        map.put("atún", CONSERVAS);
        map.put("berberechos y almejas", CONSERVAS);
        map.put("bonito", CONSERVAS);
        map.put("caballa y melva", CONSERVAS);
        map.put("conservas fruta", CONSERVAS);
        map.put("conservas verdura", CONSERVAS);
        map.put("mejillones", CONSERVAS);
        map.put("otras conservas de pescado", CONSERVAS);
        map.put("paté", CONSERVAS);
        map.put("pepinillos y otros encurtidos", CONSERVAS);
        map.put("salazones", CONSERVAS);
        map.put("sardinas", CONSERVAS);

        // ===== Aceites, salsas y especias =====
        map.put("aceite de oliva", ACEITES_SALSAS_ESPECIAS);
        map.put("allioli", ACEITES_SALSAS_ESPECIAS);
        map.put("colorante y pimentón", ACEITES_SALSAS_ESPECIAS);
        map.put("hierbas aromáticas", ACEITES_SALSAS_ESPECIAS);
        map.put("ketchup", ACEITES_SALSAS_ESPECIAS);
        map.put("mayonesa", ACEITES_SALSAS_ESPECIAS);
        map.put("mostaza", ACEITES_SALSAS_ESPECIAS);
        map.put("otras especias", ACEITES_SALSAS_ESPECIAS);
        map.put("otras salsas", ACEITES_SALSAS_ESPECIAS);
        map.put("otros aceites", ACEITES_SALSAS_ESPECIAS);
        map.put("pimienta", ACEITES_SALSAS_ESPECIAS);
        map.put("sal y bicarbonato", ACEITES_SALSAS_ESPECIAS);
        map.put("salsas orientales", ACEITES_SALSAS_ESPECIAS);
        map.put("salsas para carnes", ACEITES_SALSAS_ESPECIAS);
        map.put("salsas para pasta", ACEITES_SALSAS_ESPECIAS);
        map.put("sazonadores", ACEITES_SALSAS_ESPECIAS);
        map.put("vinagre y otros aderezos", ACEITES_SALSAS_ESPECIAS);

        // ===== Desayuno y dulces =====
        map.put("azúcar", DESAYUNO_DULCES);
        map.put("barritas de cereales", DESAYUNO_DULCES);
        map.put("bombones", DESAYUNO_DULCES);
        map.put("cacao soluble", DESAYUNO_DULCES);
        map.put("café en grano", DESAYUNO_DULCES);
        map.put("café molido", DESAYUNO_DULCES);
        map.put("café soluble", DESAYUNO_DULCES);
        map.put("cápsulas compatibles dolce gusto", DESAYUNO_DULCES);
        map.put("cápsulas compatibles nespresso", DESAYUNO_DULCES);
        map.put("cápsulas compatibles tassimo", DESAYUNO_DULCES);
        map.put("caramelos", DESAYUNO_DULCES);
        map.put("cereales", DESAYUNO_DULCES);
        map.put("cereales integrales y muesli", DESAYUNO_DULCES);
        map.put("chicles", DESAYUNO_DULCES);
        map.put("chocolate a la taza", DESAYUNO_DULCES);
        map.put("chocolate blanco", DESAYUNO_DULCES);
        map.put("chocolate con leche", DESAYUNO_DULCES);
        map.put("chocolate negro", DESAYUNO_DULCES);
        map.put("chocolatinas", DESAYUNO_DULCES);
        map.put("con chocolate y rellenas", DESAYUNO_DULCES);
        map.put("confitura y otros", DESAYUNO_DULCES);
        map.put("cremas de untar", DESAYUNO_DULCES);
        map.put("edulcorante y otros", DESAYUNO_DULCES);
        map.put("galletas desayuno", DESAYUNO_DULCES);
        map.put("galletas integrales y digestive", DESAYUNO_DULCES);
        map.put("galletas surtidas", DESAYUNO_DULCES);
        map.put("golosinas", DESAYUNO_DULCES);
        map.put("hierbas", DESAYUNO_DULCES);
        map.put("infusiones", DESAYUNO_DULCES);
        map.put("mermelada", DESAYUNO_DULCES);
        map.put("miel", DESAYUNO_DULCES);
        map.put("pastelitos surtidos", DESAYUNO_DULCES);
        map.put("tartas", DESAYUNO_DULCES);
        map.put("tartas infantiles", DESAYUNO_DULCES);
        map.put("té", DESAYUNO_DULCES);
        map.put("turrones", DESAYUNO_DULCES);

        // ===== Snacks y frutos secos =====
        map.put("aceitunas negras", SNACKS_FRUTOS_SECOS);
        map.put("aceitunas verdes", SNACKS_FRUTOS_SECOS);
        map.put("cóctel y banderillas", SNACKS_FRUTOS_SECOS);
        map.put("crakers y tartaletas", SNACKS_FRUTOS_SECOS);
        map.put("fruta desecada", SNACKS_FRUTOS_SECOS);
        map.put("frutos secos", SNACKS_FRUTOS_SECOS);
        map.put("patatas fritas", SNACKS_FRUTOS_SECOS);
        map.put("picatostes", SNACKS_FRUTOS_SECOS);
        map.put("snacks", SNACKS_FRUTOS_SECOS);
        map.put("tortitas", SNACKS_FRUTOS_SECOS);

        // ===== Bebidas =====
        map.put("agua con gas", BEBIDAS);
        map.put("agua sin gas", BEBIDAS);
        map.put("anís", BEBIDAS);
        map.put("batidos", BEBIDAS);
        map.put("bebidas frías", BEBIDAS);
        map.put("bebidas vegetales", BEBIDAS);
        map.put("brandy", BEBIDAS);
        map.put("castilla la mancha", BEBIDAS);
        map.put("cava brut", BEBIDAS);
        map.put("cava semi seco", BEBIDAS);
        map.put("cerveza botella y botellín", BEBIDAS);
        map.put("cerveza lata", BEBIDAS);
        map.put("cocktails", BEBIDAS);
        map.put("cola clásica", BEBIDAS);
        map.put("cola sin cafeína", BEBIDAS);
        map.put("cola zero", BEBIDAS);
        map.put("combinado de cerveza", BEBIDAS);
        map.put("energético", BEBIDAS);
        map.put("gaseosa", BEBIDAS);
        map.put("ginebra", BEBIDAS);
        map.put("isotónico", BEBIDAS);
        map.put("licores sin alcohol", BEBIDAS);
        map.put("lima limón", BEBIDAS);
        map.put("otros licores", BEBIDAS);
        map.put("otros refrescos sin gas", BEBIDAS);
        map.put("otros vinos tintos", BEBIDAS);
        map.put("ribera del duero", BEBIDAS);
        map.put("rioja", BEBIDAS);
        map.put("rioja y otras denominaciones", BEBIDAS);
        map.put("ron", BEBIDAS);
        map.put("rueda", BEBIDAS);
        map.put("sidra", BEBIDAS);
        map.put("smoothie", BEBIDAS);
        map.put("tinto de verano y sangría", BEBIDAS);
        map.put("tónica y bitter", BEBIDAS);
        map.put("vermouth y aperitivos", BEBIDAS);
        map.put("vino blanco de mesa", BEBIDAS);
        map.put("vino lambrusco y espumoso", BEBIDAS);
        map.put("vino rosado", BEBIDAS);
        map.put("vinos dulces y mosto", BEBIDAS);
        map.put("vinos semidulces", BEBIDAS);
        map.put("vino tinto de mesa", BEBIDAS);
        map.put("vodka", BEBIDAS);
        map.put("whisky", BEBIDAS);

        // ===== Congelados =====
        map.put("barras de helado y barquillos", CONGELADOS);
        map.put("carne rebozada", CONGELADOS);
        map.put("cucuruchos", CONGELADOS);
        map.put("empanados y elaborados", CONGELADOS);
        map.put("granizados y helados de hielo", CONGELADOS);
        map.put("hielo", CONGELADOS);
        map.put("otros helados", CONGELADOS);
        map.put("pescado congelado", CONGELADOS);
        map.put("pescado rebozado congelado", CONGELADOS);
        map.put("pizzas congeladas", CONGELADOS);
        map.put("sepia, pulpo y calamar congelado", CONGELADOS);
        map.put("tarrinas", CONGELADOS);
        map.put("verdura rebozada y otros", CONGELADOS);
        map.put("verduras al vapor", CONGELADOS);

        // ===== Platos preparados =====
        map.put("caldo en pastillas", PLATOS_PREPARADOS);
        map.put("caldo líquido", PLATOS_PREPARADOS);
        map.put("cremas y puré", PLATOS_PREPARADOS);
        map.put("ensalada preparada", PLATOS_PREPARADOS);
        map.put("ensaladilla", PLATOS_PREPARADOS);
        map.put("gazpacho y salmorejo", PLATOS_PREPARADOS);
        map.put("hummus y otros", PLATOS_PREPARADOS);
        map.put("lasaña y canelones", PLATOS_PREPARADOS);
        map.put("pizzas", PLATOS_PREPARADOS);
        map.put("pizzas refrigeradas", PLATOS_PREPARADOS);
        map.put("platos calientes", PLATOS_PREPARADOS);
        map.put("platos de cuchara", PLATOS_PREPARADOS);
        map.put("platos fríos", PLATOS_PREPARADOS);
        map.put("sándwich", PLATOS_PREPARADOS);
        map.put("sándwiches", PLATOS_PREPARADOS);
        map.put("sopa", PLATOS_PREPARADOS);
        map.put("tortilla", PLATOS_PREPARADOS);

        // ===== Limpieza del hogar =====
        map.put("absorbeolores y antihumedad", LIMPIEZA_HOGAR);
        map.put("activador y antical lavadora", LIMPIEZA_HOGAR);
        map.put("ambientador automático", LIMPIEZA_HOGAR);
        map.put("ambientador coche", LIMPIEZA_HOGAR);
        map.put("ambientador decorativo y otros", LIMPIEZA_HOGAR);
        map.put("ambientador eléctrico", LIMPIEZA_HOGAR);
        map.put("ambientador spray", LIMPIEZA_HOGAR);
        map.put("amoníaco y salfumán", LIMPIEZA_HOGAR);
        map.put("bayeta", LIMPIEZA_HOGAR);
        map.put("bolsas de basura", LIMPIEZA_HOGAR);
        map.put("cubos y barreños", LIMPIEZA_HOGAR);
        map.put("detergente en polvo y monodosis", LIMPIEZA_HOGAR);
        map.put("detergente lavado a mano", LIMPIEZA_HOGAR);
        map.put("detergente líquido y gel", LIMPIEZA_HOGAR);
        map.put("estropajo", LIMPIEZA_HOGAR);
        map.put("fregonas, escobas y mopas", LIMPIEZA_HOGAR);
        map.put("friegasuelos", LIMPIEZA_HOGAR);
        map.put("guantes", LIMPIEZA_HOGAR);
        map.put("insecticida eléctrico y otros", LIMPIEZA_HOGAR);
        map.put("insecticida spray", LIMPIEZA_HOGAR);
        map.put("lejía", LIMPIEZA_HOGAR);
        map.put("limpiacristales", LIMPIEZA_HOGAR);
        map.put("limpiahogar", LIMPIEZA_HOGAR);
        map.put("limpiavajilla a mano", LIMPIEZA_HOGAR);
        map.put("limpiavajilla a máquina", LIMPIEZA_HOGAR);
        map.put("limpieza baño y wc", LIMPIEZA_HOGAR);
        map.put("limpieza cocina", LIMPIEZA_HOGAR);
        map.put("limpieza de calzado", LIMPIEZA_HOGAR);
        map.put("limpieza muebles", LIMPIEZA_HOGAR);
        map.put("monodosis", LIMPIEZA_HOGAR);
        map.put("multiusos y otros", LIMPIEZA_HOGAR);
        map.put("otros utensilios de limpieza", LIMPIEZA_HOGAR);
        map.put("quitamanchas", LIMPIEZA_HOGAR);
        map.put("rollo cocina", LIMPIEZA_HOGAR);
        map.put("suavizante", LIMPIEZA_HOGAR);

        // ===== Higiene y cuidado personal =====
        map.put("aceite corporal", HIGIENE_CUIDADO);
        map.put("aceite y crema", HIGIENE_CUIDADO);
        map.put("acondicionador", HIGIENE_CUIDADO);
        map.put("after shave", HIGIENE_CUIDADO);
        map.put("aseo y cuidado", HIGIENE_CUIDADO);
        map.put("bandas, cera y crema", HIGIENE_CUIDADO);
        map.put("botiquín", HIGIENE_CUIDADO);
        map.put("braguita y otros", HIGIENE_CUIDADO);
        map.put("cejas", HIGIENE_CUIDADO);
        map.put("cepillo de dientes", HIGIENE_CUIDADO);
        map.put("champú", HIGIENE_CUIDADO);
        map.put("champú anticaspa", HIGIENE_CUIDADO);
        map.put("champú y jabón", HIGIENE_CUIDADO);
        map.put("colesterol y otros", HIGIENE_CUIDADO);
        map.put("colonia", HIGIENE_CUIDADO);
        map.put("coloración castaño, marrón y rubio oscuro", HIGIENE_CUIDADO);
        map.put("coloración hombre", HIGIENE_CUIDADO);
        map.put("coloración negro y castaño oscuro", HIGIENE_CUIDADO);
        map.put("coloración rojo, cobre y violín", HIGIENE_CUIDADO);
        map.put("coloración rubio", HIGIENE_CUIDADO);
        map.put("coloración superaclarante y platino", HIGIENE_CUIDADO);
        map.put("colorete", HIGIENE_CUIDADO);
        map.put("colutorio e hilo dental", HIGIENE_CUIDADO);
        map.put("compresas", HIGIENE_CUIDADO);
        map.put("contorno de ojos", HIGIENE_CUIDADO);
        map.put("correctores y prebase", HIGIENE_CUIDADO);
        map.put("crema corporal", HIGIENE_CUIDADO);
        map.put("crema de cara", HIGIENE_CUIDADO);
        map.put("crema manos", HIGIENE_CUIDADO);
        map.put("crema pies", HIGIENE_CUIDADO);
        map.put("cremas", HIGIENE_CUIDADO);
        map.put("crema y gel de cara", HIGIENE_CUIDADO);
        map.put("cuchilla", HIGIENE_CUIDADO);
        map.put("cuidado", HIGIENE_CUIDADO);
        map.put("cuidado de uñas y complementos", HIGIENE_CUIDADO);
        map.put("desodorante roll on y stick", HIGIENE_CUIDADO);
        map.put("desodorante spray", HIGIENE_CUIDADO);
        map.put("esponjas", HIGIENE_CUIDADO);
        map.put("espuma de afeitar", HIGIENE_CUIDADO);
        map.put("espuma y laca", HIGIENE_CUIDADO);
        map.put("fitoterapia", HIGIENE_CUIDADO);
        map.put("gel", HIGIENE_CUIDADO);
        map.put("gomina y cera", HIGIENE_CUIDADO);
        map.put("jabón de manos", HIGIENE_CUIDADO);
        map.put("laca de uñas", HIGIENE_CUIDADO);
        map.put("limpieza de cara", HIGIENE_CUIDADO);
        map.put("lotes hombre", HIGIENE_CUIDADO);
        map.put("lotes mujer", HIGIENE_CUIDADO);
        map.put("maquillaje compacto", HIGIENE_CUIDADO);
        map.put("maquillaje fluido", HIGIENE_CUIDADO);
        map.put("maquinillas de afeitar", HIGIENE_CUIDADO);
        map.put("máscara de pestañas", HIGIENE_CUIDADO);
        map.put("mascarilla", HIGIENE_CUIDADO);
        map.put("otros desodorantes", HIGIENE_CUIDADO);
        map.put("pañales para adulto", HIGIENE_CUIDADO);
        map.put("perfilador de ojos", HIGIENE_CUIDADO);
        map.put("perfilador labios", HIGIENE_CUIDADO);
        map.put("perfume y colonia hombre", HIGIENE_CUIDADO);
        map.put("perfume y colonia mujer", HIGIENE_CUIDADO);
        map.put("pinceles y brochas", HIGIENE_CUIDADO);
        map.put("pintalabios cremoso y brillos", HIGIENE_CUIDADO);
        map.put("pintalabios mate", HIGIENE_CUIDADO);
        map.put("polvos", HIGIENE_CUIDADO);
        map.put("preservativos", HIGIENE_CUIDADO);
        map.put("protector solar y aftersun", HIGIENE_CUIDADO);
        map.put("protegeslips", HIGIENE_CUIDADO);
        map.put("recambios maquinilla de afeitar", HIGIENE_CUIDADO);
        map.put("retoca raíces y otros", HIGIENE_CUIDADO);
        map.put("sérum y ampollas", HIGIENE_CUIDADO);
        map.put("sérum y otros", HIGIENE_CUIDADO);
        map.put("sombra de ojos", HIGIENE_CUIDADO);
        map.put("tampones", HIGIENE_CUIDADO);
        map.put("toallitas", HIGIENE_CUIDADO);
        map.put("toallitas y gel", HIGIENE_CUIDADO);

        // ===== Bebé =====
        map.put("biberón", BEBE);
        map.put("champú infantil", BEBE);
        map.put("chupete", BEBE);
        map.put("colonia infantil", BEBE);
        map.put("leche infantil", BEBE);
        map.put("pañal talla de 0 a 3", BEBE);
        map.put("pañal talla de 4 a xl", BEBE);
        map.put("papillas", BEBE);
        map.put("tarritos salados", BEBE);
        map.put("yogures y postres infantiles", BEBE);

        // ===== Mascotas =====
        map.put("accesorios", MASCOTAS);
        map.put("alimentación húmeda", MASCOTAS);
        map.put("alimentación seca", MASCOTAS);
        map.put("pájaro", MASCOTAS);

        // ===== Hogar y otros =====
        map.put("arreglos", HOGAR_OTROS);
        map.put("cubiertos, vajilla y mantel", HOGAR_OTROS);
        map.put("decoración", HOGAR_OTROS);
        map.put("encendedores, velas y carbón", HOGAR_OTROS);
        map.put("herméticos y moldes", HOGAR_OTROS);
        map.put("otros", HOGAR_OTROS);
        map.put("papel higiénico", HOGAR_OTROS);
        map.put("papel y bolsas de conservación", HOGAR_OTROS);
        map.put("pañuelos", HOGAR_OTROS);
        map.put("pilas", HOGAR_OTROS);
        map.put("planchado", HOGAR_OTROS);
        map.put("servilletas", HOGAR_OTROS);
        map.put("tratamiento piscina", HOGAR_OTROS);
        map.put("velas", HOGAR_OTROS);

        return Map.copyOf(map);
    }

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