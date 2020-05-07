import scala.sys.process._

lazy val commonSettings = Seq(
  crossSbtVersions := List("1.2.8"),
  name := "sbt-aws-lambda",
  organization := "org.gfccollective",
  releaseCrossBuild := true,
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  libraryDependencies ++= Seq(
    "com.amazonaws"  % "aws-java-sdk-iam"    % "1.11.777",
    "com.amazonaws"  % "aws-java-sdk-lambda" % "1.11.777",
    "com.amazonaws"  % "aws-java-sdk-s3"     % "1.11.777",
  ),
  // Testing
  libraryDependencies += "com.lihaoyi" %% "utest" % "0.7.4" % "test",
  testFrameworks += new TestFramework("utest.runner.Framework"),
)

lazy val root =
  project.in(file("."))
    .settings(commonSettings: _*)
    .settings(
      name := "sbt-aws-lambda",
      libraryDependencies += {
        val sbtV     = (sbtBinaryVersion in pluginCrossBuild).value
        val scalaV   = (scalaBinaryVersion in update).value
        val assembly = "com.eed3si9n" % "sbt-assembly" % "0.14.10"
        Defaults.sbtPluginExtra(assembly, sbtV, scalaV)
      },
    ).enablePlugins(SbtPlugin)

lazy val scalajsPlugin =
  project.in(file("scalajsPlugin"))
    .dependsOn(root)
    .settings(commonSettings: _*)
    .settings(
      name := "sbt-aws-lambda-scalajs",
      libraryDependencies += {
        val sbtV     = (sbtBinaryVersion in pluginCrossBuild).value
        val scalaV   = (scalaBinaryVersion in update).value
        val scalajs = "org.scala-js" %% "sbt-scalajs" % "1.0.1"
        Defaults.sbtPluginExtra(scalajs, sbtV, scalaV)
      },
    ).enablePlugins(SbtPlugin)

