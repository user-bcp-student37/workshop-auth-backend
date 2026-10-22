# Política segura de contraseñas

## Fuente
[Issue #15: HU-08 - Definir una política segura para contraseñas](https://github.com/user-bcp-student37/workshop-auth-backend/issues/15)

## Por qué importa
El Issue define criterios verificables para aceptar contraseñas: la longitud mínima depende de MFA, se bloquean contraseñas comunes o comprometidas y se admiten frases largas sin reglas arbitrarias de composición.

## Contexto actual
El contrato de `POST /auth/login` exige `username` y `password`, rechaza valores ausentes o en blanco con `400`, verifica las credenciales almacenadas y responde `401` cuando no son válidas. No documenta flujos para crear, cambiar o restablecer contraseñas, ni MFA.

## Comportamiento a preservar
`POST /auth/login` mantiene sus códigos de respuesta, la forma de la respuesta exitosa y la validación de credenciales descrita en el contrato. Esta propuesta no añade restricciones de longitud o lista de bloqueo al inicio de sesión de credenciales ya existentes.

## Alcance
- Definir la política de aceptación al establecer o reemplazar una contraseña.
- Especificar longitud mínima según el uso de MFA, caracteres permitidos, lista de bloqueo y soporte de contraseñas largas.
- Dejar explícito que los escenarios describen resultados observables de la política, no un endpoint nuevo.

## Fuera de alcance
- Cambiar el contrato o la respuesta de `POST /auth/login`.
- Implementar MFA, almacenamiento de contraseñas, limitación de intentos o un mecanismo/fuente concreta para la lista de bloqueo.
- Definir rutas de alta, cambio o restablecimiento que no están documentadas actualmente.

## Criterios de aceptación
- La contraseña requiere al menos 15 caracteres si es el único factor, y al menos 8 si se usa con MFA.
- Se rechazan valores por debajo del mínimo aplicable.
- Letras, números, espacios y símbolos son admisibles sin exigir una combinación fija.
- Se rechazan contraseñas comunes o comprometidas, incluso si cumplen la longitud.
- Se aceptan contraseñas de 64 caracteres o más sin truncamiento silencioso.

## Contratos afectados/protegidos
La política es nueva. El contrato existente de `POST /auth/login` queda protegido y no se modifica implícitamente.

## Riesgos, supuestos y preguntas abiertas
- Issue #15 deja por resolver si el flujo usa MFA y qué mecanismo identifica contraseñas bloqueadas.
- No está definido en qué flujo se establecerán o reemplazarán contraseñas.
- Debe decidirse cómo tratar cuentas existentes si se pretende aplicar esta política durante el inicio de sesión.
