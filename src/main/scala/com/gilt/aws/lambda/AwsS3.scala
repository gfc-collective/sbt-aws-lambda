package com.gilt.aws.lambda

import java.io.File
import java.nio.file.Paths

import software.amazon.awssdk.services.s3.model.{Bucket, ObjectCannedACL, PutObjectRequest}

import scala.collection.JavaConverters._
import scala.util.Try

private[lambda] class AwsS3(client: wrapper.AmazonS3) {
  def pushJarToS3(
    jar: File,
    bucketId: S3BucketId,
    s3KeyPrefix: String,
  ): Try[S3Key] = {
    val key = s3KeyPrefix + jar.getName
    val objectRequest = PutObjectRequest.builder
    .bucket(bucketId.value)
    .key(key)
    .acl(ObjectCannedACL.AUTHENTICATED_READ)
    .build

    client.putObject(objectRequest, Paths.get(jar.toURI))
      .map { _ => S3Key(key) }
  }

  def getBucket(
    bucketId: S3BucketId,
  ): Option[Bucket] = {
    client.listBuckets()
      .toOption
      .flatMap { _.buckets.asScala.find(_.name == bucketId.value) }
  }

  def createBucket(
    bucketId: S3BucketId,
  ): Try[S3BucketId] = {
    client.createBucket(bucketId.value)
      .map { _ => bucketId }
  }
}
