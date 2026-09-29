// Datas sempre no fuso de Brasília, que é o fuso em que a API conta os prazos.
const FUSO = 'America/Sao_Paulo'
const DIAS_SEMANA = ['domingo', 'segunda-feira', 'terça-feira', 'quarta-feira', 'quinta-feira', 'sexta-feira', 'sábado']

/** Dia de hoje em Brasília, no formato AAAA-MM-DD. */
export function hojeISO(): string {
  return new Intl.DateTimeFormat('en-CA', { timeZone: FUSO }).format(new Date())
}

/** Dia (AAAA-MM-DD) em que um instante da API caiu, em Brasília. */
export function diaDoInstante(instante: string): string {
  return new Intl.DateTimeFormat('en-CA', { timeZone: FUSO }).format(new Date(instante))
}

/** Dias corridos entre duas datas AAAA-MM-DD (positivo se b é depois de a). */
export function diasEntre(a: string, b: string): number {
  return Math.round((Date.parse(b + 'T00:00:00Z') - Date.parse(a + 'T00:00:00Z')) / 86_400_000)
}

/** AAAA-MM-DD para dd/mm/aaaa. */
export function dataCurta(dia: string): string {
  const [ano, mes, d] = dia.split('-')
  return `${d}/${mes}/${ano}`
}

export function diaDaSemana(dia: string): string {
  return DIAS_SEMANA[new Date(dia + 'T12:00:00Z').getUTCDay()]
}

/** Instante da API para dd/mm/aaaa, hh:mm (Brasília). */
export function dataHora(instante: string): string {
  return new Intl.DateTimeFormat('pt-BR', {
    timeZone: FUSO, day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit',
  }).format(new Date(instante))
}

export function plural(n: number, singular: string, pluralTexto: string): string {
  return `${n} ${n === 1 ? singular : pluralTexto}`
}
