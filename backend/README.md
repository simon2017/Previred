# Personas Backend — Desafío Técnico Previred 2025

API REST para el CRUD de personas del desafío técnico, con foco especial en el
requisito de resiliencia: **el registro debe quedar resguardado aunque la base de
datos no esté disponible en ese momento**.

## Stack

- Java 21
- Spring Boot 3.5.4 (Web, Data JPA, Validation)
- Maven
- SQL Server 2022 (almacén principal, vía Docker Compose)
- H2 en archivo (almacén local de contingencia — ver sección de arquitectura)
- Flyway (migraciones de esquema de SQL Server)
- springdoc-openapi / Swagger UI
- JUnit 5, Mockito, AssertJ, Testcontainers (SQL Server + Toxiproxy), Awaitility

## Requisitos previos

- JDK 21
- Maven 3.9+ (o usar el wrapper si se agrega uno)
- Docker Desktop (para SQL Server vía Docker Compose y para correr los tests de
  integración con Testcontainers)

## Cómo levantar el proyecto

```bash
# Desde la raíz del monorepo (C:\Repositorios\Previred)
docker compose up -d sqlserver
docker compose up sqlserver-init   # una sola vez: crea la base "previred" (la imagen
                                    # de SQL Server no tiene un equivalente a POSTGRES_DB)

# Desde backend/
mvn spring-boot:run
```

La API queda disponible en `http://localhost:8080`. Documentación interactiva en
`http://localhost:8080/swagger-ui.html`.

> El backend se ejecuta fuera de Docker Compose a propósito: así se puede detener
> SQL Server (`docker compose stop sqlserver`) sin reiniciar el backend, para probar en
> vivo el mecanismo de contingencia descrito abajo, y volver a levantarlo
> (`docker compose start sqlserver`) para ver la reconciliación automática.

## Endpoints

| Método | Ruta                   | Descripción                 |
|--------|------------------------|------------------------------|
| POST   | `/api/personas`        | Crea una persona            |
| GET    | `/api/personas`        | Lista todas las personas    |
| GET    | `/api/personas/{rut}`  | Obtiene una persona por RUT |
| PUT    | `/api/personas/{rut}`  | Actualiza una persona       |
| DELETE | `/api/personas/{rut}`  | Elimina una persona         |

El **RUT es la clave primaria** del recurso (ver sección de arquitectura más abajo), y
se usa directamente en la URL, con o sin puntos/guion (ej. `/api/personas/12345678-5`
o `/api/personas/12.345.678-5`, ambos resuelven al mismo registro).

Cada persona incluye `edad` (calculada al momento de la consulta, no almacenada) y
`estadoSincronizacion` (`SINCRONIZADO` o `PENDIENTE_SINCRONIZACION`, ver abajo).

## Arquitectura: resiliencia ante caída de la base de datos

Este es el requisito central del desafío: *"Debe asegurarse que el registro quede en
la BD, aunque esta no esté disponible en ese momento"*. La solución implementa un
**patrón outbox local + reconciliación automática**:

1. **Dos fuentes de datos independientes**, cada una con su propio
   `DataSource`/`EntityManagerFactory`/`PlatformTransactionManager`
   (`config/SqlServerPersistenceConfig`, `config/OutboxPersistenceConfig`):
   - **SQL Server**: el almacén definitivo de personas.
   - **H2 en archivo** (`./data/outbox`): vive en el propio proceso del backend,
     independiente de la red — el "resguardo" de contingencia.
2. Al **crear** una persona, el servicio intenta guardar en SQL Server. Si la operación
   falla por una causa de **conectividad** (SQL Server caído, timeout de conexión, etc.
   — ver `resilience/ResilienceExceptionClassifier`), el registro se guarda en el
   outbox local con estado `PENDING` y la API responde igualmente con éxito (código
   201), indicando `estadoSincronizacion=PENDIENTE_SINCRONIZACION` y un mensaje
   explicativo. Un error de **negocio** (ej. RUT duplicado) nunca cae en esta ruta: se
   propaga normalmente.
3. Un job programado (`resilience/ReconciliationScheduler`, cada 15s por defecto,
   configurable en `app.resilience.reconciliation.fixed-delay-ms`) reintenta guardar en
   SQL Server los registros `PENDING`. Al lograrlo, el registro se **elimina** del
   outbox (ya vive en el almacen definitivo, no hace falta retenerlo ahi). Si un
   registro supera el máximo de reintentos (3 por defecto, configurable en
   `app.resilience.reconciliation.max-intentos`), en cambio se marca `FAILED` y **se
   conserva** en el outbox — deja de reintentarse automáticamente y queda disponible
   para intervención manual (no se implementó una vía para esa intervención, fuera de
   alcance del desafío).
4. **Lectura combinada**: listar y obtener por RUT combinan lo que ya está en SQL
   Server con lo que sigue `PENDING` en el outbox, para que el usuario nunca "pierda de
   vista" un registro creado en contingencia mientras espera la sincronización.
5. **El RUT (normalizado) es la clave primaria**, tanto en SQL Server como en el outbox
   — no se generó un UUID artificial. En Chile el RUT ya identifica de forma única y
   natural a una persona, así que un identificador adicional sería redundante; y de
   paso esto simplifica el propio mecanismo de contingencia: no hay que generar ni
   remapear ningún id al reconciliar, el RUT ya es la misma clave en ambos lados desde
   el momento en que llega la solicitud.

Como el RUT es la PK, se trata como **inmutable**: si el `PUT` incluye un `rut` en el
cuerpo distinto al de la URL, se rechaza con `400` (`RutInmutableException`) — no tiene
sentido "mover" un registro a otra clave primaria; para cambiar el RUT de una persona
habría que eliminar el registro y crear uno nuevo.

