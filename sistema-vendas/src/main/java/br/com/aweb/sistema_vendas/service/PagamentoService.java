package br.com.aweb.sistema_vendas.service;

import br.com.aweb.sistema_vendas.model.*;
import br.com.aweb.sistema_vendas.repository.PagamentoRepository;
import br.com.aweb.sistema_vendas.repository.PedidoRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class PagamentoService {

    private final PedidoRepository pedidoRepository;
    private final PagamentoRepository pagamentoRepository;

    @Transactional
    public Pagamento iniciarPagamento(Long pedidoId, FormaPagamento forma) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));

        if (pedido.getItens().isEmpty()) {
            throw new RuntimeException("Pedido sem itens");
        }
        if (pedido.getStatus() == StatusPedido.CANCELADO) {
            throw new RuntimeException("Pedido cancelado não pode ser pago");
        }

        // idempotência simples: se já existe aprovado, não deixa pagar de novo
        Pagamento existente = pagamentoRepository.findByPedido(pedido).orElse(null);
        if (existente != null) {
            if (existente.getStatus() == StatusPagamento.APROVADO) {
                return existente; // já pago
            }
            // se pendente/recusado/cancelado, vamos atualizar o snapshot e reusar o mesmo registro
            existente.setForma(forma);
            pedido.recalcularTotais();
            existente.snapshotFrom(pedido);
            existente.setStatus(StatusPagamento.PENDENTE);
            return pagamentoRepository.save(existente);
        }

        // novo pagamento
        pedido.recalcularTotais();
        Pagamento pag = Pagamento.builder()
                .pedido(pedido)
                .forma(forma)
                .status(StatusPagamento.PENDENTE)
                .build();
        pag.snapshotFrom(pedido);
        return pagamentoRepository.save(pag);
    }

    @Transactional
    public Pagamento confirmarPagamento(Long pedidoId, boolean autorizado, String referenciaExterna, String detalhes) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));
        Pagamento pagamento = pagamentoRepository.findByPedido(pedido)
                .orElseThrow(() -> new RuntimeException("Pagamento não iniciado"));

        if (pagamento.getStatus() == StatusPagamento.APROVADO) return pagamento; // idempotente

        // aqui você chamaria o PSP e decidiria 'autorizado'
        pagamento.setStatus(autorizado ? StatusPagamento.APROVADO : StatusPagamento.RECUSADO);
        pagamento.setReferenciaExterna(referenciaExterna);
        pagamento.setDetalhes(detalhes);

        return pagamentoRepository.save(pagamento);
    }

    @Transactional
    public Pagamento atualizarSnapshotSePendente(Long pedidoId) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));
        Pagamento pagamento = pagamentoRepository.findByPedido(pedido).orElse(null);
        if (pagamento != null && pagamento.getStatus() == StatusPagamento.PENDENTE) {
            pedido.recalcularTotais();
            pagamento.snapshotFrom(pedido);
            return pagamentoRepository.save(pagamento);
        }
        return pagamento; // null ou não pendente (sem alteração)
    }
    // PagamentoService.java
@Transactional
public Pagamento garantirPagamentoPendente(Long pedidoId) {
    Pedido pedido = pedidoRepository.findById(pedidoId)
            .orElseThrow(() -> new IllegalArgumentException("Pedido não encontrado"));
    return pagamentoRepository.findByPedidoId(pedidoId)
            .orElseGet(() -> {
                Pagamento pg = new Pagamento();
                pg.setPedido(pedido);
                pg.setStatus(StatusPagamento.PENDENTE);
                pg.setForma(FormaPagamento.BOLETO); // ou deixe null até o usuário escolher
                pg.setValorFinal(pedido.getValorTotal());
                return pagamentoRepository.save(pg);
            });
}


    @Transactional
    public void cancelarPagamento(Long pedidoId) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));
        Pagamento pagamento = pagamentoRepository.findByPedido(pedido)
                .orElseThrow(() -> new RuntimeException("Pagamento não encontrado"));
        if (pagamento.getStatus() == StatusPagamento.APROVADO) {
            throw new RuntimeException("Pagamento aprovado não pode ser cancelado");
        }
        pagamento.setStatus(StatusPagamento.CANCELADO);
        pagamentoRepository.save(pagamento);
    }
}
