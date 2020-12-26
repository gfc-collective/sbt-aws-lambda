package com.gilt.aws.lambda

import scala.collection.JavaConverters._

import utest._

object AwsLambdaPluginTests extends TestSuite {
  val tests = Tests {
    "Compute Environment" - {
      "requires no updates if identical" - noUpdatesIdentical
      "requires no updates if remote changes" - noUpdatesRemote
      "requires updates otherwise" - updatesOtherwise
      "environment union of remote, and local" - environmentUnion
      "environment local over remote" - environmentLocalOverRemote
    }
  }

  def noUpdatesIdentical = {
    val env = Map("a" -> "1", "b" -> "2", "c" -> "3")
    val result = AwsLambdaPlugin.computeEnvironment(env.asJava, env)
    assert(result._1 == false)
  }

  def noUpdatesRemote = {
    val local = Map("a" -> "1", "b" -> "2", "c" -> "3")
    val remote = local
      .updated("x", "1")
      .updated("y", "2")
      .updated("z", "3")

    val result = AwsLambdaPlugin.computeEnvironment(remote.asJava, local)
    assert(result._1 == false)
  }

  def updatesOtherwise = {
    val local = Map("a" -> "1", "b" -> "2", "c" -> "3")
    val remote = Map("x" -> "1", "y" -> "2", "z" -> "3")
    val result = AwsLambdaPlugin.computeEnvironment(remote.asJava, local)
    assert(result._1 == true)
  }

  def environmentUnion = {
    val local = Map("a" -> "1", "b" -> "2", "c" -> "3")
    val remote = Map("x" -> "1", "y" -> "2", "z" -> "3")
    val result = AwsLambdaPlugin.computeEnvironment(remote.asJava, local)
    assert(result._2.variables.asScala == local ++ remote)
  }

  def environmentLocalOverRemote = {
    val local = Map("a" -> "1")
    val remote = Map("a" -> "3")
    val result = AwsLambdaPlugin.computeEnvironment(remote.asJava, local)
    assert(result._2.variables.asScala == local)
  }
}
