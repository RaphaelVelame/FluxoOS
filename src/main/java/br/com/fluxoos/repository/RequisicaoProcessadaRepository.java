package br.com.fluxoos.repository;

import br.com.fluxoos.entity.RequisicaoProcessada;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequisicaoProcessadaRepository extends JpaRepository<RequisicaoProcessada, RequisicaoProcessada.Chave> {}
