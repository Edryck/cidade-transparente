package br.edu.utfpr.cidadetransparente.repository;

import br.edu.utfpr.cidadetransparente.domain.Municipio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Município é o próprio tenant: não filtra por municipioId, o acesso é controlado por perfil. */
public interface MunicipioRepository extends JpaRepository<Municipio, Long> {

    List<Municipio> findByAtivoTrueOrderByNomeAsc();

    List<Municipio> findAllByOrderByNomeAsc();

    boolean existsByCodigoIbge(String codigoIbge);
}
