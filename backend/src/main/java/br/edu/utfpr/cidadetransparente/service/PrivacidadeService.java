package br.edu.utfpr.cidadetransparente.service;

import br.edu.utfpr.cidadetransparente.config.UsuarioAutenticado;
import br.edu.utfpr.cidadetransparente.domain.AcaoAuditoria;
import br.edu.utfpr.cidadetransparente.domain.Cidadao;
import br.edu.utfpr.cidadetransparente.domain.Municipio;
import br.edu.utfpr.cidadetransparente.domain.Perfil;
import br.edu.utfpr.cidadetransparente.domain.Usuario;
import br.edu.utfpr.cidadetransparente.dto.AlteracaoSenhaRequest;
import br.edu.utfpr.cidadetransparente.dto.AvisoPrivacidadeResponse;
import br.edu.utfpr.cidadetransparente.dto.AvisoPrivacidadeResponse.Direito;
import br.edu.utfpr.cidadetransparente.dto.AvisoPrivacidadeResponse.Encarregado;
import br.edu.utfpr.cidadetransparente.dto.AvisoPrivacidadeResponse.Tratamento;
import br.edu.utfpr.cidadetransparente.dto.EncarregadoRequest;
import br.edu.utfpr.cidadetransparente.dto.EncerramentoContaResponse;
import br.edu.utfpr.cidadetransparente.dto.MinhaContaRequest;
import br.edu.utfpr.cidadetransparente.dto.MinhaContaResponse;
import br.edu.utfpr.cidadetransparente.exception.ApiException;
import br.edu.utfpr.cidadetransparente.repository.CidadaoRepository;
import br.edu.utfpr.cidadetransparente.repository.MunicipioRepository;
import br.edu.utfpr.cidadetransparente.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/**
 * Obrigações da LGPD que viram funcionalidade: aviso de privacidade (arts. 9º e 23, I), encarregado (art. 41)
 * e direitos do titular sobre a própria conta (art. 18). O inventário completo está em docs/lgpd.md.
 */
@Service
@RequiredArgsConstructor
public class PrivacidadeService {

    /** Muda sempre que o texto do aviso mudar: permite provar qual versão estava vigente (art. 6º, X). */
    private static final String VERSAO_AVISO = "2026-09-29";

    private static final String OPERADOR = "Plataforma Cidade Transparente, operadora (LGPD art. 5º, VII): trata os "
            + "dados apenas conforme as instruções da prefeitura (art. 39). O perfil de administração da plataforma "
            + "não tem acesso a dados de cidadãos.";

    private static final List<Tratamento> TRATAMENTOS = List.of(
            new Tratamento(
                    "Cadastro e autenticação de cidadãos para registrar e acompanhar manifestações",
                    List.of("nome", "e-mail", "senha (guardada só como hash)", "CPF (opcional)", "telefone (opcional)"),
                    "Execução de política pública prevista em lei (LGPD art. 7º, III): Lei 13.460/2017 e Lei 12.527/2011",
                    "Enquanto a conta existir. Após o encerramento, a identificação é conservada junto às manifestações "
                            + "(LGPD art. 16, I)"),
            new Tratamento(
                    "Registro, encaminhamento e resposta de manifestações de ouvidoria e pedidos de acesso à informação",
                    List.of("identificação do manifestante", "conteúdo da manifestação (pode conter dados sensíveis)",
                            "anexos", "histórico de trâmites"),
                    "Cumprimento de obrigação legal (LGPD art. 7º, II; Lei 13.460/2017, art. 10; Lei 12.527/2011, art. 10). "
                            + "Dados sensíveis: LGPD art. 11, II, a e b",
                    "Conforme a tabela de temporalidade de documentos do município (Lei 8.159/1991, art. 9º). A "
                            + "identificação do manifestante tem acesso restrito por até 100 anos (Lei 12.527/2011, art. 31)"),
            new Tratamento(
                    "Contas da equipe da prefeitura (administradores, ouvidores e servidores)",
                    List.of("nome", "e-mail", "perfil", "secretaria"),
                    "Execução das competências legais do serviço público (LGPD art. 23)",
                    "Conta desativada é conservada para rastrear a autoria dos atos praticados (trâmites e respostas)"));

