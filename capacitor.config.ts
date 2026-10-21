import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.fluxo.app',
  appName: 'Fluxo',
  webDir: 'dist',
  server: {
    androidScheme: 'https'
  },
  android: {
    allowMixedContent: true,
    captureInput: true,
    webContentsDebuggingEnabled: true
  },
  plugins: {
    SplashScreen: {
      launchShowDuration: 2000,
      backgroundColor: '#0c1814',
      showSpinner: false
    },
    StatusBar: {
      style: 'dark',
      backgroundColor: '#0c1814'
    }
  }
};

export default config;
