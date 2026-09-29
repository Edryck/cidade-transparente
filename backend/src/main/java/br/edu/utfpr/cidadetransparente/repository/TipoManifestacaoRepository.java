package br.edu.utfpr.cidadetransparente.repository;

import br.edu.utfpr.cidadetransparente.domain.TipoManifestacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Tipos são nacionais (definidos em lei): não pertencem a município. */
public interface TipoManifestacaoRepository extends JpaRepository<TipoManifestacao, Long> {

    List<TipoManifestacao> findAllByOrderByIdAsc();
}
