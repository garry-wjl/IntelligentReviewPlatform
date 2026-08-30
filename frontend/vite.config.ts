import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    host: true,
    proxy: {
      '/admin/v1': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/open/v1': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
});
