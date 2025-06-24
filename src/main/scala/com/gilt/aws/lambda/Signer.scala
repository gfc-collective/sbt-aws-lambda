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
  def signPackage(bucket: S3BucketId, objectVersion: S3ObjectVersion, signingProfile: SigningProfile): Try[S3Key] = {
    val parentOpt = parent(objectVersion.key)
    for {
      jobId <- startSigningJob(bucket, objectVersion, signingProfile)
      _ <- waitJobCompletion(jobId)
    } yield parentOpt.fold(S3Key(jobId))(p => S3Key(s"$p/$jobId"))
  }

  private def startSigningJob(
    bucket: S3BucketId,
    objectVersion: S3ObjectVersion,
    signingProfile: SigningProfile,
  ): Try[String] = {
    objectVersion.version match {
      case None =>
        Failure(new RuntimeException("cannot sign an S3 object without a version - is bucket versioning enabled?"))
      case Some(version) =>
        val destinationPrefix = parent(objectVersion.key).getOrElse("")
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

  private def waitJobCompletion(jobId: String): Try[Unit] = {
    client.describeSigningJob(new DescribeSigningJobRequest().withJobId(jobId)).flatMap { r =>
      r.getStatus match {
        case "Succeeded" => Success(())
        case "InProgress" =>
          Thread.sleep(5000L)
          waitJobCompletion(jobId)
        case "Failed" => Failure(new RuntimeException(s"Signing job failed: ${r.getStatusReason}"))
        case unknownStatus => Failure(new RuntimeException(s"Signing job unexpected status $unknownStatus"))
      }
    }
  }

  private def parent(key: String): Option[String] = {
    val elems = key.split("/")
    elems.length match {
      case 0 => None
      case _ => Some(elems.dropRight(1).mkString("/"))
    }
  }
}
