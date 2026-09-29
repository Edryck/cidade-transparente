package br.edu.utfpr.cidadetransparente.repository;

import br.edu.utfpr.cidadetransparente.domain.Cidadao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CidadaoRepository extends JpaRepository<Cidadao, Long> {

    boolean existsByMunicipioIdAndCpf(Long municipioId, String cpf);

    boolean existsByMunicipioIdAndCpfAndIdNot(Long municipioId, String cpf, Long id);

    Optional<Cidadao> findByUsuarioId(Long usuarioId);
}
