--liquibase formatted sql

--changeset david:20250831-001
CREATE TABLE USERS (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL
);

--changeset david:20250831-002
CREATE TABLE AUDIT_LOG (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    action VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id BIGINT NOT NULL,
    user_id BIGINT,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    details TEXT,
    FOREIGN KEY (user_id) REFERENCES USERS(id)
);

--changeset david:20250831-003
ALTER TABLE CARDS ADD COLUMN assigned_user_id BIGINT;

--changeset david:20250831-004
ALTER TABLE CARDS ADD CONSTRAINT fk_cards_assigned_user
FOREIGN KEY (assigned_user_id) REFERENCES USERS(id);

--changeset david:20250831-005
INSERT INTO USERS (name, email) VALUES
('Admin', 'admin@board.com'),
('João Silva', 'joao@board.com'),
('Maria Santos', 'maria@board.com'),
('Pedro Costa', 'pedro@board.com');
