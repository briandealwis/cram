#!/bin/sh

# --no-server: the native-image server hiccups when tweaking # settings
${GRAAL_HOME:?}/bin/native-image \
    --no-server \
    --no-fallback \
    -H:+ReportExceptionStackTraces \
    -H:+TraceClassInitialization \
    --report-unsupported-elements-at-runtime \
    -jar target/cram-0.0.1-SNAPSHOT-jar-with-dependencies.jar \
    --enable-http \
    --enable-https \
    --initialize-at-run-time=org.apache.commons.logging.LogFactory \
    -H:ReflectionConfigurationFiles=graal-google-http-api-client-reflect.json \
    -H:ReflectionConfigurationFiles=graal-jib-reflect.json \
    -H:ReflectionConfigurationFiles=graal-apache-http-reflect.json \
    -H:ReflectionConfigurationFiles=target/graal-cli-reflect.json \
    --initialize-at-run-time=org.apache.http.conn.ssl.SSLSocketFactory \
    --initialize-at-build-time=org.apache.http.Header \
    --initialize-at-build-time=org.apache.http.client.methods.CloseableHttpResponse \
    --initialize-at-build-time=org.apache.http.HttpEntity \
    --initialize-at-build-time=org.apache.http.params.HttpParams \
    --initialize-at-build-time=org.apache.http.StatusLine \
    --initialize-at-build-time=org.apache.http.ProtocolVersion 

