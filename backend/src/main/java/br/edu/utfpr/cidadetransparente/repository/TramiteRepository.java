package br.edu.utfpr.cidadetransparente.repository;

import br.edu.utfpr.cidadetransparente.domain.Tramite;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Sem filtro de município próprio: só é chamado depois que o service confirmou o acesso à manifestação. */
public interface TramiteRepository extends JpaRepository<Tramite, Long> {

    @EntityGraph(attributePaths = {"usuario", "usuario.perfil", "secretariaDestino"})
    List<Tramite> findByManifestacaoIdOrderByRegistradoEmAscIdAsc(Long manifestacaoId);
}
