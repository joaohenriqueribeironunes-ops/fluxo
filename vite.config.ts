import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  // Caminhos relativos: o Capacitor serve os arquivos via file:// no Android,
  // onde caminhos absolutos ("/assets/...") quebram.
  base: './',
  build: {
    target: 'es2020',
    cssCodeSplit: true,
    sourcemap: false,
    // Separa o vendor do código do app para o parse inicial ser menor
    // e o chunk principal ficar cacheável entre versões.
    rollupOptions: {
      output: {
        manualChunks: {
          react: ['react', 'react-dom', 'react-router-dom'],
        },
      },
    },
  },
})
