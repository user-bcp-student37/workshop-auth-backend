# Pruebas de endpoints

Las pruebas comprueban el contrato de [`ENDPOINTS.md`](ENDPOINTS.md). `GET /q/health` no requiere checks de aplicación en la configuración normal; el caso `DOWN` usa un health check exclusivo de test para verificar el `503` condicional de SmallRye Health, sin cambiar el comportamiento de producción.

| endpoint | caso | tipo | resultado esperado |
| --- | --- | --- | --- |
| `GET /q/health` | Respuesta agregada sin checks registrados | Camino feliz | `200`; `status=UP`, `checks=[]`. |
| `GET /q/health` | `Authorization: Bearer invalid-token` | Caso borde | `200`; el endpoint es público y no valida el token. |
| `GET /q/health` | Check de readiness informa `DOWN` (fixture solo de test) | Error | `503`; `status=DOWN` y el check `test-down` aparece como `DOWN`. |
| `POST /auth/login` | Credenciales válidas y `client` presente | Camino feliz | `200`; `token`, `expiresIn`, `user` y `debug` conservan los campos/tipos del contrato. |
| `POST /auth/login` | `client` omitido y propiedad JSON desconocida | Caso borde | `200`; autenticación válida y propiedad adicional ignorada. |
| `POST /auth/login` | `displayName` de fixture contiene `<b>A&B</b>` | Caso borde de salida HTML | `200`; `user.displayName` igual a `&lt;b&gt;A&amp;B&lt;/b&gt;`. |
| `POST /auth/login` | Username con comilla | Caso borde de seguridad | `401`; no autentica ni termina en `500`. |
| `POST /auth/login` | Sin body | Error `400` | `{"message":"Request is null"}`. |
| `POST /auth/login` | Username ausente/nulo | Error `400` | `{"message":"Username is required"}`. |
| `POST /auth/login` | Username solo espacios | Error `400` | `{"message":"Username is required"}`. |
| `POST /auth/login` | Password ausente/nula | Error `400` | `{"message":"Password is required"}`. |
| `POST /auth/login` | Password solo espacios | Error `400` | `{"message":"Password is required"}`. |
| `POST /auth/login` | JSON malformado | Error `400` | Respuesta del framework sin cuerpo. |
| `POST /auth/login` | Usuario conocido y password incorrecta | Error `401` | `{"message":"Invalid user student or password"}`. |
| `POST /auth/login` | Usuario desconocido | Error `401` | `{"message":"Invalid user unknown-user or password"}`. |
| `POST /auth/login` | Se invoca `GET` en la ruta `POST` | Error `405` | Respuesta del framework sin cuerpo. |
| `POST /auth/login` | `Accept: text/plain` | Error `406` | Respuesta del framework sin cuerpo. |
| `POST /auth/login` | `Content-Type: text/plain` | Error `415` | Respuesta del framework sin cuerpo. |
| `POST /auth/login` | Clave de firma no disponible | Error `500` | `{"message":"Internal server error","exception":"INTERNAL_SERVER_ERROR"}`; el detalle de la excepción se queda en logs de servidor. |

## Código de los tests

El código ejecutable está en estos archivos fuente; no se duplica aquí para evitar que la documentación y los tests diverjan:

- [`AuthResourceTest.java`](../src/test/java/com/bcp/workshop/auth/AuthResourceTest.java): login, validación de request, content negotiation, errores HTTP, JWT, CORS, BCrypt y migración.
- [`AuthSigningFailureTest.java`](../src/test/java/com/bcp/workshop/auth/AuthSigningFailureTest.java): error `500` con clave de firma ausente y aserción de valores genéricos en el cuerpo.
- [`HealthEndpointTest.java`](../src/test/java/com/bcp/workshop/auth/HealthEndpointTest.java): health `UP`, checks vacíos y acceso sin autenticación.
- [`HealthUnavailableEndpointTest.java`](../src/test/java/com/bcp/workshop/auth/HealthUnavailableEndpointTest.java): respuesta `503` de un check `DOWN` en perfil aislado.
- [`TestDownHealthCheck.java`](../src/test/java/com/bcp/workshop/auth/TestDownHealthCheck.java) y [`HealthDownTestProfile.java`](../src/test/java/com/bcp/workshop/auth/HealthDownTestProfile.java): fixture y activación únicamente en test.
- [`JwtTestResource.java`](../src/test/java/com/bcp/workshop/auth/JwtTestResource.java): genera una clave RSA efímera bajo `target/` para las pruebas que emiten JWT y la elimina al terminar.

