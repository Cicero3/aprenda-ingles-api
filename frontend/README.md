# Aprenda Inglês — Frontend

SPA em **React 19 + Vite + TypeScript + Tailwind v4** que consome a API `/api/v1` do backend.

## Rodar em dev

```bash
# 1. Backend no ar (na raiz do repo): docker-compose up -d && ./gradlew bootRun
# 2. Frontend:
npm install
npm run dev          # http://localhost:5173
```

O Vite faz **proxy de `/api` para `http://localhost:8080`** (configurável via `VITE_BACKEND_URL`),
então frontend e API ficam same-origin no navegador — o **cookie httpOnly** do refresh token
funciona sem CORS.

## Autenticação (espelha o backend)

- `login`/`register` guardam o **access token só em memória** (`src/api/http.ts`).
- O **refresh token** vive em cookie httpOnly; em `401` o client tenta `/auth/refresh` e repete a chamada uma vez.
- No boot, `bootstrapSession()` tenta restaurar a sessão pelo cookie.

## Estrutura

```
src/
├── api/         # client HTTP + tipos + auth/curriculum/progress (INTERINO — ver abaixo)
├── auth/        # AuthContext (estado de sessão)
├── components/  # LoginScreen, ModulesDashboard
├── App.tsx      # login vs app conforme sessão
└── main.tsx
```

## Client da API (tipos)

`src/api/types.ts` é escrito à mão espelhando o contrato do backend — **interino**.
Quando o backend estiver no ar, gere o client tipado a partir do OpenAPI e migre:

```bash
npm run api:spec   # baixa /api-docs -> openapi.json
npm run api:gen    # gera src/api/generated (openapi-generator-cli)
```

Detalhes em `../docs/frontend-api-client.md`.
