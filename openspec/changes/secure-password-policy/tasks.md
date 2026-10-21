# Tareas: politica segura de contrasenas

## 1. Caracterizacion previa
- [ ] 1.1 Ejecutar `AuthResourceTest` sin cambios funcionales y registrar el resultado como baseline.
- [ ] 1.2 Agregar caracterizacion de que el login solo usa `trim()` para rechazar passwords vacios y verifica el valor original, incluidos espacios significativos.
- [ ] 1.3 Caracterizar el comportamiento de BCrypt con passwords de 64 o mas caracteres ASCII y Unicode; comprobar si entradas distintas conservan distincion y no se truncan silenciosamente.
- [ ] 1.4 Confirmar que los tests actuales de login exitoso, respuestas `400`/`401`, hashes semilla y migracion idempotente protegen el contrato documentado.

## 2. Decisiones previas a integracion
- [ ] 2.1 Confirmar el flujo existente de establecimiento o reemplazo de passwords al que aplica la politica; no crear un endpoint como parte de esta tarea.
- [ ] 2.2 Confirmar la fuente autoritativa para saber si MFA acompana al password; no inferirlo de `client` ni de datos ausentes.
- [ ] 2.3 Acordar la fuente de passwords comunes/comprometidos y el comportamiento ante su indisponibilidad.
- [ ] 2.4 Revisar el resultado de caracterizacion de BCrypt; si no preserva entradas largas, detener la integracion y obtener una decision de alcance/arquitectura antes de cambiar el almacenamiento.

## 3. Implementacion incremental
- [ ] 3.1 Implementar la regla aislada de longitud: minimo 15 caracteres sin MFA y minimo 8 cuando MFA este confirmado.
- [ ] 3.2 Implementar la regla de caracteres permitidos sin exigir combinaciones de mayusculas, numeros o simbolos; conservar espacios y el valor original.
- [ ] 3.3 Integrar el rechazo de passwords bloqueados usando la fuente y el comportamiento de fallo aprobados.
- [ ] 3.4 Integrar la politica unicamente en el flujo de establecimiento/reemplazo confirmado, sin aplicarla al login existente.
- [ ] 3.5 Asegurar soporte de 64 o mas caracteres sin truncamiento, solo con una estrategia compatible con la decision aprobada de almacenamiento.

## 4. Verificacion
- [ ] 4.1 Probar limites inferiores y exactos: 14/15 caracteres sin MFA y 7/8 con MFA.
- [ ] 4.2 Probar rechazo de passwords comunes/comprometidos aunque cumplan longitud, y aceptacion de frases con espacios/simbolos sin reglas de composicion.
- [ ] 4.3 Probar passwords de 64 o mas caracteres ASCII y Unicode y verificar que el valor completo se conserva.
- [ ] 4.4 Ejecutar de nuevo `AuthResourceTest` y confirmar que permanecen las respuestas y el comportamiento protegido de `POST /auth/login`.
- [ ] 4.5 Ejecutar la suite Maven completa y revisar el diff para confirmar que solo cambia el alcance aprobado, sin endpoints ni trabajo secundario.
