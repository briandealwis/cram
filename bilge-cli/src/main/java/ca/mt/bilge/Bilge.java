/*
 * Copyright 2018 Manumitting Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ca.mt.bilge;

import com.google.cloud.tools.jib.api.Containerizer;
import com.google.cloud.tools.jib.api.DockerDaemonImage;
import com.google.cloud.tools.jib.api.Jib;
import com.google.cloud.tools.jib.api.JibContainer;
import com.google.cloud.tools.jib.api.JibContainerBuilder;
import com.google.cloud.tools.jib.api.RegistryImage;
import com.google.cloud.tools.jib.configuration.LayerConfiguration;
import com.google.cloud.tools.jib.configuration.Port;
import com.google.cloud.tools.jib.filesystem.AbsoluteUnixPath;
import java.io.File;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/** A simple container builder. */
public class Bilge implements Callable<Void> {
  private static class LayerDefinitionParser
      implements CommandLine.ITypeConverter<LayerConfiguration> {
    @Override
    public LayerConfiguration convert(String value) throws Exception {
      String[] definition = value.split(File.pathSeparator);
      if (definition.length > 2) {
        throw new CommandLine.TypeConversionException(
            "layer definition must be one of:\n"
                + "  files/location   (files placed in container root)\n"
                + "  files/location"
                + File.pathSeparatorChar
                + "path/in/container");
      }
      String containerRoot = definition.length == 1 ? "/" : definition[1];
      return LayerConfiguration.builder()
          .addEntryRecursive(Paths.get(definition[0]), AbsoluteUnixPath.get(containerRoot))
          .build();
    }
  }

  private static class PortParser implements CommandLine.ITypeConverter<Port> {
    private static Pattern portPattern = Pattern.compile("(?<port>\\d+)(?:/(?<protocol>tcp|udp))?");

    public Port convert(String value) throws Exception {
      Matcher matcher = portPattern.matcher(value);
      if (matcher.matches()) {
        int port = Integer.parseInt(matcher.group("port"));
        String protocol = matcher.group("protocol");
        if (protocol == null) {
          protocol = "tcp";
        }
        switch (protocol) {
          case "udp":
            return Port.udp(port);
          case "tcp":
            return Port.tcp(port);
          default:
            throw new CommandLine.TypeConversionException(
                "protocol must be either tcp or udp: " + protocol);
        }
      }
      throw new CommandLine.TypeConversionException("ports must be of form 25 or 25/tcp: " + value);
    }
  }

  public static void main(String[] args) {
    CommandLine.call(new Bilge(), args);
  }

  @Spec private CommandSpec commandSpec;

  @Option(
      names = {"-f", "--from"},
      required = true,
      paramLabel = "image",
      description = "the base image (e.g., busybox, nginx, gcr.io/distroless/java)")
  private String baseImage;

  @Option(
      names = {"-t", "--docker"},
      paramLabel = "image",
      description = "push result to local Docker daemon")
  private String toDocker;

  @Option(
      names = {"-T", "--registry"},
      paramLabel = "host:port/image",
      description = "push to registry (e.g., gcr.io/project/image:latest)")
  private String toRegistry;

  @Option(
      names = {"-c", "--creation-time"},
      description = "set the image creation time")
  private Instant creationTime = Instant.now();

  @Option(
      names = {"-v", "--verbose"},
      description = "be verbose")
  private boolean verbose = false;

  @Option(
      names = {"-E", "--entrypoint"},
      paramLabel = "arg",
      split = ",",
      description = "set the container entrypoint")
  private List<String> entrypoint;

  @Option(
      names = {"-A", "--arguments"},
      split = ",",
      description = "set the container entrypoint's default arguments")
  private List<String> arguments;

  @Option(
      names = {"-e", "--environment"},
      split = ",",
      paramLabel = "key=value",
      description = "add environment pairs")
  private Map<String, String> environment;

  @Option(
      names = {"-l", "--label"},
      split = ",",
      paramLabel = "key=value",
      description = "add image labels")
  private Map<String, String> labels;

  @Option(
      names = {"-p", "--port"},
      split = ",",
      paramLabel = "port",
      description = "expose port/type (e.g., 25 or 25/tcp)",
      converter = PortParser.class)
  private List<Port> ports;

  @Option(
      names = {"-u", "--user"},
      paramLabel = "user",
      description = "set user for execution (uid or existing user id)")
  private String user;

  @Option(
      names = {"-k", "--insecure"},
      description = "allow connecting to insecure registries")
  private boolean insecure = false;

  @Parameters(
      paramLabel = "local/path[:/container/path]",
      description =
          "copies content from the local file system into the container; container path defaults to '/' if omitted.",
      converter = LayerDefinitionParser.class)
  private List<LayerConfiguration> layers;

  @Override
  public Void call() throws Exception {
    if (toDocker == null && toRegistry == null) {
      throw new CommandLine.ParameterException(
          commandSpec.commandLine(), "One of --docker or --registry is required");
    }
    JibContainerBuilder builder = Jib.from(baseImage);
    builder.setCreationTime(creationTime);
    if (entrypoint != null) {
      verbose("ENTRYPOINT: " + entrypoint);
      builder.setEntrypoint(entrypoint);
    }
    if (arguments != null) {
      verbose("CMD: " + entrypoint);
      builder.setProgramArguments(arguments);
    }
    if (environment != null) {
      verbose("ENV: " + environment);
      for (Entry<String, String> pair : environment.entrySet()) {
        builder.addEnvironmentVariable(pair.getKey(), pair.getValue());
      }
    }
    if (labels != null) {
      verbose("LABELS: " + environment);
      for (Entry<String, String> pair : labels.entrySet()) {
        builder.addLabel(pair.getKey(), pair.getValue());
      }
    }
    if (ports != null) {
      verbose("EXPOSE: " + ports);
      ports.forEach(builder::addExposedPort);
    }
    if (user != null) {
      verbose("USER: " + environment);
      builder.setUser(user);
    }
    if (layers != null) {
      for (LayerConfiguration layer : layers) {
        builder.addLayer(layer);
      }
    }
    String destinationImage = toDocker != null ? toDocker : toRegistry;
    Containerizer containerizer =
        toDocker != null
            ? Containerizer.to(DockerDaemonImage.named(toDocker))
            : Containerizer.to(RegistryImage.named(toRegistry));
    containerizer.setAllowInsecureRegistries(insecure);
    containerizer.setToolName("bilge");

    ExecutorService executor = Executors.newCachedThreadPool();
    try {
      containerizer.setExecutorService(executor);

      JibContainer result = builder.containerize(containerizer);
      System.out.printf("Containerized to %s (%s)\n", destinationImage, result.getDigest());
      return null;
    } finally {
      executor.shutdown();
    }
  }

  private void verbose(String message) {
    if (verbose) {
      System.out.println(message);
    }
  }
}
