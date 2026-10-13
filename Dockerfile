FROM node:22-alpine

WORKDIR /app

# Instalar dependências necessárias
COPY package*.json ./
COPY server/package*.json ./server/
COPY extension/package*.json ./extension/

RUN npm install

# Copiar arquivos do projeto
COPY . .

# Configurações de ambiente
ENV NODE_ENV=production
ENV HOST=0.0.0.0
ENV PORT=3001

EXPOSE 3001

# Iniciar o servidor backend
CMD ["npm", "run", "start:server"]
