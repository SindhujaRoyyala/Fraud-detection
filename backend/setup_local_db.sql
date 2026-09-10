-- Run this script in psql as the postgres superuser after installing PostgreSQL 17:
-- psql -U postgres -f setup_local_db.sql
--
-- Or run each command manually:
--   psql -U postgres
--   Then paste the commands below.

-- Create the application user
CREATE USER dems_user WITH PASSWORD 'StrongPassword123!';

-- Create the database owned by the app user
CREATE DATABASE dems_db OWNER dems_user;

-- Grant all privileges
GRANT ALL PRIVILEGES ON DATABASE dems_db TO dems_user;

-- Connect to the database and grant schema privileges
\c dems_db

GRANT ALL ON SCHEMA public TO dems_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO dems_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO dems_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO dems_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO dems_user;

\echo 'Database setup complete! dems_db created with user dems_user.'
