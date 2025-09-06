package com.fipe.shared.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Entidade que representa uma marca de veículo
 */
@Entity
@Table(name = "marcas")
public class Marca {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @NotNull
    @Column(name = "codigo_fipe", nullable = false, unique = true)
    public String codigoFipe;

    @NotBlank
    @Column(name = "nome", nullable = false)
    public String nome;

    @Column(name = "tipo_veiculo")
    public String tipoVeiculo; // carros, motos, caminhoes

    @Column(name = "data_criacao", nullable = false)
    public LocalDateTime dataCriacao;

    @Column(name = "data_atualizacao")
    public LocalDateTime dataAtualizacao;

    @OneToMany(mappedBy = "marca", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    public List<Modelo> modelos;

    public Marca() {
        this.dataCriacao = LocalDateTime.now();
    }

    public Marca(String codigoFipe, String nome, String tipoVeiculo) {
        this();
        this.codigoFipe = codigoFipe;
        this.nome = nome;
        this.tipoVeiculo = tipoVeiculo;
    }

    @PreUpdate
    public void preUpdate() {
        this.dataAtualizacao = LocalDateTime.now();
    }

    // Métodos de consulta removidos - usar EntityManager ou Repository pattern
}