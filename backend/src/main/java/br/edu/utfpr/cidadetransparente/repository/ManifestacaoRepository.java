package br.edu.utfpr.cidadetransparente.repository;

import br.edu.utfpr.cidadetransparente.domain.Manifestacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ManifestacaoRepository extends JpaRepository<Manifestacao, Long>, JpaSpecificationExecutor<Manifestacao> {

    /** Listagem: a Specification sempre inclui o município do token (montada no ManifestacaoService). */
    @Override
    @EntityGraph(attributePaths = {"tipoManifestacao", "secretaria"})
    Page<Manifestacao> findAll(Specification<Manifestacao> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"tipoManifestacao", "secretaria", "cidadao", "municipio"})
    Optional<Manifestacao> findByIdAndMunicipioId(Long id, Long municipioId);

    /** Consulta pública: sem município no token, a credencial é a chave de acesso, conferida no service. */
    @EntityGraph(attributePaths = {"tipoManifestacao", "secretaria", "municipio"})
    Optional<Manifestacao> findByProtocolo(String protocolo);

    /**
     * Próximo sequencial do protocolo em um único comando atômico: duas aberturas simultâneas nunca recebem
     * o mesmo número, e o contador reinicia sozinho a cada ano.
     */
    @Query(value = """
            INSERT INTO protocolo_sequencia (municipio_id, ano, ultimo) VALUES (:municipioId, :ano, 1)
            ON CONFLICT (municipio_id, ano) DO UPDATE SET ultimo = protocolo_sequencia.ultimo + 1
            RETURNING ultimo
            """, nativeQuery = true)
    int proximoSequencial(@Param("municipioId") Long municipioId, @Param("ano") int ano);
}
