package br.edu.utfpr.cidadetransparente.service;

import br.edu.utfpr.cidadetransparente.domain.AcaoAuditoria;
import br.edu.utfpr.cidadetransparente.domain.Auditoria;
import br.edu.utfpr.cidadetransparente.repository.AuditoriaRepository;
import br.edu.utfpr.cidadetransparente.repository.MunicipioRepository;
import br.edu.utfpr.cidadetransparente.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.OffsetDateTime;

/**
 * Registro de acesso (Marco Civil, art. 15) e trilha de operações (LGPD art. 37). Grava na mesma transação da
 * ação: se a ação falha e é desfeita, o registro dela também é.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditoriaService {

    private final AuditoriaRepository auditoriaRepository;
    private final MunicipioRepository municipioRepository;
    private final UsuarioRepository usuarioRepository;

    /** Marco Civil, art. 15: 6 meses. Configurável só para testes; em produção, não reduzir. */
    @Value("${auditoria.retencao-ip-meses:6}")
    private int retencaoIpMeses;

    @Transactional
    public void registrar(AcaoAuditoria acao, Long municipioId, Long usuarioId, String entidade, Long entidadeId,
                          String detalhe) {
        Auditoria a = new Auditoria();
        a.setAcao(acao);
        a.setMunicipio(municipioId == null ? null : municipioRepository.getReferenceById(municipioId));
        a.setUsuario(usuarioId == null ? null : usuarioRepository.getReferenceById(usuarioId));
        a.setEntidade(entidade);
        a.setEntidadeId(entidadeId);
        a.setDetalhe(detalhe);
        a.setIp(ipDaRequisicao());
        auditoriaRepository.save(a);
    }

    /**
     * Expurgo diário: apaga o IP dos registros com mais de 6 meses. Guardar além do exigido violaria a
     * necessidade (LGPD art. 6º, III) e o término do tratamento (art. 16).
     */
    @Scheduled(cron = "${auditoria.expurgo-cron:0 30 3 * * *}", zone = "America/Sao_Paulo")
    @Transactional
    public void expurgarIpsVencidos() {
        int apagados = auditoriaRepository.apagarIpsAnterioresA(OffsetDateTime.now().minusMonths(retencaoIpMeses));
        log.info("Expurgo do registro de acesso: {} IP(s) com mais de {} meses apagado(s)", apagados, retencaoIpMeses);
    }

    /**
     * IP de quem fez a requisição. Atrás de proxy reverso, configurar server.forward-headers-strategy para ler
     * o X-Forwarded-For do proxy confiável; sem proxy, o header não é lido porque o cliente poderia forjá-lo.
     */
    private static String ipDaRequisicao() {
        return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes atributos
                ? atributos.getRequest().getRemoteAddr() : null;
    }
}
