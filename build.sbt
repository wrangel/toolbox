import sbtassembly.AssemblyPlugin.autoImport._

scalaVersion := "3.3.3"

name := "timestamptools"
organization := "ch.wrangel.toolbox"
version := "3.0"

libraryDependencies ++= Seq(
  "net.sourceforge.htmlcleaner" % "htmlcleaner" % "2.29",
  "org.wvlet.airframe" %% "airframe-log" % "24.9.3", 
  "org.apache.commons" % "commons-text" % "1.12.0",
  "org.scala-lang.modules" %% "scala-parallel-collections" % "1.0.4",
  "org.scalatest" %% "scalatest" % "3.2.19" % Test
)

scalacOptions ++= Seq(
  "-explain",
  "-Xfatal-warnings",
  "-deprecation",
  "-feature",
  "-unchecked",
  "-Xmax-inlines:64"
)

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", "versions", "9", "module-info.class") => MergeStrategy.discard
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}