# Workshop Auth Backend — Java Quarkus

> **Laboratorio intencionalmente vulnerable.** Ejecutar únicamente en local, repositorios privados y ambientes aislados. No desplegar en Internet, CERT o PROD.

Backend Quarkus que autentica usuarios de laboratorio almacenados en SQLite y entrega un token consumido por el frontend Angular y la API Python.

## Ejecución

Requisitos: JDK 21, Maven 3.9+ y una clave privada RSA en formato PEM para firmar JWT.

Para desarrollo local, genera una clave de prueba fuera del repositorio (requiere OpenSSL).

PowerShell:

```powershell
New-Item -ItemType Directory -Force -Path "$HOME/.config/workshop-auth"
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$HOME/.config/workshop-auth/jwt-private.pem"
$env:JWT_SIGN_KEY_LOCATION = "$HOME/.config/workshop-auth/jwt-private.pem"
mvn quarkus:dev
```

Bash:

```bash
mkdir -p "$HOME/.config/workshop-auth"
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$HOME/.config/workshop-auth/jwt-private.pem"
export JWT_SIGN_KEY_LOCATION="$HOME/.config/workshop-auth/jwt-private.pem"
mvn quarkus:dev
```

No guardes esa clave en el repositorio ni la reutilices en producción. En despliegues, suministra `JWT_SIGN_KEY_LOCATION` mediante la configuración segura del entorno. Sin una clave compatible, el servicio puede iniciar, pero un login válido responde `500` porque no se puede firmar el JWT.

Servicio: `http://localhost:8080`

Credenciales locales:

- `student` / `Workshop123!`
- `trainer` / `Trainer123!`

## Endpoints

- `POST /auth/login`
- `GET /q/health`

## Propósito de la rama `main`

La rama contiene deuda técnica y de seguridad deliberada: SQL concatenado, contraseñas en texto plano, token reversible, secreto hardcodeado, CORS permisivo, método largo, manejo genérico de excepciones y dependencias con CVEs.
