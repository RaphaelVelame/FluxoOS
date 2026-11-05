package br.com.fluxoos.controller;

import br.com.fluxoos.service.NaoEncontradoException;
import br.com.fluxoos.service.AutenticacaoNegadaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingRequestHeaderException;
import java.util.Map;

@RestControllerAdvice
public class TratamentoErros {
    private static final Logger LOG = LoggerFactory.getLogger(TratamentoErros.class);
    @ExceptionHandler(NaoEncontradoException.class) ResponseEntity<?> ausente(RuntimeException e) { return erro(404, e.getMessage()); }
    @ExceptionHandler(AutenticacaoNegadaException.class) ResponseEntity<?> login(RuntimeException e) { return erro(401, e.getMessage()); }
    @ExceptionHandler(SecurityException.class) ResponseEntity<?> proibido(RuntimeException e) { return erro(403, e.getMessage()); }
    @ExceptionHandler(IllegalArgumentException.class) ResponseEntity<?> invalido(RuntimeException e) { return erro(400, e.getMessage()); }
    @ExceptionHandler(IllegalStateException.class) ResponseEntity<?> conflito(RuntimeException e) { return erro(409, e.getMessage()); }
    @ExceptionHandler(DataIntegrityViolationException.class) ResponseEntity<?> duplicado(RuntimeException e) {
        return erro(409, "Registro duplicado ou operação incompatível com os dados existentes. Atualize a página.");
    }
    @ExceptionHandler(ConcurrencyFailureException.class) ResponseEntity<?> concorrencia(RuntimeException e) {
        return erro(409, "Outro atendimento está alterando estes dados. Tente novamente.");
    }
    @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<?> validacao(MethodArgumentNotValidException e) {
        String mensagem = e.getBindingResult().getFieldErrors().stream().map(f -> f.getField() + ": " + f.getDefaultMessage())
                .findFirst().orElse("Confira os campos informados.");
        return erro(400, mensagem);
    }
    @ExceptionHandler({HttpMessageNotReadableException.class, MissingRequestHeaderException.class}) ResponseEntity<?> formato(Exception e) {
        return erro(400, "Dados inválidos ou cabeçalho obrigatório ausente.");
    }
    @ExceptionHandler(Exception.class) ResponseEntity<?> inesperado(Exception e) {
        LOG.error("Falha interna de tipo {}", e.getClass().getSimpleName());
        return erro(500, "Não foi possível concluir a operação. Tente novamente.");
    }
    private ResponseEntity<?> erro(int status, String mensagem) { return ResponseEntity.status(status).body(Map.of("mensagem", mensagem)); }
}
