# Configuration Maven Plugin
A simple proof-of-concept plugin that makes possible to reuse plugin configurations.

## Usage

Example use:
* declare plugin as extension in build/plugins section (will fail if not set as extension):
```
    <plugins>
      <plugin>
        <groupId>org.codehaus.mojo</groupId>
        <artifactId>config-maven-plugin</artifactId>
        <version>1.0-SNAPSHOT</version>
        <extensions>true</extensions>
        <executions>
          <execution>
            <goals>
              <goal>init</goal>
            </goals>
          </execution>
        </executions>
        <configuration>
          <configurationTemplate name="flower">
            <flower>rose</flower>
          </configurationTemplate>
          <configurationTemplate name="car">
            <name>citroen</name>
            <cc>1600</cc>
          </configurationTemplate>
        </configuration>
      </plugin>
```
* define some configuration templates, in this snippet above there are two: `flower` and `car`.
* reference the configuration templates at their targets, you might also add overrides:
```
        <plugin>
          <groupId>org.codehaus.mojo</groupId>
          <artifactId>fictive1-maven-plugin</artifactId>
          <version>1.0</version>
          <configuration>
            <configurationTemplate name="flower">
              <color>red</color>
            </configurationTemplate>
          </configuration>
        </plugin>
        <plugin>
          <groupId>org.codehaus.mojo</groupId>
          <artifactId>fictive2-maven-plugin</artifactId>
          <version>1.0</version>
          <configuration>
            <configurationTemplate name="flower">
              <color>blue</color>
            </configurationTemplate>
          </configuration>
        </plugin>
        <plugin>
          <groupId>org.codehaus.mojo</groupId>
          <artifactId>fictive3-maven-plugin</artifactId>
          <version>1.0</version>
          <configuration>
            <configurationTemplate name="car">
              <cc>1200</cc>
            </configurationTemplate>
          </configuration>
        </plugin>
```
* at the end, the final configuration (you can see it by `mvn help:effective-pom`) is like this:
```
        <plugin>
          <groupId>org.codehaus.mojo</groupId>
          <artifactId>fictive1-maven-plugin</artifactId>
          <version>1.0</version>
          <configuration>
            <flower>rose</flower>
            <color>red</color>
          </configuration>
        </plugin>
        <plugin>
          <groupId>org.codehaus.mojo</groupId>
          <artifactId>fictive2-maven-plugin</artifactId>
          <version>1.0</version>
          <configuration>
            <flower>rose</flower>
            <color>blue</color>
          </configuration>
        </plugin>
        <plugin>
          <groupId>org.codehaus.mojo</groupId>
          <artifactId>fictive3-maven-plugin</artifactId>
          <version>1.0</version>
          <configuration>
            <name>citroen</name>
            <cc>1200</cc>
          </configuration>
        </plugin>
```

## Application order

The order how each element is applied is following:
* the node from `configurationTemplate` is added to plugin `configuration`
* the node from plugin `configuration/configurationTemplate` is added to `configuration` (effectively pushing "level up")



Have fun!  
~t~

