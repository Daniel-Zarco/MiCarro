# MiCarro — Planning maestro del proyecto

**Versión del planning:** Septiembre 2026  
**Estado:** En desarrollo  
**Tipo:** Proyecto Full Stack para portfolio + proyecto personal desplegable

---

## 1. Visión del proyecto

**MiCarro** será una aplicación web para preparar y optimizar compras de supermercado.

La aplicación no pretende quedarse simplemente en:

> Buscar productos → añadir al carrito → ver el precio.

La característica diferencial será permitir que el usuario indique **qué necesita, cuánto quiere gastarse y qué tipo de compra quiere realizar**, y que MiCarro construya automáticamente una cesta adaptada a esas preferencias.

Ejemplo:

> Tengo 60 €. Quiero comprar pollo, arroz, setas y yogures. Quiero una compra equilibrada y que tenga en cuenta mis productos favoritos.

MiCarro deberá:

1. Entender qué productos necesita.
2. Buscar candidatos adecuados dentro del catálogo.
3. Tener en cuenta sus favoritos.
4. Aplicar la estrategia de compra seleccionada.
5. Respetar el presupuesto.
6. Decidir productos y cantidades.
7. Generar una propuesta de carrito.
8. Permitir modificarla.
9. Añadirla finalmente al carrito.

La intención es que MiCarro pase de ser un ecommerce de demostración a tener un **motor inteligente de planificación de compras**.

---

## 2. Stack tecnológico

### Frontend

**Angular**

Responsabilidades:

- Interfaz.
- Catálogo.
- Buscador.
- Paginación.
- Carrito.
- Favoritos.
- Panel de planificación.
- Configuración de preferencias.
- Visualización de propuestas.
- Autenticación en el futuro.
- Historial de compras.

Diseño:

- Moderno.
- Profesional.
- Estilo supermercado/ecommerce.
- Colores pastel.
- Verde como color principal.
- Fondos crema/blancos.
- Bordes redondeados moderados.
- Sombras suaves.
- Microinteracciones.
- Sin aspecto de dashboard genérico generado por IA.
- Priorizar funcionalidad y jerarquía visual.

### Backend

**Java + Spring Boot**

Responsabilidades:

- API REST propia.
- Acceso a PostgreSQL mediante JPA.
- Importación/sincronización del catálogo.
- Productos.
- Favoritos.
- Usuarios.
- Preferencias.
- Planificador de compras.
- Ranking de productos.
- Optimización del presupuesto.
- Autenticación/autorización.
- Historial.
- Lógica de negocio.

### Base de datos

**PostgreSQL 17**

Actualmente:

```text
micarro
```

Será la fuente de datos utilizada por nuestra aplicación.

Angular **no deberá depender directamente de Mercadona**.

---

## 3. Arquitectura general

La arquitectura objetivo será:

```text
                    FUENTE EXTERNA
                     MERCADONA
                         │
                         ▼
                  MercadonaClient
                         │
                         ▼
                 MercadonaProvider
                         │
                         ▼
                ProductImportService
                         │
                         ▼
                   PostgreSQL
                         │
              ┌──────────┴──────────┐
              │                     │
              ▼                     ▼
        ProductService       ShoppingPlanService
              │                     │
              │              ┌──────┴───────┐
              │              ▼              ▼
              │        ScoringEngine   BudgetOptimizer
              │              │              │
              └──────────────┴──────────────┘
                             │
                             ▼
                      Spring REST API
                             │
                             ▼
                          Angular
```

Principio importante:

> **Angular consume nuestra API. Nuestra aplicación controla los datos.**

No queremos que el frontend consulte continuamente servicios externos.

---

## 4. Alcance inicial

### V1

Inicialmente:

**Supermercado:**

```text
Mercadona
```

**Zona:**

```text
Madrid
```

No debemos asumir que el catálogo que estamos obteniendo actualmente está específicamente localizado en Madrid hasta que podamos garantizarlo.

