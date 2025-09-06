package com.fipe.api2.controller;

import com.fipe.api2.repository.MarcaRepository;
import com.fipe.api2.repository.ModeloRepository;
import com.fipe.shared.entity.Marca;
import com.fipe.shared.entity.Modelo;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import java.util.List;

/**
 * Controller público para consultar dados processados
 * Endpoints simples para testar a API sem autenticação
 */
@Path("/api/v2/data")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Data", description = "Endpoints públicos para consultar dados processados")
public class DataController {

    private static final Logger LOG = Logger.getLogger(DataController.class);

    @Inject
    MarcaRepository marcaRepository;

    @Inject
    ModeloRepository modeloRepository;

    /**
     * Lista marcas processadas com paginação
     */
    @GET
    @Path("/marcas")
    @Operation(summary = "Lista marcas com paginação", 
               description = "Retorna marcas processadas com suporte a paginação (padrão: 10 marcas)")
    @APIResponse(responseCode = "200", description = "Lista de marcas retornada")
    public Response listarMarcas(@QueryParam("page") @DefaultValue("0") int page,
                                @QueryParam("size") @DefaultValue("10") int size) {
        try {
            // Limitar o tamanho máximo para evitar sobrecarga
            if (size > 50) {
                size = 50;
            }
            
            List<Marca> marcas = marcaRepository.listAll(page, size);
            
            long total = marcaRepository.count();
            
            LOG.infof("Retornando %d marcas (página %d, tamanho %d) de um total de %d", 
                     marcas.size(), page, size, total);
            
            // Criar resposta com informações de paginação
            var response = new java.util.HashMap<String, Object>();
            response.put("marcas", marcas);
            response.put("page", page);
            response.put("size", size);
            response.put("total", total);
            response.put("totalPages", (total + size - 1) / size);
            
            return Response.ok(response).build();
        } catch (Exception e) {
            LOG.error("Erro ao listar marcas", e);
            return Response.serverError().entity("Erro interno do servidor").build();
        }
    }

    /**
     * Lista todos os modelos processados
     */
    @GET
    @Path("/modelos")
    @Operation(summary = "Lista todos os modelos", 
               description = "Retorna todos os modelos que foram processados")
    @APIResponse(responseCode = "200", description = "Lista de modelos retornada")
    public Response listarModelos() {
        try {
            List<Modelo> modelos = modeloRepository.listAll();
            LOG.infof("Retornando %d modelos", modelos.size());
            return Response.ok(modelos).build();
        } catch (Exception e) {
            LOG.error("Erro ao listar modelos", e);
            return Response.serverError()
                .entity("Erro ao listar modelos: " + e.getMessage())
                .build();
        }
    }

    /**
     * Lista modelos de uma marca específica
     * Temporariamente desabilitado devido a problemas com MarcaRepository
     */
    /*
    @GET
    @Path("/marcas/{marcaId}/modelos")
    @Operation(summary = "Lista modelos de uma marca", 
               description = "Retorna todos os modelos de uma marca específica")
    @APIResponse(responseCode = "200", description = "Lista de modelos da marca retornada")
    public Response listarModelosPorMarca(@PathParam("marcaId") Long marcaId) {
        try {
            List<Modelo> modelos = modeloRepository.listByMarcaId(marcaId);
            LOG.infof("Retornando %d modelos para marca ID %d", modelos.size(), marcaId);
            return Response.ok(modelos).build();
        } catch (Exception e) {
            LOG.errorf("Erro ao listar modelos da marca %d: %s", marcaId, e.getMessage());
            return Response.serverError()
                .entity("Erro ao listar modelos da marca: " + e.getMessage())
                .build();
        }
    }
    */

    /**
     * Busca uma marca por código FIPE
     * Temporariamente desabilitado devido a problemas com MarcaRepository
     */
    /*
    @GET
    @Path("/marcas/codigo/{codigoFipe}")
    @Operation(summary = "Busca marca por código FIPE", 
               description = "Retorna uma marca específica pelo código FIPE")
    @APIResponse(responseCode = "200", description = "Marca encontrada")
    @APIResponse(responseCode = "404", description = "Marca não encontrada")
    public Response buscarMarcaPorCodigo(@PathParam("codigoFipe") String codigoFipe) {
        try {
            Marca marca = marcaRepository.findByCodigoFipe(codigoFipe);
            if (marca == null) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity("Marca com código " + codigoFipe + " não encontrada")
                    .build();
            }
            return Response.ok(marca).build();
        } catch (Exception e) {
            LOG.errorf("Erro ao buscar marca por código %s: %s", codigoFipe, e.getMessage());
            return Response.serverError()
                .entity("Erro ao buscar marca: " + e.getMessage())
                .build();
        }
    }
    */

    /**
     * Endpoint simples para inserir dados de teste
     * Temporariamente desabilitado devido a problemas com MarcaRepository
     */
    /*
    @GET
    @Path("/test/create-sample")
    @Operation(summary = "Cria dados de exemplo", 
               description = "Cria algumas marcas e modelos de exemplo para teste")
    @APIResponse(responseCode = "200", description = "Dados de exemplo criados")
    public Response criarDadosExemplo() {
        try {
            // Criar marca de exemplo
            Marca marcaTeste = new Marca("001", "Marca Teste", "carros");
            marcaRepository.persist(marcaTeste);
            
            // Buscar a marca criada para obter o ID
            Marca marcaSalva = marcaRepository.findByCodigoFipe("001");
            
            if (marcaSalva != null) {
                // Criar modelo de exemplo
                Modelo modeloTeste = new Modelo("001001", "Modelo Teste 1", marcaSalva);
                modeloRepository.persist(modeloTeste);
                
                Modelo modeloTeste2 = new Modelo("001002", "Modelo Teste 2", marcaSalva);
                modeloRepository.persist(modeloTeste2);
            }
            
            return Response.ok("Dados de exemplo criados com sucesso!").build();
        } catch (Exception e) {
            LOG.error("Erro ao criar dados de exemplo", e);
            return Response.serverError()
                .entity("Erro ao criar dados de exemplo: " + e.getMessage())
                .build();
        }
    }
    */
}