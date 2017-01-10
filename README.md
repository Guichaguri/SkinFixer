# SkinFixer

This is a simple mod that fixes or changes skin/cape servers in any Minecraft version.

## How it works?

SkinFixer is a tweaker that looks through the classes in search of the skin/cape server URLs.
It replaces them with URLs from the config file which can contain `%UUID%` and/or `%USERNAME%`

A builtin proxy server is created when:
* The URLs need a variable that the mod doesn't have access to
* The response has to be transformed (from image to json, or from json to image)

The builtin server can download and process the information without blocking the main thread.

## Developing/Building

SkinFixer is a simple Gradle module that uses the Java plugin and a few dependencies.

To build the jar file, just run `gradle jar`