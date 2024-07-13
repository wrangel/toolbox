
scalaVersion := "3.4.2"

name := "timestamptools"
organization := "ch.wrangel.toolbox"
version := "2.1"

libraryDependencies ++= Seq(
  "org.wvlet.airframe" %% "airframe-log" % "24.7.0",
  "net.sourceforge.htmlcleaner" % "htmlcleaner" % "2.29",
  "org.apache.commons" % "commons-text" % "1.12.0"
)
