package com.fipe.api1.repository;

import com.fipe.shared.entity.Modelo;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class ModeloRepository implements PanacheRepository<Modelo> {

    public List<Modelo> findByMarcaCodigoFipe(String codigoMarca, int page, int size) {
        return find("marca.codigoFipe = ?1 ORDER BY nome", codigoMarca).page(page, size).list();
    }

    public long countByMarcaCodigoFipe(String codigoMarca) {
        return count("marca.codigoFipe", codigoMarca);
    }

    public Optional<Modelo> findByCodigoFipe(String codigoFipe) {
        return find("codigoFipe", codigoFipe).firstResultOptional();
    }

    public List<Modelo> findAllWithObservacoes() {
        return find("observacoes IS NOT NULL AND observacoes != '' ORDER BY nome").list();
    }

    public boolean existsByCodigoFipe(String codigoFipe) {
        return count("codigoFipe", codigoFipe) > 0;
    }
}