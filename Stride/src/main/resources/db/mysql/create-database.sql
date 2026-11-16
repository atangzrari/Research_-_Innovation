-- Run once as a MySQL 8 administrator, then point stride.properties at the new database.
--   mysql -u root -p < create-database.sql
-- Stride creates its own tables (db/schema.sql) and sample data on first start.
CREATE DATABASE IF NOT EXISTS stride CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'stride'@'localhost' IDENTIFIED BY 'change-this-password';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES ON stride.* TO 'stride'@'localhost';
FLUSH PRIVILEGES;
