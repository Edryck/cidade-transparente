package br.edu.utfpr.cidadetransparente.repository;

import br.edu.utfpr.cidadetransparente.domain.Resposta;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Sem filtro de município próprio: só é chamado depois que o service confirmou o acesso à manifestação. */
public interface RespostaRepository extends JpaRepository<Resposta, Long> {

    @EntityGraph(attributePaths = "secretaria")
    List<Resposta> findByManifestacaoIdOrderByRespondidaEmAscIdAsc(Long manifestacaoId);

    Optional<Resposta> findFirstByManifestacaoIdOrderByRespondidaEmDescIdDesc(Long manifestacaoId);

    /** Resposta pelo id, desde que a manifestação seja do município do token. */
    @EntityGraph(attributePaths = {"manifestacao", "manifestacao.tipoManifestacao", "manifestacao.cidadao"})
    Optional<Resposta> findByIdAndManifestacaoMunicipioId(Long id, Long municipioId);
}
