package com.gilt.aws.lambda

import java.nio.file.{ Files, Paths, StandardCopyOption }

import com.amazonaws.services.lambda.model.{ Runtime => LambdaRuntime }
import com.gilt.aws.lambda.AwsLambdaPlugin.autoImport._
import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import sbt.Keys._
import sbt._

object AwsLambdaScalaJSPlugin extends AutoPlugin {

  override def requires = ScalaJSPlugin && AwsLambdaPlugin

  override lazy val projectSettings = Seq(
    packageLambda := Def.task {
      doPackageLambda(
        projectName = name.value,
        scalaBinaryVersion = scalaBinaryVersion.value,
        baseDir = baseDirectory.value
      )
    }.dependsOn(Compile / fastOptJS).value,
    supportedLambdaRuntimes := Set(
      LambdaRuntime.Nodejs10X,
      LambdaRuntime.Nodejs12X
    ),
    defaultRuntime := LambdaRuntime.Nodejs12X
  )

  def doPackageLambda(projectName: String, scalaBinaryVersion: String, baseDir: sbt.File): sbt.File = {
    val buildDir = Paths.get(baseDir.getAbsolutePath, "target", "lambda")
    if (!Files.exists(buildDir)) Files.createDirectories(buildDir)
    else if (!Files.isDirectory(buildDir)) sys.error(s"${buildDir} should be directory.")
    val jsName = s"${projectName}-fastopt.js"
    Files.copy(
      baseDir.toPath
        .resolve("target")
        .resolve( s"scala-${scalaBinaryVersion}/${jsName}"),
      buildDir.resolve(jsName),
      StandardCopyOption.REPLACE_EXISTING)
    baseDir.toPath.toFile.listFiles(FileFilter.globFilter("*.js")).foreach { file: File =>
      Files.copy(
        file.toPath,
        buildDir.resolve(file.getName),
        StandardCopyOption.REPLACE_EXISTING)
    }
    val zipPath = Paths.get(baseDir.getAbsolutePath, "target", "lambda.zip")
    FileUtils.packageDir(buildDir, zipPath)
    zipPath.toFile
  }
}

