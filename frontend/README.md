# Previred - Personas (Frontend)

Frontend del CRUD de personas del Desafío Técnico 2025. React + Vite + TypeScript,
consume la API del [backend](../backend/README.md).

## Stack

- React 19 + TypeScript, scaffolding con Vite (plantilla oficial `react-ts`).
- `react-router-dom` para las rutas (listado, crear, editar).
- Sin librería de estado ni de fetching: el CRUD es simple, `fetch` nativo +
  `useState`/`useEffect` es suficiente y evita dependencias innecesarias.
- `oxlint` (linter incluido por la plantilla de Vite) + `tsc` en modo estricto.

## Requisitos

- Node.js 20+ (probado con Node 24).
- El [backend](../backend/README.md) corriendo en `http://localhost:8080` (con Postgres
  vía `docker-compose` en la raíz del repo).

## Instalación y ejecución

```bash
npm install
npm run dev
```

Abre `http://localhost:5173`. En desarrollo, Vite reenvía todo lo que empieza con
`/api` hacia `http://localhost:8080` (ver `vite.config.ts`), así que el navegador ve
todo como same-origin y no hace falta configurar CORS en el backend.

```bash
npm run build    # type-check (tsc -b) + build de produccion en dist/
npm run preview  # sirve el build de dist/ localmente
npm run lint     # oxlint
```

## Estructura

```
src/
  api/            # tipos que reflejan los DTOs del backend y el cliente HTTP
  components/     # piezas de UI reutilizables (formulario, badge de estado, dialogo, etc.)
  pages/          # una pagina por ruta (listado, crear/editar)
  validation/     # validacion de RUT en el cliente (espejo del backend, ver supuestos)
```

## Funcionalidad

- Listar personas (muestra edad calculada y el estado de sincronización con la BD).
- Crear, editar y eliminar una persona.
- El RUT es inmutable una vez creado (el campo se deshabilita al editar), igual que en
  el backend.
- Errores de validación devueltos por el backend (`fieldErrors`) se muestran junto a
  cada campo del formulario; errores generales (404, 409, 503) se muestran como un
  aviso en la parte superior de la página.
- Si el backend queda en modo de contingencia (Postgres caído al crear), la persona
  igual se crea y se muestra con la etiqueta "Pendiente de sincronización"; una vez que
  el backend reconcilia el registro (automático, cada ~15s), basta con recargar el
  listado para ver el estado actualizado a "Sincronizado".

## Supuestos y observaciones

- **Validación de RUT duplicada a propósito**: `src/validation/rut.ts` reimplementa el
  mismo algoritmo de dígito verificador módulo 11 que `RutUtils` en el backend, para dar
  feedback inmediato en el formulario sin esperar un round-trip. El backend sigue
  siendo la única fuente de verdad — toda request se revalida igual del lado servidor.
- **Sin actualización en tiempo real del estado de sincronización**: si un registro
  queda "Pendiente de sincronización" y el backend lo reconcilia en segundo plano, la
  UI no hace polling; hay que recargar el listado para ver el cambio. Se consideró
  fuera de alcance para este desafío (agregaría complejidad — polling o WebSocket — sin
  que el enunciado lo pida).
- **RUT no protegido**: al igual que en el backend (ver su README), el RUT viaja y se
  muestra en texto plano en la UI. Se evaluó y descartó ofuscarlo — ver la sección de
  supuestos del backend para el detalle de esa decisión.
- **Sin autenticación**: no hay login ni control de acceso, fuera del alcance
  funcional del desafío.
- **Validado manualmente de punta a punta**: se probó el flujo completo (crear, listar,
  editar, eliminar, validaciones de formulario) en un navegador real contra el backend
  corriendo localmente, incluyendo el escenario de contingencia (crear con Postgres
  caído y ver la reconciliación automática reflejada tras recargar).
