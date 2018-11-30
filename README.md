# Bilge

Bilge is a little Java-based command-line utility for building
Docker containers from file system content. 
It serves as a demonstration of [Jib Core](https://github.com/GoogleContainerTools/jib/tree/master/jib-core),
a Java library for building containers without Docker.

## Building

`mvn package`

This creates a fatjar in `bilge-cli/target/bilge-cli-0.0.1-SNAPSHOT-jar-with-dependencies.jar`.

## Examples

### nginx
The following example creates an nginx-based container to serve static content that is found in `path/to/website`.
The result is loaded to the local Docker daemon as `my-static-website`:

    $ java -jar bilge-cli/target/bilge-cli-0.0.1-SNAPSHOT-jar-with-dependencies.jar \
      --docker \
      nginx \
      my-static-website \
      --port 80 \
      --entrypoint "nginx,-g,daemon off;" \
      path/to/website:/usr/share/nginx/html
    $ docker run -it --rm -p 8080:80 my-static-website

### Java app

The following example uses _bilge_ to containerize itself.  The image is pushed to a registry at localhost:5000:

    $ java -jar bilge-cli/target/bilge-cli-0.0.1-SNAPSHOT-jar-with-dependencies.jar \
      --registry \
      gcr.io/distroless/java \
      localhost:5000/bilge:latest \
      --insecure \
      --entrypoint "java,-jar,/app/bilge.jar" \
      bilge-cli/target/bilge-cli-0.0.1-SNAPSHOT-jar-with-dependencies.jar:/app/bilge.jar
    $ docker run --rm localhost:5000/bilge:latest

We need to use `--insecure` assuming the local registry does not support SSL.

Note that we'd be better off using `jib-maven-plugin` to create the container since it would create better layer strategy that negates the need to use a fatjar.

## Compiling with Graal's `native-image`

  - switched to using SLF4j with Apache Commons Logging facade to avoid
    the need to configure reflection for LogFactory 
  - must explicitly enable [`http` and `https` support](https://github.com/oracle/graal/blob/master/substratevm/URL-PROTOCOLS.md)
  - must somehow set `java.library.path` at compile time, or copy
    in $GRAALVM/jre/lib/libsunec.* into the current directory, to make
    the [SunEC JCA extensions
    available](https://github.com/oracle/graal/blob/master/substratevm/JCA-SECURITY-SERVICES.md#native-implementations).
  - the `graal-jib-reflect.json` must be updated as Jib Core adds
    new fields to Jackson JSON templates.  Jib Core 0.2.0 will be
    bringing _volumes_ and _permissions_ support.

```
$ mvn package
$ $GRAALVM/bin/native-image \
   -jar bilge-cli/target/bilge-cli-0.0.1-SNAPSHOT-jar-with-dependencies.jar \
   --no-server --enable-http --enable-https \
   -H:ReflectionConfigurationFiles=bilge-cli/graal-google-http-api-client-reflect.json \
   -H:ReflectionConfigurationFiles=bilge-cli/graal-jib-reflect.json \
   -H:ReflectionConfigurationFiles=bilge-cli/target/graal-cli-reflect.json \
   --rerun-class-initialization-at-runtime=org.apache.http.conn.ssl.SSLSocketFactory \
   --rerun-class-initialization-at-runtime=javax.net.ssl.HttpsURLConnection
```

