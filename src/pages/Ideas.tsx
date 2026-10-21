import { useEffect, useState } from 'react'
import { Plus, Search, X, Trash2, BookOpen, Lightbulb, ArrowRight, LoaderCircle } from 'lucide-react'
import { cn } from '@/lib/utils'

interface Idea {
  id: string
  content: string
  analysis: string
  congruence: 'alta' | 'media' | 'baixa'
  createdAt: number
  local: boolean
}

interface Article {
  id: string
  title: string
  content: string
  lang: string
  translated?: string
  wordCount: number
}

const IDEAS_KEY = 'fluxo_ideas'
const ARTICLES_KEY = 'fluxo_articles'

const CONGRUENCE_STYLES = {
  alta: 'text-emerald-400 bg-emerald-400/10 border-emerald-400/20',
  media: 'text-amber-400 bg-amber-400/10 border-amber-400/20',
  baixa: 'text-rose-400 bg-rose-400/10 border-rose-400/20',
} as const

const CONGRUENCE_LABELS = { alta: 'Alta', media: 'Média', baixa: 'Baixa' } as const

const STOPWORDS = new Set([
  'a', 'o', 'e', 'de', 'da', 'do', 'das', 'dos', 'em', 'um', 'uma', 'que', 'para', 'com', 'por',
  'no', 'na', 'nos', 'nas', 'ao', 'aos', 'é', 'ser', 'se', 'como', 'mais', 'mas', 'ou', 'não',
  'eu', 'você', 'ele', 'ela', 'isso', 'este', 'esta', 'esse', 'essa', 'são', 'há', 'the', 'of',
])

/** Extrai os temas principais de um texto (análise local, sem IA). */
function extractThemes(text: string, limit = 5): string[] {
  const counts = new Map<string, number>()
  for (const raw of text.toLowerCase().split(/[^a-zà-ú0-9]+/)) {
    const word = raw.trim()
    if (word.length < 4 || STOPWORDS.has(word)) continue
    counts.set(word, (counts.get(word) || 0) + 1)
  }
  return [...counts.entries()]
    .sort((a, b) => b[1] - a[1])
    .slice(0, limit)
    .map(([word]) => word)
}

/** Análise local: compara a ideia com os artigos guardados. */
function analyzeIdea(content: string, articles: Article[]) {
  const themes = extractThemes(content)
  const haystack = articles.map((a) => a.content.toLowerCase()).join(' ')
  const hits = themes.filter((t) => haystack.includes(t))
  const ratio = themes.length ? hits.length / themes.length : 0
  const congruence: Idea['congruence'] = ratio >= 0.66 ? 'alta' : ratio >= 0.33 ? 'media' : 'baixa'

  const lines: string[] = []
  lines.push(`Temas detectados: ${themes.join(', ') || 'nenhum claro'}.`)
  if (hits.length) lines.push(`Já no acervo: ${hits.join(', ')}.`)
  const missing = themes.filter((t) => !hits.includes(t))
  if (missing.length) lines.push(`Falta material sobre: ${missing.join(', ')}.`)
  lines.push('Conecte: como esses temas se ligam entre si?')
  lines.push('Atrito: o que faria isso falhar rápido?')
  lines.push(
    `Congruência ${CONGRUENCE_LABELS[congruence]}: ${Math.round(ratio * 100)}% dos temas aparecem no seu acervo (${articles.length} artigo${articles.length === 1 ? '' : 's'} guardado${articles.length === 1 ? '' : 's'}).`,
  )

  return { analysis: lines.join('\n'), congruence, themes, missing }
}

function load<T>(key: string, fallback: T): T {
  try {
    const raw = localStorage.getItem(key)
    return raw ? (JSON.parse(raw) as T) : fallback
  } catch {
    return fallback
  }
}

/** Busca artigos na Wikipedia (API pública) e devolve o texto. */
async function searchWikipedia(query: string, lang: string): Promise<Article[]> {
  const url =
    `https://${lang}.wikipedia.org/w/api.php?action=query&format=json&origin=*` +
    `&generator=search&gsrsearch=${encodeURIComponent(query)}&gsrlimit=5` +
    `&prop=extracts|info&exintro=1&explaintext=1&inprop=url`
  const res = await fetch(url)
  if (!res.ok) throw new Error('Wikipedia indisponível')
  const data = (await res.json()) as {
    query?: { pages?: Record<string, { title: string; extract?: string }> }
  }
  const pages = data.query?.pages ? Object.values(data.query.pages) : []
  return pages
    .filter((p) => p.extract && p.extract.trim().length > 0)
    .map((p) => ({
      id: `${lang}_${p.title}`,
      title: p.title,
      content: p.extract!.trim(),
      lang,
      wordCount: p.extract!.trim().split(/\s+/).length,
    }))
}

