# Frontend P01 - ReactJS + Vite + Ant Design
# Stage 1: build file tĩnh
FROM node:22-alpine AS build
WORKDIR /app
COPY package*.json ./
# Dùng npm install (không dùng npm ci) để tránh lỗi gói theo hệ điều hành khi lockfile sinh ra trên Windows
RUN npm install --no-audit --no-fund
COPY . .
ARG VITE_USE_MOCK=true
ARG VITE_API_BASE_URL=/api/v1
ENV VITE_USE_MOCK=$VITE_USE_MOCK \
    VITE_API_BASE_URL=$VITE_API_BASE_URL
RUN npm run build

# Stage 2: Nginx phục vụ file tĩnh (SPA)
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://127.0.0.1/ || exit 1
