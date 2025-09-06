package com.fipe.api1.controller;

import com.fipe.api1.service.FipeIntegrationService;
import com.fipe.api1.service.VeiculoService;
import com.fipe.shared.entity.Marca;
import com.fipe.shared.entity.Modelo;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

/**
 * Controller REST para operações relacionadas aos dados FIPE
 */
@Path("/api/v1/fipe")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "FIPE", description = "Operações relacionadas aos dados da tabela FIPE")
public class FipeController {

    private static final Logger LOG = Logger.getLogger(FipeController.class);

    @Inject
    FipeIntegrationService fipeIntegrationService;

    @Inject
    VeiculoService veiculoService;
    
    @Inject
    JsonWebToken jwt;

    /**
     * 1.1 - Endpoint para acionar a carga inicial dos dados de veículos
     */
    @POST
    @Path("/carga-inicial")
    @Operation(summary = "Executa carga inicial dos dados FIPE", 
               description = "Busca todas as marcas na API FIPE e envia para processamento assíncrono")
    @APIResponse(responseCode = "202", description = "Carga inicial iniciada com sucesso")
    @APIResponse(responseCode = "500", description = "Erro interno do servidor")
    public Response executarCargaInicial() {
        LOG.info("Iniciando carga inicial de dados FIPE");
        
        try {
            // Executar de forma síncrona para debug
            int totalMarcasEnviadas = 0;
            
            // Buscar e processar marcas de carros
            List<com.fipe.shared.dto.MarcaDTO> marcasCarros = fipeIntegrationService.buscarMarcasPorTipo("carros");
            fipeIntegrationService.enviarMarcasParaFila(marcasCarros, "carros").toCompletableFuture().get();
            totalMarcasEnviadas += marcasCarros.size();
            LOG.infof("Enviadas %d marcas de carros para processamento", marcasCarros.size());
            
            // Buscar e processar marcas de motos
            List<com.fipe.shared.dto.MarcaDTO> marcasMotos = fipeIntegrationService.buscarMarcasPorTipo("motos");
            fipeIntegrationService.enviarMarcasParaFila(marcasMotos, "motos").toCompletableFuture().get();
            totalMarcasEnviadas += marcasMotos.size();
            LOG.infof("Enviadas %d marcas de motos para processamento", marcasMotos.size());
            
            // Buscar e processar marcas de caminhões
            List<com.fipe.shared.dto.MarcaDTO> marcasCaminhoes = fipeIntegrationService.buscarMarcasPorTipo("caminhoes");
            fipeIntegrationService.enviarMarcasParaFila(marcasCaminhoes, "caminhoes").toCompletableFuture().get();
            totalMarcasEnviadas += marcasCaminhoes.size();
            LOG.infof("Enviadas %d marcas de caminhões para processamento", marcasCaminhoes.size());
            
            String resultado = String.format(
                "Carga inicial concluída com sucesso! Total de %d marcas enviadas para processamento (Carros: %d, Motos: %d, Caminhões: %d)",
                totalMarcasEnviadas, marcasCarros.size(), marcasMotos.size(), marcasCaminhoes.size()
            );
            
            LOG.info(resultado);
            return Response.accepted()
                .entity(new ApiResponse("success", resultado))
                .build();
                
        } catch (Exception e) {
            LOG.error("Erro durante carga inicial", e);
            return Response.serverError()
                .entity(new ApiResponse("error", "Erro durante carga inicial: " + e.getMessage()))
                .build();
        }
    }

