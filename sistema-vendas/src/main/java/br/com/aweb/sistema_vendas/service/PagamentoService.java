package br.com.aweb.sistema_vendas.service;

import br.com.aweb.sistema_vendas.model.*;
import br.com.aweb.sistema_vendas.repository.PagamentoRepository;
import br.com.aweb.sistema_vendas.repository.PedidoRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PagamentoService {

    private final PedidoRepository pedidoRepository;
    private final PagamentoRepository pagamentoRepository;

    @Transactional
    public Pagamento iniciarPagamento(Long pedidoId, FormaPagamento forma) {
        if (forma == null) throw new IllegalArgumentException("Forma de pagamento é obrigatória");

        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));

        if (pedido.getItens().isEmpty()) {
            throw new RuntimeException("Pedido sem itens");
        }
        if (pedido.getStatus() == StatusPedido.CANCELADO) {
            throw new RuntimeException("Pedido cancelado não pode ser pago");
        }

        // Totais atualizados antes do snapshot
        pedido.recalcularTotais();

        // Idempotência simples
        Pagamento existente = pagamentoRepository.findByPedido(pedido).orElse(null);
        if (existente != null) {
            if (existente.getStatus() == StatusPagamento.APROVADO) {
                return existente; // já pago
            }
            existente.setForma(forma);
            existente.snapshotFrom(pedido);
            existente.setStatus(StatusPagamento.PENDENTE);
            existente.setPago(Boolean.FALSE); // ⭐ garantir NOT NULL e coerência
            return pagamentoRepository.save(existente);
        }

        // Novo pagamento
        Pagamento pag = Pagamento.builder()
                .pedido(pedido)
                .forma(forma)
                .status(StatusPagamento.PENDENTE)
                .pago(Boolean.FALSE) // ⭐ garantir NOT NULL e coerência
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

        if (pagamento.getStatus() == StatusPagamento.APROVADO) {
            return pagamento; // idempotente
        }

        if (autorizado) {
            pagamento.setStatus(StatusPagamento.APROVADO);
            pagamento.setPago(Boolean.TRUE);  // ⭐
        } else {
            pagamento.setStatus(StatusPagamento.RECUSADO);
            pagamento.setPago(Boolean.FALSE); // ⭐
        }

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
            // pago continua false enquanto estiver PENDENTE
            if (pagamento.getPago() == null) pagamento.setPago(Boolean.FALSE);
            return pagamentoRepository.save(pagamento);
        }
        return pagamento; // null ou não pendente
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
        pagamento.setPago(Boolean.FALSE); // ⭐ coerência com status
        pagamentoRepository.save(pagamento);
    }
}
