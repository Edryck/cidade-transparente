package br.edu.utfpr.cidadetransparente.service;

import br.edu.utfpr.cidadetransparente.domain.AcaoAuditoria;
import br.edu.utfpr.cidadetransparente.domain.AcaoManifestacao;
import br.edu.utfpr.cidadetransparente.domain.Anexo;
import br.edu.utfpr.cidadetransparente.domain.Manifestacao;
import br.edu.utfpr.cidadetransparente.dto.AnexoResponse;
import br.edu.utfpr.cidadetransparente.exception.ApiException;
import br.edu.utfpr.cidadetransparente.repository.AnexoRepository;
import br.edu.utfpr.cidadetransparente.repository.UsuarioRepository;
import br.edu.utfpr.cidadetransparente.service.ManifestacaoService.Acesso;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.List;

/**
 * Anexos da manifestação. Aceita só PDF, PNG e JPEG, identificados pelos primeiros bytes do arquivo:
 * extensão e content-type vêm do cliente e são fáceis de falsificar. O limite de 5 MB é garantido pelo
 * Spring (413) e por CHECK no banco.
 */
@Service
@RequiredArgsConstructor
public class AnexoService {

    private record Formato(String contentType, byte[] assinatura) {
    }

    private static final List<Formato> FORMATOS = List.of(
            new Formato("application/pdf", new byte[]{'%', 'P', 'D', 'F', '-'}),
            new Formato("image/png", new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A}),
            new Formato("image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}));

    private final ManifestacaoService manifestacaoService;
    private final AnexoRepository anexoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaService auditoria;

    @Transactional
    public AnexoResponse enviar(Long manifestacaoId, MultipartFile arquivo) {
        Acesso acesso = manifestacaoService.acessar(manifestacaoId);
        manifestacaoService.exigir(AcaoManifestacao.ANEXO, acesso);
        byte[] conteudo = lerBytes(arquivo);
        if (conteudo.length == 0) {
            throw ApiException.requisicaoInvalida("Arquivo vazio");
        }
        String contentType = FORMATOS.stream()
                .filter(f -> conteudo.length >= f.assinatura().length
                        && Arrays.equals(conteudo, 0, f.assinatura().length, f.assinatura(), 0, f.assinatura().length))
                .map(Formato::contentType).findFirst()
                .orElseThrow(() -> ApiException.requisicaoInvalida("Formato não aceito: envie PDF, PNG ou JPEG"));

        Manifestacao m = acesso.manifestacao();
        Anexo anexo = new Anexo();
        anexo.setManifestacao(m);
        anexo.setNomeArquivo(nomeSeguro(arquivo.getOriginalFilename()));
        anexo.setContentType(contentType);
        anexo.setTamanho(conteudo.length);
        anexo.setConteudo(conteudo);
        anexo.setEnviadoPor(usuarioRepository.getReferenceById(acesso.ator().usuarioId()));
        anexoRepository.save(anexo);
        auditoria.registrar(AcaoAuditoria.ANEXO_ENVIADO, m.getMunicipio().getId(), acesso.ator().usuarioId(),
                "Anexo", anexo.getId(), null);
        return new AnexoResponse(anexo.getId(), anexo.getNomeArquivo(), contentType, anexo.getTamanho(),
                anexo.getEnviadoEm());
    }

    @Transactional(readOnly = true)
    public List<AnexoResponse> listar(Long manifestacaoId) {
        manifestacaoService.acessar(manifestacaoId);
        return anexoRepository.listarSemConteudo(manifestacaoId);
    }

    /** Não é readOnly: o download é registrado na trilha de auditoria. */
    @Transactional
    public Anexo baixar(Long manifestacaoId, Long anexoId) {
        Acesso acesso = manifestacaoService.acessar(manifestacaoId);
        Anexo anexo = anexoRepository.findByIdAndManifestacaoId(anexoId, manifestacaoId)
                .orElseThrow(() -> ApiException.naoEncontrado("Anexo não encontrado"));
        auditoria.registrar(AcaoAuditoria.ANEXO_BAIXADO, acesso.manifestacao().getMunicipio().getId(),
                acesso.ator().usuarioId(), "Anexo", anexoId, null);
        return anexo;
    }

    private static byte[] lerBytes(MultipartFile arquivo) {
        try {
            return arquivo.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao ler o arquivo enviado", e);
        }
    }

    /** Só o nome, sem caminho nem caracteres de controle, no máximo 255 caracteres. */
    private static String nomeSeguro(String original) {
        if (original == null || original.isBlank()) {
            return "anexo";
        }
        String nome = original.replace('\\', '/');
        nome = nome.substring(nome.lastIndexOf('/') + 1).replaceAll("\\p{Cntrl}", "").trim();
        if (nome.isEmpty()) {
            return "anexo";
        }
        return nome.length() > 255 ? nome.substring(nome.length() - 255) : nome;
    }
}
