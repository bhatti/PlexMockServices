export MAVEN_OPTS=-Xmx512m
#./gradlew war jettyRun
#socat -v tcp-listen:8080,reuseaddr,fork tcp:192.168.180.40:80
rm -rf data/*
./gradlew --info war run
#./gradlew --debug war run
