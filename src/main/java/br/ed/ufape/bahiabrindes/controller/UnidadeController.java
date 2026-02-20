package br.ed.ufape.bahiabrindes.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/estoque/unidades")
public class UnidadeController {

    @GetMapping
    public ResponseEntity<List<String>> listar() {
        return ResponseEntity.ok(List.of("UND", "KG", "G", "M", "CM", "L", "ML"));
    }
}

