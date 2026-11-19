-- Migración V2: Mejoras para PQRSF SIAU Clinical House

-- 1. Agregar columna responsable_asignado
ALTER TABLE pqrsf ADD COLUMN IF NOT EXISTS responsable_asignado VARCHAR(150) DEFAULT 'Sin asignar';

-- 2. Agregar campos para recuperación de contraseña
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS codigo_recuperacion VARCHAR(10);
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS codigo_expiracion TIMESTAMP;

-- 3. Actualizar función de consecutivo a mayúsculas: PQRSF-CH-0001
CREATE OR REPLACE FUNCTION fn_generar_consecutivo_pqrs()
RETURNS TEXT AS $$
DECLARE
    siguiente_valor BIGINT;
    consecutivo_formateado TEXT;
BEGIN
    siguiente_valor := nextval('seq_consecutivo_pqrs');
    consecutivo_formateado := 'PQRSF-CH-' || LPAD(siguiente_valor::TEXT, 4, '0');
    RETURN consecutivo_formateado;
END;
$$ LANGUAGE plpgsql;

-- 4. Actualizar consecutivos existentes a mayúsculas
UPDATE pqrsf SET consecutivo = REPLACE(UPPER(consecutivo), 'PQRS-CH', 'PQRSF-CH') WHERE consecutivo ILIKE 'pqrs%';

-- 5. El usuario Super Administrador lo crea el backend al arrancar (sin contraseña fija).
