/// <reference types="vitest/config" />
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// API paths served by the Spring Boot backend. In development (and `vite preview`)
// they are proxied, so the browser talks to one origin and CORS never comes into play.
// Production builds are served by nginx, which proxies the same paths (see nginx.conf).
const backend = process.env.BACKEND_URL ?? 'http://localhost:8080';
// Trailing slashes matter: proxy keys are prefix matches, and a bare '/s' would also
// capture the app's own '/src/...' modules.
const apiPaths = ['/auth/', '/urls/', '/analytics/', '/s/', '/api/', '/v3/', '/swagger-ui'];
const proxy = Object.fromEntries(apiPaths.map((path) => [path, { target: backend, changeOrigin: false }]));

export default defineConfig({
  plugins: [react()],
  server: { port: 5173, proxy },
  preview: { port: 4173, proxy },
  test: {
    include: ['src/**/*.test.ts'],
    environment: 'node',
  },
});
