# Builder stage (install dependencies + build app)
FROM node:20-alpine AS builder
WORKDIR /app
#COPY package*.json ./
COPY graphrag-frontend/package*.json ./

# Install dependencies 
RUN npm install --no-cache

COPY graphrag-frontend/ ./app

WORKDIR /app/app
RUN npm run build

# Base stage (nginx runtime)
FROM nginx:alpine AS base

RUN addgroup -S appgroup && adduser -S appuser -G appgroup


# Dev Stage 
FROM base AS dev
COPY nginx/dev.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /app/app/build /usr/share/nginx/html
EXPOSE 3000
CMD ["nginx", "-g", "daemon off;"]

# Prod Stage 
FROM base AS prod
COPY nginx/prod.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /app/app/build /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
