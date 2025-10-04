package br.com.sistema.loja_api.service;

import br.com.sistema.loja_api.dto.ItemPedidoDTO;
import br.com.sistema.loja_api.dto.PedidoDTO;

import br.com.sistema.loja_api.model.ItemPedido;
import br.com.sistema.loja_api.model.Pedido;
import br.com.sistema.loja_api.model.Produto;
import br.com.sistema.loja_api.model.StatusPedido;
import br.com.sistema.loja_api.repository.PedidoRepository;
import br.com.sistema.loja_api.repository.ProdutoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PedidoService {

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    public PedidoDTO criarPedido(PedidoDTO dto) {
        Pedido pedido = new Pedido();
        pedido.setDataCriacao(LocalDateTime.now());
        pedido.setStatus(StatusPedido.PENDENTE);

        // Forma de pagamento recebida do DTO
        pedido.setFormaPagamento(dto.getFormaPagamento());

        // Converter itens DTO para entidade
        List<ItemPedido> itens = dto.getItens().stream().map(itemDTO -> {
            Produto produto = produtoRepository.findById(itemDTO.getProdutoId()).orElseThrow();
            ItemPedido item = new ItemPedido();
            item.setProduto(produto);
            item.setQuantidade(itemDTO.getQuantidade());
            item.setPrecoUnitario(produto.getPreco());
            item.setTotalItem(produto.getPreco().multiply(BigDecimal.valueOf(itemDTO.getQuantidade())));
            item.setPedido(pedido);
            return item;
        }).collect(Collectors.toList());

        pedido.setItens(itens);

        // Calcular total dos produtos
        BigDecimal totalProdutos = itens.stream()
                .map(ItemPedido::getTotalItem)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Definir frete fixo (pode ser dinâmica)
        BigDecimal frete = new BigDecimal("20.00");
        pedido.setFrete(frete);

        // Calcular desconto (ex: acima de 200 reais)
        BigDecimal desconto = BigDecimal.ZERO;
        if (totalProdutos.compareTo(new BigDecimal("200.00")) > 0) {
            desconto = totalProdutos.multiply(new BigDecimal("0.10"));
        }
        pedido.setDesconto(desconto);

        // Total final
        pedido.setValorTotal(totalProdutos.add(frete).subtract(desconto));

        Pedido salvo = pedidoRepository.save(pedido);
        return toDTO(salvo);
    }

    public List<PedidoDTO> listarTodos() {
        return pedidoRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    private PedidoDTO toDTO(Pedido pedido) {
        PedidoDTO dto = new PedidoDTO();
        dto.setId(pedido.getId());
        dto.setDataCriacao(pedido.getDataCriacao());
        dto.setStatus(pedido.getStatus());
        dto.setValorTotal(pedido.getValorTotal());
        dto.setFrete(pedido.getFrete());
        dto.setDesconto(pedido.getDesconto());
        dto.setFormaPagamento(pedido.getFormaPagamento());
        dto.setItens(pedido.getItens().stream().map(item -> {
            ItemPedidoDTO itemDTO = new ItemPedidoDTO();
            itemDTO.setProdutoId(item.getProduto().getId());
            itemDTO.setQuantidade(item.getQuantidade());
            itemDTO.setPrecoUnitario(item.getPrecoUnitario());
            itemDTO.setTotalItem(item.getTotalItem());
            return itemDTO;
        }).collect(Collectors.toList()));
        return dto;
    }
}
