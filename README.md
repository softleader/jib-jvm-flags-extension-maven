[![version](https://img.shields.io/github/v/release/softleader/jib-jvm-flags-extension-maven?color=brightgreen&sort=semver)](https://github.com/softleader/jib-jvm-flags-extension-maven/releases/latest)
[![Maven Central](https://img.shields.io/maven-central/v/tw.com.softleader.cloud.tools/jib-jvm-flags-extension-maven?color=orange)](https://central.sonatype.com/search?q=g%3Atw.com.softleader.cloud.tools&smo=true&namespace=tw.com.softleader.cloud.tools)
![GitHub tag checks state](https://img.shields.io/github/checks-status/softleader/jib-jvm-flags-extension-maven/main)
![GitHub issues](https://img.shields.io/github/issues-raw/softleader/jib-jvm-flags-extension-maven)

# Jib JVM Flags extension

A [Jib](https://github.com/GoogleContainerTools/jib) extension outputs the configured `jvmFlags` into the `/app/jib-jvm-flags-file` file, allowing a [custom entrypoint](https://github.com/GoogleContainerTools/jib/tree/master/jib-maven-plugin#custom-container-entrypoint) to access these flags.

When a custom entrypoint is used, Jib ignores the `jvmFlags` settings. This plugin allows the configured `jvmFlags` to still be accessed even in scenarios where a custom entrypoint is in use. 

For example, the following commands will be able to launch your app:

```
java $(cat /app/jib-jvm-flags-file) -cp $(cat /app/jib-classpath-file) $(cat /app/jib-main-class-file)
```

> **Requires Java 11 or newer**

## Examples

```xml

<plugin>
  <groupId>com.google.cloud.tools</groupId>
  <artifactId>jib-maven-plugin</artifactId>
  <version>...</version>
  <configuration>
    ...
    <pluginExtensions>
      <pluginExtension>
        <implementation>tw.com.softleader.cloud.tools.jib.maven.JvmFlagsExtension</implementation>
        <properties>
          <!-- Skip if no jvmFlags specified, Default: false -->
          <skipIfEmpty>true</skipIfEmpty>
          <!-- The separator character to use to join jvmFlags, Default: " " (space) --> 
          <separator>,</separator>
        </properties>
      </pluginExtension>
    </pluginExtensions>
  </configuration>
  <dependencies>
    <dependency>
      <groupId>tw.com.softleader.cloud.tools</groupId>
      <artifactId>jib-jvm-flags-extension-maven</artifactId>
      <version>${jib-jvm-flags-extension-maven.version}</version>
    </dependency>
  </dependencies>
</plugin>
```
