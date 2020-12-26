package com.gilt.aws.lambda

import java.time.Instant

import software.amazon.awssdk.services.lambda.model._
import scala.collection.JavaConverters._
import scala.util.Try

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

  def updateLambdaWithFunctionCodeRequest(
    updateFunctionCodeRequest: UpdateFunctionCodeRequest,
    version: String,
  ): Try[UpdateFunctionCodeResult] = {
    println(s"Updating lambda code ${updateFunctionCodeRequest.getFunctionName}")
    for {
      updateResult <- client.updateFunctionCode(updateFunctionCodeRequest)
      _ = println(s"Updated lambda code ${updateResult.functionArn}")
      _ <- publishVersion(name = updateResult.functionName, revisionId = updateResult.revisionId, version = version)
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

    val tagResourceReq = TagResourceRequest.builder
      .resource(functionArn)
      .tags(tags.asJava)
      .build

    client.tagResource(tagResourceReq)
  }

  def getLambdaConfig(
    functionName: LambdaName,
  ): Try[Option[GetFunctionConfigurationResult]] = {
    val request = new GetFunctionConfigurationRequest()
      .functionName(functionName.value)
      .build

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

    request = timeout.fold(request)(t => request.timeout(t.value))
    request = memory.fold(request)(m => request.memorySize(m.value))
    request = vpcConfig.fold(request)(request.vpcConfig)
    request = deadLetterName.fold(request)(d => request.deadLetterConfig(DeadLetterConfig.builder.targetArn(d.value).build))

    for {
      updateResult <- client.updateFunctionConfiguration(request.build)
      _ = println(s"Updated lambda config ${updateResult.functionArn}")
      _ <- publishVersion(name = updateResult.functionName, revisionId = updateResult.revisionId, version = version)
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
      createResult <- client.createFunction(request.build)
      _ = println(s"Create lambda ${createResult.functionArn}")
      _ <- publishVersion(name = createResult.functionName, revisionId = createResult.revisionId, version = version)
    } yield {
      createResult
    }
  }
}
