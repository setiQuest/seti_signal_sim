// Project name (artifact name in Maven)
name := "signalsimulation"

// orgnization name (e.g., the package name of the project)
organization := "org.seti"

version := "8.0"

// project description
description := "SETI Signal Simulator"

// Enables publishing to maven repo
publishMavenStyle := true

// Do not append Scala versions to the generated artifacts
crossPaths := false

// This forbids including Scala related libraries into the dependency
autoScalaLibrary := false

javaSource in Compile := baseDirectory.value / "apps"

scalaVersion := "2.11.7"

libraryDependencies ++= {
  //val sparkVersion =  "2.0.0"
  Seq(
    //"org.apache.spark" %%  "spark-core"   %  sparkVersion % "provided",
    //"org.apache.spark" %%  "spark-sql"    %  sparkVersion % "provided",
    //"org.apache.spark" %% "spark-repl" % sparkVersion % "provided",
    "com.fasterxml.jackson.core" % "jackson-core" % "2.8.5", 
    "com.fasterxml.jackson.core" % "jackson-annotations" % "2.8.5", 
    "com.fasterxml.jackson.core" % "jackson-databind" % "2.8.5"
  )
}

assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false)