    /**
     * Endpoint para adicionar uma marca individual
     */
    @POST
    @Path("/marcas")
    @Operation(summary = "Adicionar marca individual", 
               description = "Adiciona uma marca específica e envia para processamento na API-2")
    @APIResponse(responseCode = "201", description = "Marca criada e enviada com sucesso")
    @APIResponse(responseCode = "400", description = "Dados inválidos")
    @APIResponse(responseCode = "500", description = "Erro interno do servidor")
    @Transactional
    public CompletionStage<Response> adicionarMarca(@Valid CriarMarcaRequest request) {
        try {
            LOG.infof("Adicionando marca: %s - %s (%s)", request.codigo, request.nome, request.tipoVeiculo);
            
            // Verificar se a marca já existe
            Marca marcaExistente = veiculoService.buscarMarcaPorCodigo(request.codigo);
            if (marcaExistente != null) {
                return java.util.concurrent.CompletableFuture.completedFuture(
                    Response.status(Response.Status.CONFLICT)
                        .entity(new ErrorResponse("Marca com código " + request.codigo + " já existe"))
                        .build()
                );
            }
            
            // Criar e salvar a marca no banco
            Marca novaMarca = new Marca();
            novaMarca.codigoFipe = request.codigo;
            novaMarca.nome = request.nome;
            novaMarca.tipoVeiculo = request.tipoVeiculo;
            novaMarca.dataCriacao = java.time.LocalDateTime.now();
            
            veiculoService.salvarMarca(novaMarca);
            LOG.infof("Marca %s salva no banco de dados", request.nome);
            
            // Enviar para a fila da API-2
            return fipeIntegrationService.enviarMarcaIndividual(request.codigo, request.nome, request.tipoVeiculo)
                .thenApply(resultado -> {
                    LOG.infof("Marca %s enviada para processamento na API-2", request.nome);
                    
                    MarcaResponse response = new MarcaResponse(
                        novaMarca.codigoFipe,
                        novaMarca.nome,
                        novaMarca.tipoVeiculo,
                        novaMarca.dataCriacao,
                        novaMarca.dataAtualizacao
                    );
                    
                    return Response.status(Response.Status.CREATED)
                        .entity(new ApiResponse("success", "Marca criada e enviada para processamento", response))
                        .build();
                })
                .exceptionally(throwable -> {
                    LOG.errorf("Erro ao enviar marca %s para processamento: %s", request.nome, throwable.getMessage());
                    return Response.serverError()
                        .entity(new ErrorResponse("Marca criada mas erro ao enviar para processamento: " + throwable.getMessage()))
                        .build();
                });
                
        } catch (Exception e) {
            LOG.errorf("Erro ao adicionar marca: %s", e.getMessage());
            return java.util.concurrent.CompletableFuture.completedFuture(
                Response.serverError()
                    .entity(new ErrorResponse("Erro ao adicionar marca: " + e.getMessage()))
                    .build()
            );
        }
    }

    /**
     * 1.6 - Endpoint para buscar as marcas armazenadas no banco de dados
     */
    @GET
    @Path("/marcas")
    @Operation(summary = "Buscar marcas", 
               description = "Retorna todas as marcas de veículos armazenadas no banco")
    @APIResponse(responseCode = "200", description = "Marcas encontradas")
    // @RolesAllowed({"user", "admin", "fipe-access"}) // Temporariamente desabilitado para testes
    public Response buscarMarcas(@QueryParam("tipoVeiculo") String tipoVeiculo,
                                @QueryParam("page") @DefaultValue("0") int page,
                                @QueryParam("size") @DefaultValue("50") int size) {
        try {
            // String username = jwt.getName(); // JWT temporariamente desabilitado
            LOG.infof("Buscando marcas - Tipo: %s, Page: %d, Size: %d", tipoVeiculo, page, size);
            
            List<Marca> marcas = veiculoService.buscarMarcas(tipoVeiculo, page, size);
            long total = veiculoService.contarMarcas(tipoVeiculo);
            
            // Converter para DTO
            List<MarcaResponse> marcasResponse = marcas.stream()
                .map(marca -> new MarcaResponse(
                    marca.codigoFipe,
                    marca.nome,
                    marca.tipoVeiculo,
                    marca.dataCriacao,
                    marca.dataAtualizacao
                ))
                .collect(Collectors.toList());
            
            MarcasPageResponse response = new MarcasPageResponse(
                marcasResponse,
                page,
                size,
                total,
                (int) Math.ceil((double) total / size)
            );
            
            return Response.ok(response).build();
            
        } catch (Exception e) {
            LOG.error("Erro ao buscar marcas", e);
            return Response.serverError()
                .entity(new ErrorResponse("Erro ao buscar marcas: " + e.getMessage()))
                .build();
        }
    }

