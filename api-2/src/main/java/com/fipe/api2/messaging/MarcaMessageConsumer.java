package com.fipe.api2.messaging;

// Message Consumer para processar mensagens de marcas do RabbitMQ

import com.fipe.api2.service.DataProcessingService;
import com.fipe.shared.dto.MarcaQueueMessage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.logging.Logger;
import io.vertx.core.json.JsonObject;

import java.util.concurrent.CompletionStage;

@ApplicationScoped
public class MarcaMessageConsumer {

    private static final Logger LOG = Logger.getLogger(MarcaMessageConsumer.class);

    @Inject
    DataProcessingService dataProcessingService;

  
    @Incoming("marcas-in")
    public CompletionStage<Void> processarMarca(Message<JsonObject> message) {
        LOG.info("=== CONSUMER ATIVADO - Mensagem recebida ===");
        JsonObject jsonPayload = message.getPayload();
        
        try {
            // Converter JsonObject para MarcaQueueMessage
            MarcaQueueMessage marcaMessage = new MarcaQueueMessage(
                jsonPayload.getString("codigoMarca"),
                jsonPayload.getString("nomeMarca"),
                jsonPayload.getString("tipoVeiculo")
            );
            
            LOG.infof("Recebida mensagem para processamento: %s", marcaMessage);
            LOG.infof("Payload da mensagem: codigoMarca=%s, nomeMarca=%s, tipoVeiculo=%s", 
                      marcaMessage.codigoMarca, marcaMessage.nomeMarca, marcaMessage.tipoVeiculo);
            
            return processarMarcaAsync(marcaMessage)
                .thenCompose(result -> {
                    LOG.infof("Marca %s processada com sucesso", marcaMessage.nomeMarca);
                    return message.ack();
                })
                .exceptionally(throwable -> {
                    LOG.errorf("Erro ao processar marca %s: %s", marcaMessage.nomeMarca, throwable.getMessage());
                    // Em caso de erro, fazer nack para reprocessar a mensagem
                    message.nack(throwable);
                    return null;
                });
                
        } catch (Exception e) {
            LOG.errorf("Erro ao converter mensagem JSON: %s", e.getMessage());
            return message.nack(e);
        }
    }

 
    private CompletionStage<Void> processarMarcaAsync(MarcaQueueMessage marcaMessage) {
        return java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                // Validar dados da mensagem
                validarMensagem(marcaMessage);
                
                // Processar a marca: buscar modelos na API FIPE e salvar no banco
                dataProcessingService.processarMarca(
                    marcaMessage.codigoMarca,
                    marcaMessage.nomeMarca,
                    marcaMessage.tipoVeiculo
                );
                
                LOG.infof("Processamento da marca %s concluído com sucesso", marcaMessage.nomeMarca);
                
            } catch (Exception e) {
                LOG.errorf("Erro durante processamento assíncrono da marca %s: %s", 
                          marcaMessage.nomeMarca, e.getMessage());
                throw new RuntimeException("Falha no processamento da marca", e);
            }
        });
    }

    private void validarMensagem(MarcaQueueMessage marcaMessage) {
        if (marcaMessage == null) {
            throw new IllegalArgumentException("Mensagem não pode ser nula");
        }
        
        if (marcaMessage.codigoMarca == null || marcaMessage.codigoMarca.trim().isEmpty()) {
            throw new IllegalArgumentException("Código da marca é obrigatório");
        }
        
        if (marcaMessage.nomeMarca == null || marcaMessage.nomeMarca.trim().isEmpty()) {
            throw new IllegalArgumentException("Nome da marca é obrigatório");
        }
        
        if (marcaMessage.tipoVeiculo == null || marcaMessage.tipoVeiculo.trim().isEmpty()) {
            throw new IllegalArgumentException("Tipo de veículo é obrigatório");
        }
        
        // Validar tipos de veículo aceitos
        String tipoVeiculo = marcaMessage.tipoVeiculo.toLowerCase();
        if (!tipoVeiculo.equals("carros") && !tipoVeiculo.equals("motos") && !tipoVeiculo.equals("caminhoes")) {
            throw new IllegalArgumentException("Tipo de veículo inválido: " + marcaMessage.tipoVeiculo);
        }
        
        LOG.debugf("Mensagem validada com sucesso: %s", marcaMessage);
    }


    public void processarMarcaSync(MarcaQueueMessage marcaMessage) {
        LOG.infof("Processamento síncrono da marca: %s", marcaMessage.nomeMarca);
        
        try {
            validarMensagem(marcaMessage);
            
            dataProcessingService.processarMarca(
                marcaMessage.codigoMarca,
                marcaMessage.nomeMarca,
                marcaMessage.tipoVeiculo
            );
            
            LOG.infof("Marca %s processada sincronamente com sucesso", marcaMessage.nomeMarca);
            
        } catch (Exception e) {
            LOG.errorf("Erro no processamento síncrono da marca %s: %s", 
                      marcaMessage.nomeMarca, e.getMessage());
            throw e;
        }
    }
}