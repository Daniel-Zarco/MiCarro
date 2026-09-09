# 🛒 MiCarro

**MiCarro** es una aplicación Full Stack para crear, organizar y planificar compras de supermercado de forma sencilla e inteligente.

El proyecto permite consultar un catálogo real de productos, buscar artículos, gestionar un carrito de compra y, progresivamente, incorporar funcionalidades de planificación capaces de generar una compra en función del presupuesto y las preferencias del usuario.

> 🚧 Proyecto actualmente en desarrollo.

---

## ✨ Funcionalidades actuales

- Catálogo de más de 4.000 productos.
- Búsqueda de productos.
- Paginación del catálogo.
- Carrito de compra.
- Gestión de cantidades.
- Cálculo automático del precio total.
- Integración de datos externos de productos.
- Persistencia del catálogo en PostgreSQL.
- API REST desarrollada con Spring Boot.
- Interfaz responsive desarrollada con Angular.

---

## 🧠 Planificador de compras

Uno de los principales objetivos de MiCarro es incorporar un sistema capaz de generar una compra a partir de las necesidades del usuario.

Por ejemplo:

> **Presupuesto:** 60 €  
> **Necesito:** pollo, arroz, setas y yogures  
> **Modo:** Equilibrado  
> **Priorizar favoritos:** Sí

El sistema seleccionará productos adecuados y ajustará cantidades respetando el presupuesto.

La arquitectura está planteada para soportar diferentes estrategias:

- 💰 **Ahorrar** — prioriza alternativas económicas.
- ⚖️ **Equilibrada** — busca un balance entre precio, preferencias y producto.
- ⭐ **Calidad** — preparada para utilizar criterios adicionales de calidad cuando existan datos fiables.

También se contempla la incorporación futura de favoritos, preferencias personales e interpretación de peticiones mediante lenguaje natural.

---

## 🛠️ Tecnologías

### Frontend

- Angular
- TypeScript
- HTML5
- CSS3

### Backend

- Java 21
- Spring Boot
- Spring Web
- Spring Data JPA
- Hibernate
- Maven

### Base de datos

- PostgreSQL

### Herramientas

- Git
- GitHub
- REST API
- Maven Wrapper
- npm

---

## 🏗️ Arquitectura

```text
                Angular
                   │
                   │ HTTP / REST
                   ▼
              Spring Boot
                   │
          ┌────────┴────────┐
          ▼                 ▼
     PostgreSQL      Product Provider
                            │
                            ▼
                    Fuente externa
                     de productos
```

El frontend no consume directamente la fuente externa de productos.

Spring Boot se encarga de obtener, transformar y almacenar los datos en PostgreSQL, exponiendo posteriormente una API propia para Angular.

Esto permite desacoplar la aplicación de la fuente de datos externa y controlar la persistencia, lógica de negocio y futuras integraciones.

---

## 📦 Importación de productos

MiCarro utiliza una arquitectura basada en proveedores para desacoplar la aplicación de la fuente externa:

```text
External source
      ↓
MercadonaClient
      ↓
MercadonaProvider
      ↓
ProductImportService
      ↓
PostgreSQL
      ↓
MiCarro REST API
      ↓
Angular
```

Actualmente se utiliza información obtenida de endpoints externos asociados al catálogo online de Mercadona.

MiCarro es un proyecto independiente y no oficial, sin afiliación con Mercadona.

---

## 🚀 Roadmap

El desarrollo está dividido en diferentes fases:

- [x] Configuración de Angular
- [x] Configuración de Spring Boot
- [x] Integración con PostgreSQL
- [x] API REST de productos
- [x] Importación del catálogo
- [x] Búsqueda y paginación
- [x] Carrito de compra
- [x] Interfaz inicial
- [ ] Sistema de favoritos
- [ ] Planificador de compras
- [ ] Estrategias de planificación
- [ ] Optimización por presupuesto
- [ ] Usuarios y autenticación
- [ ] Interpretación mediante lenguaje natural
- [ ] Historial de compras
- [ ] Tests
- [ ] Docker
- [ ] CI/CD
- [ ] Despliegue público

---

## ⚙️ Ejecución local

### Requisitos

- Node.js
- Angular CLI
- Java 21
- PostgreSQL 17

### Backend

La conexión con PostgreSQL utiliza una variable de entorno para evitar almacenar credenciales en el repositorio:

```properties
spring.datasource.password=${DB_PASSWORD}
```

Inicia Spring Boot:

```bash
cd backend
./mvnw spring-boot:run
```

En Windows:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

La API estará disponible en:

```text
http://localhost:8080
```

### Frontend

```bash
cd frontend
npm install
ng serve
```

La aplicación estará disponible en:

```text
http://localhost:4200
```

---

## 📂 Estructura

```text
MiCarro/
├── backend/
│   └── Spring Boot REST API
│
├── frontend/
│   └── Angular application
│
├── MICARRO_PLANNING.md
├── .gitignore
└── README.md
```

---

## 🔐 Seguridad

Las credenciales y configuraciones sensibles no se almacenan en Git.

Las contraseñas de la base de datos se proporcionan mediante variables de entorno.

---

## 👨‍💻 Autor

**Daniel Zarco**

Proyecto desarrollado como aplicación personal Full Stack orientada al aprendizaje y aplicación práctica de Angular, Spring Boot, PostgreSQL y arquitectura de software.