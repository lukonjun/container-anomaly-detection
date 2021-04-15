FROM openjdk:14-jdk-alpine
RUN apk --no-cache add curl
COPY target/application.jar /opt/
# Create a group and user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
RUN chown appuser:appgroup /opt/application.jar
# Tell docker that all future commands should run as the appuser user
USER appuser
CMD ["java","-jar","-Duser.timezone=Europe/Berlin","/opt/application.jar"]