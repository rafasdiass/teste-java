package com.fipe.shared.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO para representar um modelo de veículo
 */
public class ModeloDTO {

    @JsonProperty("codigo")
    @NotNull
    public String codigo;

    @JsonProperty("nome")
    @NotBlank
    public String nome;

    public String observacoes;

    @JsonProperty("codigoMarca")
    public String codigoMarca;

    public String nomeMarca;

    public ModeloDTO() {}

    public ModeloDTO(String codigo, String nome) {
        this.codigo = codigo;
        this.nome = nome;
    }

    public ModeloDTO(String codigo, String nome, String codigoMarca, String nomeMarca) {
        this.codigo = codigo;
        this.nome = nome;
        this.codigoMarca = codigoMarca;
        this.nomeMarca = nomeMarca;
    }

    @Override
    public String toString() {
        return "ModeloDTO{" +
                "codigo='" + codigo + '\'' +
                ", nome='" + nome + '\'' +
                ", observacoes='" + observacoes + '\'' +
                ", codigoMarca='" + codigoMarca + '\'' +
                ", nomeMarca='" + nomeMarca + '\'' +
                '}';
    }
}