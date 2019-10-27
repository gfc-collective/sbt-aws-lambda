import scala.sys.process._

lazy val commonSettings = Seq(
  crossSbtVersions := List("1.2.8"),
  name := "sbt-aws-lambda",
  organization := "org.gfccollective",
  releaseCrossBuild := true,
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  libraryDependencies ++= Seq(
    "com.amazonaws"  % "aws-java-sdk-iam"    % "1.11.807",
    "com.amazonaws"  % "aws-java-sdk-lambda" % "1.11.807",
    "com.amazonaws"  % "aws-java-sdk-s3"     % "1.11.807",
  ),
  // Testing
  libraryDependencies += "com.lihaoyi" %% "utest" % "0.7.5" % "test",
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

val awsSdkVersion = "2.10.1"

libraryDependencies ++= Seq(
  "software.amazon.awssdk"  % "iam"    % awsSdkVersion,
  "software.amazon.awssdk"  % "lambda" % awsSdkVersion,
  "software.amazon.awssdk"  % "s3"     % awsSdkVersion,
  "org.scala-lang.modules" %% "scala-java8-compat" % "0.9.0"
)

javaVersionPrefix in javaVersionCheck := Some("1.8")

crossSbtVersions := List("0.13.17", "1.2.6")

releaseCrossBuild := true

releasePublishArtifactsAction := PgpKeys.publishSigned.value

// Testing
libraryDependencies += "com.lihaoyi" %% "utest" % "0.6.6" % "test"
testFrameworks += new TestFramework("utest.runner.Framework")
