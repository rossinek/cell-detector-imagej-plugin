# Mycobacterium cells detection and description in ImageJ

Tuberculosis and other diseases caused by the genus Mycobacterium remains a serious worldwide problem.
Basic research aimed at understanding the biology of Mycobacteria is very important for further work related to novel drug development.
The important part of this kind of research is the process of obtaining data from microscopic recordings of the cell cycle of model organisms.

[This project is part of my master-thesis and aims](https://github.com/rossinek/cell-detector-master-thesis) to simplify this process and create a code base that can be used for further development.
The tool was created as a plugin for the ImageJ – program that is widely used by the scientific community.

## Dependencies

- Java 8
- Apache Maven (minimum 3.3.9)

## Install

In order to install the plugin copy `Mtbt_Plugin-<version>.jar` file to the ImageJ/Fiji `/jars` directory.

**Plugin should be visible in `Menu > Mycobacterium`**

## Build and run from source

### Run without ImageJ/Fiji installed

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

### Run tests

```sh
mvn test
```
