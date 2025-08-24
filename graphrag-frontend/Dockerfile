# Base Stage 
FROM node:20-alpine AS base
WORKDIR /app
#COPY package*.json ./
COPY graphrag-frontend/package*.json ./

# Install dependencies separately for caching
RUN npm install

# Dev Stage 
FROM base AS dev

COPY graphrag-frontend/. .


# Expose React dev server port
EXPOSE 3000

# Start React in hot-reload mode
CMD ["npm", "start"]

# Build Stage 
FROM base AS build
# Copy all project files
COPY . .

# Build static files
RUN npm run build

# Prod Stage 
FROM nginx:alpine AS prod
# Copy build output to nginx html directory
COPY --from=build /app/build /usr/share/nginx/html
COPY nginx/dev.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
