package br.edu.utfpr.cidadetransparente.repository;

import br.edu.utfpr.cidadetransparente.domain.PrazoLegal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PrazoLegalRepository extends JpaRepository<PrazoLegal, Long> {

    /** Regras federais: linhas sem município. */
    List<PrazoLegal> findByMunicipioIsNull();

    /** Sobrescritas de um município. */
    List<PrazoLegal> findByMunicipioId(Long municipioId);

    Optional<PrazoLegal> findByTipoManifestacaoIdAndMunicipioIsNull(Long tipoId);

    Optional<PrazoLegal> findByTipoManifestacaoIdAndMunicipioId(Long tipoId, Long municipioId);
}
