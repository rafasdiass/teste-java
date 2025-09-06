package com.fipe.api1;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import org.jboss.logging.Logger;

/**
 * Classe principal da API-1 do serviço FIPE
 * Responsável por inicializar a aplicação Quarkus
 */
@QuarkusMain
public class FipeApi1Application implements QuarkusApplication {

    private static final Logger LOG = Logger.getLogger(FipeApi1Application.class);

    public static void main(String... args) {
        LOG.info("Iniciando FIPE API-1...");
        Quarkus.run(FipeApi1Application.class, args);
    }

    @Override
    public int run(String... args) throws Exception {
        LOG.info("FIPE API-1 iniciada com sucesso!");
        LOG.info("Swagger UI disponível em: http://localhost:8080/swagger");
        LOG.info("OpenAPI spec disponível em: http://localhost:8080/swagger-ui");
        
        Quarkus.waitForExit();
        return 0;
    }
}