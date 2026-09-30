package com.micarro.backend.service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import static com.micarro.backend.service.MainCategoryMapper.ACEITES_SALSAS_ESPECIAS;
import static com.micarro.backend.service.MainCategoryMapper.ARROZ_PASTA_LEGUMBRES;
import static com.micarro.backend.service.MainCategoryMapper.BEBE;
import static com.micarro.backend.service.MainCategoryMapper.BEBIDAS;
import static com.micarro.backend.service.MainCategoryMapper.CARNE;
import static com.micarro.backend.service.MainCategoryMapper.CHARCUTERIA;
import static com.micarro.backend.service.MainCategoryMapper.CONGELADOS;
import static com.micarro.backend.service.MainCategoryMapper.CONSERVAS;
import static com.micarro.backend.service.MainCategoryMapper.DESAYUNO_DULCES;
import static com.micarro.backend.service.MainCategoryMapper.FRUTAS_VERDURAS;
import static com.micarro.backend.service.MainCategoryMapper.HIGIENE_CUIDADO;
import static com.micarro.backend.service.MainCategoryMapper.HOGAR_OTROS;
import static com.micarro.backend.service.MainCategoryMapper.LECHE_HUEVOS_LACTEOS;
import static com.micarro.backend.service.MainCategoryMapper.LIMPIEZA_HOGAR;
import static com.micarro.backend.service.MainCategoryMapper.MASCOTAS;
import static com.micarro.backend.service.MainCategoryMapper.PANADERIA;
import static com.micarro.backend.service.MainCategoryMapper.PESCADO_MARISCO;
import static com.micarro.backend.service.MainCategoryMapper.PLATOS_PREPARADOS;
import static com.micarro.backend.service.MainCategoryMapper.SNACKS_FRUTOS_SECOS;

/*
 * Única fuente de verdad del segundo nivel de navegación:
 * mainCategory -> grupo visual -> Product.category.
 *
 * Todas las categorías conocidas tienen mapeo explícito; el grupo "Otros" es
 * solo un respaldo para futuras categorías desconocidas. No se añade ninguna
 * columna ni dato redundante a Product.
 */
@Component
public class VisualGroupMapper {

    public static final String FALLBACK_GROUP = "Otros";

    private static final Map<String, Map<String, List<String>>> TREE = buildTree();

    private static final Map<String, String> CATEGORY_TO_GROUP = buildCategoryToGroup(TREE);

    public List<String> groupsOf(String mainCategory) {
        Map<String, List<String>> groups = TREE.get(mainCategory);
        return groups == null ? List.of() : List.copyOf(groups.keySet());
    }

    public List<String> categoriesOf(String mainCategory, String group) {
        Map<String, List<String>> groups = TREE.get(mainCategory);
        if (groups == null) {
            return List.of();
        }
        List<String> categories = groups.get(group);
        return categories == null ? List.of() : categories;
    }

    public String visualGroup(String category) {
        if (category == null || category.isBlank()) {
            return null;
        }
        return CATEGORY_TO_GROUP.get(
                category.trim().toLowerCase(Locale.ROOT)
        );
    }

    public Set<String> categories() {
        return CATEGORY_TO_GROUP.keySet();
    }

    public Set<String> mainCategories() {
        return TREE.keySet();
    }

