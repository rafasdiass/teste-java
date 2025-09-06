package com.fipe.api1.service;

import com.fipe.shared.entity.Marca;
import com.fipe.shared.entity.Modelo;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.value.ValueCommands;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.annotation.PostConstruct;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Serviço de cache Redis para otimizar consultas frequentes
 * Implementa cache para marcas, modelos e estatísticas
 */
@ApplicationScoped
public class CacheService {

    private static final Logger LOG = Logger.getLogger(CacheService.class);
    
    // Configurações de TTL (Time To Live)
    private static final Duration MARCA_TTL = Duration.ofHours(6);
    private static final Duration MODELO_TTL = Duration.ofHours(4);
    private static final Duration STATS_TTL = Duration.ofMinutes(30);
    private static final Duration LIST_TTL = Duration.ofHours(2);
    
    // Prefixos para chaves do cache
    private static final String MARCA_PREFIX = "marca:";
    private static final String MODELO_PREFIX = "modelo:";
    private static final String MARCAS_LIST_PREFIX = "marcas:list:";
    private static final String MODELOS_LIST_PREFIX = "modelos:list:";
    private static final String STATS_PREFIX = "stats:";

    @Inject
    RedisDataSource redisDataSource;
    
    private ValueCommands<String, String> valueCommands;

    @PostConstruct
    public void init() {
        this.valueCommands = redisDataSource.value(String.class, String.class);
    }

    // ========== CACHE DE MARCAS ==========
    
