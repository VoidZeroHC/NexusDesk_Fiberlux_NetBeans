-- NexusDesk Fiberlux: modelo físico MySQL 8 (InnoDB, utf8mb4)
-- Ejecutar con usuario DBA: mysql -u root -p < db/mysql_schema.sql
CREATE DATABASE IF NOT EXISTS tickets_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE tickets_db;
CREATE TABLE IF NOT EXISTS usuario (
 id_usuario INT PRIMARY KEY, nombre VARCHAR(150) NOT NULL, correo VARCHAR(150) NOT NULL UNIQUE,
 password_hash VARCHAR(255) NOT NULL, rol ENUM('CLIENTE','TECNICO','ADMINISTRADOR') NOT NULL,
 empresa VARCHAR(200) NOT NULL DEFAULT '', especialidad VARCHAR(120) NOT NULL DEFAULT '',
 activo BOOLEAN NOT NULL DEFAULT TRUE,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 INDEX idx_usuario_rol(rol)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS ticket (
 id_ticket INT PRIMARY KEY, codigo VARCHAR(20) NOT NULL UNIQUE,
 id_cliente INT NOT NULL, id_tecnico INT NULL,
 categoria VARCHAR(100) NOT NULL, titulo VARCHAR(150) NOT NULL,
 descripcion TEXT NOT NULL, prioridad ENUM('P1','P2','P3','P4') NOT NULL,
 estado ENUM('ABIERTO','ASIGNADO','EN_PROCESO','RESUELTO','CERRADO','ESCALADO') NOT NULL,
 enlace VARCHAR(100) NOT NULL DEFAULT '', servicio VARCHAR(150) NOT NULL DEFAULT '', sede VARCHAR(150) NOT NULL DEFAULT '',
 fecha_creacion DATETIME NOT NULL, fecha_actualizacion DATETIME NOT NULL,
 fecha_resolucion DATETIME NULL, fecha_cierre DATETIME NULL, conformidad BOOLEAN NOT NULL DEFAULT FALSE,
 FOREIGN KEY (id_cliente) REFERENCES usuario(id_usuario) ON DELETE RESTRICT,
 FOREIGN KEY (id_tecnico) REFERENCES usuario(id_usuario) ON DELETE SET NULL,
 INDEX idx_ticket_estado (estado), INDEX idx_ticket_cliente (id_cliente),
 INDEX idx_ticket_tecnico (id_tecnico), INDEX idx_ticket_fecha (fecha_creacion),
 INDEX idx_ticket_prioridad (prioridad), INDEX idx_ticket_categoria (categoria)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS comentario (
 id_comentario INT PRIMARY KEY,id_ticket INT NOT NULL,id_usuario INT NOT NULL,
 texto TEXT NOT NULL,interno BOOLEAN NOT NULL DEFAULT FALSE,fecha DATETIME NOT NULL,
 FOREIGN KEY(id_ticket) REFERENCES ticket(id_ticket) ON DELETE CASCADE,
 FOREIGN KEY(id_usuario) REFERENCES usuario(id_usuario) ON DELETE RESTRICT,
 INDEX idx_comentario_ticket(id_ticket)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS auditoria (
 id_auditoria INT PRIMARY KEY,id_ticket INT NOT NULL,id_usuario INT NOT NULL,
 accion VARCHAR(80) NOT NULL, detalle VARCHAR(500),fecha DATETIME NOT NULL,
 FOREIGN KEY(id_ticket) REFERENCES ticket(id_ticket) ON DELETE CASCADE,
 FOREIGN KEY(id_usuario) REFERENCES usuario(id_usuario) ON DELETE RESTRICT,
 INDEX idx_auditoria_ticket(id_ticket)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS adjunto (
 id_adjunto INT PRIMARY KEY,id_ticket INT NOT NULL,id_usuario INT NOT NULL,
 nombre VARCHAR(100) NOT NULL,mime VARCHAR(120) NOT NULL,archivo VARCHAR(100) NOT NULL,
 tamano BIGINT NOT NULL,fecha DATETIME NOT NULL,
 FOREIGN KEY(id_ticket) REFERENCES ticket(id_ticket) ON DELETE CASCADE,
 FOREIGN KEY(id_usuario) REFERENCES usuario(id_usuario) ON DELETE RESTRICT,
 INDEX idx_adjunto_ticket(id_ticket)
) ENGINE=InnoDB;
-- Ejemplo de privilegio mínimo (ejecutar como DBA después de crear credenciales seguras):
-- GRANT SELECT, INSERT, UPDATE, DELETE ON tickets_db.* TO 'app_fiberlux'@'localhost';
