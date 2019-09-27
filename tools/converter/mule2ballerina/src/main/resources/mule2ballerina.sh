#!/bin/sh
#java -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005 \
java -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5006 \
        -jar target/mule2ballerina-7.0.0-SNAPSHOT.jar \
        /Users/milindaperera/WSO2/Integration/RnD/workpace/ballerina/GIT/ballerina-integrator/tools/converter/mule2ballerina/src/main/resources/sample-mule-configs/helloService.xml \
        /Users/milindaperera/WSO2/Integration/RnD/workpace/ballerina/converterWS/target/helloService.bal