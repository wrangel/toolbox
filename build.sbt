
scalaVersion := "3.3.0"

name := "timestamptools"
organization := "ch.wrangel.toolbox"
version := "2.0"

libraryDependencies ++= Seq(
  "org.wvlet.airframe" %% "airframe-log" % "23.8.0",
  "net.sourceforge.htmlcleaner" % "htmlcleaner" % "2.29",
  "org.apache.commons" % "commons-text" % "1.10.0"
)