export function Ideas() {
  const [ideas, setIdeas] = useState<Idea[]>([])
  const [articles, setArticles] = useState<Article[]>([])
  const [showIdeaModal, setShowIdeaModal] = useState(false)
  const [showWikiModal, setShowWikiModal] = useState(false)
  const [draft, setDraft] = useState('')
  const [query, setQuery] = useState('')
  const [results, setResults] = useState<Article[]>([])
  const [searching, setSearching] = useState(false)
  const [searchError, setSearchError] = useState('')
  const [selectedIdea, setSelectedIdea] = useState<Idea | null>(null)
  const [selectedArticle, setSelectedArticle] = useState<Article | null>(null)

  useEffect(() => {
    setIdeas(load<Idea[]>(IDEAS_KEY, []))
    setArticles(load<Article[]>(ARTICLES_KEY, []))
  }, [])

  const persistIdeas = (next: Idea[]) => {
    setIdeas(next)
    localStorage.setItem(IDEAS_KEY, JSON.stringify(next))
  }

  const persistArticles = (next: Article[]) => {
    setArticles(next)
    localStorage.setItem(ARTICLES_KEY, JSON.stringify(next))
  }

  const saveIdea = () => {
    if (!draft.trim()) return
    const { analysis, congruence } = analyzeIdea(draft, articles)
    const idea: Idea = {
      id: `idea_${Date.now()}`,
      content: draft.trim(),
      analysis,
      congruence,
      createdAt: Date.now(),
      local: true,
    }
    persistIdeas([idea, ...ideas])
    setDraft('')
    setShowIdeaModal(false)
  }

  const removeIdea = (id: string) => {
    persistIdeas(ideas.filter((i) => i.id !== id))
    setSelectedIdea(null)
  }

  const runSearch = async () => {
    if (!query.trim()) return
    setSearching(true)
    setSearchError('')
    setResults([])
    try {
      const found = await searchWikipedia(query.trim(), 'pt')
      setResults(found)
      if (!found.length) setSearchError('Nenhum artigo encontrado.')
    } catch {
      setSearchError('Sem internet no momento. Tente novamente.')
    } finally {
      setSearching(false)
    }
  }

  const saveArticle = (article: Article) => {
    if (articles.some((a) => a.id === article.id)) {
      setShowWikiModal(false)
      return
    }
    persistArticles([article, ...articles])
    setShowWikiModal(false)
    setQuery('')
    setResults([])
  }

  const removeArticle = (id: string) => {
    persistArticles(articles.filter((a) => a.id !== id))
    setSelectedArticle(null)
  }

  const themes = extractThemes(ideas.map((i) => i.content).join(' '), 6)
  const covered = themes.filter((t) => articles.some((a) => a.content.toLowerCase().includes(t)))
  const missing = themes.filter((t) => !covered.includes(t))

  return (
    <div className="space-y-5">
      {/* Captura de ideias */}
      <div className="glass rounded-3xl p-5">
        <div className="flex items-center justify-between gap-2 mb-3">
          <div>
            <h2 className="text-foreground font-medium">Modo Divergente</h2>
            <p className="text-muted-foreground text-xs mt-1">
              A análise roda aqui no aparelho — nada sai daqui.
            </p>
          </div>
          <button
            onClick={() => setShowIdeaModal(true)}
            aria-label="Nova ideia"
            className="w-11 h-11 shrink-0 rounded-full glass-orb flex items-center justify-center text-foreground transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
          >
            <Plus size={20} aria-hidden="true" />
          </button>
        </div>

        {ideas.length === 0 ? (
          <div className="text-center py-8">
            <Lightbulb size={40} className="mx-auto text-muted-foreground/50 mb-3" aria-hidden="true" />
            <p className="text-foreground text-sm font-medium mb-1">Você ainda não registrou ideias.</p>
            <p className="text-muted-foreground text-xs max-w-xs mx-auto">
              Escreva no modo Divergente — a análise sai aqui no aparelho e os temas alimentam o modo Dados.
            </p>
          </div>
        ) : (
          <div className="space-y-3">
            {ideas.map((idea) => (
              <button
                key={idea.id}
                onClick={() => setSelectedIdea(idea)}
                className="w-full text-left glass rounded-2xl p-4 hover:border-emerald-300/40 transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
              >
                <span className="inline-block text-[10px] uppercase tracking-wide text-emerald-200/80 mb-1">
                  Ideia
                </span>
                <p className="text-foreground text-sm line-clamp-3 whitespace-pre-wrap break-words">
                  {idea.content}
                </p>
                <span className="flex items-center justify-between mt-3">
                  <span className={cn('text-xs px-2 py-1 rounded-full border', CONGRUENCE_STYLES[idea.congruence])}>
                    {CONGRUENCE_LABELS[idea.congruence]}
                  </span>
                  <span className="text-[11px] text-muted-foreground">só no aparelho</span>
                </span>
              </button>
            ))}
          </div>
        )}
      </div>

      {/* Análise do histórico */}
      <div className="glass rounded-3xl p-5">
        <h3 className="text-foreground/90 font-medium mb-3">Analisar meu histórico</h3>
        <div className="space-y-2 text-xs">
          <div className="flex items-center justify-between gap-2">
            <span className="text-muted-foreground">Temas que mais voltam:</span>
            <span className="text-foreground text-right">{themes.join(', ') || '—'}</span>
          </div>
          <div className="flex items-center justify-between gap-2">
            <span className="text-muted-foreground">Já tem material guardado:</span>
            <span className="text-primary font-medium text-right">{covered.join(', ') || '—'}</span>
          </div>
          <div className="flex items-center justify-between gap-2">
            <span className="text-muted-foreground">Falta material sobre:</span>
            <span className="text-rose-400 font-medium text-right">{missing.join(', ') || '—'}</span>
          </div>
        </div>
      </div>

      {/* Acervo de artigos */}
      <div className="glass rounded-3xl p-5">
        <div className="flex items-center justify-between gap-2 mb-3">
          <div className="flex items-center gap-2">
            <BookOpen size={18} className="text-emerald-200" aria-hidden="true" />
            <h3 className="text-foreground/90 font-medium">Acervo</h3>
          </div>
          <button
            onClick={() => setShowWikiModal(true)}
            className="min-h-[44px] px-3 rounded-xl glass text-foreground/80 hover:text-foreground text-xs font-medium flex items-center gap-1.5 transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
          >
            <Plus size={14} aria-hidden="true" /> Adicionar
          </button>
        </div>

        {articles.length === 0 ? (
          <p className="text-muted-foreground text-xs">
            Seu acervo está vazio. Busque e baixe um artigo na Wikipedia — tudo fica lido aqui no aparelho,
            sem IA.
          </p>
        ) : (
          <div className="space-y-2">
            {articles.map((article) => (
              <button
                key={article.id}
                onClick={() => setSelectedArticle(article)}
                className="w-full text-left glass rounded-2xl p-4 hover:border-emerald-300/40 transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
              >
                <p className="text-foreground text-sm font-medium line-clamp-2">{article.title}</p>
                <p className="text-muted-foreground text-[11px] mt-2">
                  {article.lang.toUpperCase()} · {article.wordCount} palavras
                </p>
              </button>
            ))}
          </div>
        )}
      </div>

      {/* Modal — nova ideia */}
      {showIdeaModal && (
        <div
          className="fixed inset-0 z-50 flex items-end sm:items-center justify-center bg-black/50 backdrop-blur-sm p-3"
          onClick={() => setShowIdeaModal(false)}
        >
          <div
            className="glass-strong rounded-3xl w-full max-w-lg max-h-[88dvh] overflow-y-auto overscroll-contain p-5"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-start justify-between gap-3 mb-3">
              <div className="min-w-0">
                <span className="inline-block text-[10px] uppercase tracking-wide text-emerald-200/80 mb-1">
                  Nova Ideia
                </span>
                <h4 className="text-foreground font-medium">Escreva sua ideia</h4>
              </div>
              <button
                onClick={() => setShowIdeaModal(false)}
                aria-label="Fechar"
                className="p-2.5 min-h-[44px] text-foreground/50 hover:text-foreground transition rounded-lg"
              >
                <X size={20} aria-hidden="true" />
              </button>
            </div>
            <textarea
              value={draft}
              onChange={(e) => setDraft(e.target.value)}
              placeholder="Digite sua ideia aqui... A análise será gerada localmente."
              className="w-full min-h-[120px] resize-none rounded-xl border border-border bg-transparent px-4 py-3 text-base text-foreground placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
              rows={6}
            />
            <div className="flex items-center justify-end gap-3 mt-4">
              <button
                onClick={() => setShowIdeaModal(false)}
                className="min-h-[44px] px-4 rounded-full text-muted-foreground hover:text-foreground text-sm"
              >
                Cancelar
              </button>
              <button
                onClick={saveIdea}
                disabled={!draft.trim()}
                className="min-h-[44px] px-5 rounded-full bg-emerald-400 text-emerald-950 font-medium disabled:opacity-50 hover:bg-emerald-300 transition"
              >
                Salvar ideia
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal — Wikipedia */}
      {showWikiModal && (
        <div
          className="fixed inset-0 z-50 flex items-end sm:items-center justify-center bg-black/50 backdrop-blur-sm p-3"
          onClick={() => setShowWikiModal(false)}
        >
          <div
            className="glass-strong rounded-3xl w-full max-w-lg max-h-[88dvh] overflow-y-auto overscroll-contain p-5"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-start justify-between gap-3 mb-3">
              <div className="min-w-0">
                <span className="inline-block text-[10px] uppercase tracking-wide text-emerald-200/80 mb-1">
                  Wikipedia
                </span>
                <h4 className="text-foreground font-medium">Buscar artigo</h4>
              </div>
              <button
                onClick={() => setShowWikiModal(false)}
                aria-label="Fechar"
                className="p-2.5 min-h-[44px] text-foreground/50 hover:text-foreground transition rounded-lg"
              >
                <X size={20} aria-hidden="true" />
              </button>
            </div>

            <div className="space-y-4">
              <div className="relative">
                <Search
                  className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground"
                  aria-hidden="true"
                />
                <input
                  type="text"
                  value={query}
                  onChange={(e) => setQuery(e.target.value)}
                  onKeyDown={(e) => e.key === 'Enter' && void runSearch()}
                  placeholder="Digite o título do artigo..."
                  className="w-full pl-10 h-12 rounded-xl border border-border bg-transparent px-4 text-base text-foreground placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
                />
              </div>

              <button
                onClick={() => void runSearch()}
                disabled={!query.trim() || searching}
                className="w-full min-h-[44px] rounded-full bg-emerald-400 text-emerald-950 font-medium flex items-center justify-center gap-2 disabled:opacity-50 hover:bg-emerald-300 transition"
              >
                {searching ? (
                  <>
                    <LoaderCircle className="w-4 h-4 animate-spin" aria-hidden="true" />
                    Buscando...
                  </>
                ) : (
                  'Buscar na Wikipedia'
                )}
              </button>

              {searchError && <p className="text-rose-300 text-xs">{searchError}</p>}

              {results.length > 0 && (
                <div className="space-y-2 max-h-80 overflow-y-auto overscroll-contain">
                  {results.map((result) => (
                    <button
                      key={result.id}
                      onClick={() => saveArticle(result)}
                      className="w-full text-left glass border border-white/10 bg-white/5 hover:border-white/25 rounded-2xl p-3 flex items-center gap-3 transition"
                    >
                      <span className="flex-1 min-w-0">
                        <span className="block text-foreground font-medium text-sm truncate">
                          {result.title}
                        </span>
                        <span className="block text-muted-foreground text-xs truncate">
                          {result.content.slice(0, 90)}...
                        </span>
                      </span>
                      <ArrowRight size={16} className="text-muted-foreground shrink-0" aria-hidden="true" />
                    </button>
                  ))}
                </div>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Modal — detalhe da ideia */}
      {selectedIdea && (
        <div
          className="fixed inset-0 z-50 flex items-end sm:items-center justify-center bg-black/50 backdrop-blur-sm p-3"
          onClick={() => setSelectedIdea(null)}
        >
          <div
            className="glass-strong rounded-3xl w-full max-w-2xl max-h-[88dvh] overflow-y-auto overscroll-contain p-5"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-start justify-between gap-3 mb-3">
              <div className="min-w-0">
                <h4 className="text-foreground font-medium">Ideia</h4>
                <p className="text-muted-foreground text-[11px] mt-1">só no aparelho</p>
              </div>
              <button
                onClick={() => setSelectedIdea(null)}
                aria-label="Fechar"
                className="p-2.5 min-h-[44px] text-foreground/50 hover:text-foreground transition rounded-lg"
              >
                <X size={20} aria-hidden="true" />
              </button>
            </div>

            <p className="text-foreground text-sm whitespace-pre-wrap leading-relaxed mb-4">
              {selectedIdea.content}
            </p>

            <div className="border-t border-white/10 pt-4">
              <p className="text-[11px] uppercase tracking-wide text-emerald-200/80 mb-2">Análise local</p>
              <p className="text-muted-foreground text-xs whitespace-pre-wrap leading-relaxed">
                {selectedIdea.analysis}
              </p>
            </div>

            <div className="flex items-center gap-2 mt-4">
              <span
                className={cn(
                  'text-xs px-2 py-1 rounded-full border',
                  CONGRUENCE_STYLES[selectedIdea.congruence],
                )}
              >
                Congruência: {CONGRUENCE_LABELS[selectedIdea.congruence]}
              </span>
            </div>

            <div className="flex items-center justify-end gap-2 mt-6">
              <button
                onClick={() => setSelectedIdea(null)}
                className="min-h-[44px] px-4 rounded-full text-muted-foreground hover:text-foreground text-sm"
              >
                Fechar
              </button>
              <button
                onClick={() => removeIdea(selectedIdea.id)}
                className="min-h-[44px] px-3 rounded-xl glass text-foreground/60 hover:text-rose-300 text-xs font-medium flex items-center gap-1.5 transition"
              >
                <Trash2 size={14} aria-hidden="true" /> Excluir
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal — detalhe do artigo */}
      {selectedArticle && (
        <div
          className="fixed inset-0 z-50 flex items-end sm:items-center justify-center bg-black/50 backdrop-blur-sm p-3"
          onClick={() => setSelectedArticle(null)}
        >
          <div
            className="glass-strong rounded-3xl w-full max-w-2xl max-h-[88dvh] overflow-y-auto overscroll-contain p-5"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-start justify-between gap-3 mb-3">
              <div className="min-w-0">
                <span className="inline-block text-[10px] uppercase tracking-wide text-emerald-200/80 mb-1">
                  Matéria
                </span>
                <h4 className="text-foreground font-medium break-words">{selectedArticle.title}</h4>
                <p className="text-muted-foreground text-[11px] mt-1">
                  {selectedArticle.lang.toUpperCase()} · {selectedArticle.wordCount} palavras
                </p>
              </div>
              <button
                onClick={() => setSelectedArticle(null)}
                aria-label="Fechar"
                className="p-2.5 min-h-[44px] text-foreground/50 hover:text-foreground transition rounded-lg"
              >
                <X size={20} aria-hidden="true" />
              </button>
            </div>

            <p className="text-foreground text-sm whitespace-pre-wrap leading-relaxed max-h-80 overflow-y-auto overscroll-contain">
              {selectedArticle.content}
            </p>

            <div className="flex items-center justify-end gap-2 mt-6">
              <button
                onClick={() => setSelectedArticle(null)}
                className="min-h-[44px] px-4 rounded-full text-muted-foreground hover:text-foreground text-sm"
              >
                Fechar
              </button>
              <button
                onClick={() => removeArticle(selectedArticle.id)}
                className="min-h-[44px] px-3 rounded-xl glass text-foreground/60 hover:text-rose-300 text-xs font-medium flex items-center gap-1.5 transition"
              >
                <Trash2 size={14} aria-hidden="true" /> Excluir
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Botão flutuante */}
      <button
        onClick={() => setShowIdeaModal(true)}
        aria-label="Nova ideia"
        className="fixed bottom-28 right-4 z-40 w-14 h-14 rounded-full glass-orb flex items-center justify-center text-foreground shadow-lg transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
      >
        <Plus size={24} aria-hidden="true" />
      </button>
    </div>
  )
}
