import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter, HashRouter } from 'react-router-dom'
import App from './App'
import './index.css'

/**
 * No Android (Capacitor) a página é servida por file://, onde o history do
 * navegador não resolve as rotas. HashRouter mantém tudo em memória e funciona
 * igual. No navegador comum seguimos com BrowserRouter (URLs limpas).
 */
const isNative = typeof window !== 'undefined' && 'Capacitor' in window
const Router = isNative ? HashRouter : BrowserRouter

/** Esconde o splash nativo e ajusta a barra de status — só no app instalado. */
async function initNativeShell() {
  if (!isNative) return
  try {
    const [{ SplashScreen }, { StatusBar, Style }] = await Promise.all([
      import('@capacitor/splash-screen'),
      import('@capacitor/status-bar'),
    ])
    // Fundo escuro do app: barra clara e sem sobreposição do WebView.
    await StatusBar.setStyle({ style: Style.Dark })
    await StatusBar.setBackgroundColor({ color: '#0c1814' })
    await SplashScreen.hide()
  } catch {
    // Plugins ausentes ou rodando no navegador: segue normalmente.
  }
}

const root = createRoot(document.getElementById('root')!)

root.render(
  <StrictMode>
    <Router>
      <App />
    </Router>
  </StrictMode>,
)

// O splash só sai depois que o React montou, evitando o flash branco.
void initNativeShell()
