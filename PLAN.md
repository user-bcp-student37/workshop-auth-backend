# Plan de corrección

`HALLAZGOS.md` contiene dos items en estado `Pendiente`: H-05 y H-06. H-04 y H-07 están `Resuelto`; no se reabren en este plan.

## Item: La respuesta de error expone detalles internos

**Hallazgo:** H-05 de `HALLAZGOS.md`, estado `Pendiente`. `AuthResource.login` devuelve `error.getMessage()` y `error.getClass().getName()` en la respuesta `500`.

**Problema:** mensaje y clase internos revelan detalles de implementación al cliente. `docs/GOOD_PRACTICES.md` requiere errores genéricos para el cliente y detalle completo solo en logs del servidor.

**Cambio propuesto:** preservar `500`, `application/json`, las claves `message` y `exception` y sus tipos string según `docs/ENDPOINTS.md`; cambiar los valores por textos genéricos constantes, por ejemplo `Internal server error` y `INTERNAL_SERVER_ERROR`. Registrar el detalle de la excepción exclusivamente en el log del servidor. Actualizar el ejemplo del contrato para reflejar los valores genéricos, ya que H-05 pide cambiar el contenido sensible, sin alterar forma ni status.

**Archivos a tocar:**

- `src/main/java/com/bcp/workshop/auth/AuthResource.java`: cuerpo genérico y logging controlado.
- `src/test/java/com/bcp/workshop/auth/AuthSigningFailureTest.java`: caracterizar y comprobar el cuerpo de error.
- `docs/ENDPOINTS.md`: actualizar el ejemplo de `500`, preservando status, media type, claves y tipos.
- `docs/TESTING.md`: actualizar el resultado esperado de la prueba.
- `HALLAZGOS.md`: marcar H-05 `Resuelto` solo con evidencia del test posterior.

**Prueba de caracterización previa:** ejecutar `AuthSigningFailureTest` con clave ausente y registrar `500`, las claves actuales `message`/`exception`, el mensaje actual y `io.smallrye.jwt.build.JwtSignatureException`. La prueba no debe usar ni registrar una clave real.

**Prueba posterior:** repetir el mismo error y comprobar status, claves y tipos sin cambio, valores genéricos y ausencia del mensaje/clase reales en el body. Verificar que el detalle se registra solo en servidor y que el login exitoso conserva su contrato.

## Item: Versiones de dependencias SCA que requieren revisión

**Hallazgo:** H-06 de `HALLAZGOS.md`, estado `Pendiente`. `pom.xml` declara Quarkus `3.27.4` y Commons Text `1.9`; la matriz no confirma todavía un CVE aplicable.

**Problema:** una dependencia afectada por un advisory aplicable puede introducir riesgo de cadena de suministro. El número de versión no demuestra por sí solo que el código use una ruta vulnerable; no se debe actualizar a ciegas ni suprimir findings.

**Cambio propuesto:** investigar el árbol efectivo de dependencias, advisories y resultados disponibles de CodeQL/Dependency Review. Para Commons Text, considerar su uso observado (`StringEscapeUtils.escapeHtml4`) al determinar aplicabilidad; actualizar solo si el advisory afecta una ruta alcanzable y elegir una versión corregida compatible. Para Quarkus, actualizar el BOM solo si un advisory/estado de soporte confirma la necesidad y se verifica compatibilidad. Si no hay vulnerabilidad aplicable, mantener versiones y documentar la evidencia. No añadir dependencias ni reducir gates.

**Archivos a tocar:**

- `pom.xml`: únicamente la dependencia/plataforma que la investigación determine necesario actualizar o retirar.
- `src/test/java/com/bcp/workshop/auth/AuthResourceTest.java`: prueba de regresión del `displayName` escapado si cambia Commons Text.
- `docs/TESTING.md`: guardar comandos, advisory/resultado del escáner y gates ejecutados.
- `HALLAZGOS.md`: marcar H-06 `Resuelto` solo si la vulnerabilidad confirmada queda corregida o la investigación demuestra que no es aplicable y el cierre se acepta con esa evidencia.

**Prueba de caracterización previa:** añadir/ejecutar un login de test con un display name controlado que contenga caracteres HTML y comprobar el escaping actual. Registrar `mvn dependency:tree`, ejecutar `mvn verify` y conservar resultados actuales de CodeQL/Dependency Review antes de cambiar dependencias. Confirmar versiones directas/transitivas y advisory aplicable.

**Prueba posterior:** repetir el caso de escaping, `mvn verify`, árbol de dependencias y los mismos gates de seguridad. Confirmar la versión corregida resuelta y que no se suprimieron findings. Si no se confirma vulnerabilidad alcanzable, no tocar versiones y dejar documentada la evidencia antes de decidir el cierre.

## Item: `AuthResource.login` concentra responsabilidades

**Hallazgo:** H-07 de `HALLAZGOS.md`, estado `Resuelto`.

**Cambio realizado:** `AuthResource` conserva la traducción HTTP; `AuthService` coordina la validación, consulta de credenciales y emisión del token; `LegacyDatabase` implementa `UserRepository`; `JwtTokenIssuer` adapta la generación JWT; `LoginResponseMapper` arma el DTO de respuesta y conserva el escaping del `displayName`.

**Pruebas añadidas:** `LoginRequestValidatorTest` comprueba las reglas de entrada; `AuthServiceTest` comprueba que la entrada inválida no alcanza colaboradores, que credenciales válidas consultan repositorio/emisor y que el resultado se mapea; las pruebas HTTP existentes verifican el contrato completo.

**Validación ejecutada el 2026-10-20:** `mvn -Dquarkus.http.test-port=18084 verify`; `BUILD SUCCESS`, 31 tests, 0 fallos, 0 errores, 0 omitidos. JaCoCo: 98.2% global (111/113 líneas); todas las clases nuevas y `AuthResource` tienen 100%. La matriz HTTP, JWT y el fallo de firma permanecen cubiertos.

**Incidencia fuera de alcance:** durante `AuthSigningFailureTest`, `LegacyDatabase.initialize` sigue registrando `No suitable driver found` en el perfil de clave ausente; el test espera el `500` de firma y pasa. Se conserva como pendiente de investigación, no como parte de H-07.
