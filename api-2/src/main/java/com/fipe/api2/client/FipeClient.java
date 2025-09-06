package com.fipe.api2.client;

import com.fipe.shared.dto.ModeloDTO;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;

import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Cliente REST para integração com a API FIPE na API-2
 * Focado na busca de modelos por marca
 */
@RegisterRestClient(configKey = "fipe-client")
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public interface FipeClient {

    /**
     * Busca todos os modelos de uma marca de carro
     */
    @GET
    @Path("/carros/marcas/{codigoMarca}/modelos")
    @Retry(maxRetries = 3, delay = 1000)
    @Timeout(value = 30, unit = ChronoUnit.SECONDS)
    FipeModelosResponse getModelosCarros(@PathParam("codigoMarca") String codigoMarca);

    /**
     * Busca todos os modelos de uma marca de moto
     */
    @GET
    @Path("/motos/marcas/{codigoMarca}/modelos")
    @Retry(maxRetries = 3, delay = 1000)
    @Timeout(value = 30, unit = ChronoUnit.SECONDS)
    FipeModelosResponse getModelosMotos(@PathParam("codigoMarca") String codigoMarca);

    /**
     * Busca todos os modelos de uma marca de caminhão
     */
    @GET
    @Path("/caminhoes/marcas/{codigoMarca}/modelos")
    @Retry(maxRetries = 3, delay = 1000)
    @Timeout(value = 30, unit = ChronoUnit.SECONDS)
    FipeModelosResponse getModelosCaminhoes(@PathParam("codigoMarca") String codigoMarca);

    /**
     * Classe para mapear a resposta da API FIPE para modelos
     */
    class FipeModelosResponse {
        public List<ModeloDTO> modelos;
        
        public FipeModelosResponse() {}
        
        public FipeModelosResponse(List<ModeloDTO> modelos) {
            this.modelos = modelos;
        }
    }
}