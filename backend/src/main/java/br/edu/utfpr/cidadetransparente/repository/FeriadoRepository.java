package br.edu.utfpr.cidadetransparente.repository;

import br.edu.utfpr.cidadetransparente.domain.Feriado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FeriadoRepository extends JpaRepository<Feriado, Long> {

    /** Calendário do município: feriados nacionais mais os dele. Nunca os de outro município. */
    @Query("select f from Feriado f where f.municipio is null or f.municipio.id = :municipioId order by f.data")
    List<Feriado> calendarioDo(@Param("municipioId") Long municipioId);

    /** Só feriado municipal do próprio município: nacional não se altera pela API. */
    Optional<Feriado> findByIdAndMunicipioId(Long id, Long municipioId);
}
