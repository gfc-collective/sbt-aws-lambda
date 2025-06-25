package com.gilt.aws.lambda

import com.amazonaws.services.signer.model.{
  DescribeSigningJobRequest,
  Destination,
  S3Destination,
  S3Source,
  Source,
  StartSigningJobRequest,
}

import scala.util.{Failure, Success, Try}

private[lambda] class Signer(client: wrapper.AwsSigner) {
  def signPackage(bucket: S3BucketId, prefix: String, objectVersion: S3ObjectVersion, signingProfile: SigningProfile): Try[S3Key] = {
    for {
      jobId <- startSigningJob(bucket, prefix, objectVersion, signingProfile)
      signedKey <- waitJobCompletion(jobId)
    } yield signedKey
  }

  private def startSigningJob(
                               bucket: S3BucketId,
                               destinationPrefix: String,
                               objectVersion: S3ObjectVersion,
                               signingProfile: SigningProfile,
  ): Try[String] = {
    objectVersion.version match {
      case None =>
        Failure(new RuntimeException("cannot sign an S3 object without a version - is bucket versioning enabled?"))
      case Some(version) =>
        println(s"Starting signing job for s3://${bucket.value}/${objectVersion.key}...")
        client
          .startSigningJob(
            new StartSigningJobRequest()
              .withSource(
                new Source().withS3(
                  new S3Source().withBucketName(bucket.value).withKey(objectVersion.key).withVersion(version),
                ),
              )
              .withDestination(
                new Destination().withS3(
                  new S3Destination().withBucketName(bucket.value).withPrefix(destinationPrefix),
                ),
              )
              .withProfileName(signingProfile.value),
          )
          .map(_.getJobId)
    }
  }

  private def waitJobCompletion(jobId: String): Try[S3Key] = {
    println(s"Checking if signing job $jobId is complete...")
    client.describeSigningJob(new DescribeSigningJobRequest().withJobId(jobId)).flatMap { r =>
      r.getStatus match {
        case "Succeeded" =>
          println(s"Signing job $jobId is complete.")
          Success(S3Key(r.getSignedObject.getS3.getKey))
        case "InProgress" =>
          Thread.sleep(5000L)
          waitJobCompletion(jobId)
        case "Failed" => Failure(new RuntimeException(s"Signing job failed: ${r.getStatusReason}"))
        case unknownStatus => Failure(new RuntimeException(s"Signing job unexpected status $unknownStatus"))
      }
    }
  }
}
