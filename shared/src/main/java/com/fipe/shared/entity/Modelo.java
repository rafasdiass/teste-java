package com.fipe.shared.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * Entidade que representa um modelo de veículo
 */
@Entity
@Table(name = "modelos", indexes = {
    @Index(name = "idx_modelo_codigo_fipe", columnList = "codigo_fipe"),
    @Index(name = "idx_modelo_marca_id", columnList = "marca_id")
})
public class Modelo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @NotNull
    @Column(name = "codigo_fipe", nullable = false)
    public String codigoFipe;

    @NotBlank
    @Column(name = "nome", nullable = false)
    public String nome;

    @Column(name = "observacoes", columnDefinition = "TEXT")
    public String observacoes;

    @Column(name = "data_criacao", nullable = false)
    public LocalDateTime dataCriacao;

    @Column(name = "data_atualizacao")
    public LocalDateTime dataAtualizacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "marca_id", nullable = false)
    public Marca marca;

    public Modelo() {
        this.dataCriacao = LocalDateTime.now();
    }

    public Modelo(String codigoFipe, String nome, Marca marca) {
        this();
        this.codigoFipe = codigoFipe;
        this.nome = nome;
        this.marca = marca;
    }

    @PreUpdate
    public void preUpdate() {
        this.dataAtualizacao = LocalDateTime.now();
    }

    // Métodos de consulta removidos - usar EntityManager ou Repository pattern
}