// Project name 
name := "signalsimulation"

// orgnization name (e.g., the package name of the project)
organization := "org.seti"

version := "8.0"

// project description
description := "SETI Signal Simulator"

javaSource in Compile := baseDirectory.value / "apps"
scalaSource in Compile := baseDirectory.value / "spark"

scalaVersion := "2.11.8"

crossScalaVersions := Seq("2.11.7", "2.11.8") 

crossVersion := CrossVersion.binary

libraryDependencies ++= {
  Seq(    
    "com.fasterxml.jackson.core" % "jackson-core" % "2.8.5", 
    "com.fasterxml.jackson.core" % "jackson-annotations" % "2.8.5", 
    "com.fasterxml.jackson.core" % "jackson-databind" % "2.8.5",
    "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.8.5",
    "com.ibm.ibmos2spark" %% "ibmos2spark" % "0.0.7-SNAPSHOT",
    "org.pacesys" % "openstack4j" % "3.0.3",
    "com.ibm.stocator" % "stocator" % "1.0.6",
//    "org.javaswift" % "joss" % "0.9.14",
    "org.apache.hadoop" % "hadoop-client" % "2.7.3" % "provided"
  )
}

libraryDependencies ++= {
  // val sparkVersion =  scalaVersion.value match {
  //   case "2.11.8" ==> "2.1.0" 
  //   case "2.11.7" ==> "2.0.2"
  // }
  val sparkVersion =  if (scalaVersion.value == "2.11.8") "2.1.0" else "2.0.2"
  Seq(
    "org.apache.spark" %%  "spark-core"   %  sparkVersion % "provided",
    "org.apache.spark" %%  "spark-sql"    %  sparkVersion % "provided",
    "org.apache.spark" %% "spark-repl" % sparkVersion % "provided"
  )
}

// assemblyMergeStrategy in assembly := {
// //  case PathList("org", "apache", "spark", xs @ _*) => MergeStrategy.first
//   //acase PathList("scala", xs @ _*) => MergeStrategy.discard
// //  case PathList("META-INF", "ivy2", "org.slf4j", xs @ _* ) => MergeStrategy.first
//   case PathList("org", "apache", "commons", "logging", xs @ _*) =>
//       MergeStrategy.last
//   case x =>
//     val oldStrategy = (assemblyMergeStrategy in assembly).value
//     oldStrategy(x)
// }

assemblyMergeStrategy in assembly := {
//  case PathList("org", "apache", "spark", xs @ _*) => MergeStrategy.first
  //acase PathList("scala", xs @ _*) => MergeStrategy.discard
//  case PathList("META-INF", "ivy2", "org.slf4j", xs @ _* ) => MergeStrategy.first
  case PathList("org", "apache", "commons", "logging", xs @ _*) =>
      MergeStrategy.last
  case PathList("org", "apache", "commons", xs @ _*) =>
      MergeStrategy.last
  case PathList("org", "jboss", "resteasy", xs @ _*) => MergeStrategy.last
  case PathList("javax", xs @ _*) => MergeStrategy.last
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}


// assemblyMergeStrategy in assembly := {
//   case PathList("org", "apache", "commons", "logging", xs @ _*) =>
//       MergeStrategy.discard
//   case x =>
//       val oldStrategy = (assemblyMergeStrategy in assembly).value
//       oldStrategy(x)
// }

// assemblyMergeStrategy in assembly := {
//     case PathList("org", "apache", "spark", xs @ _*) => MergeStrategy.last
// //    case PathList("META-INF", "maven", "org.slf4j", xs @ _* ) => MergeStrategy.first
// //    case PathList("META-INF", "java", "util", "zip", xs @ _*) => MergeStrategy.first
//     case PathList("META-INF", xs @ _*) => MergeStrategy.discard
//     case x => 
//       val oldStrategy = (assemblyMergeStrategy in assembly).value
//       oldStrategy(x)
// }

resourceDirectory in Compile := baseDirectory.value / "resources"

compileOrder := CompileOrder.JavaThenScala

mainClass in Compile := Some("org.seti.simulator.SETISim")

resolvers +=  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

//can't remove Scala if want to retain the ability to run the output jar on local JVM(?)
//can't I remove the Scala bits and still run it with >scala target/scala_2.11/simulation-assembly.jar ??

assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false)


