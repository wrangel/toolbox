scalaVersion := "3.3.3"

name := "timestamptools"
organization := "ch.wrangel.toolbox"
version := "2.0"

val circeVersion = "0.14.10"
libraryDependencies ++= Seq(
  "org.wvlet.airframe" %% "airframe-log" % "24.9.3§",
  "net.sourceforge.htmlcleaner" % "htmlcleaner" % "2.29",
  "org.apache.commons" % "commons-text" % "1.12.0",
  "org.scala-lang.modules" %% "scala-parallel-collections" % "1.0.4",
  "org.scalatest" %% "scalatest" % "3.2.19" % Test,
  "org.typelevel" %% "cats-effect" % "3.5.4",
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion
)

scalacOptions ++= Seq(
  "-explain",           // Explain errors in more detail
  "-Xfatal-warnings",   // Fail the compilation if there are any warnings
  "-deprecation",       // Emit warning and location for usages of deprecated APIs
  "-feature",           // Emit warning and location for usages of features that should be imported explicitly
  "-unchecked",         // Enable additional warnings where generated code depends on assumptions
  "-Xmax-inlines:64"    // Increase the maximum number of inlines
)