La aplicación deberá indicar en el futuro que **MiCarro no es una aplicación oficial de Mercadona**.

Antes de un despliegue público definitivo habrá que revisar las condiciones aplicables al uso de:

- catálogo;
- precios;
- imágenes;
- nombres;
- logos;
- endpoints externos;
- frecuencia de sincronización.

La fuente que utilizamos actualmente no debe describirse como una API pública/oficial de Mercadona.

---

## 5. Estado actual

### Backend

Funcionando:

```text
Angular
   ↓
Spring Boot
   ↓
PostgreSQL
```

Base de datos:

**4.325 productos aproximadamente** importados.

Cada `Product` dispone actualmente de:

```text
id
externalId
name
brand
category
imageUrl
format
price
```

`brand` actualmente puede ser `null` porque no queremos inventar la marca a partir del nombre del producto.

### API de productos

Actualmente tenemos:

```http
GET /api/products
```

Con:

- paginación;
- búsqueda;
- tamaño de página.

También existe CRUD de productos:

```text
POST
GET
PUT
DELETE
```

### Importación

Arquitectura existente:

```text
ProductProvider
      ▲
      │
MercadonaProvider
      │
MercadonaClient
```

Y:

```text
ProductImportService
```

permite almacenar los productos externos en PostgreSQL.

---

## 6. Frontend actual

Ya tenemos:

- Header.
- Branding MiCarro.
- Buscador.
- Catálogo.
- Cards de productos.
- Imágenes.
- Formato.
- Precio.
- Paginación superior.
- Paginación inferior.
- Salto directo a una página.
- Carrito lateral.
- Añadir productos.
- Eliminar productos.
- Aumentar/disminuir cantidad.
- Total del carrito.
- Contador del carrito.
- Overlay.
- Diseño responsive inicial.

La card cambia cuando un producto está en el carrito.

Antes:

```text
[ + Añadir ]
```

Después:

```text
[ − | 2 | + ]
```

---

## 7. Sistema de favoritos

### Objetivo

Los usuarios podrán marcar productos:

```text
♡ → ♥
```

Los favoritos tendrán dos funciones.

#### Función visual

Encontrar rápidamente productos habituales.

En el futuro podremos tener:

```text
Mis favoritos
```

#### Función inteligente

Los favoritos influirán en el planificador.

Ejemplo:

El usuario pide:

> Arroz

Y existen:

```text
Arroz A
Arroz B
Arroz C ♥
Arroz D
```

Si está activado:

```text
☑ Priorizar favoritos
```

el producto C recibirá mayor puntuación.

**Favorito no significa necesariamente obligatorio.**

Si seleccionar el favorito perjudica mucho al presupuesto o impide completar la compra, el sistema podrá escoger otro producto.

---

## 8. Usuarios

Los favoritos terminarán estando asociados a usuarios.

Modelo conceptual:

```text
USER
 │
 ├── FAVORITES
 │       │
 │       └──── PRODUCT
 │
 ├── PREFERENCES
 │
 ├── SHOPPING PLANS
 │
 └── SHOPPING HISTORY
```

Esto requerirá posteriormente:

- registro;
- login;
- autenticación;
- autorización;
- contraseñas correctamente protegidas;
- posiblemente JWT.

No necesitamos implementar todo esto antes de comenzar a experimentar con el planificador, pero la arquitectura debe prepararse para ello.

---

## 9. Función principal: Planifica tu compra

Esta será una de las funcionalidades principales de MiCarro.

Podría existir una sección:

### Planifica tu compra

El usuario configura su compra.

Conceptualmente:

