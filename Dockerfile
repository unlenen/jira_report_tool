FROM openjdk:17-jdk-alpine3.14
LABEL NAME="Argela Report Tool"
LABEL AUTHORS="Nebi UNLENEN <unlenen@gmail.com>"

RUN apk add tzdata ;  cp /usr/share/zoneinfo/Etc/GMT-3 /etc/localtime ; echo 'Etc/GMT-3' > /etc/timezone   ;  date 

RUN mkdir -p /opt/argela
ADD target/ArtReport-1.0.jar /opt/argela/ArtReport-1.0.jar

CMD  ["java -jar /opt/argela/ArtReport-1.0.jar"]
