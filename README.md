# Track

Aplicación web para gestionar tareas activas. Está desarrollada con Spring Boot, ofrece una interfaz HTML y una API REST, y limita el acceso a las tareas según el usuario autenticado.

## Tecnologías

- Java 21
- Spring Boot 4.1
- Spring MVC y Thymeleaf
- Spring Data JPA
- Spring Security
- Flyway
- H2 (base de datos en memoria)
- Gradle

## Requisitos

- JDK 21

No es necesario instalar Gradle: el proyecto incluye el *wrapper* (`gradlew`).

## Ejecutar la aplicación

```bash
./gradlew bootRun
```

Después, abre [http://localhost:8080/app/tasks](http://localhost:8080/app/tasks).

La base de datos H2 se crea en memoria, por lo que su contenido se reinicia al detener la aplicación. La consola está disponible en [http://localhost:8080/h2-console](http://localhost:8080/h2-console).

El esquema se crea y actualiza mediante las migraciones de Flyway en `src/main/resources/db/migration`. Hibernate solo valida el esquema resultante.

| Campo | Valor |
| --- | --- |
| JDBC URL | `jdbc:h2:mem:trackdb` |
| Usuario | `sa` |
| Contraseña | *(vacía)* |

## Usuarios iniciales

| Usuario | Contraseña | Rol |
| --- | --- | --- |
| `admin` | `admin123` | `ADMIN` |
| `javi` | `password` | `USER` |
| `alex` | `password` | `USER` |

## Roles y permisos

### USER

- Crear tareas propias.
- Consultar, editar y finalizar exclusivamente sus tareas activas.
- Ver únicamente sus tareas activas.

### ADMIN

- Consultar y editar las tareas activas de cualquier usuario.
- Buscar usuarios y tareas por identificador desde el panel.
- No puede crear ni finalizar tareas.

Finalizar una tarea no la elimina de la base de datos: se registra su fecha de finalización y deja de aparecer entre las tareas activas.

## Interfaz web

| Ruta | Descripción |
| --- | --- |
| `/app/tasks` | Lista de tareas activas. |
| `/app/tasks/new` | Formulario de creación (solo `USER`). |
| `/app/tasks/{id}` | Detalle de una tarea. |
| `/app/tasks/{id}/edit` | Edición de una tarea. |

La autenticación web usa el formulario de inicio de sesión predeterminado de Spring Security.

## API REST

Los endpoints requieren autenticación. Se puede utilizar HTTP Basic.

| Método | Ruta | Descripción |
| --- | --- | --- |
| `GET` | `/tasks` | Lista las tareas activas permitidas al usuario. |
| `GET` | `/tasks/{id}` | Obtiene una tarea. |
| `POST` | `/tasks` | Crea una tarea (`USER`). |
| `PUT` | `/tasks/{id}` | Actualiza una tarea autorizada. |
| `DELETE` | `/tasks/{id}` | Finaliza una tarea propia (`USER`). |
| `GET` | `/users?query={texto}` | Busca usuarios (`ADMIN`). |

Ejemplo de creación de tarea:

```bash
curl -u javi:password \
  -H 'Content-Type: application/json' \
  -X POST http://localhost:8080/tasks \
  -d '{"name":"Preparar documentación","description":"Crear el README del proyecto"}'
```

## Ejecutar pruebas

```bash
./gradlew test
```

Las pruebas cubren validación de formularios, controlador REST, páginas web, repositorio, servicio y reglas de seguridad.
