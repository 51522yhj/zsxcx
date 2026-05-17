FROM eclipse-temurin:17-jre
WORKDIR /app
ENV SERVER_PORT=8080
ENV LOG_PATH=/app/logs
ENV JAVA_OPTS="-Xms256m -Xmx768m"
RUN mkdir -p /app/logs /app/uploads
COPY app.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar --spring.profiles.active=prod"]
