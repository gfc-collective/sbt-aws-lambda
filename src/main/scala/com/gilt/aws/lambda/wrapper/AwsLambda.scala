package com.gilt.aws.lambda.wrapper

// import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
// import com.amazonaws.services.lambda.AWSLambdaClientBuilder
// import com.amazonaws.services.lambda.model._
import software.amazon.awssdk.auth.credentials.AwsCredentialsProviderChain

import scala.util.Try
import com.gilt.aws.lambda.Region
import software.amazon.awssdk.services.lambda.LambdaClient
import software.amazon.awssdk.services.lambda.model._

trait AwsLambda {
  def createFunction(req: CreateFunctionRequest): Try[CreateFunctionResponse]
  def updateFunctionCode(req: UpdateFunctionCodeRequest): Try[UpdateFunctionCodeResponse]
  def getFunctionConfiguration(req: GetFunctionConfigurationRequest): Try[GetFunctionConfigurationResponse]
  def updateFunctionConfiguration(req: UpdateFunctionConfigurationRequest): Try[UpdateFunctionConfigurationResponse]
  def tagResource(req: TagResourceRequest): Try[TagResourceResponse]
  def publishVersion(
      request: PublishVersionRequest): Try[PublishVersionResponse]
}

object AwsLambda {
  def instance(region: Region): AwsLambda = {
    val auth = AwsCredentialsProviderChain.builder.build
    val client = LambdaClient.builder
      .credentialsProvider(auth)
      .region(region)
      .build

    new AwsLambda {
      def createFunction(req: CreateFunctionRequest) = Try(client.createFunction(req))
      def updateFunctionCode(req: UpdateFunctionCodeRequest) = Try(client.updateFunctionCode(req))
      def getFunctionConfiguration(req: GetFunctionConfigurationRequest) = Try(client.getFunctionConfiguration(req))
      def updateFunctionConfiguration(req: UpdateFunctionConfigurationRequest) = Try(client.updateFunctionConfiguration(req))
      def tagResource(req: TagResourceRequest) = Try(client.tagResource(req))
      def publishVersion(request: PublishVersionRequest) = Try(client.publishVersion(request))
    }
  }
}
