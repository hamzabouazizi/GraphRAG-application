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

# Dev Stage (hot-reload using the React dev server)
FROM node:20-alpine AS dev
WORKDIR /app

# copy package files and install inside the dev image
COPY graphrag-frontend/package*.json ./

RUN npm install --no-cache

# copy app source for hot-reload
COPY graphrag-frontend/ ./

# ensure the dev server listens on all interfaces
ENV HOST=0.0.0.0
ENV PORT=3000

EXPOSE 3000
CMD ["npm", "start"]

# Prod Stage 
FROM base AS prod
COPY nginx/prod.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /app/app/build /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
