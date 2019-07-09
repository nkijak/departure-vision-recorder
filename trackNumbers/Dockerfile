FROM bellsoft/liberica-openjre-alpine:latest

ENTRYPOINT ["java", "-jar", "/usr/share/dvr-diff-stream/main.jar"]

ADD target/lib  /usr/share/dvr-diff-stream/lib

ARG JAR_FILE
ADD target/${JAR_FILE} /usr/share/dvr-diff-stream/main.jar