```text
┌──────────────────────────────────────────────────────┐
│ PLANIFICA TU COMPRA                                  │
│                                                      │
│ Presupuesto                                          │
│ [ 60,00 €                                      ]     │
│                                                      │
│ ¿Qué necesitas?                                      │
│ [ Pollo, arroz, setas, yogures...              ]     │
│                                                      │
│ Tipo de compra                                       │
│                                                      │
│ ○ Ahorrar                                            │
│ ● Equilibrada                                        │
│ ○ Calidad                                            │
│                                                      │
│ Preferencias                                         │
│                                                      │
│ ☑ Priorizar mis favoritos                           │
│ ☐ Aprovechar al máximo el presupuesto               │
│                                                      │
│                  [ Preparar mi compra → ]            │
└──────────────────────────────────────────────────────┘
```

El diseño definitivo se realizará cuando lleguemos a esta fase.

---

## 10. Modos de compra

Inicialmente tendremos tres estrategias.

### Ahorrar

Objetivo:

> Conseguir lo solicitado gastando lo mínimo posible.

Mayor importancia al precio.

Conceptualmente:

```text
Precio          ██████████
Relevancia      █████
Favoritos       ██
Calidad         -
```

### Equilibrada

Será probablemente la opción predeterminada.

Objetivo:

> Encontrar un buen equilibrio entre precio, adecuación del producto y preferencias.

Conceptualmente:

```text
Relevancia      ████████
Precio          ███████
Favoritos       █████
Calidad         futuro
```

### Calidad

Objetivo:

> Priorizar productos considerados mejores aunque sean más caros.

**IMPORTANTE:**

Actualmente **no tenemos datos suficientes para determinar objetivamente la calidad**.

No utilizaremos:

```text
más caro = más calidad
```

porque sería incorrecto.

Este modo se implementará realmente cuando dispongamos de criterios fiables.

Podrían ser en el futuro:

- valoraciones;
- composición;
- características;
- relación cantidad/precio;
- preferencias del usuario;
- datos adicionales fiables.

---

## 11. Preferencias

Las preferencias estarán separadas del modo de compra.

Esto permitirá:

```text
Modo:
● Equilibrado

Preferencias:
☑ Favoritos
☑ Aprovechar presupuesto
☐ Formatos grandes
☐ Ofertas
```

Estructura prevista:

```json
{
  "budget": 60,
  "items": [
    "pollo",
    "arroz",
    "setas",
    "yogures"
  ],
  "mode": "BALANCED",
  "preferences": {
    "prioritizeFavorites": true,
    "maximizeBudget": false
  }
}
```

La estructura deberá ser ampliable.

En el futuro:

```json
{
  "prioritizeFavorites": true,
  "maximizeBudget": true,
  "preferOffers": true,
  "preferLargeFormats": false,
  "preferredBrands": [],
  "avoidProducts": []
}
```

---

## 12. Búsqueda inteligente de productos

Este será uno de los problemas técnicos más importantes.

Si el usuario solicita:

```text
pollo
```

una búsqueda SQL simple podría encontrar:

```text
Pechuga de pollo
Muslo de pollo
Pollo entero
Tiras de pollo
Caldo de pollo
Pizza de pollo
Sopa sabor pollo
...
```

Pero:

```text
pollo
```

no debería terminar seleccionando automáticamente:

```text
Caldo de pollo
```

Necesitamos un sistema de **candidatos + relevancia**.

---

## 13. Candidate Selection

Para cada necesidad:

```text
pollo
```

generaremos varios candidatos:

```text
                   RELEVANCIA

Pechuga pollo          95
Pollo entero           92
Muslo pollo            90
Tiras pollo            86
Hamburguesa pollo      65
Caldo pollo            25
Pizza pollo            15
```

Estos valores son únicamente ejemplos conceptuales.

El sistema deberá determinar qué productos realmente representan la intención del usuario.

---

## 14. Scoring Engine

Después de encontrar candidatos, tendremos un sistema de puntuación.

Conceptualmente:

```text
ProductCandidate
│
├── relevanceScore
├── priceScore
├── favoriteScore
├── qualityScore
├── formatScore
└── finalScore
```

Por ejemplo:

```text
Pechuga de pollo

Relevancia             94/100
Precio                  81/100
Favorito                +20
Calidad                 N/A

Puntuación final        91/100
```

