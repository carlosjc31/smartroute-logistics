import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

// Em desenvolvimento, /api é repassado ao backend-core (sem precisar de CORS).
// Em produção, defina VITE_API_URL se a API estiver em outra origem.
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  return {
    plugins: [react()],
    server: {
      proxy: {
        '/api': env.VITE_PROXY_TARGET || 'http://localhost:8080',
      },
    },
  }
})
