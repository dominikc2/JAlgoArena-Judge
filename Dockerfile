FROM alpine
RUN apk add --no-cache openjdk8
RUN mkdir -p /app/
COPY build/libs/*.zip /app/jalgoarena.zip
WORKDIR app
RUN unzip /app/jalgoarena.zip
ENTRYPOINT java -Xms1g -Xmx2g -XX:+HeapDumpOnOutOfMemoryError -XX:OnOutOfMemoryError="kill -9 %p" -classpath "lib/*" -jar /app/jalgoarena*.jar
