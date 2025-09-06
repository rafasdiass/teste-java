package com.fipe.api1.repository;

import com.fipe.shared.entity.Marca;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class MarcaRepository implements PanacheRepository<Marca> {

    public List<Marca> findByTipoVeiculo(String tipoVeiculo) {
        if (tipoVeiculo == null || tipoVeiculo.trim().isEmpty()) {
            return find("ORDER BY nome").list();
        }
        return find("LOWER(tipoVeiculo) = LOWER(?1) ORDER BY nome", tipoVeiculo).list();
    }

    public List<Marca> findByTipoVeiculo(String tipoVeiculo, int page, int size) {
        if (tipoVeiculo == null || tipoVeiculo.trim().isEmpty()) {
            return find("ORDER BY nome").page(page, size).list();
        }
        return find("LOWER(tipoVeiculo) = LOWER(?1) ORDER BY nome", tipoVeiculo).page(page, size).list();
    }

    public List<Marca> findAll(int page, int size) {
        return find("ORDER BY nome").page(page, size).list();
    }

    public long countByTipoVeiculo(String tipoVeiculo) {
        if (tipoVeiculo == null || tipoVeiculo.trim().isEmpty()) {
            return count();
        }
        return count("LOWER(tipoVeiculo) = LOWER(?1)", tipoVeiculo);
    }

    public Optional<Marca> findByCodigoFipe(String codigoFipe) {
        return find("codigoFipe", codigoFipe).firstResultOptional();
    }

    public boolean existsByCodigoFipe(String codigoFipe) {
        return count("codigoFipe", codigoFipe) > 0;
    }
}