import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.fluxo.app',
  appName: 'Fluxo',
  webDir: 'dist',
  // O app é 100% local — nenhuma origem remota é permitida.
  server: {
    androidScheme: 'https',
  },
  android: {
    // Desligado: o inspetor remoto do Chrome desativa otimizações do V8
    // e adiciona latência perceptível no arranque.
    webContentsDebuggingEnabled: false,
    // Desligado: não há conteúdo misto, e a flag enfraquece a política de origem.
    allowMixedContent: false,
    // Desligado: intercepta eventos de input sem necessidade.
    captureInput: false,
    // Já tratamos o fundo pelo CSS (app-bg).
    backgroundColor: '#0c1814',
  },
  plugins: {
    SplashScreen: {
      // Curto de propósito: o app monta em ~200ms. O splash existe só para
      // evitar o flash branco, não para atrasar a abertura.
      launchShowDuration: 300,
      backgroundColor: '#0c1814ff',
      androidScaleType: 'CENTER_CROP',
      showSpinner: false,
      // Some sozinho assim que o React monta (ver main.tsx).
      autoHide: false,
      splashFullScreen: true,
      splashImmersive: false,
    },
    StatusBar: {
      style: 'DARK',
      backgroundColor: '#0c1814',
      overlaysWebView: false,
    },
  },
};

export default config;
