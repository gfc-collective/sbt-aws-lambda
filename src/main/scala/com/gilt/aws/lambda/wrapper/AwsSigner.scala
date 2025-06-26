package com.gilt.aws.lambda.wrapper

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.services.signer.AWSsigner
import com.amazonaws.services.signer.AWSsignerClientBuilder
import com.amazonaws.services.signer.model.{DescribeSigningJobRequest, DescribeSigningJobResult, StartSigningJobRequest, StartSigningJobResult}
import com.gilt.aws.lambda.Region

import scala.util.Try

trait AwsSigner {
  def startSigningJob(req: StartSigningJobRequest): Try[StartSigningJobResult]
  def describeSigningJob(req: DescribeSigningJobRequest): Try[DescribeSigningJobResult]
}

object AwsSigner {
  def instance(region: Region): AwsSigner = {
    val auth = new DefaultAWSCredentialsProviderChain()
    val client: AWSsigner = AWSsignerClientBuilder.standard()
      .withCredentials(auth)
      .withRegion(region.value)
      .build()

    new AwsSigner {
      def startSigningJob(req: StartSigningJobRequest): Try[StartSigningJobResult] =
        Try(client.startSigningJob(req))

      def describeSigningJob(req: DescribeSigningJobRequest): Try[DescribeSigningJobResult] =
        Try(client.describeSigningJob(req))
    }
  }
}
