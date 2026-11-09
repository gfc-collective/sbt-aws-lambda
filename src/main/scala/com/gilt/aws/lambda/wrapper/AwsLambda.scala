package com.gilt.aws.lambda.wrapper

import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.retry.PredefinedRetryPolicies
import com.amazonaws.services.lambda.AWSLambdaClientBuilder
import com.amazonaws.services.lambda.model._
import scala.util.Try

import com.gilt.aws.lambda.Region

trait AwsLambda {
  def createFunction(req: CreateFunctionRequest): Try[CreateFunctionResult]
  def updateFunctionCode(req: UpdateFunctionCodeRequest): Try[UpdateFunctionCodeResult]
  def getFunctionConfiguration(req: GetFunctionConfigurationRequest): Try[GetFunctionConfigurationResult]
  def updateFunctionConfiguration(req: UpdateFunctionConfigurationRequest): Try[UpdateFunctionConfigurationResult]
  def tagResource(req: TagResourceRequest): Try[TagResourceResult]
  def publishVersion(
      request: PublishVersionRequest): Try[PublishVersionResult]
}

object AwsLambda {
  def instance(region: Region): AwsLambda = {
    val auth = new DefaultAWSCredentialsProviderChain()
    val client = AWSLambdaClientBuilder.standard()
      .withCredentials(auth)
      .withRegion(region.value)
      .withClientConfiguration(
        new ClientConfiguration().withRetryPolicy(PredefinedRetryPolicies.getDefaultRetryPolicyWithCustomMaxRetries(10))
      )
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
