import process from 'node:process'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// U razvoju sve što počinje s /api šaljemo na Spring Boot (podrazumijevano port 8080,
// a može se promijeniti varijablom API_TARGET). Browser misli da priča s istim izvorom
// (5173), pa CORS ne smeta.
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': process.env.API_TARGET ?? 'http://localhost:8080',
    },
  },
})
