// Project name
name := "signalsimulation"

// organization name (e.g., the package name of the project)
organization := "org.seti"

version := "8.0"

// project description
description := "SETI Signal Simulator"

javaSource in Compile := baseDirectory.value / "apps" / "src" / "main" / "java"
scalaSource in Compile := baseDirectory.value / "spark" / "src" / "main" / "scala"

scalaVersion := "2.11.8"

crossVersion := CrossVersion.binary

libraryDependencies ++= {
  Seq(
    "com.fasterxml.jackson.core" % "jackson-core" % "2.8.5",
    "com.fasterxml.jackson.core" % "jackson-annotations" % "2.8.5",
    "com.fasterxml.jackson.core" % "jackson-databind" % "2.8.5",
    "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.8.5",
    "com.ibm.ibmos2spark" %% "ibmos2spark" % "0.0.7",
    "org.pacesys" % "openstack4j" % "3.0.3",
    "com.ibm.stocator" % "stocator" % "1.0.6",
    "org.apache.hadoop" % "hadoop-client" % "2.7.3" % "provided"
  )
}

libraryDependencies ++= {
  val sparkVersion = "2.1.0"
  Seq(
    "org.apache.spark" %% "spark-core" % sparkVersion % "provided",
    "org.apache.spark" %% "spark-sql" % sparkVersion % "provided",
    "org.apache.spark" %% "spark-repl" % sparkVersion % "provided"
  )
}

assemblyMergeStrategy in assembly := {
  case PathList("org", "apache", "commons", "logging", _*) =>
    MergeStrategy.last

  case PathList("org", "apache", "commons", _*) =>
    MergeStrategy.last

  case PathList("org", "jboss", "resteasy", _*) =>
    MergeStrategy.last

  case PathList("javax", _*) =>
    MergeStrategy.last

  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

resourceDirectory in Compile := baseDirectory.value / "resources"

compileOrder := CompileOrder.JavaThenScala

mainClass in Compile := Some("org.seti.simulator.SETISim")
