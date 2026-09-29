package br.edu.utfpr.cidadetransparente.repository;

import br.edu.utfpr.cidadetransparente.domain.RelatorioGestao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Relatórios e as consultas agregadas que os alimentam. Toda consulta filtra pelo município e pelo período
 * de abertura; nenhuma devolve dado pessoal (o id do cidadão só serve para contar solicitantes distintos).
 */
public interface RelatorioGestaoRepository extends JpaRepository<RelatorioGestao, Long> {

    Optional<RelatorioGestao> findByMunicipioIdAndAno(Long municipioId, int ano);

    List<RelatorioGestao> findByMunicipioIdAndPublicadoEmIsNotNullOrderByAnoDesc(Long municipioId);

    /** id, tipo, categoria, sigla da secretaria, status, id do cidadão, prorrogada, abertura, data limite. */
    @Query("""
            select m.id, t.nome, t.categoria, s.sigla, m.status, c.id, m.prorrogada, m.dataAbertura, m.dataLimite
            from Manifestacao m join m.tipoManifestacao t left join m.secretaria s left join m.cidadao c
            where m.municipio.id = :municipioId and m.dataAbertura >= :inicio and m.dataAbertura < :fim
            """)
    List<Object[]> manifestacoesDoPeriodo(@Param("municipioId") Long municipioId,
                                          @Param("inicio") OffsetDateTime inicio, @Param("fim") OffsetDateTime fim);

    /** id da manifestação, data da resposta, resultado LAI; em ordem cronológica. */
    @Query("""
            select r.manifestacao.id, r.respondidaEm, r.resultadoLai from Resposta r
            where r.manifestacao.municipio.id = :municipioId
              and r.manifestacao.dataAbertura >= :inicio and r.manifestacao.dataAbertura < :fim
            order by r.respondidaEm, r.id
            """)
    List<Object[]> respostasDoPeriodo(@Param("municipioId") Long municipioId,
                                      @Param("inicio") OffsetDateTime inicio, @Param("fim") OffsetDateTime fim);

    @Query("""
            select rc.status from Recurso rc
            where rc.resposta.manifestacao.municipio.id = :municipioId
              and rc.resposta.manifestacao.dataAbertura >= :inicio and rc.resposta.manifestacao.dataAbertura < :fim
            """)
    List<Object> statusDosRecursosDoPeriodo(@Param("municipioId") Long municipioId,
                                            @Param("inicio") OffsetDateTime inicio, @Param("fim") OffsetDateTime fim);
}
