package com.fipe.shared.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO para representar uma marca de veículo
 */
public class MarcaDTO {

    @JsonProperty("codigo")
    @NotNull
    public String codigo;

    @JsonProperty("nome")
    @NotBlank
    public String nome;

    public MarcaDTO() {}

    public MarcaDTO(String codigo, String nome) {
        this.codigo = codigo;
        this.nome = nome;
    }

    @Override
    public String toString() {
        return "MarcaDTO{" +
                "codigo='" + codigo + '\'' +
                ", nome='" + nome + '\'' +
                '}';
    }
}