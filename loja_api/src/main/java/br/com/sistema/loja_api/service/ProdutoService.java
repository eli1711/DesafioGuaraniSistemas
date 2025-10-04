package br.com.sistema.loja_api.service;

import br.com.sistema.loja_api.dto.ProdutoDTO;
import br.com.sistema.loja_api.model.Produto;
import br.com.sistema.loja_api.repository.ProdutoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProdutoService {

    @Autowired
    private ProdutoRepository repository;

    public List<ProdutoDTO> listarTodos() {
        return repository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public ProdutoDTO salvar(ProdutoDTO dto) {
        Produto produto = toEntity(dto);
        return toDTO(repository.save(produto));
    }

    public ProdutoDTO buscarPorId(Long id) {
        Produto produto = repository.findById(id).orElseThrow();
        return toDTO(produto);
    }

    public void deletar(Long id) {
        repository.deleteById(id);
    }

    private ProdutoDTO toDTO(Produto p) {
        ProdutoDTO dto = new ProdutoDTO();
        dto.setId(p.getId());
        dto.setNome(p.getNome());
        dto.setDescricao(p.getDescricao());
        dto.setPreco(p.getPreco());
        dto.setCategoria(p.getCategoria());
        return dto;
    }

    private Produto toEntity(ProdutoDTO dto) {
        Produto p = new Produto();
        p.setId(dto.getId());
        p.setNome(dto.getNome());
        p.setDescricao(dto.getDescricao());
        p.setPreco(dto.getPreco());
        p.setCategoria(dto.getCategoria());
        return p;
    }
}
