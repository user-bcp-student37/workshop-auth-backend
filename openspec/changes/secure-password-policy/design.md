# Diseno: politica segura de contrasenas

## Contexto tecnico
El servicio usa Quarkus. `AuthResource` expone `POST /auth/login`; `AuthService` delega la validacion de campos a `LoginRequestValidator` y pasa el username y password originales a `UserRepository`. `LegacyDatabase` verifica hashes con `BcryptUtil`, crea usuarios semilla y migra contrasenas legadas a BCrypt. No hay flujos documentados de alta, cambio o restablecimiento de contrasenas, ni estado de MFA o proveedor de contrasenas comprometidas.

`AuthResourceTest` caracteriza el login exitoso, errores de request y credenciales, negociacion HTTP, credenciales semilla y migracion idempotente. La propuesta protege ese comportamiento; la politica se aplica al establecer o reemplazar contrasenas, no durante el login existente.

## Goals
- Definir una politica verificable segun si la contrasena es el unico factor o se combina con MFA.
- Aceptar frases y caracteres diversos sin reglas de composicion.
- Rechazar contrasenas identificadas como comunes o comprometidas.
- Admitir contrasenas de 64 caracteres o mas sin truncamiento silencioso.
- Mantener intacto el contrato actual de autenticacion.

## Non-goals
- Cambiar `POST /auth/login`, su payload, sus respuestas o la validacion de credenciales existentes.
- Anadir flujos HTTP de alta, cambio o restablecimiento.
- Implementar MFA, limitacion de intentos o cambios de almacenamiento como parte de este diseno.
- Seleccionar una fuente concreta para la lista de bloqueo antes de resolver la pregunta abierta del Issue #15.

## Contratos protegidos
`docs/ENDPOINTS.md` define `400` para request o campos ausentes/en blanco, `401` para credenciales invalidas y `200` con la representacion existente para login valido. El valor original de password se verifica sin recortarlo; `trim()` solo detecta valores en blanco. Estos comportamientos, los hashes existentes y la migracion idempotente deben conservarse.

## Componentes afectados
- **Limite de politica de contrasena:** nueva responsabilidad separada del login, invocada por futuros flujos de establecimiento o reemplazo.
- **Contexto de factores:** la evaluacion necesita saber de forma autoritativa si MFA acompana a la contrasena; ese dato no existe en el flujo documentado.
- **Comprobacion de lista de bloqueo:** responsabilidad separada de la longitud; su fuente y comportamiento ante indisponibilidad estan pendientes.
- **Persistencia de credenciales:** debe caracterizarse frente al requisito de 64+ caracteres; no se propone cambiar el esquema ni el algoritmo en este alcance.
- **Login y pruebas existentes:** permanecen protegidos y no son el punto de integracion de esta politica.

## Decisiones tecnicas y alternativas
1. **Separar la politica del login.** Integrarla en `LoginRequestValidator` o aplicarla en cada login podria rechazar credenciales existentes y cambiar respuestas documentadas. Se descarta: la politica corresponde a establecer o reemplazar contrasenas.
2. **Exigir contexto MFA explicito.** No inferir MFA desde `client` ni desde datos ausentes. Hasta resolver la configuracion del flujo, no debe suponerse que aplica el minimo de 8 caracteres.
3. **Aislar la lista de bloqueo.** No incrustar una lista estatica en la regla. La alternativa de consultar un proveedor externo ofrece datos actualizados, pero agrega latencia, disponibilidad y consideraciones de privacidad; un conjunto local requiere actualizacion operativa. La fuente y el modo de fallo quedan pendientes.
4. **No normalizar ni recortar contrasenas.** La validacion no debe alterar el valor almacenado o verificado. Interpretar "caracter" como punto de codigo Unicode y probar esta semantica; `String.length()` cuenta unidades UTF-16 y puede diferir.
5. **Caracterizar BCrypt antes de afirmar compatibilidad con 64+.** No asumir que `BcryptUtil` conserva entradas largas: medir el comportamiento con 64 caracteres ASCII y Unicode antes de integrar. Si existe un limite efectivo por bytes, decidir por separado como cumplir el requisito sin truncar ni colisionar entradas.

## Estrategia incremental
1. Antes de cambiar comportamiento, conservar y ejecutar la caracterizacion actual de `AuthResourceTest`: login valido, errores `400`/`401`, campos en blanco, password original, credenciales semilla y migracion idempotente.
2. Agregar caracterizacion de `BcryptUtil` para entradas de 64+ caracteres ASCII y Unicode, incluyendo verificacion de que contrasenas distintas no se vuelven equivalentes por truncamiento.
3. Una vez resueltos MFA y la fuente de bloqueo, probar la politica de forma aislada: limites 14/15 sin MFA y 7/8 con MFA; contrasenas bloqueadas; espacios y simbolos; ausencia de reglas de composicion; y contrasenas de al menos 64 caracteres.
4. Integrar la politica solamente en el flujo de escritura de contrasenas que se apruebe. No agregar validacion de longitud/lista al login existente. Ejecutar la suite y verificar el contrato HTTP antes de aceptar el cambio funcional.

## Riesgos y trade-offs
- No hay hoy un flujo documentado de escritura de contrasenas al que conectar la politica.
- Sin una fuente autoritativa de MFA puede aplicarse un minimo incorrecto.
- Una lista externa mejora cobertura, pero introduce latencia, privacidad y dependencia de disponibilidad; una lista local requiere actualizacion.
- El limite efectivo de BCrypt puede contradecir la aceptacion de 64+ caracteres, especialmente con caracteres Unicode multibyte.
- Una discrepancia en la forma de contar caracteres puede producir resultados inesperados en limites.

## Rollback
Este cambio de diseno no modifica runtime; se revierte retirando `design.md`. Una implementacion futura debe desplegarse por separado y preservar el login y los hashes existentes. Cualquier cambio de almacenamiento requiere una estrategia de compatibilidad y rollback propia.
