FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

RUN addgroup --system spring && \
    adduser --system spring --ingroup spring

USER spring:spring

COPY target/order-service.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java","-jar","app.jar"]