    private static final List<String> COMPARTILHAMENTO = List.of(
            "Os dados não são vendidos nem compartilhados para fins comerciais.",
            "A identidade de quem se manifesta é informação pessoal de acesso restrito (Lei 13.460/2017, art. 10, § 7º): "
                    + "só a ouvidoria a vê; as secretarias recebem a manifestação sem identificação.",
            "Denúncias podem ser encaminhadas a órgãos de apuração com a identidade preservada. A identidade só é "
                    + "revelada nas hipóteses da Lei 13.608/2018, art. 4º-B, ou por ordem judicial (Lei 12.527/2011, "
                    + "art. 31, § 3º, III).");

    private static final List<Direito> DIREITOS = List.of(
            new Direito("Confirmação da existência de tratamento e acesso aos dados (art. 18, I e II)",
                    "GET /api/v1/minha-conta"),
            new Direito("Correção de dados incompletos, inexatos ou desatualizados (art. 18, III)",
                    "PUT /api/v1/minha-conta"),
            new Direito("Encerramento da conta (art. 18, IV e VI, observado o art. 16)",
                    "DELETE /api/v1/minha-conta. As manifestações são documentos públicos e são conservadas"),
            new Direito("Informação sobre compartilhamento (art. 18, VII)", "Seção 'compartilhamento' deste aviso"),
            new Direito("Anonimização, bloqueio ou eliminação de dados desnecessários ou tratados em desconformidade, "
                    + "e oposição ao tratamento (art. 18, IV e § 2º)", "Pedido ao encarregado, pelo e-mail indicado"),
            new Direito("Petição à Agência Nacional de Proteção de Dados (art. 18, § 1º)", "https://www.gov.br/anpd"));

    private static final String PRAZO_ATENDIMENTO = "Pedidos dos titulares seguem os prazos da Lei de Acesso à "
            + "Informação (LGPD art. 23, § 3º): até 20 dias, prorrogáveis por mais 10 mediante justificativa.";

    private static final String INCIDENTES = "Incidentes de segurança que possam gerar risco ou dano relevante são "
            + "comunicados à ANPD e aos titulares afetados em até 3 dias úteis (LGPD art. 48; Resolução CD/ANPD nº 15/2024).";

    private final MunicipioRepository municipioRepository;
    private final UsuarioRepository usuarioRepository;
    private final CidadaoRepository cidadaoRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaService auditoria;

    @Transactional(readOnly = true)
    public AvisoPrivacidadeResponse aviso(Long municipioId) {
        Municipio m = municipioRepository.findById(municipioId)
                .orElseThrow(() -> ApiException.naoEncontrado("Município não encontrado"));
        Encarregado encarregado = m.getEncarregadoNome() == null
                ? null : new Encarregado(m.getEncarregadoNome(), m.getEncarregadoEmail());
        return new AvisoPrivacidadeResponse(VERSAO_AVISO,
                "Prefeitura Municipal de " + m.getNome() + "/" + m.getUf() + ", controladora (LGPD art. 5º, VI)",
                OPERADOR, encarregado, TRATAMENTOS, COMPARTILHAMENTO, DIREITOS, PRAZO_ATENDIMENTO, INCIDENTES);
    }

    /** O encarregado é indicado pela controladora (art. 41): só o ADMIN da própria prefeitura altera. */
    @Transactional
    public void definirEncarregado(Long municipioId, EncarregadoRequest request) {
        if (!municipioId.equals(UsuarioAutenticado.atual().municipioId())) {
            throw ApiException.naoEncontrado("Município não encontrado");
        }
        Municipio municipio = municipioRepository.getReferenceById(municipioId);
        municipio.setEncarregadoNome(request.nome().trim());
        municipio.setEncarregadoEmail(request.email().trim().toLowerCase(Locale.ROOT));
    }

    @Transactional(readOnly = true)
    public MinhaContaResponse minhaConta() {
        Usuario usuario = usuarioLogado();
        return paraResposta(usuario, cidadaoRepository.findByUsuarioId(usuario.getId()).orElse(null));
    }