El cálculo dependerá del modo.

---

## 15. Strategy Pattern

Una evolución profesional interesante será que cada modo tenga su propia estrategia.

Conceptualmente:

```text
ShoppingStrategy
       │
 ┌─────┼─────────┐
 ▼     ▼         ▼
Cheap Balanced Quality
```

En Java podría terminar existiendo algo parecido a:

```text
ShoppingStrategy

CheapShoppingStrategy
BalancedShoppingStrategy
QualityShoppingStrategy
```

Esto evita llenar `ShoppingPlanService` de:

```java
if (...)
else if (...)
else if (...)
```

y permitirá añadir en el futuro:

```text
HealthyShoppingStrategy
BulkShoppingStrategy
FavoriteShoppingStrategy
```

sin modificar todo el sistema.

---

## 16. Optimización del presupuesto

Una vez puntuados los candidatos:

```text
Necesidades
     ↓
Candidatos
     ↓
Scoring
     ↓
Budget Optimizer
```

El optimizador decidirá:

- qué producto escoger;
- qué cantidad;
- cuánto gastar;
- cuándo sustituir un favorito;
- cómo respetar el presupuesto.

Regla fundamental:

```text
TOTAL ≤ PRESUPUESTO
```

Ejemplo:

```text
Presupuesto                         60,00 €

2 × Pechuga de pollo               13,20 €
2 × Arroz                            2,60 €
2 × Setas                            4,20 €
4 × Yogur                            6,00 €
...

Total                               57,85 €
Restante                              2,15 €
```

---

## 17. Maximizar presupuesto

Será una preferencia independiente:

```text
☑ Aprovechar al máximo mi presupuesto
```

Sin ella:

> Compra lo necesario y no gastes dinero simplemente porque queda presupuesto.

Con ella:

> Intenta mejorar cantidades/productos y acercarte al presupuesto sin superarlo.

Esto diferencia:

```text
Tengo hasta 60 €
```

de:

```text
Quiero gastarme aproximadamente 60 €
```

Muy importante.

---

## 18. Resultado del planificador

Nunca meteremos directamente una cesta generada sin enseñársela al usuario.

Primero:

```text
Tu compra está preparada

Presupuesto              60,00 €
Total                     54,72 €
Disponible                 5,28 €

────────────────────────────────

Pechuga de pollo
2 unidades                12,40 €

Arroz
2 unidades                 2,60 €

Setas
2 unidades                 4,30 €

Yogures
3 unidades                 4,20 €

...

────────────────────────────────

[ Modificar ]

[ Añadir todo al carrito ]
```

El usuario mantiene siempre el control.

---

## 19. Modificación de la propuesta

Queremos permitir:

```text
−  2  +
```

Eliminar un producto:

```text
Eliminar
```

Y posiblemente:

```text
Cambiar producto
```

que mostraría candidatos alternativos:

```text
Has elegido:

Pechuga de pollo      6,20 €

Alternativas:

Muslos                4,80 €
Contramuslos          5,10 €
Pollo entero          7,30 €
```

---

## 20. Lenguaje natural

Una vez funcione el motor estructurado, podremos añadir:

```text
¿Qué quieres comprar?

┌────────────────────────────────────────────┐
│ Tengo 60€ y quiero pollo, arroz,           │
│ setas y yogures. Intenta ahorrar y usa     │
│ mis favoritos cuando puedas.               │
└────────────────────────────────────────────┘
```

El sistema interpretará:

```text
Lenguaje natural
       ↓
Intent Parser
       ↓
ShoppingPlanRequest
```

Resultado:

```json
{
  "budget": 60,
  "items": [
    "pollo",
    "arroz",
    "setas",
    "yogures"
  ],
  "mode": "CHEAP",
  "preferences": {
    "prioritizeFavorites": true
  }
}
```

---

## 21. Papel de la IA

La IA **no debería controlar directamente precios ni inventar productos**.

Arquitectura:

