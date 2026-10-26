# ---------------------------------------------------
# Stage 1: Build client dist with Node.js
# ---------------------------------------------------
FROM node:22-alpine AS builder

WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .

ARG VITE_API_URL=/api/v1
ENV VITE_API_URL=$VITE_API_URL

RUN npm run build

# ---------------------------------------------------
# Stage 2: Serve with lightweight Nginx
# ---------------------------------------------------
FROM nginx:alpine

COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