    @Transactional
    public MinhaContaResponse corrigirMinhaConta(MinhaContaRequest request) {
        Usuario usuario = usuarioLogado();
        Cidadao cidadao = cidadaoRepository.findByUsuarioId(usuario.getId()).orElse(null);
        if (cidadao == null && (request.cpf() != null || request.telefone() != null)) {
            throw ApiException.requisicaoInvalida("CPF e telefone só se aplicam a contas de cidadão");
        }

        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (usuarioRepository.existsByEmailIgnoreCaseAndIdNot(email, usuario.getId())) {
            throw ApiException.conflito("Já existe uma conta com este e-mail");
        }
        usuario.setNome(request.nome().trim());
        usuario.setEmail(email);

        if (cidadao != null) {
            String cpf = request.cpf() == null ? null : request.cpf().replaceAll("\\D", "");
            if (cpf != null && cidadaoRepository.existsByMunicipioIdAndCpfAndIdNot(
                    cidadao.getMunicipio().getId(), cpf, cidadao.getId())) {
                throw ApiException.conflito("Já existe um cidadão com este CPF neste município");
            }
            // Mantém o cadastro do cidadão igual ao da conta (LGPD art. 6º, V, qualidade dos dados)
            cidadao.setNome(usuario.getNome());
            cidadao.setEmail(email);
            cidadao.setCpf(cpf);
            cidadao.setTelefone(request.telefone());
        }
        return paraResposta(usuario, cidadao);
    }

    /**
     * Encerra a conta do cidadão: bloqueia o login, mas não elimina os dados, porque as manifestações são
     * documentos públicos. A resposta informa as razões de direito, como exige o art. 18, § 4º, II.
     */
    @Transactional
    public EncerramentoContaResponse encerrarMinhaConta() {
        Usuario usuario = usuarioLogado();
        if (!Perfil.CIDADAO.equals(usuario.getPerfil().getNome())) {
            throw ApiException.proibido("Contas da equipe são desativadas pelo ADMIN da prefeitura");
        }
        usuario.setAtivo(false);
        return new EncerramentoContaResponse(
                "Conta encerrada: o acesso com este e-mail foi bloqueado. Os dados de identificação continuam "
                        + "guardados junto às suas manifestações, com acesso restrito.",
                List.of("As manifestações são documentos públicos e só podem ser eliminadas com autorização da "
                                + "instituição arquivística (Lei 8.159/1991, art. 9º).",
                        "A LGPD autoriza conservar os dados para cumprimento de obrigação legal (art. 16, I).",
                        "A identificação de quem se manifesta tem acesso restrito por até 100 anos "
                                + "(Lei 12.527/2011, art. 31, § 1º, I; Lei 13.460/2017, art. 10, § 7º)."));
    }

    /**
     * Troca de senha do próprio usuário, qualquer perfil. Exige a senha atual e registra a troca na auditoria
     * (LGPD arts. 37 e 46). Tokens já emitidos continuam válidos até expirar (no máximo 2h): o JWT não tem estado.
     */
    @Transactional
    public void alterarSenha(AlteracaoSenhaRequest request) {
        Usuario usuario = usuarioLogado();
        if (!passwordEncoder.matches(request.senhaAtual(), usuario.getSenhaHash())) {
            throw ApiException.requisicaoInvalida("Senha atual incorreta");
        }
        if (passwordEncoder.matches(request.novaSenha(), usuario.getSenhaHash())) {
            throw ApiException.requisicaoInvalida("A nova senha precisa ser diferente da atual");
        }
        usuario.setSenhaHash(passwordEncoder.encode(request.novaSenha()));
        auditoria.registrar(AcaoAuditoria.SENHA_ALTERADA,
                usuario.getMunicipio() == null ? null : usuario.getMunicipio().getId(),
                usuario.getId(), "Usuario", usuario.getId(), null);
    }

    private Usuario usuarioLogado() {
        return usuarioRepository.findWithPerfilAndMunicipioById(UsuarioAutenticado.atual().id())
                .orElseThrow(() -> ApiException.naoEncontrado("Conta não encontrada"));
    }

    private static MinhaContaResponse paraResposta(Usuario u, Cidadao c) {
        Municipio m = u.getMunicipio();
        return new MinhaContaResponse(u.getId(), u.getNome(), u.getEmail(), u.getPerfil().getNome(),
                m == null ? null : m.getId(), m == null ? null : m.getNome(), u.isAtivo(), u.getCriadoEm(),
                c == null ? null : c.getCpf(), c == null ? null : c.getTelefone());
    }
}