```text
Usuario
   │
   ▼
"Prepárame una compra..."
   │
   ▼
IA / NLP
   │
   ▼
Petición estructurada
   │
   ▼
Nuestro backend
   │
   ├── PostgreSQL
   ├── precios reales almacenados
   ├── favoritos
   ├── scoring
   └── optimizador
   │
   ▼
Carrito propuesto
```

La IA principalmente podrá ayudar a **interpretar intención**, no a sustituir nuestra lógica de negocio.

Esto reduce:

- alucinaciones;
- precios inventados;
- productos inexistentes;
- resultados impredecibles.

---

## 22. Evoluciones futuras del planificador

Queremos diseñarlo para poder añadir opciones como:

```text
☑ Priorizar favoritos
☑ Aprovechar presupuesto
☑ Priorizar ofertas
☑ Formatos grandes
☑ Marcas favoritas
☐ Evitar determinados productos
```

Y posteriormente, si tenemos datos suficientes:

```text
☑ Compra saludable
☑ Alta en proteínas
☑ Vegetariana
☑ Sin gluten
...
```

Estas últimas requerirían información fiable del producto y no deben inferirse únicamente por el nombre.

---

## 23. Historial

En una fase posterior:

```text
Mis compras
```

Ejemplo:

```text
02/10/2026
Compra equilibrada
57,32 €

25/09/2026
Compra ahorro
42,18 €

18/09/2026
Compra equilibrada
61,04 €
```

Podremos reutilizar compras:

```text
[ Repetir compra ]
```

o incluso:

```text
[ Generar una compra parecida ]
```

---

## 24. Aprendizaje de preferencias

Más adelante MiCarro podría detectar patrones.

Por ejemplo:

```text
El usuario suele:

✓ escoger este arroz
✓ sustituir este yogur por otro
✓ comprar 2 paquetes de pollo
✓ marcar determinada leche como favorita
```

Esto podría alimentar recomendaciones.

Pero **no es necesario para V1**.

---

## 25. Sincronización del catálogo

No queremos importar manualmente productos eternamente.

Eventualmente necesitaremos:

```text
Scheduler
    ↓
ProductSyncService
    ↓
MercadonaProvider
    ↓
Comparar
    ↓
PostgreSQL
```

Registrar:

```text
source
externalId
lastSyncedAt
```

Y gestionar:

- productos nuevos;
- cambios de precio;
- productos eliminados/no disponibles;
- imágenes;
- errores parciales.

La frecuencia deberá definirse teniendo en cuenta las condiciones y limitaciones de la fuente.

---

## 26. Mejoras necesarias del backend

Antes de considerar el backend preparado para producción:

- DTOs propios para nuestra API.
- No exponer directamente entidades JPA.
- Validación con Jakarta Validation.
- Global Exception Handler.
- Respuesta de paginación propia.
- `spring.jpa.open-in-view=false`.
- Configuración mediante variables de entorno.
- Eliminar contraseña PostgreSQL del repositorio.
- Usuario PostgreSQL específico para MiCarro.
- Migraciones con Flyway/Liquibase.
- Optimizar importación masiva.
- Timeouts en peticiones externas.
- Gestión de errores externos.
- Logging.
- Tests.
- Seguridad.
- CORS de producción.

---

## 27. Frontend pendiente

Además del planificador:

- Favoritos.
- Página/sección de favoritos.
- Formato monetario español (`0,80 €`).
- Skeleton loaders.
- Mejor gestión de errores.
- Mejorar responsive.
- Accesibilidad del carrito.
- Cerrar carrito con Escape.
- Bloquear correctamente scroll del fondo.
- Focus management.
- Animaciones de entrada/salida.
- API URL mediante `environment`.
- Componentizar `app.html`.
- Separar componentes a medida que crezca.

Especialmente:

```text
ProductCardComponent
HeaderComponent
CartComponent
PaginationComponent
ShoppingPlannerComponent
ShoppingPlanResultComponent
```

