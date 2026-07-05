import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Proxy /api → Spring Boot during dev so cookies/CORS stay simple.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
});
