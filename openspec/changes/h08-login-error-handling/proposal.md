# Manejo de errores de persistencia en login

## Fuente
Hallazgo confirmado H-08 en `HALLAZGOS.md`: errores de persistencia SQLite pueden confundirse con credenciales inválidas. No se encontró un issue asociado en Project ReqFlow al confirmar el hallazgo.

## Why
Cuando la verificación de credenciales no puede completarse por un error de persistencia, el comportamiento actual puede parecer un rechazo de credenciales. Distinguir ambos resultados permite a clientes y operadores reconocer fallos internos sin exponer detalles de almacenamiento.

## What Changes
- Definir el resultado HTTP observable para un fallo interno durante la verificación de credenciales.
- Conservar la respuesta existente para credenciales válidas e inválidas.
- Mantener genéricos los detalles internos comunicados al cliente.

## Capabilities

### New Capabilities
- `login-error-handling`: resultados observables de errores al autenticar, distinguiendo credenciales inválidas de fallos internos de persistencia.

### Modified Capabilities
Ninguna.

## Impact
El comportamiento de error de `POST /auth/login` y su contrato HTTP. No se proponen rutas nuevas ni cambios en `GET /q/health`.

## Fuera de alcance
- Añadir o modificar flujos de alta, cambio o restablecimiento de contraseñas.
- Cambiar autenticación, MFA, almacenamiento de contraseñas, esquema de base de datos o proveedor SQLite.
- Definir health checks o readiness de persistencia.