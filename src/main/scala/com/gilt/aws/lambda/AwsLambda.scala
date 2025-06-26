package com.gilt.aws.lambda

import java.time.Instant
import com.amazonaws.services.lambda.model._
import scala.collection.JavaConverters._
import scala.util.{Success, Try}

private[lambda] class AwsLambda(client: wrapper.AwsLambda) {

  def publishVersion(
    name: String,
    revisionId: String,
    version: String,
  ): Try[PublishVersionResult] = {
    val request = new PublishVersionRequest()
      .withFunctionName(name)
      .withRevisionId(revisionId)
      .withDescription(version)
    client.publishVersion(request)
  }

  def waitForLambdaReady(name: LambdaName): Try[Option[String]] = {
    getLambdaConfig(name).flatMap {
      case None => Success(None)
      case Some(result) if result.getLastUpdateStatus == "Successful" && result.getState == "Active" => Success(Some(result.getRevisionId))
      case Some(result) =>
        println(s"Waiting for lambda [name: ${name.value} status: ${result.getLastUpdateStatus} state: ${result.getState}] to be ready.")
        Thread.sleep(500)
        waitForLambdaReady(name)
    }
  }

  def updateLambdaWithFunctionCodeRequest(
    updateFunctionCodeRequest: UpdateFunctionCodeRequest,
    version: String,
  ): Try[UpdateFunctionCodeResult] = {
    println(s"Updating lambda code ${updateFunctionCodeRequest.getFunctionName} to s3://${updateFunctionCodeRequest.getS3Bucket}/${updateFunctionCodeRequest.getS3Key}")
    for {
      updateResult <- client.updateFunctionCode(updateFunctionCodeRequest)
      _ = println(s"Updated lambda code ${updateResult.getFunctionArn} revision ${updateResult.getRevisionId}")
      revisionId <- waitForLambdaReady(LambdaName(updateFunctionCodeRequest.getFunctionName))
      _ <- publishVersion(name = updateResult.getFunctionName, revisionId = revisionId.getOrElse(updateResult.getRevisionId), version = version)
    } yield {
      updateResult
    }
  }

  def tagLambda(
    functionArn: String,
    version: String,
  ): Try[TagResourceResult] = {
    val tags = Map(
      "deploy.code.version" -> version,
      "deploy.timestamp" -> Instant.now.toString
    )

    val tagResourceReq = new TagResourceRequest()
      .withResource(functionArn)
      .withTags(tags.asJava)

    client.tagResource(tagResourceReq)
  }

  def getLambdaConfig(
    functionName: LambdaName,
  ): Try[Option[GetFunctionConfigurationResult]] = {
    val request = new GetFunctionConfigurationRequest()
      .withFunctionName(functionName.value)

    client.getFunctionConfiguration(request)
      .map(Option.apply)
      .recover {
        case _: ResourceNotFoundException => None
      }
  }

  def updateLambdaConfig(
    functionName: LambdaName,
    handlerName: HandlerName,
    roleName: RoleARN,
    timeout:  Option[Timeout],
    memory: Option[Memory],
    deadLetterName: Option[DeadLetterARN],
    vpcConfig: Option[VpcConfig],
    environment: Environment,
    runtime: Runtime,
    version: String,
  ): Try[UpdateFunctionConfigurationResult] = {

    var request = new UpdateFunctionConfigurationRequest()
        .withFunctionName(functionName.value)
        .withHandler(handlerName.value)
        .withRole(roleName.value)
        .withRuntime(runtime)
        .withEnvironment(environment)

    request = timeout.fold(request)(t => request.withTimeout(t.value))
    request = memory.fold(request)(m => request.withMemorySize(m.value))
    request = vpcConfig.fold(request)(request.withVpcConfig)
    request = deadLetterName.fold(request)(d => request.withDeadLetterConfig(new DeadLetterConfig().withTargetArn(d.value)))

    for {
      updateResult <- client.updateFunctionConfiguration(request)
      _ = println(s"Updated lambda config ${updateResult.getFunctionArn}")
      _ <- publishVersion(name = updateResult.getFunctionName, revisionId = updateResult.getRevisionId, version = version)
    } yield {
      updateResult
    }
  }

  def createLambda(
    functionName: LambdaName,
    handlerName: HandlerName,
    roleName: RoleARN,
    timeout:  Option[Timeout],
    memory: Option[Memory],
    deadLetterName: Option[DeadLetterARN],
    vpcConfig: Option[VpcConfig],
    functionCode: FunctionCode,
    environment: Environment,
    runtime: Runtime,
    version: String,
  ): Try[CreateFunctionResult] = {

    var request = new CreateFunctionRequest()
      .withFunctionName(functionName.value)
      .withHandler(handlerName.value)
      .withRole(roleName.value)
      .withRuntime(runtime)
      .withEnvironment(environment)
      .withCode(functionCode)
    request = timeout.fold(request)(t => request.withTimeout(t.value))
    request = memory.fold(request)(m => request.withMemorySize(m.value))
    request = vpcConfig.fold(request)(request.withVpcConfig)
    request = deadLetterName.fold(request)(n => request.withDeadLetterConfig(new DeadLetterConfig().withTargetArn(n.value)))

    for {
      createResult <- client.createFunction(request)
      _ = println(s"Create lambda ${createResult.getFunctionArn}")
      _ <- publishVersion(name = createResult.getFunctionName, revisionId = createResult.getRevisionId, version = version)
    } yield {
      createResult
    }
  }
}
