package com.fipe.api2;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;
import org.jboss.logging.Logger;

/**
 * Classe principal da API-2 do serviço FIPE
 * Responsável por inicializar a aplicação Quarkus para processamento assíncrono
 */
@QuarkusMain
public class FipeApi2Application {

    private static final Logger LOG = Logger.getLogger(FipeApi2Application.class);

    public static void main(String... args) {
        LOG.info("Iniciando FIPE API-2 (Processador de Dados)...");
        LOG.info("API-2 rodando na porta 8081");
        LOG.info("Aguardando mensagens da fila para processamento...");
        LOG.info("Endpoints de monitoramento disponíveis em: http://localhost:8081/api/v2/monitoring");
        Quarkus.run(args);
    }
}