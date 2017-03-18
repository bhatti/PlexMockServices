export MAVEN_OPTS=-Xmx512m 
#./gradlew war jettyRun 
rm data/*
./gradlew war appRun
