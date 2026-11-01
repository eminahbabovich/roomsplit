import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// U razvoju sve što počinje s /api šaljemo na Spring Boot (port 8080).
// Browser misli da priča s istim izvorom (5173), pa CORS ne smeta.
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