    private static Map<String, Map<String, List<String>>> buildTree() {

        Map<String, Map<String, List<String>>> tree = new LinkedHashMap<>();

        // ===== Frutas y verduras =====
        add(tree, FRUTAS_VERDURAS, "Fruta",
                "Cítricos", "Fruta", "Fruta tropical", "Limón", "Manzana y pera",
                "Melocotón", "Melón y sandía", "Naranja", "Otras frutas",
                "Piña", "Plátano y uva");
        add(tree, FRUTAS_VERDURAS, "Verdura",
                "Calabacín y pimiento", "Cebolla y ajo", "Lechuga",
                "Otras verduras y hortalizas", "Pepino y zanahoria", "Repollo y col",
                "Setas y champiñones", "Tomate", "Verdura");
        add(tree, FRUTAS_VERDURAS, "Patatas",
                "Patata", "Patatas");

        // ===== Carne =====
        add(tree, CARNE, "Aves",
                "Pavo y otras aves", "Pavo y otros", "Pollo");
        add(tree, CARNE, "Vacuno",
                "Carne", "Vacuno");
        add(tree, CARNE, "Cerdo",
                "Cerdo");
        add(tree, CARNE, "Cordero y conejo",
                "Conejo", "Cordero");
        add(tree, CARNE, "Hamburguesas y picadas",
                "Hamburguesas", "Picadas y otros");

        // ===== Pescado y marisco =====
        add(tree, PESCADO_MARISCO, "Pescado blanco",
                "Bacalao", "Corvina", "Dorada", "Lubina", "Merluza",
                "Pescado", "Rodaballo");
        add(tree, PESCADO_MARISCO, "Pescado azul",
                "Boquerón", "Salmón", "Sardina", "Trucha");
        add(tree, PESCADO_MARISCO, "Marisco",
                "Marisco", "Marisco de concha", "Sepia, pulpo y calamar");
        add(tree, PESCADO_MARISCO, "Ahumados y preparados",
                "Ahumados", "Surimi y otros");

        // ===== Charcutería =====
        add(tree, CHARCUTERIA, "Jamón",
                "Jamón cocido", "Jamón serrano");
        add(tree, CHARCUTERIA, "Embutidos y salchichas",
                "Chorizo", "Embutido", "Lomo y otros", "Salchichas",
                "Salchichón", "Sobrasada");
        add(tree, CHARCUTERIA, "Fiambres",
                "Bacón", "Chopped", "Mortadela");

        // ===== Leche, huevos y lácteos =====
        add(tree, LECHE_HUEVOS_LACTEOS, "Leche",
                "Leche", "Leche condensada y otros", "Leche desnatada",
                "Leche en polvo", "Leche entera", "Leche semidesnatada");
        add(tree, LECHE_HUEVOS_LACTEOS, "Huevos",
                "Huevos");
        add(tree, LECHE_HUEVOS_LACTEOS, "Yogures",
                "Fruta variada y otros sabores", "Otros sabores",
                "Yogures de sabores", "Yogures desnatados", "Yogures griegos",
                "Yogures líquidos", "Yogures naturales");
        add(tree, LECHE_HUEVOS_LACTEOS, "Bífidus",
                "Bífidus de sabores", "Bífidus naturales", "L-Casei");
        add(tree, LECHE_HUEVOS_LACTEOS, "Quesos",
                "Queso curado", "Queso en porciones", "Queso especialidades",
                "Queso fresco", "Queso lonchas", "Queso rallado",
                "Queso semicurado", "Queso tierno", "Queso untable");
        add(tree, LECHE_HUEVOS_LACTEOS, "Mantequilla y nata",
                "Mantequilla", "Margarina", "Nata");
        add(tree, LECHE_HUEVOS_LACTEOS, "Postres",
                "Flan", "Fruta + leche", "Gelatina", "Natillas",
                "Otros postres", "Postres de soja");

        // ===== Panadería y bollería =====
        add(tree, PANADERIA, "Pan de molde",
                "Pan de hamburguesa y wrap", "Pan de molde",
                "Pan rebanado", "Pan tostado");
        add(tree, PANADERIA, "Pan fresco",
                "Barra de pan", "Otros panes", "Pan de bocadillo",
                "Roscas, quiche y baguettes");
        add(tree, PANADERIA, "Bollería",
                "Bollería dulce", "Bollería envasada", "Bollería salada",
                "Churros");
        add(tree, PANADERIA, "Pan rallado y picos",
                "Pan rallado", "Picos", "Rosquilletas");
        add(tree, PANADERIA, "Masas y bases de pizza",
                "Base de pizza", "Masas");

        // ===== Arroz, pasta y legumbres =====
        add(tree, ARROZ_PASTA_LEGUMBRES, "Pasta",
                "Fideos", "Fideos orientales", "Macarrones, pajaritas y hélices",
                "Pasta", "Pasta rellena", "Spaghetti y tallarines");
        add(tree, ARROZ_PASTA_LEGUMBRES, "Arroz y legumbres",
                "Alubias", "Arroz", "Garbanzos", "Lentejas y otros");
        add(tree, ARROZ_PASTA_LEGUMBRES, "Harina y repostería",
                "Harina", "Levadura y preparado repostería");

        // ===== Conservas =====
        add(tree, CONSERVAS, "Conservas de pescado",
                "Atún", "Bonito", "Caballa y melva",
                "Otras conservas de pescado", "Salazones", "Sardinas");
        add(tree, CONSERVAS, "Mariscos en conserva",
                "Berberechos y almejas", "Mejillones");
        add(tree, CONSERVAS, "Conservas vegetales y fruta",
                "Conservas fruta", "Conservas verdura",
                "Pepinillos y otros encurtidos");
        add(tree, CONSERVAS, "Paté",
                "Paté");

        // ===== Aceites, salsas y especias =====
        add(tree, ACEITES_SALSAS_ESPECIAS, "Aceites",
                "Aceite de oliva", "Otros aceites");
        add(tree, ACEITES_SALSAS_ESPECIAS, "Salsas y vinagres",
                "Allioli", "Ketchup", "Mayonesa", "Mostaza", "Otras salsas",
                "Salsas orientales", "Salsas para carnes", "Salsas para pasta",
                "Vinagre y otros aderezos");
        add(tree, ACEITES_SALSAS_ESPECIAS, "Especias y sal",
                "Colorante y pimentón", "Hierbas aromáticas", "Otras especias",
                "Pimienta", "Sal y bicarbonato", "Sazonadores");

        // ===== Desayuno y dulces =====
        add(tree, DESAYUNO_DULCES, "Café",
                "Café en grano", "Café molido", "Café soluble",
                "Cápsulas compatibles Dolce gusto", "Cápsulas compatibles Nespresso",
                "Cápsulas compatibles Tassimo");
        add(tree, DESAYUNO_DULCES, "Infusiones",
                "Hierbas", "Infusiones", "Té");
        add(tree, DESAYUNO_DULCES, "Azúcar y edulcorantes",
                "Azúcar", "Edulcorante y otros");
        add(tree, DESAYUNO_DULCES, "Cacao y chocolate",
                "Bombones", "Cacao soluble", "Chocolate a la taza",
                "Chocolate blanco", "Chocolate con leche", "Chocolate negro",
                "Chocolatinas", "Cremas de untar");
        add(tree, DESAYUNO_DULCES, "Galletas",
                "Con chocolate y rellenas", "Galletas desayuno",
                "Galletas integrales y digestive", "Galletas surtidas");
        add(tree, DESAYUNO_DULCES, "Cereales",
                "Barritas de cereales", "Cereales", "Cereales integrales y muesli");
        add(tree, DESAYUNO_DULCES, "Mermeladas y miel",
                "Confitura y otros", "Mermelada", "Miel");
        add(tree, DESAYUNO_DULCES, "Repostería",
                "Pastelitos surtidos", "Tartas", "Tartas infantiles");
        add(tree, DESAYUNO_DULCES, "Dulces y golosinas",
                "Caramelos", "Chicles", "Golosinas", "Turrones");

        // ===== Snacks y frutos secos =====
        add(tree, SNACKS_FRUTOS_SECOS, "Frutos secos",
                "Fruta desecada", "Frutos secos");
        add(tree, SNACKS_FRUTOS_SECOS, "Aperitivos salados",
                "Crakers y tartaletas", "Patatas fritas", "Picatostes",
                "Snacks", "Tortitas");
        add(tree, SNACKS_FRUTOS_SECOS, "Aceitunas",
                "Aceitunas negras", "Aceitunas verdes", "Cóctel y banderillas");

        // ===== Bebidas =====
        add(tree, BEBIDAS, "Agua",
                "Agua con gas", "Agua sin gas");
        add(tree, BEBIDAS, "Refrescos",
                "Bebidas frías", "Cola clásica", "Cola sin cafeína", "Cola zero",
                "Energético", "Gaseosa", "Isotónico", "Lima limón",
                "Otros refrescos sin gas", "Tónica y bitter");
        add(tree, BEBIDAS, "Batidos y bebidas vegetales",
                "Batidos", "Bebidas vegetales", "Smoothie");
        add(tree, BEBIDAS, "Cerveza y sidra",
                "Cerveza botella y botellín", "Cerveza lata",
                "Combinado de cerveza", "Sidra");
        add(tree, BEBIDAS, "Vino tinto",
                "Castilla la Mancha", "Otros vinos tintos", "Ribera del Duero",
                "Rioja", "Rioja y otras denominaciones", "Rueda",
                "Vino tinto de mesa");
        add(tree, BEBIDAS, "Vinos y aperitivos",
                "Tinto de verano y sangría", "Vermouth y aperitivos",
                "Vino blanco de mesa", "Vino rosado", "Vinos dulces y mosto",
                "Vinos semidulces");
        add(tree, BEBIDAS, "Espumosos",
                "Cava brut", "Cava semi seco", "Vino lambrusco y espumoso");
        add(tree, BEBIDAS, "Licores y cócteles",
                "Anís", "Brandy", "Cocktails", "Ginebra", "Licores sin alcohol",
                "Otros licores", "Ron", "Vodka", "Whisky");

        // ===== Congelados =====
        add(tree, CONGELADOS, "Helados",
                "Barras de helado y barquillos", "Cucuruchos",
                "Granizados y helados de hielo", "Otros Helados", "Tarrinas");
        add(tree, CONGELADOS, "Pescado congelado",
                "Pescado congelado", "Pescado rebozado congelado",
                "Sepia, pulpo y calamar congelado");
        add(tree, CONGELADOS, "Verdura congelada",
                "Verduras al vapor", "Verdura rebozada y otros");
        add(tree, CONGELADOS, "Congelados listos",
                "Carne rebozada", "Empanados y elaborados", "Pizzas congeladas");
        add(tree, CONGELADOS, "Hielo",
                "Hielo");

        // ===== Platos preparados =====
        add(tree, PLATOS_PREPARADOS, "Sopas y cremas",
                "Caldo en pastillas", "Caldo líquido", "Cremas y puré",
                "Gazpacho y salmorejo", "Sopa");
        add(tree, PLATOS_PREPARADOS, "Pizzas",
                "Pizzas", "Pizzas refrigeradas");
        add(tree, PLATOS_PREPARADOS, "Sándwiches",
                "Sándwich", "Sándwiches");
        add(tree, PLATOS_PREPARADOS, "Ensaladas preparadas",
                "Ensalada preparada", "Ensaladilla");
        add(tree, PLATOS_PREPARADOS, "Platos preparados",
                "Lasaña y canelones", "Platos calientes", "Platos de cuchara",
                "Platos fríos");
        add(tree, PLATOS_PREPARADOS, "Otros preparados",
                "Hummus y otros", "Tortilla");

        // ===== Limpieza del hogar =====
        add(tree, LIMPIEZA_HOGAR, "Detergente y lavado",
                "Activador y antical lavadora", "Detergente en polvo y monodosis",
                "Detergente lavado a mano", "Detergente líquido y gel",
                "Monodosis", "Quitamanchas", "Suavizante");
        add(tree, LIMPIEZA_HOGAR, "Limpieza general",
                "Amoníaco y salfumán", "Friegasuelos", "Lejía", "Limpiacristales",
                "Limpiahogar", "Limpieza baño y WC", "Limpieza cocina",
                "Limpieza de calzado", "Limpieza muebles", "Multiusos y otros");
        add(tree, LIMPIEZA_HOGAR, "Lavavajillas",
                "Limpiavajilla a mano", "Limpiavajilla a máquina");
        add(tree, LIMPIEZA_HOGAR, "Ambientadores",
                "Absorbeolores y antihumedad", "Ambientador automático",
                "Ambientador coche", "Ambientador decorativo y otros",
                "Ambientador eléctrico", "Ambientador spray");
        add(tree, LIMPIEZA_HOGAR, "Insecticidas",
                "Insecticida eléctrico y otros", "Insecticida spray");
        add(tree, LIMPIEZA_HOGAR, "Utensilios y bolsas",
                "Bayeta", "Bolsas de basura", "Cubos y barreños", "Estropajo",
                "Fregonas, escobas y mopas", "Guantes",
                "Otros utensilios de limpieza", "Rollo cocina");

        // ===== Higiene y cuidado personal =====
        add(tree, HIGIENE_CUIDADO, "Cabello",
                "Acondicionador", "Champú", "Champú anticaspa", "Champú y jabón",
                "Espuma y laca", "Gomina y cera");
        add(tree, HIGIENE_CUIDADO, "Cuidado facial",
                "Contorno de ojos", "Crema de cara", "Crema y gel de cara",
                "Cremas", "Limpieza de cara", "Mascarilla", "Sérum y ampollas",
                "Sérum y otros", "Toallitas", "Toallitas y gel");
        add(tree, HIGIENE_CUIDADO, "Cuidado corporal",
                "Aceite corporal", "Aceite y crema", "Aseo y cuidado",
                "Crema corporal", "Crema manos", "Crema pies", "Cuidado",
                "Esponjas", "Gel", "Jabón de manos", "Protector solar y aftersun");
        add(tree, HIGIENE_CUIDADO, "Maquillaje",
                "Cejas", "Colorete", "Correctores y prebase",
                "Cuidado de uñas y complementos", "Laca de uñas",
                "Maquillaje compacto", "Maquillaje fluido", "Máscara de pestañas",
                "Perfilador de ojos", "Perfilador labios", "Pinceles y brochas",
                "Pintalabios cremoso y brillos", "Pintalabios mate", "Polvos",
                "Sombra de ojos");
        add(tree, HIGIENE_CUIDADO, "Higiene bucal",
                "Cepillo de dientes", "Colutorio e hilo dental", "Pasta de dientes");
        add(tree, HIGIENE_CUIDADO, "Afeitado y depilación",
                "After shave", "Bandas, cera y crema", "Cuchilla",
                "Espuma de afeitar", "Maquinillas de afeitar",
                "Recambios maquinilla de afeitar");
        add(tree, HIGIENE_CUIDADO, "Perfumería",
                "Colonia", "Lotes hombre", "Lotes mujer",
                "Perfume y colonia hombre", "Perfume y colonia mujer");
        add(tree, HIGIENE_CUIDADO, "Higiene femenina",
                "Braguita y otros", "Compresas", "Protegeslips", "Tampones");
        add(tree, HIGIENE_CUIDADO, "Desodorantes",
                "Desodorante roll on y stick", "Desodorante Spray",
                "Otros desodorantes");
        add(tree, HIGIENE_CUIDADO, "Coloración",
                "Coloración castaño, marrón y rubio oscuro", "Coloración hombre",
                "Coloración negro y castaño oscuro",
                "Coloración rojo, cobre y violín", "Coloración rubio",
                "Coloración superaclarante y platino", "Retoca raíces y otros");
        add(tree, HIGIENE_CUIDADO, "Salud y botiquín",
                "Botiquín", "Colesterol y otros", "Fitoterapia",
                "Pañales para adulto", "Preservativos");

        // ===== Bebé =====
        add(tree, BEBE, "Pañales",
                "Pañal talla de 0 a 3", "Pañal talla de 4 a XL");
        add(tree, BEBE, "Alimentación infantil",
                "Leche Infantil", "Papillas", "Tarritos salados",
                "Yogures y postres infantiles");
        add(tree, BEBE, "Higiene y cuidado",
                "Champú infantil", "Colonia infantil");
        add(tree, BEBE, "Accesorios",
                "Biberón", "Chupete");

        // ===== Mascotas =====
        add(tree, MASCOTAS, "Alimentación",
                "Alimentación húmeda", "Alimentación seca");
        add(tree, MASCOTAS, "Pájaros",
                "Pájaro");
        add(tree, MASCOTAS, "Accesorios",
                "Accesorios");

        // ===== Hogar y otros =====
        add(tree, HOGAR_OTROS, "Papel y celulosa",
                "Papel higiénico", "Papel y bolsas de conservación",
                "Pañuelos", "Servilletas");
        add(tree, HOGAR_OTROS, "Cocina y menaje",
                "Cubiertos, vajilla y mantel", "Herméticos y moldes",
                "Planchado");
        add(tree, HOGAR_OTROS, "Decoración y velas",
                "Decoración", "Encendedores, velas y carbón", "Velas");
        add(tree, HOGAR_OTROS, "Varios",
                "Arreglos", "Otros", "Pilas", "Tratamiento piscina");

        return tree;
    }

    private static void add(
            Map<String, Map<String, List<String>>> tree,
            String mainCategory,
            String group,
            String... categories) {

        tree.computeIfAbsent(mainCategory, k -> new LinkedHashMap<>())
                .put(group, List.of(categories));
    }

    private static Map<String, String> buildCategoryToGroup(
            Map<String, Map<String, List<String>>> tree) {

        Map<String, String> map = new HashMap<>();

        for (Map.Entry<String, Map<String, List<String>>> main : tree.entrySet()) {
            for (Map.Entry<String, List<String>> group : main.getValue().entrySet()) {
                for (String category : group.getValue()) {
                    map.put(
                            category.trim().toLowerCase(Locale.ROOT),
                            group.getKey()
                    );
                }
            }
        }

        return map;
    }
}