### Cómo probar la contingencia manualmente

```bash
docker compose stop sqlserver
# crear una persona vía POST /api/personas o desde el frontend -> queda PENDIENTE_SINCRONIZACION
docker compose start sqlserver
# esperar ~15s (o el valor configurado) -> GET /api/personas/{rut} debe mostrar SINCRONIZADO
```

También hay un test automatizado de este flujo completo:
`src/test/java/.../integration/ContingenciaIT.java` (usa Testcontainers + Toxiproxy
para cortar y restaurar la conexión a SQL Server de forma determinista).

## Validaciones

- **RUT chileno**: anotación `@Rut` (Bean Validation custom), valida el dígito
  verificador con el algoritmo módulo 11 (`util/RutUtils`). Acepta formato con o sin
  puntos/guion.
- **Fecha de nacimiento**: `@NotFutureDate`, no puede ser una fecha futura.
- Campos de texto obligatorios (`@NotBlank`) con longitud máxima razonable.
- RUT único: al ser la clave primaria de la tabla, la unicidad la garantiza la propia
  base de datos; se valida proactivamente también en el servicio (para un mensaje de
  error más claro) y se traduce cualquier violación de constraint remanente (ej. una
  carrera entre dos solicitudes concurrentes) al mismo error de negocio (409).
- El RUT en la URL (`/api/personas/{rut}`) también se valida con `@Rut` directamente
  sobre el `@PathVariable`: un RUT con formato o dígito verificador inválido en la URL
  se rechaza con `400` antes de llegar al service.

## Manejo de errores

`exception/GlobalExceptionHandler` centraliza las respuestas de error en un formato
consistente (`ErrorResponse`): 400 (validación), 404 (no encontrado), 409 (RUT
duplicado), 503 (BD no disponible para actualizar/eliminar un registro ya
sincronizado — ver supuestos), 500 (error inesperado, sin exponer detalles internos).

## Tests

```bash
mvn test      # unitarios (JUnit5 + Mockito): validación de RUT, edad, lógica de
              # resiliencia del service y del scheduler de reconciliación
mvn verify    # además corre los tests de integración con Testcontainers
              # (requiere Docker Desktop corriendo)
```

> **Estado de validación real:** `mvn verify` se ejecutó con JDK 21, Maven 3.9.16 y
> Docker Desktop — build exitoso, **41/41 tests en verde** (36 unitarios: `RutUtilsTest`,
> `EdadCalculatorTest`, `PersonaServiceImplTest`, `ReconciliationSchedulerTest`; 5 de
> integración con Testcontainers: `PersonaControllerIT`, `ContingenciaIT`, contra SQL
> Server real). El backend también se probó de punta a punta corriendo localmente
> (`mvn spring-boot:run` + SQL Server vía `docker-compose`) con el CRUD completo por
> `curl` y el escenario de contingencia real (detener/reiniciar el contenedor de SQL
> Server, verificar `PENDIENTE_SINCRONIZACION` → reconciliación automática →
> `SINCRONIZADO`, y que el registro se elimina del outbox H2 al sincronizar).

## Supuestos y observaciones

- **El RUT es la clave primaria** (no se usa un UUID artificial): es un identificador
  natural y único en el dominio, y usarlo directamente simplifica el mecanismo de
  contingencia (mismo RUT en SQL Server y en el outbox, sin remapeos). Como
  consecuencia, se trata como inmutable: un `PUT` que intente cambiar el RUT de un
  registro existente se rechaza con `400`.
- **El RUT no está protegido**: viaja en texto plano en la URL (`/api/personas/{rut}`),
  en los bodies de request/response y en el almacenamiento (SQL Server y el outbox H2) —
  es un dato personal sensible en Chile, más aún en el contexto de una empresa que
  maneja datos previsionales. Para este desafío se dejó así deliberadamente: el
  enunciado pide usar el RUT como identificador del recurso, y protegerlo (ej. cifrarlo
  en la URL) rompería esa interfaz. Se evaluó reemplazar el RUT en la URL por un hash
  o un identificador opaco generado al crear, pero un hash simple del RUT es
  reversible por fuerza bruta (el universo de RUTs válidos es pequeño) y no protege el
  RUT en el resto del tráfico (sigue viajando en los bodies); no se justificaba la
  complejidad adicional (HMAC con secreto, o un UUID subrogado) para este alcance. **En
  un sistema productivo real, el RUT (y el resto de los datos personales) debería
  protegerse con cifrado en reposo y en tránsito**, y evaluarse un identificador opaco
  para la URL en vez de la clave natural.
- El alcance de la resiliencia cubre completamente **creación** y **lectura** (el caso
  explícito del enunciado). Para **actualizar/eliminar** un registro que ya está
  sincronizado en SQL Server: si aún está `PENDIENTE_SINCRONIZACION` en el outbox, la
  actualización/eliminación se aplica directamente ahí; si ya está sincronizado y SQL
  Server cae justo en ese momento, se responde `503` con un mensaje claro en vez de
  intentar generalizar el outbox a esos casos (se consideró fuera del alcance
  estrictamente pedido).
- El esquema del H2 de contingencia se gestiona con `hibernate.ddl-auto=update` (es
  infraestructura técnica interna, no schema de negocio); el esquema de SQL Server sí
  se gestiona formalmente con Flyway.
- El scheduler de reconciliación asume una única instancia del backend corriendo (no
  hay lock distribuido tipo ShedLock); en un entorno con múltiples instancias, dos
  nodos podrían intentar reconciliar el mismo registro al mismo tiempo.
- No se implementó **autenticación/autorización**: fuera del alcance funcional descrito en
  el enunciado.