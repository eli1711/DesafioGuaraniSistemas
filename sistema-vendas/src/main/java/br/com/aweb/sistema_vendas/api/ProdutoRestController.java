package br.com.aweb.sistema_vendas.api;

import br.com.aweb.sistema_vendas.api.dto.produto.ProdutoRequest;
import br.com.aweb.sistema_vendas.api.dto.produto.ProdutoResponse;
import br.com.aweb.sistema_vendas.api.mapper.ProdutoMapper;
import br.com.aweb.sistema_vendas.model.Produto;
import br.com.aweb.sistema_vendas.service.ProdutoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/produtos")
@RequiredArgsConstructor
public class ProdutoRestController {

    private final ProdutoService produtoService;

    // GET /api/produtos?nome=&page=&size=&sort=nome,asc
    @GetMapping
    public Page<ProdutoResponse> listar(
        @RequestParam(required = false) String nome,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "id,desc") String sort
    ) {
        Sort s = Sort.by(sort.split(",")[0]).descending();
        if (sort.endsWith(",asc")) s = Sort.by(sort.split(",")[0]).ascending();
        Pageable pageable = PageRequest.of(page, size, s);

        List<Produto> origem = (nome == null || nome.isBlank())
            ? produtoService.listarTodos()
            : produtoService.buscarPorNome(nome);

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), origem.size());
        List<ProdutoResponse> content = origem.subList(Math.min(start, end), end).stream()
            .map(ProdutoMapper::toResponse).toList();

        return new PageImpl<>(content, pageable, origem.size());
    }

    @GetMapping("/{id}")
    public ProdutoResponse detalhar(@PathVariable Long id) {
        Produto p = produtoService.buscarPorId(id)
            .orElseThrow(() -> new RuntimeException("Produto não encontrado"));
        return ProdutoMapper.toResponse(p);
    }

    @PostMapping
    public ResponseEntity<ProdutoResponse> criar(@Valid @RequestBody ProdutoRequest body) {
        Produto p = ProdutoMapper.toEntity(body);
        Produto salvo = produtoService.salvar(p);
        return ResponseEntity.status(201).body(ProdutoMapper.toResponse(salvo));
    }

    @PutMapping("/{id}")
    public ProdutoResponse atualizar(@PathVariable Long id, @Valid @RequestBody ProdutoRequest body) {
        Produto existente = produtoService.buscarPorId(id)
            .orElseThrow(() -> new RuntimeException("Produto não encontrado"));
        ProdutoMapper.copyToEntity(body, existente);
        return ProdutoMapper.toResponse(produtoService.salvar(existente));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        produtoService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
