import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Vite dev-server proxy — every /api/** and /actuator/** request is forwarded
// to the Spring Boot backend, so the browser sees same-origin requests and we
// don't have to configure CORS on the Java side.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/actuator': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
});
