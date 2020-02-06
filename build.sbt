import scala.sys.process._

crossSbtVersions := List("1.2.8")

name := "sbt-aws-lambda"

organization := "org.gfccollective"

enablePlugins(SbtPlugin)

libraryDependencies += {
  val sbtV     = (sbtBinaryVersion in pluginCrossBuild).value
  val scalaV   = (scalaBinaryVersion in update).value
  val assembly = "com.eed3si9n" % "sbt-assembly" % "0.14.10"
  Defaults.sbtPluginExtra(assembly, sbtV, scalaV)
}

val awsSdkVersion = "1.11.713"

libraryDependencies ++= Seq(
  "com.amazonaws"  % "aws-java-sdk-iam"    % awsSdkVersion,
  "com.amazonaws"  % "aws-java-sdk-lambda" % awsSdkVersion,
  "com.amazonaws"  % "aws-java-sdk-s3"     % awsSdkVersion
)

releaseCrossBuild := true

releasePublishArtifactsAction := PgpKeys.publishSigned.value

// Testing
libraryDependencies += "com.lihaoyi" %% "utest" % "0.7.4" % "test"
testFrameworks += new TestFramework("utest.runner.Framework")
