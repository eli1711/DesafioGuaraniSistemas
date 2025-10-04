package br.com.sistema.loja_api.controller;

import br.com.sistema.loja_api.dto.PedidoDTO;
import br.com.sistema.loja_api.service.PedidoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    @Autowired
    private PedidoService pedidoService;

    @PostMapping
    public PedidoDTO criar(@RequestBody PedidoDTO dto) {
        return pedidoService.criarPedido(dto);
    }

    @GetMapping
    public List<PedidoDTO> listar() {
        return pedidoService.listarTodos();
    }
}
