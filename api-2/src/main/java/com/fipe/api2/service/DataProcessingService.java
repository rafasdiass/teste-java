package com.fipe.api2.service;

import com.fipe.api2.client.FipeClient;
import com.fipe.api2.repository.MarcaRepository;
import com.fipe.api2.repository.ModeloRepository;
import com.fipe.shared.dto.ModeloDTO;
import com.fipe.shared.entity.Marca;
import com.fipe.shared.entity.Modelo;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.util.List;

/**
 * Serviço responsável pelo processamento e persistência dos dados FIPE
 * Processa as mensagens da fila e salva os dados no banco
 */
@ApplicationScoped
public class DataProcessingService {

    private static final Logger LOG = Logger.getLogger(DataProcessingService.class);

    @Inject
    @RestClient
    FipeClient fipeClient;

    @Inject
    MarcaRepository marcaRepository;

    @Inject
    ModeloRepository modeloRepository;

    @ConfigProperty(name = "fipe.processing.delay-between-requests", defaultValue = "100")
    int delayBetweenRequests;

    @ConfigProperty(name = "fipe.processing.max-retries", defaultValue = "3")
    int maxRetries;

    @ConfigProperty(name = "fipe.processing.retry-delay", defaultValue = "5000")
    int retryDelay;

    /**
     * Processa uma marca: salva a marca e busca/salva todos os seus modelos
     */
    @Transactional
    public void processarMarca(String codigoMarca, String nomeMarca, String tipoVeiculo) {
        LOG.infof("Iniciando processamento da marca: %s (%s) - Tipo: %s", nomeMarca, codigoMarca, tipoVeiculo);
        
        try {
            // 1. Salvar ou buscar a marca
            Marca marca = salvarMarca(codigoMarca, nomeMarca, tipoVeiculo);
            
            // 2. Buscar modelos na API FIPE
            List<ModeloDTO> modelos = buscarModelosNaApiFipe(codigoMarca, tipoVeiculo);
            
            if (modelos == null || modelos.isEmpty()) {
                LOG.warnf("Nenhum modelo encontrado para a marca %s (%s)", nomeMarca, codigoMarca);
                return;
            }
            
            // 3. Salvar modelos no banco
            int modelosSalvos = salvarModelos(modelos, marca);
            
            LOG.infof("Processamento da marca %s concluído. %d modelos processados", nomeMarca, modelosSalvos);
            
        } catch (Exception e) {
            LOG.errorf("Erro ao processar marca %s (%s): %s", nomeMarca, codigoMarca, e.getMessage());
            throw new RuntimeException("Falha no processamento da marca: " + nomeMarca, e);
        }
    }

    /**
     * Salva uma marca no banco de dados (ou retorna existente)
     */
    private Marca salvarMarca(String codigoFipe, String nome, String tipoVeiculo) {
        // Verificar se a marca já existe
        Marca marcaExistente = marcaRepository.findByCodigoFipe(codigoFipe);
        
        if (marcaExistente != null) {
            LOG.debugf("Marca já existe no banco: %s", nome);
            return marcaExistente;
        }
        
        // Criar nova marca
        Marca novaMarca = new Marca(codigoFipe, nome, tipoVeiculo);
        marcaRepository.persist(novaMarca);
        
        LOG.infof("Nova marca salva: %s (ID: %d)", nome, novaMarca.id);
        return novaMarca;
    }

