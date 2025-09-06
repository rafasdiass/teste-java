package com.fipe.api1.service;

import com.fipe.api1.repository.MarcaRepository;
import com.fipe.api1.repository.ModeloRepository;
import com.fipe.shared.entity.Marca;
import com.fipe.shared.entity.Modelo;
import io.quarkus.cache.CacheResult;
import io.quarkus.cache.CacheInvalidate;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Serviço para operações relacionadas a veículos (marcas e modelos)
 * Inclui cache para otimizar consultas frequentes
 */
@ApplicationScoped
public class VeiculoService {

    private static final Logger LOG = Logger.getLogger(VeiculoService.class);
    
    @Inject
    CacheService cacheService;
    
    @Inject
    MarcaRepository marcaRepository;
    
    @Inject
    ModeloRepository modeloRepository;

    /**
     * Busca marcas com paginação e filtro opcional por tipo
     * Implementa cache Redis para melhor performance
     */
    @CacheResult(cacheName = "marcas-cache")
    public List<Marca> buscarMarcas(String tipoVeiculo, int page, int size) {
        LOG.infof("Buscando marcas - Tipo: %s, Page: %d, Size: %d", tipoVeiculo, page, size);
        return marcaRepository.findByTipoVeiculo(tipoVeiculo, page, size);
    }
    
    public long contarMarcas(String tipoVeiculo) {
        return marcaRepository.countByTipoVeiculo(tipoVeiculo);
    }

    /**
     * Busca modelos por marca com paginação
     * Implementa cache Redis para melhor performance
     */
    @CacheResult(cacheName = "modelos-cache")
    public List<Modelo> buscarModelosPorMarca(String codigoMarca, int page, int size) {
        LOG.infof("Buscando modelos para marca: %s, Page: %d, Size: %d", codigoMarca, page, size);
        return modeloRepository.findByMarcaCodigoFipe(codigoMarca, page, size);
    }
    
    public long contarModelosPorMarca(String codigoMarca) {
        return modeloRepository.countByMarcaCodigoFipe(codigoMarca);
    }
    
    public Marca buscarMarcaPorCodigo(String codigoFipe) {
        Optional<Marca> marca = marcaRepository.findByCodigoFipe(codigoFipe);
        return marca.orElse(null);
    }
    
    public Modelo buscarModeloPorCodigo(String codigoFipe) {
        Optional<Modelo> modelo = modeloRepository.findByCodigoFipe(codigoFipe);
        return modelo.orElse(null);
    }



    /**
     * Atualiza os dados de um modelo (nome e observações)
     * Invalida cache relacionado
     */
    @Transactional
    @CacheInvalidate(cacheName = "modelos-cache")
    public Modelo atualizarModelo(String codigoFipe, String nome, String observacoes) {
        LOG.infof("Atualizando modelo: %s", codigoFipe);
        
        Optional<Modelo> modeloOpt = modeloRepository.findByCodigoFipe(codigoFipe);
        if (modeloOpt.isEmpty()) {
            throw new IllegalArgumentException("Modelo não encontrado: " + codigoFipe);
        }
        
        Modelo modelo = modeloOpt.get();
        if (nome != null && !nome.trim().isEmpty()) {
            modelo.nome = nome;
        }
        if (observacoes != null) {
            modelo.observacoes = observacoes;
        }
        modelo.dataAtualizacao = LocalDateTime.now();
        
        modeloRepository.persist(modelo);
        
        // Invalidar cache relacionado
        if (modelo.marca != null) {
            cacheService.invalidateModelosLists(modelo.marca.codigoFipe);
        }
        
        return modelo;
    }

    @Transactional
    public Marca salvarMarca(Marca marca) {
        LOG.infof("Salvando marca: %s - %s", marca.codigoFipe, marca.nome);
        
        // Verificar se já existe
        if (marcaRepository.existsByCodigoFipe(marca.codigoFipe)) {
            return marcaRepository.findByCodigoFipe(marca.codigoFipe).get();
        }
        
        marca.dataCriacao = LocalDateTime.now();
        marca.dataAtualizacao = LocalDateTime.now();
        marcaRepository.persist(marca);
        return marca;
    }

    @Transactional
    public Modelo salvarModelo(Modelo modelo) {
        LOG.infof("Salvando modelo: %s - %s", modelo.codigoFipe, modelo.nome);
        
        // Verificar se já existe
        if (modeloRepository.existsByCodigoFipe(modelo.codigoFipe)) {
            return modeloRepository.findByCodigoFipe(modelo.codigoFipe).get();
        }
        
        modelo.dataCriacao = LocalDateTime.now();
        modelo.dataAtualizacao = LocalDateTime.now();
        modeloRepository.persist(modelo);
        return modelo;
    }
    


    /**
     * Lista modelos que possuem observações
     * Método removido temporariamente - não implementado no repositório JDBC
     */
    // public List<Modelo> listarModelosComObservacoes() {
    //     LOG.info("Buscando modelos com observações");
    //     return modeloRepository.findAllWithObservacoes();
    // }

    public long contarMarcasPorTipo(String tipoVeiculo) {
        return marcaRepository.countByTipoVeiculo(tipoVeiculo);
    }

    public void limparCaches() {
        cacheService.clearAll();
    }
}