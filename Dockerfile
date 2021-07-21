FROM maven:3-amazoncorretto-8
COPY ./pom.xml ./pom.xml
COPY ./src ./src
RUN mvn package

FROM amazoncorretto:11
WORKDIR /data
COPY --from=0 target/MuxTunnelServer-*.jar /opt/muxtun/MuxTunnelServer.jar
COPY --from=0 target/lib/*.jar /opt/muxtun/lib/
CMD java -server -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=50 -jar /opt/muxtun/MuxTunnelServer.jar --discardOut
