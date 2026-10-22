import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// In development the UI runs on http://localhost:5173 and proxies the API to the Quarkus admin app (port 8081),
// so the session cookie stays same-origin and no CORS configuration is needed.
const backend = process.env.JPRIME_ADMIN_API ?? 'http://localhost:8081';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': backend,
      '/j_security_check': backend,
      '/q': backend,
    },
  },
  build: {
    outDir: 'dist',
    emptyOutDir: true,
  },
});
