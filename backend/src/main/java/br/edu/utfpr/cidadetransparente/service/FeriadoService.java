package br.edu.utfpr.cidadetransparente.service;

import br.edu.utfpr.cidadetransparente.config.UsuarioAutenticado;
import br.edu.utfpr.cidadetransparente.domain.Feriado;
import br.edu.utfpr.cidadetransparente.dto.FeriadoRequest;
import br.edu.utfpr.cidadetransparente.dto.FeriadoResponse;
import br.edu.utfpr.cidadetransparente.dto.FeriadoResponse.Ambito;
import br.edu.utfpr.cidadetransparente.exception.ApiException;
import br.edu.utfpr.cidadetransparente.repository.FeriadoRepository;
import br.edu.utfpr.cidadetransparente.repository.MunicipioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.MonthDay;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/** Calendário de dias sem expediente de cada município, usado na contagem de todos os prazos. */
@Service
@RequiredArgsConstructor
public class FeriadoService {

    private final FeriadoRepository feriadoRepository;
    private final MunicipioRepository municipioRepository;

    /**
     * Dia sem expediente no município: sábado, domingo, feriado nacional ou feriado/ponto facultativo
     * cadastrado pela prefeitura (Lei 9.784, art. 66, § 1º).
     */
    @Transactional(readOnly = true)
    public Predicate<LocalDate> semExpediente(Long municipioId) {
        List<Feriado> feriados = feriadoRepository.calendarioDo(municipioId);
        return dia -> dia.getDayOfWeek() == DayOfWeek.SATURDAY || dia.getDayOfWeek() == DayOfWeek.SUNDAY
                || feriados.stream().anyMatch(f -> f.cai(dia));
    }

    /** Ocorrências no ano (nacionais e do município), em ordem de data. */
    @Transactional(readOnly = true)
    public List<FeriadoResponse> listar(Integer ano) {
        int anoConsultado = ano == null ? FluxoManifestacao.hoje().getYear() : ano;
        return feriadoRepository.calendarioDo(UsuarioAutenticado.atual().municipioId()).stream()
                .map(f -> ocorrencia(f, anoConsultado))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(FeriadoResponse::data))
                .toList();
    }

    @Transactional(readOnly = true)
    public FeriadoResponse buscar(Long id) {
        Feriado f = feriadoRepository.findById(id)
                .filter(x -> x.getMunicipio() == null
                        || x.getMunicipio().getId().equals(UsuarioAutenticado.atual().municipioId()))
                .orElseThrow(() -> ApiException.naoEncontrado("Feriado não encontrado"));
        return paraResposta(f, f.getData());
    }

    /**
     * Um feriado a mais só adia prazos futuros e o prazo de recurso ainda aberto: as datas limite já gravadas
     * nas manifestações não mudam.
     */
    @Transactional
    public FeriadoResponse criar(FeriadoRequest request) {
        Long municipioId = UsuarioAutenticado.atual().municipioId();
        boolean anual = Boolean.TRUE.equals(request.anual());
        feriadoRepository.calendarioDo(municipioId).stream()
                // Se um dos dois repete todo ano, basta coincidir dia e mês; senão, a data exata
                .filter(f -> anual || f.isAnual()
                        ? MonthDay.from(f.getData()).equals(MonthDay.from(request.data()))
                        : f.getData().equals(request.data()))
                .findFirst()
                .ifPresent(f -> {
                    throw ApiException.conflito("Já há feriado " + (f.getMunicipio() == null ? "nacional" : "municipal")
                            + " nesse dia: " + f.getDescricao());
                });

        Feriado f = new Feriado();
        f.setMunicipio(municipioRepository.getReferenceById(municipioId));
        f.setData(request.data());
        f.setAnual(anual);
        f.setDescricao(request.descricao().trim());
        feriadoRepository.save(f);
        return paraResposta(f, f.getData());
    }

    /** Feriado é configuração, não registro da administração: pode ser excluído. Nacional não (404). */
    @Transactional
    public void excluir(Long id) {
        Feriado f = feriadoRepository.findByIdAndMunicipioId(id, UsuarioAutenticado.atual().municipioId())
                .orElseThrow(() -> ApiException.naoEncontrado("Feriado municipal não encontrado"));
        feriadoRepository.delete(f);
    }

    private static FeriadoResponse ocorrencia(Feriado f, int ano) {
        if (!f.isAnual()) {
            return f.getData().getYear() == ano ? paraResposta(f, f.getData()) : null;
        }
        MonthDay md = MonthDay.from(f.getData());
        return md.isValidYear(ano) ? paraResposta(f, md.atYear(ano)) : null;
    }

    private static FeriadoResponse paraResposta(Feriado f, LocalDate data) {
        return new FeriadoResponse(f.getId(), data, f.getDescricao(), f.isAnual(),
                f.getMunicipio() == null ? Ambito.NACIONAL : Ambito.MUNICIPAL);
    }
}
