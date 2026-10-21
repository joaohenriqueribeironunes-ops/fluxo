import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter, HashRouter } from 'react-router-dom'
import { Capacitor } from '@capacitor/core'
import App from './App'
import './index.css'

/**
 * No Android (Capacitor) o app é servido pelo WebView e o history do navegador
 * não resolve as rotas de forma confiável. HashRouter mantém tudo em memória.
 * No navegador comum seguimos com BrowserRouter (URLs limpas).
 *
 * Usamos Capacitor.isNativePlatform() em vez de checar `'Capacitor' in window`:
 * o global só existe quando o bundle do core carrega, o que tornava a detecção
 * dependente da ordem dos imports.
 */
const isNative = Capacitor.isNativePlatform()
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
