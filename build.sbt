
lazy val root = project
  .in(file("."))
  .enablePlugins(SbtPlugin)
  .settings(
    organization := "com.knoldus",
    name := "sql_parser_generator",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := "2.12.14",
    libraryDependencies ++= Seq(
      "org.freemarker" % "freemarker" % "2.3.29",
      "net.sourceforge.fmpp" % "fmpp" % "0.9.16",
      "net.java.dev.javacc" % "javacc" % "4.0",
      "org.scalatest" %% "scalatest" % "3.2.8" % Test,
      "org.apache.calcite" % "calcite-core" % "1.27.0" % Test
    ),
    crossPaths := false
  )