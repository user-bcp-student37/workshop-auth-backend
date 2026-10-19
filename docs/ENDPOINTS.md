# Contrato HTTP

Contrato derivado del código y de respuestas observadas localmente. Todas las rutas usan el host/puerto configurado en Quarkus; valores locales de ejemplo solo sirven para el laboratorio.

## `GET /q/health`

### 1. Qué hace

Permite a un operador o monitor comprobar que el servicio de autenticación responde y conocer su estado agregado de salud.

### 2. Autenticación y headers

- Autenticación: no requiere autenticación.
- `Authorization`: no requerido.
- `Accept: application/json`: opcional; la respuesta observada es JSON.
- No requiere `Content-Type` porque no tiene cuerpo de solicitud.

### 3. Request

| campo | tipo | obligatorio | descripción |
| --- | --- | --- | --- |
| Sin campos | No aplica | No | Es una solicitud `GET` sin cuerpo JSON ni parámetros requeridos por este backend. |

Ejemplo de solicitud completa (sin cuerpo JSON):

```http
GET /q/health HTTP/1.1
Host: localhost:8080
Accept: application/json
```

### 4. Response exitosa

Código: `200 OK`.

| campo | tipo | descripción |
| --- | --- | --- |
| `status` | string | Estado agregado de salud. En la configuración actual observada es `UP`. |
| `checks` | array | Checks registrados por SmallRye Health. Actualmente está vacío porque este componente no declara checks propios. |

Ejemplo observado:

```json
{
  "status": "UP",
  "checks": []
}
```

### 5. Respuestas de error

No se observó una respuesta de error para la configuración actual: no hay checks propios que puedan declarar el servicio `DOWN`. SmallRye Health puede producir `503 Service Unavailable` cuando un check registrado marca `DOWN`, pero este backend no implementa ese caso ni permite derivar un ejemplo concreto de check. No se documenta un cuerpo inventado.

### 6. Notas de seguridad y preguntas abiertas

- El endpoint queda publicado por la dependencia `quarkus-smallrye-health`; no hay autenticación configurada para `/q/health` en este componente.
- Pregunta abierta: si se agregan health checks, ¿qué dependencias deben medir readiness y qué datos de diagnóstico se autoriza exponer en la respuesta `DOWN`?
- Evidencia: `pom.xml` (dependencia `quarkus-smallrye-health`); respuesta `200` comprobada localmente en `/q/health`. La guía de SmallRye Health describe los campos agregados `status` y `checks`.

## `POST /auth/login`

### 1. Qué hace

Valida las credenciales de un usuario de laboratorio y, si son correctas, devuelve sus datos públicos junto con un token JWT firmado de acceso.

### 2. Autenticación y headers

- Autenticación previa: no requiere token ni header `Authorization`; este endpoint inicia la sesión.
- `Content-Type: application/json`: requerido para enviar el DTO reconocido por el recurso.
- `Accept: application/json`: recomendado para recibir la representación JSON; si el cliente solo acepta un tipo incompatible, Quarkus responde `406`.

### 3. Request

| campo | tipo | obligatorio | descripción |
| --- | --- | --- | --- |
| `username` | string | Sí | Nombre de usuario. No puede ser nulo ni quedar vacío tras `trim()`. El valor original se consulta sin recortar espacios. |
| `password` | string | Sí | Contraseña. No puede ser nula ni quedar vacía tras `trim()`. El valor original se verifica contra el hash BCrypt persistido. |
| `client` | string | No | Campo presente en el DTO, pero no se usa para validar credenciales ni para construir la respuesta. |

Las propiedades JSON desconocidas se aceptan porque `quarkus.jackson.fail-on-unknown-properties=false`; no participan en el login.

Ejemplo JSON completo con credenciales ficticias del laboratorio:

```json
{
  "username": "student",
  "password": "Workshop123!",
  "client": "angular-workshop"
}
```

### 4. Response exitosa

Código: `200 OK`; `Content-Type: application/json`.

