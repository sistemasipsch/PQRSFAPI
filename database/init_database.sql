-- ============================================================================
-- BASE DE DATOS: PQRSF SIAU - IPS CLINICAL HOUSE
-- MOTOR: PostgreSQL 14+ / 16+ / 17
-- DOMINIO: clinicalhouse.co
-- CORREO DESTINO JEFE SIAU: siau@clinicalhouse.co
-- FORMATO CONSECUTIVO: pqrs-ch-0001 en adelante
-- ============================================================================

-- Crear base de datos (ejecutar por separado si es necesario)
-- CREATE DATABASE pqrsf_clinicalhouse WITH ENCODING 'UTF8';

-- Extensiones útiles
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ----------------------------------------------------------------------------
-- 1. SECUENCIA Y FUNCIÓN PARA CONSECUTIVO AUTOMÁTICO
-- Inicia en 1: pqrs-ch-0001
-- ----------------------------------------------------------------------------
CREATE SEQUENCE IF NOT EXISTS seq_consecutivo_pqrs
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE OR REPLACE FUNCTION fn_generar_consecutivo_pqrs()
RETURNS TEXT AS $$
DECLARE
    siguiente_valor BIGINT;
    consecutivo_formateado TEXT;
BEGIN
    siguiente_valor := nextval('seq_consecutivo_pqrs');
    consecutivo_formateado := 'pqrs-ch-' || LPAD(siguiente_valor::TEXT, 4, '0');
    RETURN consecutivo_formateado;
END;
$$ LANGUAGE plpgsql;

