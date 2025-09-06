package com.fipe.api2.repository;

import com.fipe.shared.entity.Modelo;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.List;

@ApplicationScoped
public class ModeloRepository implements PanacheRepository<Modelo> {

    @Transactional
    public void persist(Modelo modelo) {
        // Verificar se já existe para evitar duplicatas
        Modelo existente = findByCodigoFipe(modelo.codigoFipe);
        if (existente == null) {
            PanacheRepository.super.persist(modelo);
        }
    }

    public List<Modelo> listAll() {
        return find("ORDER BY nome").list();
    }

    public Modelo findById(Long id) {
        return PanacheRepository.super.findById(id);
    }

    public List<Modelo> listByMarcaId(Long marcaId) {
        return find("marca.id = ?1 ORDER BY nome", marcaId).list();
    }

    public long count() {
        return PanacheRepository.super.count();
    }

    public long countByMarca(com.fipe.shared.entity.Marca marca) {
        return count("marca = ?1", marca);
    }

    public Modelo findByCodigoFipe(String codigoFipe) {
        return find("codigoFipe = ?1", codigoFipe).firstResult();
    }

    @Transactional
    public Modelo merge(Modelo modelo) {
        return getEntityManager().merge(modelo);
    }
}