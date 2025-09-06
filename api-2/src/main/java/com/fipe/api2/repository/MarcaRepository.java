package com.fipe.api2.repository;

import com.fipe.shared.entity.Marca;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.List;

@ApplicationScoped
public class MarcaRepository implements PanacheRepositoryBase<Marca, Long> {

    public Marca findByCodigoFipe(String codigoFipe) {
        return find("codigoFipe = ?1", codigoFipe).firstResult();
    }

    @Transactional
    public void persist(Marca marca) {
        // Verificar se já existe para evitar duplicatas
        Marca existente = findByCodigoFipe(marca.codigoFipe);
        if (existente == null) {
            PanacheRepositoryBase.super.persist(marca);
        }
    }

    public List<Marca> listAll() {
        return find("ORDER BY nome").list();
    }

    public List<Marca> listAll(int page, int size) {
        return find("ORDER BY nome").page(page, size).list();
    }

    @Transactional
    public Marca merge(Marca marca) {
        return getEntityManager().merge(marca);
    }
}