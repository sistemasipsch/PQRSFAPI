# Diccionario de Datos - Sistema PQRSF SIAU (IPS Clinical House)

## Visión General
Este modelo de datos soporta el ciclo de vida completo de las Peticiones, Quejas, Reclamos, Sugerencias y Felicitaciones (PQRSF) gestionadas por el Servicio de Información y Atención al Usuario (SIAU) de **IPS Clinical House** (`clinicalhouse.co`), garantizando trazabilidad rigurosa, auditoría inmutable y cumplimiento de la Ley 1581 de 2012 de Habeas Data en Salud.

---

## 1. Tabla: `usuarios`
Almacena las cuentas administrativas y operativas de SIAU autorizadas para acceder al portal de gestión.

| Campo | Tipo | Nulo | Descripción |
|---|---|---|---|
| `id` | SERIAL | NO | Clave primaria autoincremental |
| `nombre` | VARCHAR(120) | NO | Nombre completo del funcionario (ej. "Jefe SIAU Clinical House") |
| `correo` | VARCHAR(150) | NO | Correo corporativo único (ej. `siau@clinicalhouse.co`) |
| `password_hash` | VARCHAR(255) | NO | Contraseña cifrada con algoritmo BCrypt |
| `rol` | VARCHAR(40) | NO | Rol en el sistema: `JEFE_SIAU`, `AUDITOR`, `ADMIN` |
| `cargo` | VARCHAR(100) | SÍ | Cargo institucional ("Jefe de SIAU") |
| `activo` | BOOLEAN | NO | Estado de la cuenta (TRUE = Activo, FALSE = Inactivo) |
| `ultimo_acceso` | TIMESTAMP | SÍ | Fecha y hora del último login exitoso |
| `creado_el` | TIMESTAMP | NO | Fecha de creación del registro |
| `actualizado_el` | TIMESTAMP | NO | Fecha de última modificación |

---

## 2. Tabla: `pqrsf`
Entidad principal que almacena el formulario radicado por el usuario/paciente.

| Campo | Tipo | Nulo | Descripción |
|---|---|---|---|
| `id` | BIGSERIAL | NO | Clave primaria |
| `consecutivo` | VARCHAR(30) | NO | Identificador único formateado. **Inicia en `pqrs-ch-0001`** |
| `fecha_recepcion` | DATE | NO | Fecha en que se recibe o presenta la solicitud |
| `fecha_radicacion` | TIMESTAMP | NO | Marca de tiempo del registro en el sistema |
| **Datos Acompañante** | | | |
| `fam_primer_apellido` | VARCHAR(80) | NO | Primer apellido del familiar o acompañante |
| `fam_segundo_apellido` | VARCHAR(80) | NO | Segundo apellido del familiar o acompañante |
| `fam_primer_nombre` | VARCHAR(80) | NO | Primer nombre del familiar o acompañante |
| `fam_segundo_nombre` | VARCHAR(80) | SÍ | Segundo nombre del familiar o acompañante |
| `fam_tipo_documento` | VARCHAR(50) | NO | CC, TI, CE, Pasaporte, Registro Civil |
| `fam_numero_documento` | VARCHAR(30) | NO | Número de identificación |
| `fam_email` | VARCHAR(150) | NO | Correo electrónico de contacto del remitente |
| `fam_direccion` | VARCHAR(200) | NO | Dirección domiciliaria del familiar |
| `fam_telefono` | VARCHAR(30) | NO | Teléfono de contacto |
| **Datos Paciente** | | | |
| `pac_primer_apellido` | VARCHAR(80) | NO | Primer apellido del paciente |
| `pac_segundo_apellido` | VARCHAR(80) | NO | Segundo apellido del paciente |
| `pac_primer_nombre` | VARCHAR(80) | NO | Primer nombre del paciente |
| `pac_segundo_nombre` | VARCHAR(80) | SÍ | Segundo nombre del paciente |
| `pac_tipo_documento` | VARCHAR(50) | NO | CC, TI, CE, Pasaporte, Registro Civil |
| `pac_numero_documento` | VARCHAR(30) | NO | Número de documento del paciente |
| `pac_email` | VARCHAR(150) | NO | Correo electrónico del paciente |
| `pac_direccion` | VARCHAR(200) | NO | Dirección del paciente |
| `pac_telefono` | VARCHAR(30) | NO | Teléfono del paciente |
| `pac_regimen` | VARCHAR(50) | NO | Contributivo, Subsidiado, Especial, Particular |
| `pac_entidad` | VARCHAR(120) | NO | EPS / Aseguradora de salud |
| `pac_poblacion` | VARCHAR(50) | NO | Adulto Mayor, Primera Infancia, Infancia, Adolescencia, etc. |
| **Detalle Solicitud** | | | |
| `tipo_solicitud` | VARCHAR(30) | NO | `(P) Petición`, `(Q) Queja`, `(R) Reclamo`, `(S) Sugerencia`, `(F) Felicitación` |
| `descripcion` | TEXT | NO | Narración detallada de los hechos |
| **Respuesta (A/B/C)** | | | |
| `medio_respuesta` | VARCHAR(2) | NO | Canal elegido: `A` (Domicilio), `B` (Personal/Teléfono), `C` (Correo) |
| `resp_direccion` | VARCHAR(200) | SÍ | Requerido si `medio_respuesta = 'A'` |
| `resp_barrio` | VARCHAR(100) | SÍ | Requerido si `medio_respuesta = 'A'` |
| `resp_municipio` | VARCHAR(100) | SÍ | Requerido si `medio_respuesta = 'A'` |
| `resp_celular` | VARCHAR(30) | SÍ | Requerido si `medio_respuesta = 'B'` |
| `resp_telefono_fijo` | VARCHAR(30) | SÍ | Opcional si `medio_respuesta = 'B'` |
| `resp_correo` | VARCHAR(150) | SÍ | Requerido si `medio_respuesta = 'C'` |
| **Control** | | | |
| `estado` | VARCHAR(30) | NO | `radicado`, `revisado`, `en proceso`, `solucionado` |
| `fecha_actualizacion` | TIMESTAMP | NO | Actualizado por trigger en cada cambio |

