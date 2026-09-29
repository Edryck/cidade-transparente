export const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
export const PROTOCOLO = /^\d{4}-\d{7}-\d{6}$/

/** CPF com dígitos verificadores válidos; aceita com ou sem pontuação (mesma regra do backend). */
export function cpfValido(valor: string): boolean {
  const d = valor.replace(/\D/g, '')
  if (d.length !== 11 || /^(\d)\1{10}$/.test(d)) return false
  const digito = (n: number) => {
    const soma = d.slice(0, n).split('').reduce((s, c, i) => s + Number(c) * (n + 1 - i), 0)
    const resto = (soma * 10) % 11
    return resto === 10 ? 0 : resto
  }
  return digito(9) === Number(d[9]) && digito(10) === Number(d[10])
}
