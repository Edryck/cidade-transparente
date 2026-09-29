// Contraste WCAG entre duas cores hexadecimais. Usado para aceitar ou recusar a cor de destaque do município.
function luminancia(hex: string): number {
  const valor = hex.replace('#', '')
  const canais = [0, 2, 4].map((i) => parseInt(valor.slice(i, i + 2), 16) / 255)
  const [r, g, b] = canais.map((c) => (c <= 0.03928 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4))
  return 0.2126 * r + 0.7152 * g + 0.0722 * b
}

export function contraste(a: string, b: string): number {
  const [claro, escuro] = [luminancia(a), luminancia(b)].sort((x, y) => y - x)
  return (claro + 0.05) / (escuro + 0.05)
}

export function hexValido(cor: string | null | undefined): cor is string {
  return !!cor && /^#[0-9a-fA-F]{6}$/.test(cor)
}
