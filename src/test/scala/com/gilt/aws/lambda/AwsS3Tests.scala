package com.gilt.aws.lambda

import com.amazonaws.services.s3.model._
import java.io.File
import scala.util.{Failure, Success, Try}

import utest._

trait NotImplementedAmazonS3Wrapper extends wrapper.AmazonS3 {
  def listBuckets(): Try[java.util.List[Bucket]] = ???
  def createBucket(bucket: String): Try[Bucket] = ???
  def putObject(req: PutObjectRequest): Try[PutObjectResult] = ???
}

object AwsS3Tests extends TestSuite {
  val tests = Tests {
    "Put file" - {
      "puts with bucket" - putsWithBucket
      "puts with key" - putsWithKey
      "puts with file" - putsWithFile
      "puts with acl" - putsWithAcl
      "puts returns key" - putsReturnsKey
    }
    "Get bucket" - {
      "gets bucket if name match" - getSome
      "gets none if no name match" - getNoneNoMatch
      "gets none if failure" - getNoneFailure
    }
    "Create bucket" - {
      "creates with name" - createsWithName
      "creates returns name" - createsReturnsName
    }
  }

  def putsWithBucket = {
    val bucket = "my-bucket"
    val client = new NotImplementedAmazonS3Wrapper {
      override def putObject(req: PutObjectRequest) = {
        assert(req.getBucketName == bucket)
        Failure(new Throwable)
      }
    }

    new AwsS3(client).pushJarToS3(new File(""), S3BucketId(bucket), "")
  }

  def putsWithKey = {
    val prefix = "my-prefix"
    val fileName = "my-file"
    val client = new NotImplementedAmazonS3Wrapper {
      override def putObject(req: PutObjectRequest) = {
        assert(req.getKey == s"${prefix}${fileName}")
        Failure(new Throwable)
      }
    }

    new AwsS3(client).pushJarToS3(new File(fileName), S3BucketId(""), prefix)
  }

  def putsWithFile = {
    val fileName = "my-file"
    val client = new NotImplementedAmazonS3Wrapper {
      override def putObject(req: PutObjectRequest) = {
        assert(req.getFile.getName == fileName)
        Failure(new Throwable)
      }
    }

    new AwsS3(client).pushJarToS3(new File(fileName), S3BucketId(""), "")
  }

  def putsWithAcl = {
    val client = new NotImplementedAmazonS3Wrapper {
      override def putObject(req: PutObjectRequest) = {
        assert(req.getCannedAcl == null)
        Failure(new Throwable)
      }
    }

    new AwsS3(client).pushJarToS3(new File(""), S3BucketId(""), "")
  }

  def putsReturnsKey = {
    val prefix = "my-prefix"
    val fileName = "my-file"
    val client = new NotImplementedAmazonS3Wrapper {
      override def putObject(req: PutObjectRequest) = {
        Success(new PutObjectResult)
      }
    }

    val result = new AwsS3(client).pushJarToS3(new File(fileName), S3BucketId(""), prefix)
    assert(result.isSuccess)
    assert(result.get == S3Key(s"${prefix}${fileName}"))
  }

  def getSome = {
    val bucket = "my-bucket"
    val client = new NotImplementedAmazonS3Wrapper {
      override def listBuckets() = {
        val buckets = new java.util.ArrayList[Bucket]()
        buckets.add(new Bucket("a"))
        buckets.add(new Bucket("b"))
        buckets.add(new Bucket("c"))
        buckets.add(new Bucket(bucket))

        Success(buckets)
      }
    }

    val result = new AwsS3(client).getBucket(S3BucketId(bucket))
    assert(result.nonEmpty)
  }

  def getNoneNoMatch = {
    val bucket = "my-bucket"
    val client = new NotImplementedAmazonS3Wrapper {
      override def listBuckets() = {
        val buckets = new java.util.ArrayList[Bucket]()
        buckets.add(new Bucket("a"))
        buckets.add(new Bucket("b"))
        buckets.add(new Bucket("c"))

        Success(buckets)
      }
    }

    val result = new AwsS3(client).getBucket(S3BucketId(bucket))
    assert(result.isEmpty)
  }

  def getNoneFailure = {
    val bucket = "my-bucket"
    val client = new NotImplementedAmazonS3Wrapper {
      override def listBuckets() = {
        Failure(new Throwable)
      }
    }

    val result = new AwsS3(client).getBucket(S3BucketId(bucket))
    assert(result.isEmpty)
  }

  def createsWithName = {
    val bucket = "my-bucket"
    val client = new NotImplementedAmazonS3Wrapper {
      override def createBucket(arg: String) = {
        assert(arg == bucket)
        Failure(new Throwable)
      }
    }

    new AwsS3(client).createBucket(S3BucketId(bucket))
  }

  def createsReturnsName = {
    val bucket = "my-bucket"
    val client = new NotImplementedAmazonS3Wrapper {
      override def createBucket(arg: String) = {
        Success(new Bucket("a"))
      }
    }

    val result = new AwsS3(client).createBucket(S3BucketId(bucket))
    assert(result.isSuccess)
    assert(result.get == S3BucketId(bucket))
  }
}
