// Desenha as réguas de prazo a partir de data-atributos, com a mesma regra da API (Lei 9.784, art. 66):
// conta dias corridos a partir do dia seguinte à abertura; fim de semana e feriado aparecem como dias sem
// expediente. No frontend definitivo esses dados virão da API (dataAbertura, dataLimite, /feriados).

const NACIONAIS = ["01-01", "04-21", "05-01", "09-07", "10-12", "11-02", "11-15", "11-20", "12-25"];
const DIAS_SEMANA = ["dom", "seg", "ter", "qua", "qui", "sex", "sáb"];

const data = (iso) => new Date(iso + "T12:00:00");
const iso = (d) => d.toISOString().slice(0, 10);
const curta = (d) => d.toLocaleDateString("pt-BR", { day: "2-digit", month: "2-digit" });
const hoje = () => data(document.body.dataset.hoje);
const diasEntre = (a, b) => Math.round((b - a) / 86400000);

function semExpediente(d, feriados) {
    return d.getDay() === 0 || d.getDay() === 6 || feriados.has(iso(d)) || NACIONAIS.includes(iso(d).slice(5));
}

function situacao(limite) {
    const faltam = diasEntre(hoje(), limite);
    if (faltam < 0) return { classe: "vencido", texto: `Vencida há ${-faltam} ${-faltam === 1 ? "dia" : "dias"}` };
    if (faltam === 0) return { classe: "atencao", texto: "Vence hoje" };
    if (faltam <= 7) return { classe: "atencao", texto: `Faltam ${faltam} ${faltam === 1 ? "dia" : "dias"}` };
    return { classe: "ok", texto: `Faltam ${faltam} dias` };
}

function desenharRegua(el) {
    const abertura = data(el.dataset.abertura);
    const limite = data(el.dataset.limite);
    const limiteOriginal = el.dataset.limiteOriginal ? data(el.dataset.limiteOriginal) : null;
    const feriados = new Set((el.dataset.feriados || "").split(",").filter(Boolean));
    const h = hoje();

    const dias = document.createElement("div");
    dias.className = "regua-dias";
    dias.setAttribute("role", "img");
    for (let d = new Date(abertura); d < limite; ) {
        d.setDate(d.getDate() + 1);
        const dia = document.createElement("div");
        dia.className = "regua-dia";
        if (d <= h) dia.classList.add("passado");
        if (semExpediente(d, feriados)) dia.classList.add(feriados.has(iso(d)) || NACIONAIS.includes(iso(d).slice(5)) ? "feriado" : "sem-expediente");
        if (limiteOriginal && d > limiteOriginal) dia.classList.add("prorrogacao");
        if (iso(d) === iso(limite)) dia.classList.add("vencimento");
        if (iso(d) === iso(h)) dia.classList.add("hoje");
        dia.title = `${curta(d)} (${DIAS_SEMANA[d.getDay()]})`;
        dias.appendChild(dia);
    }
    // Respondida: o prazo parou de correr; o destaque diz se a resposta veio dentro dele
    const respondida = el.dataset.respondida ? data(el.dataset.respondida) : null;
    const s = respondida
        ? respondida <= limite
            ? { classe: "ok", texto: `Respondida em ${curta(respondida)}, dentro do prazo` }
            : { classe: "vencido", texto: `Respondida em ${curta(respondida)}, fora do prazo` }
        : situacao(limite);
    dias.setAttribute("aria-label", `Prazo de ${diasEntre(abertura, limite)} dias, de ${curta(abertura)} a ${curta(limite)}. ${s.texto}.`);

    const destaque = document.createElement("p");
    destaque.className = `regua-destaque ${s.classe}`;
    destaque.textContent = s.texto;

    const rotulos = document.createElement("div");
    rotulos.className = "regua-rotulos";
    rotulos.innerHTML = `<span>Aberta em <strong>${curta(abertura)}</strong></span>
        <span>Vence em <strong>${curta(limite)} (${DIAS_SEMANA[limite.getDay()]})</strong></span>`;

    const legenda = document.createElement("div");
    legenda.className = "regua-legenda";
    legenda.innerHTML = `<span><i style="background:var(--carimbo)"></i>dias corridos</span>
        <span><i style="background:var(--fim-de-semana)"></i>fim de semana</span>
        <span><i class="regua-dia feriado"></i>feriado</span>
        ${limiteOriginal ? '<span><i class="regua-dia prorrogacao"></i>prorrogação</span>' : ""}
        <span>a linha vertical marca hoje</span>`;

    el.replaceChildren(destaque, dias, rotulos, legenda);
}

function desenharMiniRegua(el) {
    const abertura = data(el.dataset.abertura);
    const limite = data(el.dataset.limite);
    const total = Math.max(1, diasEntre(abertura, limite));
    const usado = Math.min(1, Math.max(0, diasEntre(abertura, hoje()) / total));
    const s = situacao(limite);
    el.classList.add(s.classe);
    el.innerHTML = `<b style="width:${Math.round(usado * 100)}%"></b>`;
    el.setAttribute("role", "img");
    el.setAttribute("aria-label", `${Math.round(usado * 100)}% do prazo consumido`);
    const texto = el.parentElement.querySelector("small");
    if (texto) {
        texto.textContent = s.texto;
        texto.className = `selo ${s.classe}`;
    }
}

document.querySelectorAll(".regua[data-limite]").forEach(desenharRegua);
document.querySelectorAll(".mini-regua[data-limite]").forEach(desenharMiniRegua);
