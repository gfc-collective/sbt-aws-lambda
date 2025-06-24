package com.gilt.aws.lambda

import com.amazonaws.services.s3.model._
import java.io.File
import scala.collection.JavaConverters._
import scala.util.Try

private[lambda] class AwsS3(client: wrapper.AmazonS3) {
  def pushJarToS3(
    jar: File,
    bucketId: S3BucketId,
    s3KeyPrefix: String,
  ): Try[S3Key] = {
    val key = s3KeyPrefix + jar.getName
    val objectRequest = new PutObjectRequest(bucketId.value, key, jar)

    client
      .putObject(objectRequest)
      .map { _ => S3Key(key) }
  }

  def getBucket(
    bucketId: S3BucketId,
  ): Option[Bucket] = {
    client
      .listBuckets()
      .toOption
      .flatMap { _.asScala.find(_.getName == bucketId.value) }
  }

  def createBucket(
    bucketId: S3BucketId,
  ): Try[S3BucketId] = {
    client
      .createBucket(bucketId.value)
      .map { _ => bucketId }
  }
}