-- ----------------------------------------------------------------------------
-- 2. TABLA DE USUARIOS (AUTENTICACIÓN DE LA JEFE SIAU)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS usuarios (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(120) NOT NULL,
    correo VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    rol VARCHAR(40) NOT NULL DEFAULT 'JEFE_SIAU',
    cargo VARCHAR(100) DEFAULT 'Jefe de SIAU',
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    ultimo_acceso TIMESTAMP NULL,
    creado_el TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_el TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------------------------------------------------------
-- 3. TABLA PRINCIPAL DE PQRSF
-- Cumple con todos los ítems obligatorios del formulario oficial
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS pqrsf (
    id BIGSERIAL PRIMARY KEY,
    consecutivo VARCHAR(30) NOT NULL UNIQUE DEFAULT fn_generar_consecutivo_pqrs(),
    fecha_recepcion DATE NOT NULL,
    fecha_radicacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- DATOS DEL FAMILIAR, CUIDADOR Y/O ACOMPAÑANTE (Obligatorios según formulario)
    fam_primer_apellido VARCHAR(80) NOT NULL,
    fam_segundo_apellido VARCHAR(80) NOT NULL,
    fam_primer_nombre VARCHAR(80) NOT NULL,
    fam_segundo_nombre VARCHAR(80),
    fam_tipo_documento VARCHAR(50) NOT NULL,
    fam_numero_documento VARCHAR(30) NOT NULL,
    fam_email VARCHAR(150) NOT NULL,
    fam_direccion VARCHAR(200) NOT NULL,
    fam_telefono VARCHAR(30) NOT NULL,

    -- DATOS DEL PACIENTE (Obligatorios según formulario)
    pac_primer_apellido VARCHAR(80) NOT NULL,
    pac_segundo_apellido VARCHAR(80) NOT NULL,
    pac_primer_nombre VARCHAR(80) NOT NULL,
    pac_segundo_nombre VARCHAR(80),
    pac_tipo_documento VARCHAR(50) NOT NULL,
    pac_numero_documento VARCHAR(30) NOT NULL,
    pac_email VARCHAR(150) NOT NULL,
    pac_direccion VARCHAR(200) NOT NULL,
    pac_telefono VARCHAR(30) NOT NULL,
    pac_regimen VARCHAR(50) NOT NULL,
    pac_entidad VARCHAR(120) NOT NULL,
    pac_poblacion VARCHAR(50) NOT NULL,

    -- DETALLE DE LA SOLICITUD
    -- '(P) Petición', '(Q) Queja', '(R) Reclamo', '(S) Sugerencia', '(F) Felicitación'
    tipo_solicitud VARCHAR(30) NOT NULL,
    descripcion TEXT NOT NULL,

    -- FORMA EN LA QUE DESEA RECIBIR LA RESPUESTA (Exclusiva A, B, o C)
    medio_respuesta VARCHAR(2) NOT NULL CHECK (medio_respuesta IN ('A', 'B', 'C')),
    
    -- Opción A: Domicilio
    resp_direccion VARCHAR(200),
    resp_barrio VARCHAR(100),
    resp_municipio VARCHAR(100),

    -- Opción B: Personalmente / Vía Telefónica
    resp_celular VARCHAR(30),
    resp_telefono_fijo VARCHAR(30),

    -- Opción C: Correo Electrónico
    resp_correo VARCHAR(150),

    -- ESTADO DE GESTIÓN (radicado -> revisado -> en proceso -> solucionado)
    estado VARCHAR(30) NOT NULL DEFAULT 'radicado' 
        CHECK (estado IN ('radicado', 'revisado', 'en proceso', 'solucionado')),

    fecha_actualizacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Índices para búsquedas frecuentes y optimización
CREATE INDEX IF NOT EXISTS idx_pqrsf_consecutivo ON pqrsf(consecutivo);
CREATE INDEX IF NOT EXISTS idx_pqrsf_estado ON pqrsf(estado);
CREATE INDEX IF NOT EXISTS idx_pqrsf_fecha_rad ON pqrsf(fecha_radicacion DESC);
CREATE INDEX IF NOT EXISTS idx_pqrsf_pac_doc ON pqrsf(pac_numero_documento);
CREATE INDEX IF NOT EXISTS idx_pqrsf_fam_email ON pqrsf(fam_email);

-- ----------------------------------------------------------------------------
-- 4. TABLA DE ADJUNTOS (Archivos de soporte: Word, PDF, JPG)
-- Máx 3 archivos, máx 2MB por archivo
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS pqrsf_adjuntos (
    id BIGSERIAL PRIMARY KEY,
    pqrsf_id BIGINT NOT NULL REFERENCES pqrsf(id) ON DELETE CASCADE,
    nombre_original VARCHAR(255) NOT NULL,
    nombre_almacenado VARCHAR(255) NOT NULL,
    ruta_almacenamiento VARCHAR(500) NOT NULL,
    tipo_mime VARCHAR(120) NOT NULL,
    tamano_bytes BIGINT NOT NULL,
    subido_el TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_adjuntos_pqrsf_id ON pqrsf_adjuntos(pqrsf_id);

-- ----------------------------------------------------------------------------
-- 5. TABLA DE TRAZA (HISTORIAL DE ESTADOS Y GESTIÓN)
-- Registra cada cambio de estado, observación y responsable
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS traza_pqrsf (
    id BIGSERIAL PRIMARY KEY,
    pqrsf_id BIGINT NOT NULL REFERENCES pqrsf(id) ON DELETE CASCADE,
    estado_anterior VARCHAR(30) NULL,
    estado_nuevo VARCHAR(30) NOT NULL,
    observacion TEXT,
    usuario_responsable VARCHAR(150) NOT NULL,
    fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_traza_pqrsf_id ON traza_pqrsf(pqrsf_id);
CREATE INDEX IF NOT EXISTS idx_traza_fecha ON traza_pqrsf(fecha_registro DESC);

-- ----------------------------------------------------------------------------
-- 6. TABLA DE AUDITORÍA GENERAL DEL SISTEMA
-- Inmutable, registra todos los eventos clave
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS auditoria_pqrsf (
    id BIGSERIAL PRIMARY KEY,
    entidad VARCHAR(50) NOT NULL,         -- 'PQRSF', 'USUARIO', 'ADJUNTO'
    entidad_id VARCHAR(50) NULL,          -- ID o consecutivo
    accion VARCHAR(50) NOT NULL,          -- 'RADICACION', 'LOGIN_EXITOSO', 'LOGIN_FALLIDO', 'CAMBIO_ESTADO', 'DESCARGA_ADJUNTO'
    usuario VARCHAR(150) NOT NULL,        -- 'SISTEMA_WEB', 'siau@clinicalhouse.co', etc.
    ip_origen VARCHAR(45) NULL,
    navegador_agente VARCHAR(255) NULL,
    detalles JSONB NULL,                  -- Snapshot o resumen en JSON de los cambios
    fecha_evento TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_auditoria_fecha ON auditoria_pqrsf(fecha_evento DESC);
CREATE INDEX IF NOT EXISTS idx_auditoria_accion ON auditoria_pqrsf(accion);
CREATE INDEX IF NOT EXISTS idx_auditoria_usuario ON auditoria_pqrsf(usuario);

-- ----------------------------------------------------------------------------
-- 7. TRIGGER PARA ACTUALIZAR AUTOMÁTICAMENTE fecha_actualizacion
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_actualizar_timestamp_pqrsf()
RETURNS TRIGGER AS $$
BEGIN
    NEW.fecha_actualizacion = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_actualizar_pqrsf ON pqrsf;
CREATE TRIGGER trg_actualizar_pqrsf
    BEFORE UPDATE ON pqrsf
    FOR EACH ROW
    EXECUTE FUNCTION fn_actualizar_timestamp_pqrsf();

-- ----------------------------------------------------------------------------
-- 8. DATOS INICIALES SEMILLA (SEED DATA)
-- Las cuentas iniciales (siau@clinicalhouse.co y admin@clinicalhouse.co) las crea el backend al
-- arrancar, con la contraseña de ADMIN_PASSWORD_INICIAL / JEFE_PASSWORD_INICIAL o una aleatoria
-- que se muestra una sola vez en el registro. No hay contraseñas fijas en los scripts.
-- ----------------------------------------------------------------------------

-- Registro de auditoría de inicialización
INSERT INTO auditoria_pqrsf (entidad, entidad_id, accion, usuario, detalles)
VALUES (
    'SISTEMA',
    'INIT-001',
    'CREACION_ESQUEMA',
    'SYSTEM_INSTALL',
    '{"mensaje": "Esquema PQRSF SIAU Clinical House inicializado correctamente", "consecutivo_inicial": "pqrs-ch-0001"}'::jsonb
);
