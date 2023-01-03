# RedCommands
A powerful command framework for Spigot plugins using a custom file format. Support for rdcml file format in Intellij: https://github.com/Ciel-MC/RedLib-Command-File

Support Discord: https://discord.gg/agu5xGy2YZ

# Installation for Development

Gradle:

```groovy
repositories {
        maven { url 'https://redempt.dev' }
}

```

```groovy
dependencies {
        compileOnly 'com.github.Redempt:RedCommands:Tag'
}
```

Replace `Tag` with a release tag for RedLib. Example: `1.5.3.1`. You can also use `master` as the tag to get the latest version, though you will have to clear your gradle caches in order to update it.

Maven:

```xml
<repository>
        <id>redempt.dev</id>
        <url>https://redempt.dev</url>
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
Replace `Tag` with a release tag for RedLib. Example: `1.5.3.1`. You can also use `master` as the tag to get the latest version, though you will have to clear your maven caches in order to update it.

# Usage

For information on how to use RedCommands, please see the [Command Manager section in the RedLib wiki](https://github.com/Redempt/RedLib/wiki/Command-Manager).
