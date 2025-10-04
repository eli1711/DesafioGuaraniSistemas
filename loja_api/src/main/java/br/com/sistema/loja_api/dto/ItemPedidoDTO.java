package br.com.sistema.loja_api.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ItemPedidoDTO {
    private Long produtoId;
    private Integer quantidade;
    private BigDecimal precoUnitario;
    private BigDecimal totalItem;
}
