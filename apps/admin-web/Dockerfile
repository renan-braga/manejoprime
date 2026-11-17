FROM node:20-alpine AS build

WORKDIR /workspace

COPY package.json package-lock.json ./
COPY tsconfig.base.json ./
COPY apps/admin-web/package.json apps/admin-web/package.json

RUN npm ci --workspace @curralzap/admin-web
RUN npm install --no-save @rollup/rollup-linux-x64-musl@$(node -p "require('./node_modules/rollup/package.json').version")

COPY apps/admin-web apps/admin-web

RUN npm run build --workspace @curralzap/admin-web

FROM nginx:1.27-alpine AS runtime

ENV CURRALZAP_API_BASE_URL=http://localhost:8080

COPY apps/admin-web/docker/nginx.conf /etc/nginx/conf.d/default.conf
COPY apps/admin-web/docker/runtime-config.js.template /opt/curralzap/runtime-config.js.template
COPY apps/admin-web/docker/40-runtime-config.sh /docker-entrypoint.d/40-runtime-config.sh
COPY --from=build /workspace/apps/admin-web/dist /usr/share/nginx/html

RUN chmod +x /docker-entrypoint.d/40-runtime-config.sh

EXPOSE 80
