FROM maven:3-amazoncorretto-8
COPY ./pom.xml ./pom.xml
COPY ./src ./src
RUN mvn package

FROM amazoncorretto:21
WORKDIR /data
COPY --from=0 target/MuxTunnelServer-*.jar /opt/muxtun/MuxTunnelServer.jar
COPY --from=0 target/lib/*.jar /opt/muxtun/lib/
CMD java -server -Xmx512m -XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 -XX:+UnlockExperimentalVMOptions -XX:+DisableExplicitGC -XX:+AlwaysPreTouch -XX:G1NewSizePercent=30 -XX:G1MaxNewSizePercent=40 -XX:G1HeapRegionSize=8M -XX:G1ReservePercent=20 -XX:G1HeapWastePercent=5 -XX:G1MixedGCCountTarget=4 -XX:InitiatingHeapOccupancyPercent=15 -XX:G1MixedGCLiveThresholdPercent=90 -XX:G1RSetUpdatingPauseTimePercent=5 -XX:SurvivorRatio=32 -XX:+PerfDisableSharedMem -XX:MaxTenuringThreshold=1 -jar /opt/muxtun/MuxTunnelServer.jar --discardOut
