package br.edu.utfpr.cidadetransparente.repository;

import br.edu.utfpr.cidadetransparente.domain.Usuario;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /** Usado só no login, antes de existir token: por isso não filtra por município. */
    @EntityGraph(attributePaths = {"perfil", "municipio"})
    Optional<Usuario> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    /** Equipe da prefeitura: o filtro por perfil impede que o ADMIN enxergue contas de cidadãos (LGPD art. 6º, III). */
    @EntityGraph(attributePaths = {"perfil", "secretaria"})
    List<Usuario> findByMunicipioIdAndPerfilNomeInOrderByNomeAsc(Long municipioId, Collection<String> perfis);

    @EntityGraph(attributePaths = {"perfil", "secretaria"})
    Optional<Usuario> findByIdAndMunicipioIdAndPerfilNomeIn(Long id, Long municipioId, Collection<String> perfis);

    /** Conta do próprio usuário logado (direitos do titular). */
    @EntityGraph(attributePaths = {"perfil", "municipio"})
    Optional<Usuario> findWithPerfilAndMunicipioById(Long id);
}
