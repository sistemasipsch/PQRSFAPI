@echo off
rem La clave de PostgreSQL se toma de la variable PGPASSWORD o se pide al ejecutar.
if "%PGPASSWORD%"=="" set /p PGPASSWORD=Clave del usuario postgres: 
"C:\Program Files\PostgreSQL\17\bin\psql.exe" -U postgres -d pqrsf-siau -f "%~dp0update_responsables.sql"
