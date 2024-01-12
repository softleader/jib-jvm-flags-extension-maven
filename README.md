# Jib JVM Flags extension

A [Jib](https://github.com/GoogleContainerTools/jib) extension that outputs the contents of the configured `jvmFlags` into `/app/jib-jvm-flags-file` file, allowing custom `entrypoint` to still access the `jvmFlags`

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
          <skipIfEmpty>false</skipIfEmpty>
          <!-- Separator character to join jvmFlags, Default: " " (space) -->
          <separator> </separator>
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
