package com.fipe.api1.messaging;

import com.fipe.shared.dto.MarcaQueueMessage;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.logging.Logger;

import jakarta.inject.Inject;
import java.time.LocalDateTime;
import java.util.concurrent.CompletionStage;

/**
 * Producer responsável por enviar mensagens de marcas para a fila
 * Implementa o padrão de messaging assíncrono
 */
@ApplicationScoped
public class MarcaMessageProducer {

    private static final Logger LOG = Logger.getLogger(MarcaMessageProducer.class);

    @Inject
    @Channel("marcas-out")
    Emitter<MarcaQueueMessage> marcasEmitter;

    /**
     * Envia uma marca para a fila de processamento
     * 1.3 - Implementa o envio de marcas para fila
     */
    public CompletionStage<Void> enviarMarca(String codigoMarca, String nomeMarca, String tipoVeiculo) {
        try {
            // Validar parâmetros
            validarParametros(codigoMarca, nomeMarca, tipoVeiculo);
            
            // Criar mensagem
            MarcaQueueMessage mensagem = new MarcaQueueMessage(
                codigoMarca,
                nomeMarca,
                tipoVeiculo
            );
            
            LOG.infof("Enviando marca para fila: %s - %s (%s)", 
                     codigoMarca, nomeMarca, tipoVeiculo);
            
            // Enviar mensagem
            marcasEmitter.send(Message.of(mensagem));
            LOG.infof("Marca %s enviada para a fila", nomeMarca);
            
            return java.util.concurrent.CompletableFuture.completedStage(null);
                
        } catch (Exception e) {
            LOG.errorf("Erro ao preparar envio da marca %s: %s", nomeMarca, e.getMessage());
            return java.util.concurrent.CompletableFuture.failedStage(e);
        }
    }

    /**
     * Envia múltiplas marcas para a fila
     */
    public CompletionStage<Void> enviarMarcas(java.util.List<com.fipe.shared.dto.MarcaDTO> marcas, String tipoVeiculo) {
        if (marcas == null || marcas.isEmpty()) {
            LOG.warn("Lista de marcas vazia, nenhuma mensagem será enviada");
            return java.util.concurrent.CompletableFuture.completedStage(null);
        }
        
        LOG.infof("Enviando %d marcas do tipo %s para a fila", marcas.size(), tipoVeiculo);
        
        // Criar lista de CompletionStages para envio paralelo
        java.util.List<CompletionStage<Void>> envios = marcas.stream()
            .map(marca -> enviarMarca(marca.codigo, marca.nome, tipoVeiculo))
            .collect(java.util.stream.Collectors.toList());
        
        // Aguardar todos os envios completarem
        return java.util.concurrent.CompletableFuture.allOf(
            envios.toArray(new java.util.concurrent.CompletableFuture[0])
        ).thenRun(() -> {
            LOG.infof("Todas as %d marcas do tipo %s foram enviadas com sucesso", 
                     marcas.size(), tipoVeiculo);
        }).exceptionally(throwable -> {
            LOG.errorf("Erro ao enviar algumas marcas do tipo %s: %s", 
                      tipoVeiculo, throwable.getMessage());
            throw new RuntimeException("Falha no envio em lote de marcas", throwable);
        });
    }

    /**
     * Envia marca de forma síncrona (para casos específicos)
     */
    public void enviarMarcaSync(String codigoMarca, String nomeMarca, String tipoVeiculo) {
        try {
            validarParametros(codigoMarca, nomeMarca, tipoVeiculo);
            
            MarcaQueueMessage mensagem = new MarcaQueueMessage(
                codigoMarca,
                nomeMarca,
                tipoVeiculo
            );
            
            LOG.infof("Enviando marca sincronamente: %s - %s (%s)", 
                     codigoMarca, nomeMarca, tipoVeiculo);
            
            marcasEmitter.send(mensagem);
            
            LOG.infof("Marca %s enviada sincronamente com sucesso", nomeMarca);
            
        } catch (Exception e) {
            LOG.errorf("Erro no envio síncrono da marca %s: %s", nomeMarca, e.getMessage());
            throw new RuntimeException("Falha no envio síncrono", e);
        }
    }

    /**
     * Valida os parâmetros da mensagem
     */
    private void validarParametros(String codigoMarca, String nomeMarca, String tipoVeiculo) {
        if (codigoMarca == null || codigoMarca.trim().isEmpty()) {
            throw new IllegalArgumentException("Código da marca é obrigatório");
        }
        
        if (nomeMarca == null || nomeMarca.trim().isEmpty()) {
            throw new IllegalArgumentException("Nome da marca é obrigatório");
        }
        
        if (tipoVeiculo == null || tipoVeiculo.trim().isEmpty()) {
            throw new IllegalArgumentException("Tipo de veículo é obrigatório");
        }
        
        // Validar tipos de veículo aceitos
        String tipo = tipoVeiculo.toLowerCase();
        if (!tipo.equals("carros") && !tipo.equals("motos") && !tipo.equals("caminhoes")) {
            throw new IllegalArgumentException("Tipo de veículo inválido: " + tipoVeiculo);
        }
    }

    /**
     * Verifica se o emitter está disponível
     */
    public boolean isEmitterReady() {
        try {
            return marcasEmitter != null && !marcasEmitter.isCancelled();
        } catch (Exception e) {
            LOG.warn("Erro ao verificar status do emitter", e);
            return false;
        }
    }

    /**
     * Obtém estatísticas do producer (se disponível)
     */
    public ProducerStats getStats() {
        return new ProducerStats(
            isEmitterReady(),
            marcasEmitter != null ? !marcasEmitter.isCancelled() : false
        );
    }

    // Classe auxiliar para estatísticas
    public static class ProducerStats {
        public boolean emitterReady;
        public boolean emitterActive;
        
        public ProducerStats() {}
        
        public ProducerStats(boolean emitterReady, boolean emitterActive) {
            this.emitterReady = emitterReady;
            this.emitterActive = emitterActive;
        }
    }
}