| campo | tipo | descripción |
| --- | --- | --- |
| `token` | string | JWT firmado RS256. Su valor se genera por ejecución y no es un valor fijo de ejemplo. Claims observados: `sub`, `role`, `iat` y `exp`. |
| `expiresIn` | integer | Duración del token en segundos; actualmente `3600`. Coincide con la diferencia entre `exp` e `iat`. |
| `user` | object | Datos públicos del usuario autenticado. |
| `user.username` | string | Nombre de usuario. |
| `user.displayName` | string | Nombre visible; se escapa como HTML antes de incluirlo. |
| `user.role` | string | Rol almacenado para el usuario, por ejemplo `STUDENT` o `TRAINER` para las semillas locales. |
| `debug` | string | Valor actual `authenticated-with-legacy-flow`. |

Ejemplo JSON completo de forma; el texto de `token` es un marcador para el JWT dinámico y no sirve como credencial:

```json
{
  "token": "<JWT RS256 generado por el servidor>",
  "expiresIn": 3600,
  "user": {
    "username": "student",
    "displayName": "Workshop Student",
    "role": "STUDENT"
  },
  "debug": "authenticated-with-legacy-flow"
}
```

### 5. Respuestas de error

| código | cuándo ocurre | ejemplo de cuerpo |
| --- | --- | --- |
| `400 Bad Request` | El recurso recibe request nulo. Un HTTP body ausente fue observado como esta respuesta. | `{"message":"Request is null"}` |
| `400 Bad Request` | `username` no está, es nulo o contiene solo espacios. | `{"message":"Username is required"}` |
| `400 Bad Request` | `password` no está, es nula o contiene solo espacios. | `{"message":"Password is required"}` |
| `400 Bad Request` | JSON malformado; rechazado por Quarkus antes de entrar al método. En la respuesta local observada no hay cuerpo. | Sin cuerpo (`Content-Length: 0`). |
| `401 Unauthorized` | El repositorio no devuelve un usuario para el username/contraseña. | `{"message":"Invalid user student or password"}` |
| `405 Method Not Allowed` | Se usa un método distinto de `POST` en `/auth/login`; por ejemplo, `GET`. Respuesta observada sin cuerpo. | Sin cuerpo (`Content-Length: 0`). |
| `406 Not Acceptable` | `Accept` excluye `application/json`. Respuesta observada con `Accept: text/plain`, sin cuerpo. | Sin cuerpo (`Content-Length: 0`). |
| `415 Unsupported Media Type` | El `Content-Type` no coincide con `application/json`. Respuesta observada con `text/plain`, sin cuerpo. | Sin cuerpo (`Content-Length: 0`). |
| `500 Internal Server Error` | Una excepción llega al `catch` del recurso. Por ejemplo, la firma falla si no se configura una clave en `JWT_SIGN_KEY_LOCATION`. | `{"message":"Internal server error","exception":"INTERNAL_SERVER_ERROR"}` |

### 6. Notas de seguridad y preguntas abiertas

- Las contraseñas semilla se almacenan con BCrypt y las credenciales se consultan usando parámetros SQL.
- El token es firmado, no cifrado: sus claims pueden leerse, pero la firma permite detectar modificaciones. Expira realmente mediante `exp` después de 3600 segundos.
- La firma requiere que el despliegue configure `JWT_SIGN_KEY_LOCATION` con una clave privada compatible. Sin esa variable el servicio puede iniciar, pero un login válido observado devuelve `500` al firmar.
- CORS permite por defecto `http://localhost:4200` y `http://127.0.0.1:4200`; se puede sustituir la lista mediante `AUTH_CORS_ORIGINS`.
- Para `500`, los valores enviados al cliente son genéricos; los detalles de la excepción se registran solo en el log del servidor.
- Pregunta abierta: ¿el campo `debug` de la respuesta exitosa es un campo público intencional o un dato de diagnóstico que debe retirarse en un cambio futuro explícito?
- Evidencia: `src/main/java/com/bcp/workshop/auth/AuthResource.java:15,16,17,22,23,26,29,32,35,37,39,41,42,43,44,45,46,47,48,49,54,55,56,57,58`; `src/main/java/com/bcp/workshop/auth/LoginRequest.java:4,5,6`; `src/main/java/com/bcp/workshop/auth/LegacyDatabase.java:20,76,82`; `src/main/java/com/bcp/workshop/auth/LegacyTokenUtil.java:8,15,16,17,18,19`; `src/main/resources/application.properties:5,9`; `pom.xml` (dependencias Quarkus REST y SmallRye Health); `src/test/java/com/bcp/workshop/auth/AuthResourceTest.java` (casos de contrato y CORS).