No hace falta componentizar todo de golpe. Lo haremos cuando la complejidad lo justifique.

---

## 28. Base de datos futura

Conceptualmente podremos terminar con:

```text
users
products
favorites
shopping_preferences
shopping_plans
shopping_plan_items
shopping_history
```

Las relaciones exactas las diseñaremos antes de implementarlas.

No crear tablas innecesariamente por adelantado.

---

## 29. Seguridad

Antes del despliegue:

```text
Spring Security
```

y un sistema de autenticación adecuado.

Endpoints especialmente sensibles:

```text
/api/mercadona/import
```

no deben quedar públicamente accesibles como ahora.

Los usuarios solamente podrán modificar:

- sus favoritos;
- sus preferencias;
- sus carritos;
- su historial.

---

## 30. Despliegue

Objetivo final:

```text
                 INTERNET

Angular ────────────────┐
                        │
                        ▼
                  Spring Boot
                        │
                        ▼
                   PostgreSQL
```

Con:

- frontend desplegado;
- backend desplegado;
- PostgreSQL remoto;
- HTTPS;
- variables de entorno;
- CORS configurado;
- secretos fuera de Git;
- build de producción;
- dominio/subdominio si merece la pena.

Posteriormente:

```text
Docker
CI/CD
GitHub Actions
```

serían muy buenos añadidos para portfolio.

---

## 31. README / Portfolio

El proyecto debe poder explicarse fácilmente a un recruiter.

No queremos venderlo como:

> “Una página de Mercadona.”

Sino como algo del estilo:

> **MiCarro es una aplicación Full Stack para planificación inteligente de compras que integra un catálogo externo, almacena y gestiona los productos mediante PostgreSQL y genera cestas optimizadas según presupuesto y preferencias del usuario.**

Tecnologías:

```text
Angular
TypeScript
Java
Spring Boot
Spring Data JPA
PostgreSQL
REST API
Docker
Git/GitHub
```

Y posteriormente, si se implementa:

```text
Spring Security
JWT
IA/NLP
CI/CD
```

---

## 32. Roadmap recomendado

Nuestro orden de desarrollo a partir del estado actual será:

### Fase 1 — Favoritos

```text
♡ Producto
↓
Guardar favorito
↓
♥ Producto favorito
↓
Listado de favoritos
```

Primero podremos hacer funcionar el concepto y después asociarlo a usuarios cuando implementemos autenticación.

### Fase 2 — Panel del planificador

Construir UI:

```text
Presupuesto
Productos
Modo
Preferencias
```

Todavía sin inteligencia compleja.

### Fase 3 — API del planificador

Crear aproximadamente:

```text
ShoppingPlanController
ShoppingPlanService

ShoppingPlanRequest
ShoppingPlanResponse
ShoppingPlanItemResponse
ShoppingPreferences
ShoppingMode
```

### Fase 4 — Candidate Selection

Transformar:

```text
"pollo"
```

en un conjunto de productos relevantes.

Evitar falsos positivos.

### Fase 5 — Scoring Engine

Crear puntuaciones por:

```text
relevancia
precio
favoritos
calidad (cuando exista información)
otras preferencias
```

### Fase 6 — Estrategias

Implementar:

```text
CHEAP
BALANCED
QUALITY
```

con una arquitectura ampliable.

### Fase 7 — Budget Optimizer

Seleccionar:

```text
productos
+
cantidades
```

respetando:

```text
total <= presupuesto
```

y las preferencias.

### Fase 8 — Preview

Mostrar:

```text
Compra propuesta
Total
Restante
Productos
Cantidades
```

sin modificar todavía el carrito real.

### Fase 9 — Integración con carrito

```text
[ Añadir compra al carrito ]
```

fusionando correctamente cantidades con productos que ya estén dentro.

### Fase 10 — Usuarios

Implementar:

```text
Registro
Login
Spring Security
Autenticación
Favoritos por usuario
Preferencias por usuario
```

### Fase 11 — Lenguaje natural

Permitir:

