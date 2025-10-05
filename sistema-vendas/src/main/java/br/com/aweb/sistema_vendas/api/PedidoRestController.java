package br.com.aweb.sistema_vendas.api;

import br.com.aweb.sistema_vendas.api.dto.pedido.*;
import br.com.aweb.sistema_vendas.api.mapper.PedidoMapper;
import br.com.aweb.sistema_vendas.model.Pedido;
import br.com.aweb.sistema_vendas.model.StatusPedido;
import br.com.aweb.sistema_vendas.service.ClienteService;
import br.com.aweb.sistema_vendas.service.PedidoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
@RequiredArgsConstructor
public class PedidoRestController {

    private final PedidoService pedidoService;
    private final ClienteService clienteService;

    // GET /api/pedidos?status=&dataIni=&dataFim=&valorMin=&valorMax=&page=&size=&sort=
    @GetMapping
    public Page<PedidoResponse> listar(
        @RequestParam(required = false) StatusPedido status,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataIni,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataFim,
        @RequestParam(required = false) BigDecimal valorMin,
        @RequestParam(required = false) BigDecimal valorMax,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "dataHora,desc") String sort
    ) {
        Sort s = Sort.by(sort.split(",")[0]).descending();
        if (sort.endsWith(",asc")) s = Sort.by(sort.split(",")[0]).ascending();
        Pageable pageable = PageRequest.of(page, size, s);

        List<Pedido> base = (status == null)
            ? pedidoService.listarTodosOrdenado()
            : pedidoService.listarPorStatusOrdenado(status);

        List<Pedido> filtrado = base.stream()
            .filter(p -> dataIni == null || (p.getDataHora()!=null && !p.getDataHora().isBefore(dataIni)))
            .filter(p -> dataFim == null  || (p.getDataHora()!=null && !p.getDataHora().isAfter(dataFim)))
            .filter(p -> valorMin == null || (p.getValorTotal()!=null && p.getValorTotal().compareTo(valorMin) >= 0))
            .filter(p -> valorMax == null || (p.getValorTotal()!=null && p.getValorTotal().compareTo(valorMax) <= 0))
            .toList();

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filtrado.size());
        List<PedidoResponse> content = filtrado.subList(Math.min(start, end), end).stream()
            .map(PedidoMapper::toResponse).toList();

        return new PageImpl<>(content, pageable, filtrado.size());
    }

    @GetMapping("/{id}")
    public PedidoResponse detalhar(@PathVariable Long id) {
        Pedido p = pedidoService.buscarPorId(id)
            .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));
        return PedidoMapper.toResponse(p);
    }

    @PostMapping
    public ResponseEntity<PedidoResponse> criar(@Valid @RequestBody PedidoCreateRequest req) {
        var cliente = clienteService.buscarPorId(req.clienteId())
            .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));

        Pedido pedido = pedidoService.criarPedido(cliente);

        if (req.frete()!=null || req.desconto()!=null) {
            pedidoService.aplicarFreteEDesconto(
                pedido.getId(),
                req.frete()==null ? BigDecimal.ZERO : req.frete(),
                req.desconto()==null ? BigDecimal.ZERO : req.desconto()
            );
        }

        req.itens().forEach(it -> pedidoService.adicionarOuSomarItem(pedido.getId(), it.produtoId(), it.quantidade()));

        Pedido atualizado = pedidoService.buscarPorId(pedido.getId()).orElseThrow();
        return ResponseEntity.status(201).body(PedidoMapper.toResponse(atualizado));
    }

    // Atualiza campos do resumo (frete/desconto)
    @PutMapping("/{id}")
    public PedidoResponse atualizar(@PathVariable Long id,
                                    @RequestParam(defaultValue = "0") BigDecimal frete,
                                    @RequestParam(defaultValue = "0") BigDecimal desconto) {
        Pedido p = pedidoService.aplicarFreteEDesconto(id, frete, desconto);
        return PedidoMapper.toResponse(p);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        pedidoService.cancelarPedido(id);
        return ResponseEntity.noContent().build();
    }

    // ------- Itens -------
    @PostMapping("/{id}/itens")
    public PedidoResponse adicionarItem(@PathVariable Long id, @Valid @RequestBody ItemPedidoRequest req) {
        pedidoService.adicionarOuSomarItem(id, req.produtoId(), req.quantidade());
        return PedidoMapper.toResponse(pedidoService.buscarPorId(id).orElseThrow());
    }

    @PutMapping("/{id}/itens/{itemId}")
    public PedidoResponse atualizarItem(@PathVariable Long id, @PathVariable Long itemId, @RequestParam Integer quantidade) {
        pedidoService.atualizarQuantidadeItem(id, itemId, quantidade);
        return PedidoMapper.toResponse(pedidoService.buscarPorId(id).orElseThrow());
    }

    @DeleteMapping("/{id}/itens/{itemId}")
    public PedidoResponse removerItem(@PathVariable Long id, @PathVariable Long itemId) {
        pedidoService.removerItem(id, itemId);
        return PedidoMapper.toResponse(pedidoService.buscarPorId(id).orElseThrow());
    }
}
