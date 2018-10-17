# Cells detection and description

## Dependencies
- Java
- Maven

## Build / run / install

Plugins should be visible in `Menu > Developement`

### Build and run
In this case there is no need for ImageJ/Fiji to be installed in your system.
```sh
# build
mvn

# run Main class
mvn exec:java -Dexec.mainClass="dev.mtbt.Main"
```

### Build and install

You can build jar and manually install it to ImageJ.
```sh
# build
mvn

# copy jar to ImageJ /jars directory
cp target/Mtbt_Plugin-{{version}}.jar {{path-to-imagej}}/jars
```

You can also build with auto installation.
```sh
# build and automatically copy jars to ImageJ
mvn -Dimagej.app.directory={{path-to-imagej}}

# example with Fiji installed in macOS Applications directory
# mvn -Dimagej.app.directory=/Applications/Fiji.app
```