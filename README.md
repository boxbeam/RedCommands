# RedCommands
A powerful command framework for Spigot plugins using a custom file format. Support for rdcml file format in Intellij: https://github.com/Shuaiouke/RedLib-Command-File

# Installation for Development

Gradle:

```groovy
repositories {
        maven { url 'https://jitpack.io' }
}

```

```groovy
dependencies {
        compileOnly 'com.github.Redempt:RedCommands:Tag'
}
```

Replace `Tag` with a release tag for RedLib. Example: `1.0`. You can also use `master` as the tag to get the latest version, though you will have to clear your gradle caches in order to update it.

Maven:

```xml
<repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
</repository>
```

```xml
<dependency>
        <groupId>com.github.Redempt</groupId>
        <artifactId>RedCommands</artifactId>
        <version>Tag</version>
        <scope>provided</scope>
</dependency>
```
Replace `Tag` with a release tag for RedLib. Example: `1.0`. You can also use `master` as the tag to get the latest version, though you will have to clear your maven caches in order to update it.

# Usage

For information on how to use RedCommands, please see the [Command Manager section in the RedLib wiki](https://github.com/Redempt/RedLib/wiki/Command-Manager).
