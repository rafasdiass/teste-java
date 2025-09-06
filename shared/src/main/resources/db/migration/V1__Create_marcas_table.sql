-- Criação da tabela de marcas
CREATE TABLE marcas (
    id BIGSERIAL PRIMARY KEY,
    codigo_fipe VARCHAR(10) NOT NULL UNIQUE,
    nome VARCHAR(255) NOT NULL,
    tipo_veiculo VARCHAR(50),
    data_criacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao TIMESTAMP
);

-- Índices para otimizar consultas
CREATE INDEX idx_marcas_codigo_fipe ON marcas(codigo_fipe);
CREATE INDEX idx_marcas_tipo_veiculo ON marcas(tipo_veiculo);
CREATE INDEX idx_marcas_nome ON marcas(nome);

-- Comentários para documentação
COMMENT ON TABLE marcas IS 'Tabela que armazena as marcas de veículos obtidas da API FIPE';
COMMENT ON COLUMN marcas.codigo_fipe IS 'Código único da marca na API FIPE';
COMMENT ON COLUMN marcas.nome IS 'Nome da marca do veículo';
COMMENT ON COLUMN marcas.tipo_veiculo IS 'Tipo do veículo: carros, motos, caminhoes';
COMMENT ON COLUMN marcas.data_criacao IS 'Data de criação do registro';
COMMENT ON COLUMN marcas.data_atualizacao IS 'Data da última atualização do registro';