    /**
     * Busca modelos na API FIPE com retry automático
     */
    private List<ModeloDTO> buscarModelosNaApiFipe(String codigoMarca, String tipoVeiculo) {
        int tentativas = 0;
        Exception ultimaExcecao = null;
        
        while (tentativas < maxRetries) {
            try {
                // Delay entre requisições para evitar rate limiting
                if (tentativas > 0) {
                    Thread.sleep(retryDelay);
                }
                
                FipeClient.FipeModelosResponse response = switch (tipoVeiculo.toLowerCase()) {
                    case "carros" -> fipeClient.getModelosCarros(codigoMarca);
                    case "motos" -> fipeClient.getModelosMotos(codigoMarca);
                    case "caminhoes" -> fipeClient.getModelosCaminhoes(codigoMarca);
                    default -> throw new IllegalArgumentException("Tipo de veículo inválido: " + tipoVeiculo);
                };
                
                if (response != null && response.modelos != null) {
                    LOG.debugf("Encontrados %d modelos para marca %s", response.modelos.size(), codigoMarca);
                    return response.modelos;
                }
                
                LOG.warnf("Resposta vazia da API FIPE para marca %s", codigoMarca);
                return List.of();
                
            } catch (Exception e) {
                ultimaExcecao = e;
                tentativas++;
                LOG.warnf("Tentativa %d/%d falhou ao buscar modelos para marca %s: %s", 
                         tentativas, maxRetries, codigoMarca, e.getMessage());
                
                if (tentativas < maxRetries) {
                    try {
                        Thread.sleep(retryDelay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrompido durante retry", ie);
                    }
                }
            }
        }
        
        throw new RuntimeException("Falha ao buscar modelos após " + maxRetries + " tentativas", ultimaExcecao);
    }

    /**
     * Salva uma lista de modelos no banco de dados
     */
    private int salvarModelos(List<ModeloDTO> modelos, Marca marca) {
        int contador = 0;
        
        for (ModeloDTO modeloDTO : modelos) {
            try {
                // Verificar se o modelo já existe
                Modelo modeloExistente = modeloRepository.findByCodigoFipe(modeloDTO.codigo);
                
                if (modeloExistente != null) {
                    LOG.debugf("Modelo já existe: %s", modeloDTO.nome);
                    continue;
                }
                
                // Criar novo modelo
                Modelo novoModelo = new Modelo(modeloDTO.codigo, modeloDTO.nome, marca);
                modeloRepository.persist(novoModelo);
                
                contador++;
                LOG.debugf("Modelo salvo: %s (ID: %d)", modeloDTO.nome, novoModelo.id);
                
                // Pequeno delay para evitar sobrecarga do banco
                if (delayBetweenRequests > 0) {
                    Thread.sleep(delayBetweenRequests);
                }
                
            } catch (Exception e) {
                LOG.errorf("Erro ao salvar modelo %s: %s", modeloDTO.nome, e.getMessage());
                // Continua processando outros modelos mesmo se um falhar
            }
        }
        
        return contador;
    }

    /**
     * Verifica se uma marca já foi processada (tem modelos salvos)
     * Temporariamente desabilitado devido a problemas com MarcaRepository
     */
    /*
    public boolean marcaJaProcessada(String codigoMarca) {
        Marca marca = marcaRepository.findByCodigoFipe(codigoMarca);
        
        if (marca == null) {
            return false;
        }
        
        long quantidadeModelos = modeloRepository.countByMarca(marca);
        return quantidadeModelos > 0;
    }
    */

    /**
     * Retorna estatísticas de processamento
     * Temporariamente desabilitado devido a problemas com MarcaRepository
     */
    /*
    public ProcessingStats getProcessingStats() {
        long totalMarcas = marcaRepository.count();
        long totalModelos = modeloRepository.count();
        long marcasCarros = marcaRepository.count("tipoVeiculo", "carros");
        long marcasMotos = marcaRepository.count("tipoVeiculo", "motos");
        long marcasCaminhoes = marcaRepository.count("tipoVeiculo", "caminhoes");
        
        return new ProcessingStats(totalMarcas, totalModelos, marcasCarros, marcasMotos, marcasCaminhoes);
    }
    */

    /**
     * Classe para estatísticas de processamento
     */
    public static class ProcessingStats {
        public long totalMarcas;
        public long totalModelos;
        public long marcasCarros;
        public long marcasMotos;
        public long marcasCaminhoes;
        
        public ProcessingStats() {}
        
        public ProcessingStats(long totalMarcas, long totalModelos, long marcasCarros, long marcasMotos, long marcasCaminhoes) {
            this.totalMarcas = totalMarcas;
            this.totalModelos = totalModelos;
            this.marcasCarros = marcasCarros;
            this.marcasMotos = marcasMotos;
            this.marcasCaminhoes = marcasCaminhoes;
        }
    }
}