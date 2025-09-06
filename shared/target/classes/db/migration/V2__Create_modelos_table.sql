-- Criação da tabela de modelos
CREATE TABLE modelos (
    id BIGSERIAL PRIMARY KEY,
    codigo_fipe VARCHAR(20) NOT NULL,
    nome VARCHAR(500) NOT NULL,
    observacoes TEXT,
    data_criacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao TIMESTAMP,
    marca_id BIGINT NOT NULL,
    CONSTRAINT fk_modelos_marca FOREIGN KEY (marca_id) REFERENCES marcas(id) ON DELETE CASCADE
);

-- Índices para otimizar consultas
CREATE INDEX idx_modelos_codigo_fipe ON modelos(codigo_fipe);
CREATE INDEX idx_modelos_marca_id ON modelos(marca_id);
CREATE INDEX idx_modelos_nome ON modelos(nome);
CREATE INDEX idx_modelos_observacoes ON modelos(observacoes) WHERE observacoes IS NOT NULL;

-- Índice composto para consultas por marca e código
CREATE INDEX idx_modelos_marca_codigo ON modelos(marca_id, codigo_fipe);

-- Comentários para documentação
COMMENT ON TABLE modelos IS 'Tabela que armazena os modelos de veículos obtidos da API FIPE';
COMMENT ON COLUMN modelos.codigo_fipe IS 'Código do modelo na API FIPE';
COMMENT ON COLUMN modelos.nome IS 'Nome do modelo do veículo';
COMMENT ON COLUMN modelos.observacoes IS 'Observações adicionais sobre o modelo (editável pelo usuário)';
COMMENT ON COLUMN modelos.data_criacao IS 'Data de criação do registro';
COMMENT ON COLUMN modelos.data_atualizacao IS 'Data da última atualização do registro';
COMMENT ON COLUMN modelos.marca_id IS 'Referência para a marca do veículo';