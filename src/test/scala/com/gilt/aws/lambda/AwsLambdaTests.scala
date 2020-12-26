package com.gilt.aws.lambda

import java.{lang, util}
import java.util.function.Consumer

import software.amazon.awssdk.awscore.AwsResponseMetadata
import software.amazon.awssdk.core.{SdkField, SdkResponse}
import software.amazon.awssdk.http.SdkHttpResponse
import software.amazon.awssdk.services.lambda.model
import software.amazon.awssdk.services.lambda.model.{CreateFunctionRequest, CreateFunctionResponse, DeadLetterConfig, Environment, EnvironmentResponse, GetFunctionConfigurationRequest, GetFunctionConfigurationResponse, LambdaResponse, LambdaResponseMetadata, Layer, PublishVersionRequest, PublishVersionResponse, ResourceNotFoundException, TagResourceRequest, TagResourceResponse, TracingConfigResponse, UpdateFunctionCodeRequest, UpdateFunctionCodeResponse, UpdateFunctionConfigurationRequest, UpdateFunctionConfigurationResponse, VpcConfig, VpcConfigResponse}

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}
import utest._

trait NotImplementedAwsLambdaWrapper extends wrapper.AwsLambda {
  def createFunction(req: CreateFunctionRequest): Try[CreateFunctionResponse] = ???
  def updateFunctionCode(req: UpdateFunctionCodeRequest): Try[UpdateFunctionCodeResponse] = ???
  def getFunctionConfiguration(req: GetFunctionConfigurationRequest): Try[GetFunctionConfigurationResponse] = ???
  def updateFunctionConfiguration(req: UpdateFunctionConfigurationRequest): Try[UpdateFunctionConfigurationResponse] = ???
  def tagResource(req: TagResourceRequest): Try[TagResourceResponse] = ???
  def publishVersion(request: PublishVersionRequest): Try[PublishVersionResponse] = ???
}

object AwsLambdaTests extends TestSuite {
  val tests = Tests {
    "Tag lambda" - {
      "tags proper arn" - tagWithArn
      "adds version" - tagWithVersion
      "adds timestamp" - tagWithTimestamp
    }
    "Publishes Version" - {
      "publishes proper version" - publishVersion
    }
    "Get lambda config" - {
      "gets proper function" - getWithFunctionName
      "gets some result" - getSuccess
      "gets none for missing resource" - getSuccessEmpty
      "gets failure otherwise" - getFailure
    }
    "Update lambda config" - {
      "updates with function name" - updateWithFunctionName
      "updates with handler" - updateWithHandler
      "updates with role" - updateWithRole
      "updates with runtime" - updateWithRuntime
      "updates with environment" - updateWithEnvironment
      "updates with timeout if present" - updateWithTimeout
      "updates without timeout if missing" - updateWithoutTimeout
      "updates with memory if present" - updateWithMemory
      "updates without memory if missing" - updateWithoutMemory
      "updates with vpc if present" - updateWithVpcConfig
      "updates without vpc if missing" - updateWithoutVpcConfig
      "updates with dead letter if present" - updateWithDeadLetterConfig
      "updates without dead letter if missing" - updateWithoutDeadLetterConfig
    }
    "Create lambda function" - {
      "creates with function name" - createWithFunctionName
      "creates with handler" - createWithHandler
      "creates with role" - createWithRole
      "creates with runtime" - createWithRuntime
      "creates with environment" - createWithEnvironment
      "creates with code" - createWithFunctionCode
      "creates with timeout if present" - createWithTimeout
      "creates without timeout if missing" - createWithoutTimeout
      "creates with memory if present" - createWithMemory
      "creates without memory if missing" - createWithoutMemory
      "creates with vpc if present" - createWithVpcConfig
      "creates without vpc if missing" - createWithoutVpcConfig
      "creates with dead letter if present" - createWithDeadLetterConfig
      "creates without dead letter if missing" - createWithoutDeadLetterConfig
    }
  }

  def tagWithArn = {
    val arn = "my-arn"
    val client = new NotImplementedAwsLambdaWrapper {
      override def tagResource(req: TagResourceRequest) = {
        assert(req.resource() == arn)
        Failure(new Throwable)
      }
    }

    new AwsLambda(client).tagLambda(arn, "")
  }

  def publishVersion = {
    val name = "my-name"
    val revisionId = "my-revision-id"
    val version = "version"
    val client = new NotImplementedAwsLambdaWrapper {
      override def publishVersion(request: PublishVersionRequest) = {
        assert(request.functionName == name)
        assert(request.revisionId == revisionId)
        assert(request.description == version)
        Failure(new Throwable)
      }
    }

    new AwsLambda(client).publishVersion(name, revisionId, version)
  }

