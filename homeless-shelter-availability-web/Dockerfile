FROM nginx:1.27-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY dist/ /usr/share/nginx/html/
COPY docker-entrypoint.d/ /docker-entrypoint.d/

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
