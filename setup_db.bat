@echo off
REM Run this script as Administrator after PostgreSQL 17 installs
REM It will create the dems_db database and dems_user

SET PGPATH=C:\PostgreSQL\17\bin
SET PGPASSWORD=postgres

echo Creating dems_user...
"%PGPATH%\psql.exe" -U postgres -c "CREATE USER dems_user WITH PASSWORD 'StrongPassword123!';" 2>nul || echo User already exists

echo Creating dems_db database...
"%PGPATH%\psql.exe" -U postgres -c "CREATE DATABASE dems_db OWNER dems_user;" 2>nul || echo DB already exists

echo Granting privileges...
"%PGPATH%\psql.exe" -U postgres -d dems_db -c "GRANT ALL ON SCHEMA public TO dems_user;"
"%PGPATH%\psql.exe" -U postgres -d dems_db -c "ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO dems_user;"
"%PGPATH%\psql.exe" -U postgres -d dems_db -c "ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO dems_user;"

echo.
echo Done! Database setup complete.
echo You can now run: java -jar backend\target\dems-backend-1.0.0.jar --spring.profiles.active=local
pause
