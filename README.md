# Desafío Técnico Previred 2025 — CRUD de Personas

Monorepo con la solución al desafío técnico de Previred: un CRUD de personas con
validaciones, cálculo de edad, manejo de errores y, como requisito central,
**resiliencia ante caída de la base de datos**.

- [`backend/`](backend/README.md) — API REST en Java 21 + Spring Boot 3.5.4 + Maven.
- [`frontend/`](frontend/README.md) — React + Vite + TypeScript, consume la API del backend.
- [`postman/`](postman/Previred-Personas.postman_collection.json) — colección de
  Postman para validar todos los endpoints (CRUD feliz, validaciones y el escenario de
  contingencia).

## Enunciado

Ver `Desafío_2025_Remoto.pdf` en la raíz de este repositorio.

## Arquitectura, en breve

El requisito *"debe asegurarse que el registro quede en la BD, aunque esta no esté
disponible en ese momento"* se resuelve con un patrón de **outbox local +
reconciliación automática** en el backend: si SQL Server no está disponible al crear una
persona, el registro se resguarda en un almacén local independiente (H2 en archivo,
vive en el propio proceso del backend) y un job programado lo sincroniza
automáticamente en cuanto SQL Server vuelve a estar disponible, sin que el usuario
pierda el dato ni la operación falle. El detalle completo está en
[`backend/README.md`](backend/README.md), sección "Arquitectura: resiliencia ante
caída de la base de datos".

## Cómo levantar todo

```bash
# 1. Base de datos
docker compose up -d sqlserver
docker compose up sqlserver-init   # una sola vez: crea la base "previred"

# 2. Backend (ver backend/README.md para más detalle)
cd backend
mvn spring-boot:run

# 3. Frontend (ver frontend/README.md para más detalle)
cd frontend
npm install
npm run dev
```

## Cómo validar los endpoints (Postman)

Importa `postman/Previred-Personas.postman_collection.json` en Postman. Incluye 3
carpetas, ejecutables de punta a punta con el Collection Runner (cada una limpia lo
que crea):

1. **01 - CRUD feliz**: crear, listar, obtener, actualizar y eliminar una persona.
2. **02 - Validaciones**: RUT inválido, campos obligatorios faltantes, fecha de
   nacimiento futura, RUT duplicado (409), intento de cambiar el RUT en un `PUT`
   (400, el RUT es inmutable), y casos 404.
3. **03 - Contingencia (manual)**: requiere detener/reiniciar SQL Server a mano
   (`docker compose stop/start sqlserver`) entre pasos, para verificar en vivo que un
   registro creado con la BD caída queda `PENDIENTE_SINCRONIZACION` y luego pasa a
   `SINCRONIZADO` automáticamente. Los pasos y tiempos de espera están documentados en
   la descripción de esa carpeta dentro de la colección.

La variable de colección `baseUrl` apunta a `http://localhost:8080` por defecto.

## Estado actual

- [x] Backend: CRUD completo, validaciones (incluye RUT chileno con dígito
      verificador), cálculo de edad, manejo global de errores, mecanismo de
      resiliencia (outbox + reconciliación), tests unitarios y de integración.
- [x] Frontend: CRUD completo (listar, crear, editar, eliminar), validación de
      formulario (incluye RUT), manejo de errores del backend y visualización del
      estado de sincronización. Validado manualmente en navegador contra el backend
      real, incluyendo el escenario de contingencia.

## Supuestos generales

Los supuestos y observaciones específicos de cada proyecto están documentados en su
propio README (`backend/README.md`, `frontend/README.md`).
