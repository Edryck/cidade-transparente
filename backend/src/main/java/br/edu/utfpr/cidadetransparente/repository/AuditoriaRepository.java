package br.edu.utfpr.cidadetransparente.repository;

import br.edu.utfpr.cidadetransparente.domain.Auditoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;

public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {

    /** Expurgo do registro de acesso: o IP some, a trilha de quem fez o quê continua (Marco Civil art. 15). */
    @Modifying
    @Query("update Auditoria a set a.ip = null where a.ip is not null and a.registradoEm < :limite")
    int apagarIpsAnterioresA(@Param("limite") OffsetDateTime limite);
}
