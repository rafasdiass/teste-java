-- Script para criar tabelas no banco H2 em memória
CREATE TABLE IF NOT EXISTS marcas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    codigo_fipe VARCHAR(10) NOT NULL,
    nome VARCHAR(255) NOT NULL,
    tipo_veiculo VARCHAR(50) NOT NULL,
    data_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS modelos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    codigo_fipe VARCHAR(10) NOT NULL,
    nome VARCHAR(255) NOT NULL,
    marca_id BIGINT NOT NULL,
    data_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (marca_id) REFERENCES marcas(id)
);

-- Inserir alguns dados de exemplo
INSERT INTO marcas (codigo_fipe, nome, tipo_veiculo) VALUES 
('001', 'Volkswagen', 'carros'),
('002', 'Ford', 'carros'),
('003', 'Chevrolet', 'carros');

INSERT INTO modelos (codigo_fipe, nome, marca_id) VALUES 
('001001', 'Gol', 1),
('001002', 'Polo', 1),
('002001', 'Fiesta', 2),
('002002', 'Focus', 2),
('003001', 'Onix', 3),
('003002', 'Cruze', 3);