FROM nginx:1.27-alpine

COPY homeless-shelter-availability-web/nginx.conf /etc/nginx/conf.d/default.conf
COPY homeless-shelter-availability-web/dist/ /usr/share/nginx/html/
COPY homeless-shelter-availability-web/docker-entrypoint.d/ /docker-entrypoint.d/

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
