# Jib JVM Flags extension

A [Jib](https://github.com/GoogleContainerTools/jib) extension that outputs the contents of the configured `jvmFlags` into a file, allowing custom `entrypoint` to still access the `jvmFlags`

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
        <implementation>tw.com.softleader.cloud.tools.jib.maven.JvmFlagsCalculatorExtension</implementation>
        <properties>
          <!-- skip if no jvmFlags specified, Default: false -->
          <skipIfEmpty>true</skipIfEmpty>
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
