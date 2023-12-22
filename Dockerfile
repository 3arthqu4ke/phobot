FROM openjdk:17-alpine

WORKDIR /phobot
COPY . /phobot

RUN mkdir -p fabric/run/mods
RUN wget -O fabric/run/mods/hmc-specifics-1.20.1.jar \
    https://github.com/3arthqu4ke/HMC-Specifics/releases/download/v1.20.1-1.2.2/hmc-specifics-fabric-1.20.1-1.2.2.jar

RUN chmod +x gradlew
RUN ./gradlew -Phmc.lwjgl=true :fabric:build

# prepare everything for runClientWithoutDependencies
RUN ./gradlew -Phmc.lwjgl=true :fabric:generateDLIConfig
RUN ./gradlew -Phmc.lwjgl=true :fabric:generateLog4jConfig
RUN ./gradlew -Phmc.lwjgl=true :fabric:generateRemapClasspath
RUN ./gradlew -Phmc.lwjgl=true :fabric:prepareArchitecturyTransformer
RUN ./gradlew -Phmc.lwjgl=true :fabric:configureLaunch
RUN ./gradlew -Phmc.lwjgl=true :fabric:downloadAssets
RUN ./gradlew -Phmc.lwjgl=true :fabric:configureClientLaunch
ENTRYPOINT sh -c "./gradlew -Phmc.lwjgl=true :fabric:runClientWithoutDependencies"
