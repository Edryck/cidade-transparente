package br.edu.utfpr.cidadetransparente.repository;

import br.edu.utfpr.cidadetransparente.domain.Anexo;
import br.edu.utfpr.cidadetransparente.dto.AnexoResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/** Sem filtro de município próprio: só é chamado depois que o service confirmou o acesso à manifestação. */
public interface AnexoRepository extends JpaRepository<Anexo, Long> {

    /** Projeção sem o conteúdo: listar não carrega megabytes de arquivo na memória. */
    @Query("""
            select new br.edu.utfpr.cidadetransparente.dto.AnexoResponse(a.id, a.nomeArquivo, a.contentType, a.tamanho, a.enviadoEm)
            from Anexo a where a.manifestacao.id = :manifestacaoId order by a.enviadoEm, a.id
            """)
    List<AnexoResponse> listarSemConteudo(@Param("manifestacaoId") Long manifestacaoId);

    Optional<Anexo> findByIdAndManifestacaoId(Long id, Long manifestacaoId);
}
