-- Limpieza de usuarios de prueba y eliminación de títulos honoríficos (Lic., Dra.)
DELETE FROM usuarios WHERE correo LIKE '%@clinicalhouse.test';
UPDATE usuarios SET nombre = 'Sandra Ortiz' WHERE correo = 'sandra.ortiz@clinicalhouse.co';
UPDATE usuarios SET nombre = 'Claudia Mendez' WHERE correo = 'claudia.mendez@clinicalhouse.co';
UPDATE usuarios SET nombre = 'Jefe SIAU Clinical House' WHERE correo = 'siau@clinicalhouse.co';
SELECT id, nombre, correo, rol, cargo, activo FROM usuarios;
