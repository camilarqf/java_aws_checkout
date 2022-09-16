package com.mscompra.mscompra.service;

import com.mscompra.mscompra.models.Pedido;
import com.mscompra.mscompra.repository.PedidoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PedidoService {

    @Autowired
    PedidoRepository pedidoRepository;

    public Pedido salvar (Pedido pedido){
        return pedidoRepository.save(pedido);
    }


}
