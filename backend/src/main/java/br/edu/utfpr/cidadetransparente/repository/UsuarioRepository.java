package br.edu.utfpr.cidadetransparente.repository;

import br.edu.utfpr.cidadetransparente.domain.Usuario;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /** Usado só no login, antes de existir token: por isso não filtra por município. */
    @EntityGraph(attributePaths = {"perfil", "municipio"})
    Optional<Usuario> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    @EntityGraph(attributePaths = {"perfil", "secretaria"})
    List<Usuario> findByMunicipioIdOrderByNomeAsc(Long municipioId);

    @EntityGraph(attributePaths = {"perfil", "secretaria"})
    List<Usuario> findByMunicipioIdAndPerfilNomeOrderByNomeAsc(Long municipioId, String perfil);

    @EntityGraph(attributePaths = {"perfil", "secretaria"})
    Optional<Usuario> findByIdAndMunicipioId(Long id, Long municipioId);
}