> Tengo 60 € para pollo, arroz, setas y yogures. Hazla barata y utiliza mis favoritos cuando puedas.

Convertirlo internamente al mismo `ShoppingPlanRequest`.

### Fase 12 — Historial

Guardar compras y permitir repetirlas.

### Fase 13 — Sincronización profesional

Automatizar actualización del catálogo si la fuente y sus condiciones lo permiten.

### Fase 14 — Producción

```text
Secrets
Migrations
Tests
Docker
CI/CD
Deployment
HTTPS
CORS
Seguridad
Monitoring
```

---

## 33. Principios que debemos mantener

Hay varias reglas que debemos mantener durante todo el desarrollo:

**1. No inventar datos.**  
Si no conocemos la calidad de un producto, no diremos que es de mayor calidad.

**2. La IA no sustituye nuestra lógica.**  
La lógica importante debe vivir en Spring Boot.

**3. PostgreSQL es nuestra fuente para la aplicación.**  
No hacer depender cada interacción del usuario de Mercadona.

**4. Arquitectura ampliable.**  
Hoy Mercadona; mañana podría existir:

```text
MercadonaProvider
CarrefourProvider
AlcampoProvider
DiaProvider
```

sin reescribir MiCarro.

**5. No añadir funcionalidades falsas a la interfaz.**  
Si ponemos “Ofertas”, “Calidad”, “Categorías”, etc., tienen que tener datos reales detrás.

**6. El usuario controla la compra.**  
Una cesta generada debe poder revisarse/modificarse antes de aceptarla.

**7. Mantener el proyecto entendible.**  
No meter patrones de diseño o abstracciones simplemente para hacer parecer el proyecto más complejo.

**8. Desarrollo progresivo.**

```text
Funciona
   ↓
Está bien diseñado
   ↓
Está bien probado
   ↓
Está optimizado
   ↓
Está preparado para producción
```

No intentar resolver las cinco cosas simultáneamente.

---

## 34. Objetivo final de MiCarro

La experiencia que queremos conseguir finalmente es:

```text
                    MiCarro

"¿Qué compra quieres preparar?"

Tengo 60 € y necesito pollo, arroz,
setas, yogures y leche.

Modo
● Equilibrado

Preferencias
✓ Mis favoritos
✓ Aprovechar presupuesto

             Preparar mi compra
                     │
                     ▼
             ANALIZAR PETICIÓN
                     │
                     ▼
             BUSCAR CANDIDATOS
                     │
                     ▼
              PUNTUAR PRODUCTOS
                     │
                     ▼
            OPTIMIZAR PRESUPUESTO
                     │
                     ▼

              TU COMPRA

Pechuga de pollo        12,40 €
Arroz                     2,60 €
Setas                     4,30 €
Yogures                   4,20 €
Leche                     5,04 €
...

Total                    58,73 €
Te quedan                 1,27 €

    [Modificar] [Añadir al carrito]
```

Y posteriormente poder escribir directamente:

> **“MiCarro, tengo 60 €. Prepárame una compra para esta semana intentando ahorrar, pero usa mis favoritos siempre que puedas.”**

y que toda la infraestructura que hemos construido por debajo sea la que realmente resuelva la compra.

---

## 35. Punto exacto en el que estamos

```text
ESTADO ACTUAL
    ↓
Catálogo ✔
Búsqueda ✔
Paginación ✔
Salto de página ✔
Carrito ✔
Diseño inicial ✔
Importación ✔
PostgreSQL ✔

    ↓

SIGUIENTE
♥ FAVORITOS

    ↓
PANEL PLANIFICA TU COMPRA

    ↓
MOTOR INTELIGENTE
```

---

## Recomendación de ubicación

Guardar este archivo como:

```text
Compra/
├── frontend/
├── backend/
├── MICARRO_PLANNING.md
└── .git/
```

Así queda como documentación viva del proyecto dentro del propio repositorio y puede actualizarse conforme se completen las fases.
