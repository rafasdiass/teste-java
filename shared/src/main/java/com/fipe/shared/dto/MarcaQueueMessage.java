package com.fipe.shared.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO para mensagens da fila de processamento de marcas
 */
public class MarcaQueueMessage {

    @JsonProperty("codigoMarca")
    @NotNull
    public String codigoMarca;

    @JsonProperty("nomeMarca")
    @NotBlank
    public String nomeMarca;

    @JsonProperty("tipoVeiculo")
    @NotBlank
    public String tipoVeiculo;

    @JsonProperty("timestamp")
    public Long timestamp;

    public MarcaQueueMessage() {
        this.timestamp = System.currentTimeMillis();
    }

    public MarcaQueueMessage(String codigoMarca, String nomeMarca, String tipoVeiculo) {
        this();
        this.codigoMarca = codigoMarca;
        this.nomeMarca = nomeMarca;
        this.tipoVeiculo = tipoVeiculo;
    }

    @Override
    public String toString() {
        return "MarcaQueueMessage{" +
                "codigoMarca='" + codigoMarca + '\'' +
                ", nomeMarca='" + nomeMarca + '\'' +
                ", tipoVeiculo='" + tipoVeiculo + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}