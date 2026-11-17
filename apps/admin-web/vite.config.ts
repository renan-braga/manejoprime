import tailwindcss from "@tailwindcss/vite";
import react from "@vitejs/plugin-react";
import path from "path";
import { defineConfig } from "vite";

export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    proxy: {
      "/api": "http://localhost:8080"
    }
  },
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src")
    }
  },
  test: {
    environment: "jsdom",
    setupFiles: "./src/test/setup.ts",
    alias: {
      recharts: path.resolve(__dirname, "src/test/mocks/recharts.tsx"),
      "framer-motion": path.resolve(__dirname, "src/test/mocks/framer-motion.tsx")
    },
    pool: "forks",
    poolOptions: {
      forks: {
        maxForks: 1,
        minForks: 1,
        execArgv: ["--expose-gc", "--max-old-space-size=4096"]
      }
    }
  }
});
