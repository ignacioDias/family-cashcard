# Family Cash Card

Family Cash Card es una aplicación construida con **Spring Boot** que permite a padres administrar de forma simple y segura las "tarjetas de efectivo" virtuales de sus hijos. La idea es reemplazar el manejo manual del dinero por un sistema digital para enviar, recibir y controlar fondos.

La app evoluciona desde operaciones básicas (crear una tarjeta) hasta un CRUD completo, incorporando autenticación, autorización y pruebas automatizadas.

---

## 📁 Estructura del Proyecto

El proyecto sigue la convención estándar de Spring Boot con Gradle:

```
src/
├── main/
│   ├── java/example/
│   │   ├── cashcard/
│   │   │   ├── CashCard.java
│   │   │   ├── CashCardController.java
│   │   │   ├── CashCardRepository.java
│   │   │   └── SecurityConfig.java
│   │   ├── FamilyCashCardApplication.java
│   │   └── user/
│   │       ├── User.java
│   │       ├── UserController.java
│   │       ├── UserRepository.java
│   │       ├── ChangePasswordRequest.java
│   │       ├── UserRegistrationRequest.java
│   │       └── DatabaseUserDetailsService.java
│   └── resources/
│       ├── application.properties
│       ├── schema.sql
│       ├── static/
│       └── templates/
└── test/
    ├── java/example/
    │   ├── cashcard/
    │   │   ├── CashCardApplicationTests.java
    │   │   └── CashCardJsonTest.java
    │   ├── user/UserTests.java
    │   └── GeneratePasswordHash.java
    └── resources/
        ├── application.properties
        ├── data.sql
        └── example/cashcard/
            ├── list.json
            └── single.json
```

---

## 🚀 Funcionalidades

### Gestión de Cash Cards

* Crear una nueva tarjeta.
* Obtener una tarjeta por ID.
* Listar todas las tarjetas del usuario autenticado.
* Actualizar su monto.
* Eliminar una tarjeta.

### Sistema de Usuarios

* Registro de usuarios.
* Inicio de sesión.
* Borrado de cuenta.
* Cambio de contraseña.

### Seguridad

* Implementación de **Spring Security**.
* Servicio `DatabaseUserDetailsService` para cargar usuarios desde la BD.
* Verificación de contraseña con `PasswordEncoder`.
* Protección de endpoints para evitar accesos no autorizados.

### Base de Datos

* Generación de tablas vía `schema.sql`.
* Datos de prueba con `data.sql`.

### Pruebas

* Pruebas unitarias y de integración para:

  * API de Cash Cards
  * API de Usuarios
* Fixtures JSON para validar serialización/deserialización.

---

## 🛠️ Tecnologías

* **Java**
* **Spring Boot**
* **Spring Web**
* **Spring Security**
* **H2** (test) / base de datos relacional
* **Gradle Kotlin DSL** (`build.gradle.kts`)
* **JUnit** para pruebas

---

## ▶️ Cómo ejecutar

### 1. Clonar el proyecto

```bash
git clone <repo-url>
cd family-cashcard
```

### 2. Ejecutar con Gradle Wrapper

```bash
./gradlew bootRun
```

### 3. Acceder a la aplicación

Por defecto corre en:

```
http://localhost:8080
```

---

## 📦 Empaquetar el JAR

```bash
./gradlew build
```

El archivo quedará en:

```
build/libs/family-cashcard-0.0.1-SNAPSHOT.jar
```

---

## 🧪 Ejecutar pruebas

```bash
./gradlew test
```

Los reportes HTML se generan en:

```
build/reports/tests/test/index.html
```

---

## 📚 Descripción del objetivo

Family Cash Card fue diseñada como una aplicación pedagógica para aprender a:

* Modelar entidades en Spring Boot (`CashCard`, `User`).
* Construir APIs RESTful completas.
* Usar repositorios basados en Spring Data.
* Manejar seguridad con Spring Security.
* Aplicar validaciones.
* Escribir pruebas unitarias e integradas.
* Estructurar y empaquetar un proyecto profesional.
