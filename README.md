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
      --from nginx \
      --port 80 \
      --docker my-static-website 
      --entrypoint "nginx,-g,daemon off;" \
      path/to/website:/usr/share/nginx/html
    $ docker run -it --rm -p 8080:80 my-static-website

### Java app

The following example uses _bilge_ to containerize itself.  The image is loaded to the local Docker daemon, which can then push it elsewhere.

    $ java -jar bilge-cli/target/bilge-cli-0.0.1-SNAPSHOT-jar-with-dependencies.jar \
      --from gcr.io/distroless/java \
      --docker localhost:5000/bilge:latest \
      --entrypoint "java,-jar,/app/bilge.jar" \
      bilge-cli/target/bilge-cli-0.0.1-SNAPSHOT-jar-with-dependencies.jar:/app/bilge.jar
    $ docker run --rm bilge

We'd be better off using `jib-maven-plugin` to create the container since it would create better layer strategy that negates the need to use a fatjar.
