# Plan de corrección

`HALLAZGOS.md` contiene tres items en estado `Pendiente`: H-05, H-06 y H-07. H-04 está `Resuelto`; no se cuenta como un cuarto pendiente ni se reabre en este plan.

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

**Hallazgo:** H-07 de `HALLAZGOS.md`, estado `Pendiente`. Un método combina validación de request, autenticación, acceso SQLite, creación de token y respuesta con anidamiento profundo.

**Problema:** responsabilidades acopladas dificultan probar cada parte y aumentan el riesgo de duplicación o regresión del contrato al cambiar una regla.

**Cambio propuesto:** mantener `AuthResource` como adaptador HTTP; extraer orquestación de negocio a `AuthService`, persistencia a `UserRepository`, seguridad/token a su componente de responsabilidad única y mapear el resultado a DTOs de salida, sin devolver `UserRecord`. Aplicar guard clauses, nombres descriptivos, una responsabilidad por método y constantes para valores no triviales. Este apartado no cambia la política de errores H-05 ni el análisis de dependencias H-06; no repetir en él sus correcciones.

**Archivos a tocar:**

- `src/main/java/com/bcp/workshop/auth/AuthResource.java`: dejar en el recurso la traducción HTTP y delegación.
- `src/main/java/com/bcp/workshop/auth/LegacyDatabase.java` o un repositorio nuevo: aislar persistencia sin cambiar SQL, hashes o migración.
- Nuevos archivos en `src/main/java/com/bcp/workshop/auth/` solo para servicio, DTOs o componentes necesarios por la separación.
- `src/test/java/com/bcp/workshop/auth/AuthResourceTest.java` y tests unitarios nuevos para servicio, repositorio y mapeo.
- `HALLAZGOS.md`: cambiar H-07 a `Resuelto` al completar pruebas y refactor.

**Prueba de caracterización previa:** completar/ejecutar la matriz HTTP de `docs/ENDPOINTS.md`: login válido; body ausente; username/password ausentes o en blanco; JSON malformado; credenciales inválidas; método y media types no admitidos; error `500` controlado; `client` omitido y propiedad desconocida. Registrar ruta, media types, status, claves y tipos para cada caso antes de mover responsabilidades.

**Prueba posterior:** repetir sin cambios la matriz HTTP y añadir pruebas unitarias independientes por capa y mapeo. Medir JaCoCo con al menos 85% de cobertura del código modificado. Mantener los gates existentes y verificar en revisión que no haya métodos con múltiples responsabilidades, duplicación, anidamiento profundo ni valores mágicos.
