/**
 * Armazenamento local do Fluxo.
 *
 * O app roda dentro de um WebView, onde o localStorage pode estar indisponível
 * ou estourar a cota (o Fluxo guarda áudio em base64 e artigos inteiros). Sem
 * tratamento, um QuotaExceeded lançado dentro de um handler derruba a ação do
 * usuário sem aviso. Aqui toda leitura e escrita é tolerante a falha.
 */

/** Lê e desserializa uma chave. Devolve `fallback` se não existir ou falhar. */
export function loadStored<T>(key: string, fallback: T): T {
  try {
    const raw = localStorage.getItem(key)
    return raw ? (JSON.parse(raw) as T) : fallback
  } catch {
    return fallback
  }
}

/** Resultado de uma tentativa de escrita. */
export interface SaveResult {
  ok: boolean
  /** Presente quando a escrita falhou, para exibir ao usuário. */
  error?: string
}

/**
 * Serializa e grava uma chave. Nunca lança: devolve o motivo da falha para a
 * interface decidir o que mostrar.
 */
export function saveStored(key: string, value: unknown): SaveResult {
  try {
    localStorage.setItem(key, JSON.stringify(value))
    return { ok: true }
  } catch (err) {
    const quota =
      err instanceof DOMException &&
      (err.name === 'QuotaExceededError' || err.name === 'NS_ERROR_DOM_QUOTA_REACHED')
    return {
      ok: false,
      error: quota
        ? 'Sem espaço no aparelho. Apague algum item guardado.'
        : 'Não foi possível salvar neste aparelho.',
    }
  }
}

/** Remove uma chave, ignorando falhas. */
export function removeStored(key: string): void {
  try {
    localStorage.removeItem(key)
  } catch {
    /* nada a fazer */
  }
}
