FROM --platform=$BUILDPLATFORM openjdk:11-jdk-slim as BuilderImage

ENV DEBIAN_FRONTEND="noninteractive"
ARG TARGETPLATFORM
ARG BUILDPLATFORM
RUN export REALARCH=$(echo $TARGETPLATFORM | cut -d "/" -f 2);echo building for $REALARCH;

RUN mkdir /opt/autologger
COPY build.gradle /opt/autologger
RUN echo "buildscript {\next.kotlin_version = '1.4.31'\nrepositories {\njcenter();\n}\ndependencies {\nclasspath \"org.jetbrains.kotlin:kotlin-gradle-plugin:\$kotlin_version\"\n}\n}\n" > /opt/build.gradle
# RUN cat /opt/build.gradle | cat - /opt/autologger/build.gradle > /opt/temp && mv /opt/temp /opt/autologger/build.gradle
RUN cat /opt/build.gradle > /opt/autologger/build.gradle2
RUN cat /opt/autologger/build.gradle >> /opt/autologger/build.gradle2
RUN rm /opt/autologger/build.gradle
RUN mv /opt/autologger/build.gradle2 /opt/autologger/build.gradle
RUN cat /opt/autologger/build.gradle
COPY src/ /opt/autologger/src
COPY gradlew /opt/autologger
COPY gradle/ /opt/autologger/gradle
WORKDIR /opt/autologger
RUN ["./gradlew", "clean", "shadowJar"]
RUN mkdir /opt/logger
RUN cp build/libs/a*.jar /opt/logger/app.jar

FROM openjdk:11-jdk-slim as AppImage
ENV DEBIAN_FRONTEND="noninteractive"
COPY --from=BuilderImage /opt/logger /opt/logger

EXPOSE 9000

ENTRYPOINT java -cp /opt/logger/app.jar com.fyber.marketplace.VertxHandler
