package com.mscompra.mscompra.service.exception;

public class EntidadeNaoEncontradaException extends NegocioException{
    private static final long serialVersionUID = 1L;

    public EntidadeNaoEncontradaException(String mensagem) {
        super(mensagem);
    }
}