  def tagWithTimestamp = {
    val client = new NotImplementedAwsLambdaWrapper {
      override def tagResource(req: TagResourceRequest) = {
        assert(req.tags().asScala.contains("deploy.timestamp"))
        Failure(new Throwable)
      }
    }

    new AwsLambda(client).tagLambda("", "")
  }

  def tagWithVersion = {
    val version = "my-version"
    val client = new NotImplementedAwsLambdaWrapper {
      override def tagResource(req: TagResourceRequest) = {
        assert(req.tags().asScala.get("deploy.code.version") == Some(version))
        Failure(new Throwable)
      }
    }

    new AwsLambda(client).tagLambda("", version)
  }

  def getWithFunctionName = {
    val functionName = "my-function-name"
    val client = new NotImplementedAwsLambdaWrapper {
      override def getFunctionConfiguration(req: GetFunctionConfigurationRequest) = {
        assert(req.functionName() == functionName)
        Failure(new Throwable)
      }
    }

    new AwsLambda(client).getLambdaConfig(LambdaName(functionName))
  }

  def getSuccess = {
    val expected = GetFunctionConfigurationResponse.builder().build()

    val client = new NotImplementedAwsLambdaWrapper {
      override def getFunctionConfiguration(req: GetFunctionConfigurationRequest) = {
        Success(expected)
      }
    }

    val result = new AwsLambda(client).getLambdaConfig(LambdaName(""))
    assert(result.isSuccess)
    assert(result.get.exists(_ == expected))
  }

  def getSuccessEmpty = {
    val client = new NotImplementedAwsLambdaWrapper {
      override def getFunctionConfiguration(req: GetFunctionConfigurationRequest) = {
        Failure(new ResourceNotFoundException(""))
      }
    }

    val result = new AwsLambda(client).getLambdaConfig(LambdaName(""))
    assert(result.isSuccess)
    assert(result.get.isEmpty)
  }

  def getFailure = {
    val client = new NotImplementedAwsLambdaWrapper {
      override def getFunctionConfiguration(req: GetFunctionConfigurationRequest) = {
        Failure(new Throwable)
      }
    }

    val result = new AwsLambda(client).getLambdaConfig(LambdaName(""))
    assert(result.isFailure)
  }

  def updateLambdaConfig(
    client: wrapper.AwsLambda,
    lambdaName: LambdaName = LambdaName(""),
    handlerName: HandlerName = HandlerName(""),
    roleArn: RoleARN = RoleARN(""),
    timeout:  Option[Timeout] = None,
    memory: Option[Memory] = None,
    deadLetterArn: Option[DeadLetterARN] = None,
    vpcConfig: Option[VpcConfig] = None,
    environment: Environment = new Environment(),
    lambdaRuntime: Runtime = Runtime.Java8,
  ) = new AwsLambda(client).updateLambdaConfig(
    lambdaName,
    handlerName,
    roleArn,
    timeout,
    memory,
    deadLetterArn,
    vpcConfig,
    environment,
    lambdaRuntime,
    ""
  )

  def updateWithFunctionName = {
    val functionName = "my-function-name"
    val client = new NotImplementedAwsLambdaWrapper {
      override def updateFunctionConfiguration(req: UpdateFunctionConfigurationRequest) = {
        assert(req.functionName == functionName)
        Failure(new Throwable)
      }
    }

    updateLambdaConfig(client, lambdaName = LambdaName(functionName))
  }

  def updateWithHandler = {
    val handler = "my-handler"
    val client = new NotImplementedAwsLambdaWrapper {
      override def updateFunctionConfiguration(req: UpdateFunctionConfigurationRequest) = {
        assert(req.handler == handler)
        Failure(new Throwable)
      }
    }

    updateLambdaConfig(client, handlerName = HandlerName(handler))
  }

  def updateWithRole = {
    val role = "my-role"
    val client = new NotImplementedAwsLambdaWrapper {
      override def updateFunctionConfiguration(req: UpdateFunctionConfigurationRequest) = {
        assert(req.role == role)
        Failure(new Throwable)
      }
    }

    updateLambdaConfig(client, roleArn = RoleARN(role))
  }

  def updateWithRuntime = {
    val client = new NotImplementedAwsLambdaWrapper {
      override def updateFunctionConfiguration(req: UpdateFunctionConfigurationRequest) = {
        assert(req.runtime.name == "java8")
        Failure(new Throwable)
      }
    }

    updateLambdaConfig(client)
  }

