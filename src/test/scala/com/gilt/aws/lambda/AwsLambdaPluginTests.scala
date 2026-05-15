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
    "Split into waves" - {
      "empty list yields no waves" - splitEmpty
      "single item yields one wave of one" - splitSingle
      "doubling pattern with remainder preserves order and size" - splitNineteen
      "exact doubling boundary uses only powers of two" - splitFifteen
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
    assert(result._2.getVariables.asScala == local ++ remote)
  }

  def environmentLocalOverRemote = {
    val local = Map("a" -> "1")
    val remote = Map("a" -> "3")
    val result = AwsLambdaPlugin.computeEnvironment(remote.asJava, local)
    assert(result._2.getVariables.asScala == local)
  }

  def splitEmpty = {
    assert(AwsLambdaPlugin.splitIntoWaves(Seq.empty[Int]) == Seq.empty)
  }

  def splitSingle = {
    assert(AwsLambdaPlugin.splitIntoWaves(Seq(42)) == Seq(Seq(42)))
  }

  def splitNineteen = {
    val items = (1 to 19).toSeq
    val waves = AwsLambdaPlugin.splitIntoWaves(items)
    assert(waves.map(_.size) == Seq(1, 2, 4, 8, 4))
    assert(waves.flatten == items)
  }

  def splitFifteen = {
    val waves = AwsLambdaPlugin.splitIntoWaves((1 to 15).toSeq)
    assert(waves.map(_.size) == Seq(1, 2, 4, 8))
  }
}
