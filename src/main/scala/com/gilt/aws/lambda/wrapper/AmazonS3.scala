package com.gilt.aws.lambda.wrapper

import software.amazon.awssdk.services.s3.model._
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider

import scala.util.Try
import com.gilt.aws.lambda.Region
import software.amazon.awssdk.services.s3.S3Client

trait AmazonS3 {
  def listBuckets(): Try[ListBucketsResponse]
  def createBucket(bucket: String): Try[CreateBucketResponse]
  def putObject(req: PutObjectRequest): Try[PutObjectResponse]
}

object AmazonS3 {
  def instance(region: Region): AmazonS3 = {
    val auth = DefaultCredentialsProvider.create
    val client = S3Client.builder
      .credentialsProvider(auth)
      .region(region.value)
      .build

    new AmazonS3 {
        def listBuckets() = Try(client.listBuckets)
        def createBucket(bucket: String) = Try( {
          val req = CreateBucketRequest.builder
            .bucket(bucket)
            .build
          client.createBucket(req)
        })
        def putObject(req: PutObjectRequest) = Try(client.putObject(req))
    }
  }
}
