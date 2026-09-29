package br.edu.utfpr.cidadetransparente.repository;

import br.edu.utfpr.cidadetransparente.domain.Secretaria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Toda consulta recebe o municipioId do token: não existe busca de secretaria só por id. */
public interface SecretariaRepository extends JpaRepository<Secretaria, Long> {

    List<Secretaria> findByMunicipioIdOrderByNomeAsc(Long municipioId);

    Optional<Secretaria> findByIdAndMunicipioId(Long id, Long municipioId);

    boolean existsByMunicipioIdAndSigla(Long municipioId, String sigla);

    boolean existsByMunicipioIdAndSiglaAndIdNot(Long municipioId, String sigla, Long id);
}
