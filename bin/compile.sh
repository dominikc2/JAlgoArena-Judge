#!/bin/bash


export JAVA_TOOL_OPTIONS="-Xmx10m -Xss512k -Dfile.encoding=UTF-8"
java -jar /app/lib/jruby-*jar -S jrubyc $1 --java -t out >> ~/heroku.log
cd out && javac -d $PWD -cp /app/lib/jruby-complete-9.1.14.0.jar Solution.java
