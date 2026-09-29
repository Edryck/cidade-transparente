package br.edu.utfpr.cidadetransparente.controller;

import br.edu.utfpr.cidadetransparente.dto.ConsultaPublicaResponse;
import br.edu.utfpr.cidadetransparente.service.ManifestacaoService;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/protocolos")
@RequiredArgsConstructor
@Tag(name = "Consulta pública")
public class ProtocoloController {

    private final ManifestacaoService manifestacaoService;

    /**
     * Acompanhamento sem login. A chave vai no header, não na URL: URLs ficam em logs de servidor, proxies e
     * histórico do navegador. Protocolo sozinho não basta, porque é sequencial e previsível.
     */
    @GetMapping("/{protocolo}")
    @SecurityRequirements
    public ConsultaPublicaResponse consultar(@PathVariable String protocolo,
                                            @RequestHeader("X-Chave-Acesso") String chave) {
        return manifestacaoService.consultaPublica(protocolo, chave);
    }
}
