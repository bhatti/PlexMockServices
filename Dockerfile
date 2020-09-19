FROM gradle:jdk8 as builder

COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle war

FROM jetty:9-jre8
COPY --from=builder /home/gradle/src/build/libs/*.war /var/lib/jetty/webapps/root.war
EXPOSE 8080