---

## 3. Tabla: `pqrsf_adjuntos`
Archivos adjuntos cargados como soporte (hasta 3 archivos, máx 2MB por archivo, formatos Word, PDF, JPG).

| Campo | Tipo | Nulo | Descripción |
|---|---|---|---|
| `id` | BIGSERIAL | NO | Clave primaria |
| `pqrsf_id` | BIGINT | NO | FK a `pqrsf.id` (ON DELETE CASCADE) |
| `nombre_original` | VARCHAR(255) | NO | Nombre con el que el usuario cargó el archivo |
| `nombre_almacenado` | VARCHAR(255) | NO | Nombre único generado en el servidor con UUID/timestamp |
| `ruta_almacenamiento` | VARCHAR(500) | NO | Ruta en el sistema de archivos del servidor |
| `tipo_mime` | VARCHAR(120) | NO | MIME type (`application/pdf`, `image/jpeg`, etc.) |
| `tamano_bytes` | BIGINT | NO | Peso del archivo en bytes |
| `subido_el` | TIMESTAMP | NO | Fecha y hora de carga |

---

## 4. Tabla: `traza_pqrsf`
Historial de estados y cambios de gestión de la solicitud (Tracking del SIAU).

| Campo | Tipo | Nulo | Descripción |
|---|---|---|---|
| `id` | BIGSERIAL | NO | Clave primaria |
| `pqrsf_id` | BIGINT | NO | FK a `pqrsf.id` |
| `estado_anterior` | VARCHAR(30) | SÍ | Estado previo (`NULL` al momento de radicación) |
| `estado_nuevo` | VARCHAR(30) | NO | Nuevo estado asignado (`radicado`, `revisado`, `en proceso`, `solucionado`) |
| `observacion` | TEXT | SÍ | Justificación, notas internas o comentarios de la Jefe SIAU |
| `usuario_responsable` | VARCHAR(150) | NO | Usuario que ejecutó la acción (`siau@clinicalhouse.co` o `SISTEMA_WEB`) |
| `fecha_registro` | TIMESTAMP | NO | Marca de tiempo exacta del cambio |

---

## 5. Tabla: `auditoria_pqrsf`
Bitácora inmutable de auditoría para seguridad, control interno y cumplimiento normativo.

| Campo | Tipo | Nulo | Descripción |
|---|---|---|---|
| `id` | BIGSERIAL | NO | Clave primaria |
| `entidad` | VARCHAR(50) | NO | Entidad afectada (`PQRSF`, `USUARIO`, `ADJUNTO`, `SISTEMA`) |
| `entidad_id` | VARCHAR(50) | SÍ | Consecutivo o ID del registro |
| `accion` | VARCHAR(50) | NO | `RADICACION`, `LOGIN_EXITOSO`, `LOGIN_FALLIDO`, `CAMBIO_ESTADO`, `CONSULTA_DETALLE` |
| `usuario` | VARCHAR(150) | NO | Correo o identificador de quien realiza la acción |
| `ip_origen` | VARCHAR(45) | SÍ | Dirección IP del cliente |
| `navegador_agente` | VARCHAR(255) | SÍ | User-Agent del navegador |
| `detalles` | JSONB | SÍ | Objeto JSON con snapshot estructurado del evento |
| `fecha_evento` | TIMESTAMP | NO | Momento exacto del registro |
