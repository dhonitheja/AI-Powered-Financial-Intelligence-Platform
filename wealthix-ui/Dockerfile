FROM node:20-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM node:20-alpine AS runtime

# Non-root user
RUN addgroup -S wealthix && adduser -S wealthix -G wealthix

WORKDIR /app
COPY --from=builder /app/.next/standalone ./
COPY --from=builder /app/.next/static ./.next/static
COPY --from=builder /app/public ./public

RUN chown -R wealthix:wealthix /app
USER wealthix

EXPOSE 3000

HEALTHCHECK --interval=30s --timeout=10s \
    CMD wget -qO- http://localhost:3000/api/health || exit 1

CMD ["node", "server.js"]
