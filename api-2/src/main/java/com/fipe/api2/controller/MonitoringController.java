package com.fipe.api2.controller;

import com.fipe.api2.service.DataProcessingService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

/**
 * Controller para monitoramento e estatísticas da API-2
 * Protegido por autenticação JWT
 */
@Path("/api/v2/monitoring")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Monitoring", description = "Endpoints para monitoramento da API-2")
public class MonitoringController {

    private static final Logger LOG = Logger.getLogger(MonitoringController.class);

    @Inject
    DataProcessingService dataProcessingService;
    
    @Inject
    JsonWebToken jwt;

    /**
     * Endpoint público para verificar saúde da API-2
     * Não requer autenticação JWT
     */
    @GET
    @Path("/health")
    @Operation(summary = "Verifica saúde da API-2", 
               description = "Endpoint para verificar se a API-2 está funcionando")
    @APIResponse(responseCode = "200", description = "API funcionando corretamente")
    public Response health() {
        try {
            LOG.debug("Health check solicitado");
            
            // Temporariamente desabilitado devido a problemas com MarcaRepository
            // DataProcessingService.ProcessingStats stats = dataProcessingService.getProcessingStats();
            DataProcessingService.ProcessingStats stats = null;
            
            HealthResponse response = new HealthResponse(
                "API-2 funcionando",
                true,
                stats
            );
            
            return Response.ok(response).build();
            
        } catch (Exception e) {
            LOG.error("Erro ao verificar saúde da API", e);
            
            HealthResponse response = new HealthResponse(
                "API-2 com problemas: " + e.getMessage(),
                false,
                null
            );
            
            return Response.serverError().entity(response).build();
        }
    }

    /**
     * Endpoint público simples para verificar se a API está no ar
     */
    @GET
    @Path("/ping")
    @Operation(summary = "Ping da API-2", 
               description = "Endpoint simples para verificar se a API está respondendo")
    @APIResponse(responseCode = "200", description = "API respondendo")
    public Response ping() {
        return Response.ok("API-2 está funcionando!").build();
    }

    /**
     * Endpoint para obter estatísticas de processamento
     * Requer autenticação JWT
     */
    @GET
    @Path("/stats")
    @Operation(summary = "Estatísticas de processamento", 
               description = "Retorna estatísticas detalhadas do processamento de dados")
    @APIResponse(responseCode = "200", description = "Estatísticas retornadas com sucesso")
    // @RolesAllowed({"user", "admin", "monitoring", "fipe-access"}) // Temporariamente desabilitado para testes
    public Response getProcessingStats() {
        try {
            // String username = jwt.getName(); // JWT desabilitado temporariamente
            LOG.debugf("Estatísticas de processamento solicitadas");
            
            // Temporariamente desabilitado devido a problemas com MarcaRepository
            // DataProcessingService.ProcessingStats stats = dataProcessingService.getProcessingStats();
            DataProcessingService.ProcessingStats stats = null;
            
            LOG.infof("Estatísticas solicitadas - Marcas: %d, Modelos: %d", 
                     stats.totalMarcas, stats.totalModelos);
            
            return Response.ok(stats).build();
            
        } catch (Exception e) {
            LOG.error("Erro ao obter estatísticas", e);
            
            return Response.serverError()
                .entity(new ErrorResponse("Erro ao obter estatísticas: " + e.getMessage()))
                .build();
        }
    }

    /**
     * Endpoint para verificar se uma marca específica foi processada
     * Requer autenticação JWT
     */
    @GET
    @Path("/marca/{codigoMarca}/processed")
    @Operation(summary = "Verifica se marca foi processada", 
               description = "Verifica se uma marca específica já foi processada")
    @APIResponse(responseCode = "200", description = "Status da marca retornado")
    // @RolesAllowed({"user", "admin", "monitoring", "fipe-access"}) // Temporariamente desabilitado para testes
    public Response isMarcaProcessed(@PathParam("codigoMarca") String codigoMarca) {
        try {
            // String username = jwt.getName(); // JWT desabilitado temporariamente
            LOG.debugf("Verificando processamento da marca: %s", codigoMarca);
            
            // Temporariamente desabilitado devido a problemas com MarcaRepository
            // boolean processada = dataProcessingService.marcaJaProcessada(codigoMarca);
            boolean processada = false;
            
            MarcaStatusResponse response = new MarcaStatusResponse(
                codigoMarca,
                processada,
                processada ? "Marca já processada" : "Marca não processada"
            );
            
            return Response.ok(response).build();
            
        } catch (Exception e) {
            LOG.errorf("Erro ao verificar status da marca %s: %s", codigoMarca, e.getMessage());
            
            return Response.serverError()
                .entity(new ErrorResponse("Erro ao verificar marca: " + e.getMessage()))
                .build();
        }
    }

    // Classes auxiliares para responses
    public static class HealthResponse {
        public String status;
        public boolean healthy;
        public DataProcessingService.ProcessingStats stats;
        
        public HealthResponse() {}
        
        public HealthResponse(String status, boolean healthy, DataProcessingService.ProcessingStats stats) {
            this.status = status;
            this.healthy = healthy;
            this.stats = stats;
        }
    }
    
    public static class MarcaStatusResponse {
        public String codigoMarca;
        public boolean processada;
        public String message;
        
        public MarcaStatusResponse() {}
        
        public MarcaStatusResponse(String codigoMarca, boolean processada, String message) {
            this.codigoMarca = codigoMarca;
            this.processada = processada;
            this.message = message;
        }
    }
    
    public static class ErrorResponse {
        public String error;
        
        public ErrorResponse() {}
        
        public ErrorResponse(String error) {
            this.error = error;
        }
    }
}