    /**
     * 1.7 - Endpoint para buscar códigos, modelos e observações por marca
     */
    @GET
    @Path("/marcas/{codigoMarca}/modelos")
    @Operation(summary = "Buscar modelos por marca", 
               description = "Retorna códigos, modelos e observações dos veículos por marca")
    @APIResponse(responseCode = "200", description = "Modelos encontrados")
    @RolesAllowed({"user", "admin", "fipe-access"})
    public Response buscarModelosPorMarca(@PathParam("codigoMarca") String codigoMarca,
                                         @QueryParam("page") @DefaultValue("0") int page,
                                         @QueryParam("size") @DefaultValue("50") int size) {
        try {
            String username = jwt.getName();
            LOG.infof("Usuário %s buscando modelos para marca: %s, Page: %d, Size: %d", username, codigoMarca, page, size);
            
            // Verificar se a marca existe
            Marca marca = veiculoService.buscarMarcaPorCodigo(codigoMarca);
            if (marca == null) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Marca não encontrada: " + codigoMarca))
                    .build();
            }
            
            List<Modelo> modelos = veiculoService.buscarModelosPorMarca(codigoMarca, page, size);
            long total = veiculoService.contarModelosPorMarca(codigoMarca);
            
            // Converter para DTO
            List<ModeloResponse> modelosResponse = modelos.stream()
                .map(modelo -> new ModeloResponse(
                    modelo.codigoFipe,
                    modelo.nome,
                    modelo.observacoes,
                    modelo.marca.codigoFipe,
                    modelo.marca.nome,
                    modelo.dataCriacao,
                    modelo.dataAtualizacao
                ))
                .collect(Collectors.toList());
            
            ModelosPageResponse response = new ModelosPageResponse(
                modelosResponse,
                marca.codigoFipe,
                marca.nome,
                page,
                size,
                total,
                (int) Math.ceil((double) total / size)
            );
            
            return Response.ok(response).build();
            
        } catch (Exception e) {
            LOG.errorf("Erro ao buscar modelos para marca %s: %s", codigoMarca, e.getMessage());
            return Response.serverError()
                .entity(new ErrorResponse("Erro ao buscar modelos: " + e.getMessage()))
                .build();
        }
    }

    /**
     * 1.8 - Endpoint para salvar dados alterados do veículo
     */
    @PUT
    @Path("/modelos/{codigoModelo}")
    @Operation(summary = "Atualizar modelo", 
               description = "Atualiza dados do modelo como nome e observações")
    @APIResponse(responseCode = "200", description = "Modelo atualizado com sucesso")
    @RolesAllowed({"admin", "fipe-admin"})
    public Response atualizarModelo(@PathParam("codigoModelo") String codigoModelo,
                                   @Valid AtualizarModeloRequest request) {
        try {
            String username = jwt.getName();
            LOG.infof("Usuário %s atualizando modelo: %s", username, codigoModelo);
            
            // Validar request
            if (request == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Dados de atualização são obrigatórios"))
                    .build();
            }
            
            // Buscar modelo existente
            Modelo modelo = veiculoService.buscarModeloPorCodigo(codigoModelo);
            if (modelo == null) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Modelo não encontrado: " + codigoModelo))
                    .build();
            }
            
            // Atualizar dados
            Modelo modeloAtualizado = veiculoService.atualizarModelo(
                codigoModelo,
                request.nome,
                request.observacoes
            );
            
            // Converter para response
            ModeloResponse response = new ModeloResponse(
                modeloAtualizado.codigoFipe,
                modeloAtualizado.nome,
                modeloAtualizado.observacoes,
                modeloAtualizado.marca.codigoFipe,
                modeloAtualizado.marca.nome,
                modeloAtualizado.dataCriacao,
                modeloAtualizado.dataAtualizacao
            );
            
            LOG.infof("Modelo %s atualizado com sucesso", codigoModelo);
            return Response.ok(response).build();
            
        } catch (IllegalArgumentException e) {
            LOG.warnf("Dados inválidos para atualização do modelo %s: %s", codigoModelo, e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(e.getMessage()))
                .build();
        } catch (Exception e) {
            LOG.errorf("Erro ao atualizar modelo %s: %s", codigoModelo, e.getMessage());
            return Response.serverError()
                .entity(new ErrorResponse("Erro ao atualizar modelo: " + e.getMessage()))
                .build();
        }
    }

    /**
     * Endpoint para verificar status da API
     */
    @GET
    @Path("/health")
    @Operation(summary = "Verifica saúde da API", description = "Endpoint para verificar se a API está funcionando")
    public Response health() {
        boolean fipeDisponivel = fipeIntegrationService.verificarDisponibilidadeApi();
        
        return Response.ok(new HealthResponse("API-1 funcionando", fipeDisponivel)).build();
    }
    
    /**
     * Endpoint de teste para verificar integração com API FIPE
     */
    @GET
    @Path("/test-fipe")
    @Operation(summary = "Testa integração com API FIPE", description = "Endpoint para testar se a integração com a API FIPE está funcionando")
    public Response testarIntegracaoFipe() {
        try {
            LOG.info("Testando integração com API FIPE...");
            List<com.fipe.shared.dto.MarcaDTO> marcas = fipeIntegrationService.buscarMarcasPorTipo("carros");
            LOG.infof("Sucesso! Encontradas %d marcas de carros", marcas.size());
            
            return Response.ok()
                .entity(new ApiResponse("success", 
                    String.format("Integração funcionando! Encontradas %d marcas de carros", marcas.size())))
                .build();
                
        } catch (Exception e) {
            LOG.error("Erro ao testar integração com API FIPE", e);
            return Response.serverError()
                .entity(new ApiResponse("error", "Erro na integração: " + e.getMessage()))
                .build();
        }
    }

    // Classes auxiliares para requests e responses
    public static class ApiResponse {
        public String status;
        public String message;
        public Object data;
        
        public ApiResponse() {}
        
        public ApiResponse(String status, String message) {
            this.status = status;
            this.message = message;
        }
        
        public ApiResponse(String status, String message, Object data) {
            this.status = status;
            this.message = message;
            this.data = data;
        }
    }
    
    public static class AtualizarModeloRequest {
        public String nome;
        public String observacoes;
    }
    
    public static class HealthResponse {
        public String status;
        public boolean fipeApiDisponivel;
        
        public HealthResponse() {}
        
        public HealthResponse(String status, boolean fipeApiDisponivel) {
            this.status = status;
            this.fipeApiDisponivel = fipeApiDisponivel;
        }
    }
    
    public static class MarcaResponse {
        public String codigo;
        public String nome;
        public String tipoVeiculo;
        public java.time.LocalDateTime dataCriacao;
        public java.time.LocalDateTime dataAtualizacao;
        
        public MarcaResponse() {}
        
        public MarcaResponse(String codigo, String nome, String tipoVeiculo, java.time.LocalDateTime dataCriacao, java.time.LocalDateTime dataAtualizacao) {
            this.codigo = codigo;
            this.nome = nome;
            this.tipoVeiculo = tipoVeiculo;
            this.dataCriacao = dataCriacao;
            this.dataAtualizacao = dataAtualizacao;
        }
    }
    
    public static class MarcasPageResponse {
        public List<MarcaResponse> marcas;
        public int page;
        public int size;
        public long total;
        public int totalPages;
        
        public MarcasPageResponse() {}
        
        public MarcasPageResponse(List<MarcaResponse> marcas, int page, int size, long total, int totalPages) {
            this.marcas = marcas;
            this.page = page;
            this.size = size;
            this.total = total;
            this.totalPages = totalPages;
        }
    }
    
    public static class ModeloResponse {
        public String codigo;
        public String nome;
        public String observacoes;
        public String codigoMarca;
        public String nomeMarca;
        public java.time.LocalDateTime dataCriacao;
        public java.time.LocalDateTime dataAtualizacao;
        
        public ModeloResponse() {}
        
        public ModeloResponse(String codigo, String nome, String observacoes, String codigoMarca, String nomeMarca, java.time.LocalDateTime dataCriacao, java.time.LocalDateTime dataAtualizacao) {
            this.codigo = codigo;
            this.nome = nome;
            this.observacoes = observacoes;
            this.codigoMarca = codigoMarca;
            this.nomeMarca = nomeMarca;
            this.dataCriacao = dataCriacao;
            this.dataAtualizacao = dataAtualizacao;
        }
    }
    
    public static class ModelosPageResponse {
        public List<ModeloResponse> modelos;
        public String codigoMarca;
        public String nomeMarca;
        public int page;
        public int size;
        public long total;
        public int totalPages;
        
        public ModelosPageResponse() {}
        
        public ModelosPageResponse(List<ModeloResponse> modelos, String codigoMarca, String nomeMarca, int page, int size, long total, int totalPages) {
            this.modelos = modelos;
            this.codigoMarca = codigoMarca;
            this.nomeMarca = nomeMarca;
            this.page = page;
            this.size = size;
            this.total = total;
            this.totalPages = totalPages;
        }
    }
    
    public static class ErrorResponse {
        public String error;
        public java.time.LocalDateTime timestamp;
        
        public ErrorResponse() {}
        
        public ErrorResponse(String error) {
            this.error = error;
            this.timestamp = java.time.LocalDateTime.now();
        }
    }
    
    public static class CriarMarcaRequest {
        @NotBlank(message = "Código da marca é obrigatório")
        public String codigo;
        
        @NotBlank(message = "Nome da marca é obrigatório")
        public String nome;
        
        @NotBlank(message = "Tipo de veículo é obrigatório")
        public String tipoVeiculo;
        
        public CriarMarcaRequest() {}
        
        public CriarMarcaRequest(String codigo, String nome, String tipoVeiculo) {
            this.codigo = codigo;
            this.nome = nome;
            this.tipoVeiculo = tipoVeiculo;
        }
    }
}