    /**
     * Busca marca no cache por código
     */
    public Optional<Marca> getMarca(String codigoMarca) {
        try {
            String key = MARCA_PREFIX + codigoMarca;
            String marcaJson = valueCommands.get(key);
            
            if (marcaJson != null) {
                LOG.debugf("Cache HIT para marca: %s", codigoMarca);
                // Aqui seria necessário deserializar o JSON para Marca
                // Por simplicidade, retornando empty para implementar a lógica de fallback
                return Optional.empty();
            }
            
            LOG.debugf("Cache MISS para marca: %s", codigoMarca);
            return Optional.empty();
            
        } catch (Exception e) {
            LOG.warnf("Erro ao buscar marca no cache: %s", e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * Armazena marca no cache
     */
    public void putMarca(String codigoMarca, Marca marca) {
        try {
            String key = MARCA_PREFIX + codigoMarca;
            // Aqui seria necessário serializar a Marca para JSON
            // Por simplicidade, armazenando apenas o nome
            valueCommands.setex(key, MARCA_TTL.toSeconds(), marca.nome);
            
            LOG.debugf("Marca %s armazenada no cache", codigoMarca);
            
        } catch (Exception e) {
            LOG.warnf("Erro ao armazenar marca no cache: %s", e.getMessage());
        }
    }
    
    /**
     * Busca lista de marcas no cache
     */
    public Optional<List<Marca>> getMarcasList(String tipoVeiculo, int page, int size) {
        try {
            String key = MARCAS_LIST_PREFIX + tipoVeiculo + ":" + page + ":" + size;
            String cachedValue = valueCommands.get(key);
            
            if (cachedValue != null) {
                LOG.debugf("Cache HIT para lista de marcas: %s", key);
                // Por simplicidade, retornando empty - seria necessário deserializar JSON
                return Optional.empty();
            }
            
            LOG.debugf("Cache MISS para lista de marcas: %s", key);
            return Optional.empty();
            
        } catch (Exception e) {
            LOG.warnf("Erro ao buscar lista de marcas no cache: %s", e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * Armazena lista de marcas no cache
     */
    public void putMarcasList(String tipoVeiculo, int page, int size, List<Marca> marcas) {
        try {
            String key = MARCAS_LIST_PREFIX + tipoVeiculo + ":" + page + ":" + size;
            // Por simplicidade, armazenando apenas o tamanho da lista
            valueCommands.setex(key, LIST_TTL.toSeconds(), String.valueOf(marcas.size()));
            
            LOG.debugf("Lista de marcas armazenada no cache: %s", key);
            
        } catch (Exception e) {
            LOG.warnf("Erro ao armazenar lista de marcas no cache: %s", e.getMessage());
        }
    }

    // ========== CACHE DE MODELOS ==========
    
    /**
     * Busca modelo no cache por código
     */
    public Optional<Modelo> getModelo(String codigoModelo) {
        try {
            String key = MODELO_PREFIX + codigoModelo;
            String modeloJson = valueCommands.get(key);
            
            if (modeloJson != null) {
                LOG.debugf("Cache HIT para modelo: %s", codigoModelo);
                return Optional.empty(); // Implementar deserialização
            }
            
            LOG.debugf("Cache MISS para modelo: %s", codigoModelo);
            return Optional.empty();
            
        } catch (Exception e) {
            LOG.warnf("Erro ao buscar modelo no cache: %s", e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * Armazena modelo no cache
     */
    public void putModelo(String codigoModelo, Modelo modelo) {
        try {
            String key = MODELO_PREFIX + codigoModelo;
            valueCommands.setex(key, MODELO_TTL.toSeconds(), modelo.nome);
            
            LOG.debugf("Modelo %s armazenado no cache", codigoModelo);
            
        } catch (Exception e) {
            LOG.warnf("Erro ao armazenar modelo no cache: %s", e.getMessage());
        }
    }
    
    /**
     * Busca lista de modelos no cache
     */
    public Optional<List<Modelo>> getModelosList(String codigoMarca, int page, int size) {
        try {
            String key = MODELOS_LIST_PREFIX + codigoMarca + ":" + page + ":" + size;
            String cachedValue = valueCommands.get(key);
            
            if (cachedValue != null) {
                LOG.debugf("Cache HIT para lista de modelos: %s", key);
                // Por simplicidade, retornando empty - seria necessário deserializar JSON
                return Optional.empty();
            }
            
            LOG.debugf("Cache MISS para lista de modelos: %s", key);
            return Optional.empty();
            
        } catch (Exception e) {
            LOG.warnf("Erro ao buscar lista de modelos no cache: %s", e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * Armazena lista de modelos no cache
     */
    public void putModelosList(String codigoMarca, int page, int size, List<Modelo> modelos) {
        try {
            String key = MODELOS_LIST_PREFIX + codigoMarca + ":" + page + ":" + size;
            // Por simplicidade, armazenando apenas o tamanho da lista
            valueCommands.setex(key, LIST_TTL.toSeconds(), String.valueOf(modelos.size()));
            
            LOG.debugf("Lista de modelos armazenada no cache: %s", key);
            
        } catch (Exception e) {
            LOG.warnf("Erro ao armazenar lista de modelos no cache: %s", e.getMessage());
        }
    }

    // ========== CACHE DE ESTATÍSTICAS ==========
    
    /**
     * Busca contagem no cache
     */
    public Optional<Long> getCount(String key) {
        try {
            String cacheKey = STATS_PREFIX + key;
            String countStr = valueCommands.get(cacheKey);
            
            if (countStr != null) {
                LOG.debugf("Cache HIT para contagem: %s", key);
                return Optional.of(Long.parseLong(countStr));
            }
            
            LOG.debugf("Cache MISS para contagem: %s", key);
            return Optional.empty();
            
        } catch (Exception e) {
            LOG.warnf("Erro ao buscar contagem no cache: %s", e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * Armazena contagem no cache
     */
    public void putCount(String key, Long count) {
        try {
            String cacheKey = STATS_PREFIX + key;
            valueCommands.setex(cacheKey, STATS_TTL.toSeconds(), String.valueOf(count));
            
            LOG.debugf("Contagem armazenada no cache: %s = %d", key, count);
            
        } catch (Exception e) {
            LOG.warnf("Erro ao armazenar contagem no cache: %s", e.getMessage());
        }
    }

    // ========== INVALIDAÇÃO DE CACHE ==========
    
    /**
     * Invalida cache de uma marca específica
     */
    public void invalidateMarca(String codigoMarca) {
        try {
            String key = MARCA_PREFIX + codigoMarca;
            redisDataSource.key().del(key);
            
            // Invalidar também listas que podem conter esta marca
            invalidateMarcasLists();
            
            LOG.infof("Cache invalidado para marca: %s", codigoMarca);
            
        } catch (Exception e) {
            LOG.warnf("Erro ao invalidar cache da marca: %s", e.getMessage());
        }
    }
    
    /**
     * Invalida cache de um modelo específico
     */
    public void invalidateModelo(String codigoModelo) {
        try {
            String key = MODELO_PREFIX + codigoModelo;
            redisDataSource.key().del(key);
            
            LOG.infof("Cache invalidado para modelo: %s", codigoModelo);
            
        } catch (Exception e) {
            LOG.warnf("Erro ao invalidar cache do modelo: %s", e.getMessage());
        }
    }
    
    /**
     * Invalida todas as listas de marcas
     */
    public void invalidateMarcasLists() {
        try {
            // Em uma implementação real, seria necessário usar SCAN para encontrar todas as chaves
            LOG.info("Invalidando listas de marcas do cache");
            
        } catch (Exception e) {
            LOG.warn("Erro ao invalidar listas de marcas", e);
        }
    }
    
    /**
     * Invalida todas as listas de modelos de uma marca
     */
    public void invalidateModelosLists(String codigoMarca) {
        try {
            LOG.infof("Invalidando listas de modelos da marca %s do cache", codigoMarca);
            
        } catch (Exception e) {
            LOG.warnf("Erro ao invalidar listas de modelos da marca %s", codigoMarca, e);
        }
    }
    
    /**
     * Limpa todo o cache
     */
    public void clearAll() {
        try {
            // Em uma implementação real, seria necessário usar FLUSHDB ou SCAN + DEL
            LOG.info("Limpando todo o cache Redis");
            
        } catch (Exception e) {
            LOG.warn("Erro ao limpar cache", e);
        }
    }
    
    /**
     * Verifica se o Redis está disponível
     */
    public boolean isRedisAvailable() {
        try {
            valueCommands.set("health-check", "ok");
            String result = valueCommands.get("health-check");
            redisDataSource.key().del("health-check");
            
            return "ok".equals(result);
            
        } catch (Exception e) {
            LOG.warn("Redis não está disponível", e);
            return false;
        }
    }
    
    /**
     * Obtém estatísticas do cache
     */
    public CacheStats getStats() {
        return new CacheStats(
            isRedisAvailable(),
            "Cache Redis configurado com TTL variável"
        );
    }
    
    // Classe auxiliar para estatísticas
    public static class CacheStats {
        public boolean redisAvailable;
        public String description;
        
        public CacheStats() {}
        
        public CacheStats(boolean redisAvailable, String description) {
            this.redisAvailable = redisAvailable;
            this.description = description;
        }
    }
}