
FROM node:20-alpine

RUN apk update && apk add --no-cache sqlite sqlite-dev bash


RUN addgroup -S appgroup && adduser -S appuser -G appgroup


WORKDIR /usr/src/app


COPY package*.json ./

RUN npm install --production

COPY . .

RUN chmod -R 777 /usr/src/app

USER appuser

EXPOSE 2095

CMD ["node", "server.js"]
