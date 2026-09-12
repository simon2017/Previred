import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    // Evita configurar CORS en el backend durante desarrollo: el navegador ve todo
    // como same-origin (localhost:5173) y Vite reenvia /api al backend real.
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
