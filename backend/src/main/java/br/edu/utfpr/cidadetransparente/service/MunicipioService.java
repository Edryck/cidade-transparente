package br.edu.utfpr.cidadetransparente.service;

import br.edu.utfpr.cidadetransparente.dto.MunicipioResumo;
import br.edu.utfpr.cidadetransparente.repository.MunicipioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MunicipioService {

    private final MunicipioRepository municipioRepository;

    @Transactional(readOnly = true)
    public List<MunicipioResumo> listarAtivos() {
        return municipioRepository.findByAtivoTrueOrderByNomeAsc().stream()
                .map(MunicipioResumo::de)
                .toList();
    }
}
