package br.edu.utfpr.cidadetransparente.repository;

import br.edu.utfpr.cidadetransparente.domain.Recurso;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RecursoRepository extends JpaRepository<Recurso, Long> {

    /** Instância única (LAI art. 15): no máximo um recurso por manifestação, qualquer que seja a resposta. */
    Optional<Recurso> findByRespostaManifestacaoId(Long manifestacaoId);

    /** Recurso pelo id, desde que a manifestação seja do município do token. */
    @EntityGraph(attributePaths = {"resposta", "resposta.manifestacao"})
    Optional<Recurso> findByIdAndRespostaManifestacaoMunicipioId(Long id, Long municipioId);
}