  def updateWithEnvironment = {
    val environmentVariables = Map("a" -> "1", "b" -> "2", "c" -> "3").asJava
    val client = new NotImplementedAwsLambdaWrapper {
      override def updateFunctionConfiguration(req: UpdateFunctionConfigurationRequest) = {
        assert(req.environment.variables == environmentVariables)
        Failure(new Throwable)
      }
    }

    updateLambdaConfig(client, environment = new Environment().withVariables(environmentVariables))
  }

  def updateWithTimeout = {
    val timeout = 1
    val client = new NotImplementedAwsLambdaWrapper {
      override def updateFunctionConfiguration(req: UpdateFunctionConfigurationRequest) = {
        assert(req.timeout.intValue == timeout.intValue)
        Failure(new Throwable)
      }
    }

    updateLambdaConfig(client, timeout = Some(Timeout(timeout)))
  }

  def updateWithoutTimeout = {
    val client = new NotImplementedAwsLambdaWrapper {
      override def updateFunctionConfiguration(req: UpdateFunctionConfigurationRequest) = {
        assert(req.timeout == null)
        Failure(new Throwable)
      }
    }

    updateLambdaConfig(client, timeout = None)
  }

  def updateWithMemory = {
    val memory = 128
    val client = new NotImplementedAwsLambdaWrapper {
      override def updateFunctionConfiguration(req: UpdateFunctionConfigurationRequest) = {
        assert(req.getMemorySize == memory)
        Failure(new Throwable)
      }
    }

    updateLambdaConfig(client, memory = Some(Memory(memory)))
  }

  def updateWithoutMemory = {
    val client = new NotImplementedAwsLambdaWrapper {
      override def updateFunctionConfiguration(req: UpdateFunctionConfigurationRequest) = {
        assert(req.getMemorySize == null)
        Failure(new Throwable)
      }
    }

    updateLambdaConfig(client, memory = None)
  }

  def updateWithVpcConfig = {
    val vpcConfig = new VpcConfig().withSubnetIds("abc")
    val client = new NotImplementedAwsLambdaWrapper {
      override def updateFunctionConfiguration(req: UpdateFunctionConfigurationRequest) = {
        assert(req.getVpcConfig == vpcConfig)
        Failure(new Throwable)
      }
    }

    updateLambdaConfig(client, vpcConfig = Some(vpcConfig))
  }

  def updateWithoutVpcConfig = {
    val client = new NotImplementedAwsLambdaWrapper {
      override def updateFunctionConfiguration(req: UpdateFunctionConfigurationRequest) = {
        assert(req.getVpcConfig == null)
        Failure(new Throwable)
      }
    }

    updateLambdaConfig(client, vpcConfig = None)
  }

  def updateWithDeadLetterConfig = {
    val deadLetter = "dead-letter"
    val client = new NotImplementedAwsLambdaWrapper {
      override def updateFunctionConfiguration(req: UpdateFunctionConfigurationRequest) = {
        assert(req.getDeadLetterConfig.getTargetArn == deadLetter)
        Failure(new Throwable)
      }
    }

    updateLambdaConfig(client, deadLetterArn = Some(DeadLetterARN(deadLetter)))
  }

  def updateWithoutDeadLetterConfig = {
    val client = new NotImplementedAwsLambdaWrapper {
      override def updateFunctionConfiguration(req: UpdateFunctionConfigurationRequest) = {
        assert(req.getDeadLetterConfig == null)
        Failure(new Throwable)
      }
    }

    updateLambdaConfig(client, deadLetterArn = None)
  }

  def createFunction(
    client: wrapper.AwsLambda,
    lambdaName: LambdaName = LambdaName(""),
    handlerName: HandlerName = HandlerName(""),
    roleArn: RoleARN = RoleARN(""),
    timeout:  Option[Timeout] = None,
    memory: Option[Memory] = None,
    deadLetterArn: Option[DeadLetterARN] = None,
    vpcConfig: Option[VpcConfig] = None,
    functionCode: FunctionCode = new FunctionCode(),
    environment: Environment = new Environment(),
    lambdaRuntime: Runtime = Runtime.Java8,
  ) = new AwsLambda(client).createLambda(
    lambdaName,
    handlerName,
    roleArn,
    timeout,
    memory,
    deadLetterArn,
    vpcConfig,
    functionCode,
    environment,
    lambdaRuntime,
    ""
  )

  def createWithFunctionName = {
    val functionName = "my-function-name"
    val client = new NotImplementedAwsLambdaWrapper {
      override def createFunction(req: CreateFunctionRequest) = {
        assert(req.getFunctionName == functionName)
        Failure(new Throwable)
      }
    }

    createFunction(client, lambdaName = LambdaName(functionName))
  }

