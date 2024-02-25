ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.12"

lazy val root = (project in file("."))
  .settings(
    name             := "zio-crawler",
    idePackagePrefix := Some("zio.crawler")
  )

libraryDependencies ++=
  Seq(
    "dev.zio" %% "zio"               % "2.0.19",
    "dev.zio" %% "zio-json"          % "0.6.2",
    "dev.zio" %% "zio-http"          % "3.0.0-RC2",
    "dev.zio" %% "zio-test"          % "2.1-RC1",
    "dev.zio" %% "zio-test-sbt"      % "2.1-RC1",
    "dev.zio" %% "zio-test-magnolia" % "2.1-RC1",
    "com.softwaremill.sttp.client3" %% "zio" % "3.9.3"  // for ZIO 2.x

  )
