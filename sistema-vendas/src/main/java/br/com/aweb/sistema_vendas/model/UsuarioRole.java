package br.com.aweb.sistema_vendas.model;

public enum UsuarioRole {
    ADMIN,     // acesso a tudo
    CLIENTE,   // acesso a produtos e pedidos
    OPERADOR   // acesso a cadastro de produtos e pedidos
}