  def createWithHandler = {
    val handler = "my-handler"
    val client = new NotImplementedAwsLambdaWrapper {
      override def createFunction(req: CreateFunctionRequest) = {
        assert(req.getHandler == handler)
        Failure(new Throwable)
      }
    }

    createFunction(client, handlerName = HandlerName(handler))
  }

  def createWithRole = {
    val role = "my-role"
    val client = new NotImplementedAwsLambdaWrapper {
      override def createFunction(req: CreateFunctionRequest) = {
        assert(req.getRole == role)
        Failure(new Throwable)
      }
    }

    createFunction(client, roleArn = RoleARN(role))
  }

  def createWithRuntime = {
    val client = new NotImplementedAwsLambdaWrapper {
      override def createFunction(req: CreateFunctionRequest) = {
        assert(req.getRuntime == "java8")
        Failure(new Throwable)
      }
    }

    createFunction(client)
  }

  def createWithEnvironment = {
    val environmentVariables = Map("a" -> "1", "b" -> "2", "c" -> "3").asJava
    val client = new NotImplementedAwsLambdaWrapper {
      override def createFunction(req: CreateFunctionRequest) = {
        assert(req.getEnvironment.getVariables() == environmentVariables)
        Failure(new Throwable)
      }
    }

    createFunction(client, environment = new Environment().withVariables(environmentVariables))
  }

  def createWithFunctionCode = {
    val s3Key = "my-s3-key"
    val client = new NotImplementedAwsLambdaWrapper {
      override def createFunction(req: CreateFunctionRequest) = {
        assert(req.getCode.getS3Key() == s3Key)
        Failure(new Throwable)
      }
    }

    createFunction(client, functionCode = new FunctionCode().withS3Key(s3Key))
  }

  def createWithTimeout = {
    val timeout = 1
    val client = new NotImplementedAwsLambdaWrapper {
      override def createFunction(req: CreateFunctionRequest) = {
        assert(req.getTimeout == timeout)
        Failure(new Throwable)
      }
    }

    createFunction(client, timeout = Some(Timeout(timeout)))
  }

  def createWithoutTimeout = {
    val client = new NotImplementedAwsLambdaWrapper {
      override def createFunction(req: CreateFunctionRequest) = {
        assert(req.getTimeout == null)
        Failure(new Throwable)
      }
    }

    createFunction(client, timeout = None)
  }

  def createWithMemory = {
    val memory = 128
    val client = new NotImplementedAwsLambdaWrapper {
      override def createFunction(req: CreateFunctionRequest) = {
        assert(req.getMemorySize == memory)
        Failure(new Throwable)
      }
    }

    createFunction(client, memory = Some(Memory(memory)))
  }

  def createWithoutMemory = {
    val client = new NotImplementedAwsLambdaWrapper {
      override def createFunction(req: CreateFunctionRequest) = {
        assert(req.getMemorySize == null)
        Failure(new Throwable)
      }
    }

    createFunction(client, memory = None)
  }

  def createWithVpcConfig = {
    val vpcConfig = new VpcConfig().withSubnetIds("abc")
    val client = new NotImplementedAwsLambdaWrapper {
      override def createFunction(req: CreateFunctionRequest) = {
        assert(req.getVpcConfig == vpcConfig)
        Failure(new Throwable)
      }
    }

    createFunction(client, vpcConfig = Some(vpcConfig))
  }

  def createWithoutVpcConfig = {
    val client = new NotImplementedAwsLambdaWrapper {
      override def createFunction(req: CreateFunctionRequest) = {
        assert(req.getVpcConfig == null)
        Failure(new Throwable)
      }
    }

    createFunction(client, vpcConfig = None)
  }

  def createWithDeadLetterConfig = {
    val deadLetter = "dead-letter"
    val client = new NotImplementedAwsLambdaWrapper {
      override def createFunction(req: CreateFunctionRequest) = {
        assert(req.getDeadLetterConfig.getTargetArn == deadLetter)
        Failure(new Throwable)
      }
    }

    createFunction(client, deadLetterArn = Some(DeadLetterARN(deadLetter)))
  }

  def createWithoutDeadLetterConfig = {
    val client = new NotImplementedAwsLambdaWrapper {
      override def createFunction(req: CreateFunctionRequest) = {
        assert(req.getDeadLetterConfig == null)
        Failure(new Throwable)
      }
    }

    createFunction(client, deadLetterArn = None)
  }
}
