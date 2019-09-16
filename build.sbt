import sbt.Keys._
import play.sbt.PlaySettings
//resolvers += "Atlassian Releases" at "https://maven.atlassian.com/public/"
//resolvers += Resolver.jcenterRepo
lazy val root = (project in file("."))
  .enablePlugins(PlayService, PlayLayoutPlugin, Common)
  .settings(
    name := "play-scala-rest-api-example",
    version := "2.7.x",
    scalaVersion := "2.12.8" ,
    
    libraryDependencies ++= Seq(
      guice,
      "com.mohiva" %% "play-silhouette" % "6.1.0",
      "com.mohiva" %% "play-silhouette-password-bcrypt" % "6.1.0",
      "com.mohiva" %% "play-silhouette-crypto-jca" % "6.1.0",
      "com.mohiva" %% "play-silhouette-persistence" % "6.1.0",
      "com.mohiva" %% "play-silhouette-testkit" % "6.1.0" % "test",
      "com.iheart" %% "ficus" % "1.4.3",
      "org.joda" % "joda-convert" % "2.1.2",
      "net.logstash.logback" % "logstash-logback-encoder" % "5.2",
      "io.lemonlabs" %% "scala-uri" % "1.4.10",
      "net.codingwell" %% "scala-guice" % "4.2.5",
      "org.scalatestplus.play" %% "scalatestplus-play" % "4.0.3" % Test
    )
  )

lazy val gatlingVersion = "3.1.3"
lazy val gatling = (project in file("gatling"))
  .enablePlugins(GatlingPlugin)
  .settings(
    scalaVersion := "2.12.8",
    libraryDependencies ++= Seq(
      "io.gatling.highcharts" % "gatling-charts-highcharts" % gatlingVersion % Test,
      "io.gatling" % "gatling-test-framework" % gatlingVersion % Test
    )
  )

// Documentation for this project:
//    sbt "project docs" "~ paradox"
//    open docs/target/paradox/site/index.html
lazy val docs = (project in file("docs")).enablePlugins(ParadoxPlugin).
  settings(
    scalaVersion := "2.13.0",
    paradoxProperties += ("download_url" -> "https://example.lightbend.com/v1/download/play-rest-api")
  )