`AuthResourceTest.logsInWithWorkshopUser` verifica también la firma y claims del JWT de la respuesta usando la clave pública correspondiente a esa clave efímera. `AuthSigningFailureTest` usa un perfil separado con ruta de clave inexistente para caracterizar el `500` cuando falta la clave; no usa ni imprime una clave real.

## Medición y ejecución

Directorio: raíz del componente `workshop-auth-backend/`.

Requisitos usados: JDK 21 y Maven disponible en `PATH`. Se aisló SQLite en `target/` mediante `AUTH_DB_URL`, se usó el puerto de test `18084` y se desactivó la telemetría de build para la ejecución local.

Comando PowerShell ejecutado:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
$dbName = [guid]::NewGuid().ToString('N')
$env:AUTH_DB_URL = "jdbc:sqlite:./target/test-$dbName.db"
$env:QUARKUS_ANALYTICS_DISABLED = 'true'
mvn '-Dquarkus.http.test-port=18084' verify
```

Resultado ejecutado el 2026-10-19 con Quarkus 3.40.1 y Commons Text 1.15.0: código de salida `0`, `BUILD SUCCESS`; 24 tests, 0 fallos, 0 errores y 0 omitidos. `verify` generó `target/site/jacoco/jacoco.csv` y el reporte HTML `target/site/jacoco/index.html`.

Cobertura de líneas JaCoCo (método: `LINE_COVERED / (LINE_COVERED + LINE_MISSED)` del CSV generado):

| clase de producción | cobertura de líneas |
| --- | ---: |
| `AuthResource` | 100% (25/25) |
| `LegacyDatabase` | 96% (48/50) |
| `LegacyTokenUtil` | 100% (7/7) |
| Agregado de clases de producción | 97.6% (82/84) |

La clase sintética `LegacyDatabase.StoredPassword` se excluye del agregado. JaCoCo está configurado por `pom.xml` y no se modificaron sus gates.

## Fallos observados

- Una ejecución aislada inicial de la prueba de migración falló porque asumía que otro test ya había creado `legacy_users`. Se corrigió el setup para crear la base con un login válido; aislada con SQLite nueva, volvió a pasar (`1` test, salida `0`).
- Durante `verify` con SQLite nueva se imprimió un stacktrace `java.sql.SQLException: No suitable driver found for jdbc:sqlite:./target/test-<id>.db` desde `LegacyDatabase.initialize`. El inicializador captura la excepción y el build continuó; las 24 pruebas pasaron. Se conserva como incidencia pendiente de investigación porque el error no debe considerarse validado solo por el resultado verde del suite.

- Sin `JWT_SIGN_KEY_LOCATION`, el login válido responde `500` al intentar firmar. Es intencionalmente comprobado por `AuthSigningFailureTest`; el flujo normal de desarrollo ahora requiere configurar la clave como indica `README.md`.

## Pendientes y propuestas

- El test `503` usa un check de salud exclusivo de test, ya que producción no registra checks propios. Si se agrega un check de aplicación, deberá añadirse una prueba de su condición `UP`/`DOWN` y revisar qué diagnósticos expone.
- No quedan pruebas propuestas sin ejecutar para las respuestas actuales de `docs/ENDPOINTS.md`. El stacktrace de SQLite descrito arriba es una incidencia pendiente, no un test adaptado ni una respuesta HTTP aprobada.
- H-06: `dependency:tree` confirmó `commons-text:1.15.0` y los módulos Quarkus `3.40.1`. NVD CVE-2022-42889 afecta Commons Text 1.5–1.9; el código solo utiliza `StringEscapeUtils.escapeHtml4`, pero se actualizó igualmente. CodeQL CLI no está instalado localmente y Dependency Review solo está configurado para `pull_request`; no hay PR abierta, así que esos dos gates no se ejecutaron aquí. Sus workflows no se modificaron ni deshabilitaron.
