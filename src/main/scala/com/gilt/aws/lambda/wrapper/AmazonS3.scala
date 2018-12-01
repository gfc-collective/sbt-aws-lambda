package com.gilt.aws.lambda.wrapper

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.{Region => _, _}
import software.amazon.awssdk.auth.credentials.AwsCredentialsProviderChain

import scala.util.Try

import com.gilt.aws.lambda.Region

trait AmazonS3 {
  def listBuckets(): Try[java.util.List[Bucket]]
  def createBucket(bucket: String): Try[Bucket]
  def putObject(req: PutObjectRequest): Try[PutObjectResult]
}

object AmazonS3 {
  def instance(region: Region): AmazonS3 = {
    val auth = new DefaultAWSCredentialsProviderChain()
    val client = AmazonS3ClientBuilder.standard()
      .withCredentials(auth)
      .withRegion(region.value)
      .build

    new AmazonS3 {
        def listBuckets() = Try(client.listBuckets)
        def createBucket(bucket: String) = Try(client.createBucket(bucket))
        def putObject(req: PutObjectRequest) = Try(client.putObject(req))
    }
  }
}
