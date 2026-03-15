@echo off
setlocal
set MVNW_PROJECTBASEDIR=%~dp0
set MVNW_JAR_PATH=%MVNW_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.jar
java -jar "%MVNW_JAR_PATH%" %*
