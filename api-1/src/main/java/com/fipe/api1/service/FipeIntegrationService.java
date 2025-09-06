package com.fipe.api1.service;

import com.fipe.api1.client.FipeClient;
import com.fipe.api1.messaging.MarcaMessageProducer;
import com.fipe.shared.dto.MarcaDTO;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Serviço responsável pela integração com a API FIPE
 * e envio de mensagens para processamento assíncrono
 */
@ApplicationScoped
public class FipeIntegrationService {

    private static final Logger LOG = Logger.getLogger(FipeIntegrationService.class);

    @Inject
    @RestClient
    FipeClient fipeClient;
    
    @Inject
    MarcaMessageProducer marcaMessageProducer;



    /**
     * Executa a carga inicial de todas as marcas de veículos
     * Busca marcas de carros, motos e caminhões e envia para processamento
     */
    public CompletionStage<String> executarCargaInicial() {
        LOG.info("Iniciando carga inicial de dados FIPE");
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                int totalMarcasEnviadas = 0;
                
                // Buscar e processar marcas de carros
                List<MarcaDTO> marcasCarros = fipeClient.getMarcasCarros();
                enviarMarcasParaFila(marcasCarros, "carros");
                totalMarcasEnviadas += marcasCarros.size();
                LOG.infof("Enviadas %d marcas de carros para processamento", marcasCarros.size());
                
                // Buscar e processar marcas de motos
                List<MarcaDTO> marcasMotos = fipeClient.getMarcasMotos();
                enviarMarcasParaFila(marcasMotos, "motos");
                totalMarcasEnviadas += marcasMotos.size();
                LOG.infof("Enviadas %d marcas de motos para processamento", marcasMotos.size());
                
                // Buscar e processar marcas de caminhões
                List<MarcaDTO> marcasCaminhoes = fipeClient.getMarcasCaminhoes();
                enviarMarcasParaFila(marcasCaminhoes, "caminhoes");
                totalMarcasEnviadas += marcasCaminhoes.size();
                LOG.infof("Enviadas %d marcas de caminhões para processamento", marcasCaminhoes.size());
                
                String resultado = String.format(
                    "Carga inicial concluída com sucesso! Total de %d marcas enviadas para processamento (Carros: %d, Motos: %d, Caminhões: %d)",
                    totalMarcasEnviadas, marcasCarros.size(), marcasMotos.size(), marcasCaminhoes.size()
                );
                
                LOG.info(resultado);
                return resultado;
                
            } catch (Exception e) {
                LOG.error("Erro durante a carga inicial de dados FIPE", e);
                throw new RuntimeException("Falha na carga inicial: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Busca apenas as marcas de um tipo específico de veículo
     */
    public List<MarcaDTO> buscarMarcasPorTipo(String tipoVeiculo) {
        LOG.infof("Buscando marcas para tipo de veículo: %s", tipoVeiculo);
        
        try {
            return switch (tipoVeiculo.toLowerCase()) {
                case "carros" -> fipeClient.getMarcasCarros();
                case "motos" -> fipeClient.getMarcasMotos();
                case "caminhoes" -> fipeClient.getMarcasCaminhoes();
                default -> {
                    LOG.warnf("Tipo de veículo não reconhecido: %s", tipoVeiculo);
                    yield new ArrayList<>();
                }
            };
        } catch (Exception e) {
            LOG.errorf("Erro ao buscar marcas para tipo %s: %s", tipoVeiculo, e.getMessage());
            throw new RuntimeException("Falha ao buscar marcas: " + e.getMessage(), e);
        }
    }

    /**
     * Envia marcas para a fila de processamento
     * 1.3 - Implementa o envio para fila
     */
    public CompletionStage<Void> enviarMarcasParaFila(List<MarcaDTO> marcas, String tipoVeiculo) {
        LOG.infof("Enviando %d marcas do tipo %s para a fila", marcas.size(), tipoVeiculo);
        
        return marcaMessageProducer.enviarMarcas(marcas, tipoVeiculo)
            .thenRun(() -> {
                LOG.infof("Todas as %d marcas do tipo %s foram enviadas para a fila com sucesso", 
                         marcas.size(), tipoVeiculo);
            })
            .exceptionally(throwable -> {
                LOG.errorf("Erro ao enviar marcas do tipo %s para a fila: %s", 
                          tipoVeiculo, throwable.getMessage());
                throw new RuntimeException("Falha no envio das marcas para fila", throwable);
            });
    }
    
    /**
     * Envia uma marca individual para a fila de processamento
     */
    public CompletionStage<Void> enviarMarcaIndividual(String codigoMarca, String nomeMarca, String tipoVeiculo) {
        LOG.infof("Enviando marca individual para a fila: %s - %s (%s)", codigoMarca, nomeMarca, tipoVeiculo);
        
        return marcaMessageProducer.enviarMarca(codigoMarca, nomeMarca, tipoVeiculo)
            .thenRun(() -> {
                LOG.infof("Marca individual %s enviada para a fila com sucesso", nomeMarca);
            })
            .exceptionally(throwable -> {
                LOG.errorf("Erro ao enviar marca individual %s para a fila: %s", 
                          nomeMarca, throwable.getMessage());
                throw new RuntimeException("Falha no envio da marca individual para fila", throwable);
            });
    }

    /**
     * Verifica se a API FIPE está disponível
     */
    public boolean verificarDisponibilidadeApi() {
        try {
            List<MarcaDTO> marcas = fipeClient.getMarcasCarros();
            return marcas != null && !marcas.isEmpty();
        } catch (Exception e) {
            LOG.errorf("API FIPE indisponível: %s", e.getMessage());
            return false;
        }
    }
}