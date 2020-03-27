package com.gilt.aws.lambda

import java.util.{Collections, Map => JMap}

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}
import com.amazonaws.services.lambda.model.{Environment, FunctionCode, Runtime, UpdateFunctionCodeRequest, VpcConfig}
import sbt.Keys._
import sbt._

object AwsLambdaPlugin extends AutoPlugin {

  object autoImport {
    val configureLambda = taskKey[Map[String, LambdaARN]]("Creates a new AWS Lambda if it doesn't exist, or updates it if the configuration has changed.")
    val deployLambda = taskKey[Map[String, LambdaARN]]("Packages and deploys the current project to an existing AWS Lambda")
    val createLambda = taskKey[Map[String, LambdaARN]]("[Deprecated] Create a new AWS Lambda function from the current project. Use configureLambda instead.")
    val updateLambda = taskKey[Map[String, LambdaARN]]("[Deprecated] Packages and deploys the current project to an existing AWS Lambda. Use deployLambda instead.")

    val s3Bucket = settingKey[Option[String]]("ID of the S3 bucket where the jar will be uploaded")
    val s3KeyPrefix = settingKey[String]("The prefix to the S3 key where the jar will be uploaded")
    val lambdaName = settingKey[Option[String]]("Name of the AWS Lambda to update")
    val handlerName = settingKey[Option[String]]("Name of the handler to be executed by AWS Lambda")
    val roleArn = settingKey[Option[String]]("ARN of the IAM role for the Lambda function")
    val region = settingKey[Option[String]]("Name of the AWS region to connect to")
    val awsLambdaTimeout = settingKey[Option[Int]]("The Lambda timeout length in seconds (1-900)")
    val awsLambdaMemory = settingKey[Option[Int]]("The amount of memory in MB for the Lambda function (128-1536, multiple of 64)")
    val lambdaHandlers = settingKey[Seq[(String, String)]]("A sequence of pairs of Lambda function names to handlers (for multiple handlers in one jar)")
    val deployMethod = settingKey[Option[String]]("S3 for using an S3 bucket to upload the jar or DIRECT for directly uploading a jar file.")
    val deadLetterArn = settingKey[Option[String]]("ARN of the Dead Letter Queue or Topic to send unprocessed messages")
    val vpcConfigSubnetIds = settingKey[Option[String]]("Comma separated list of subnet IDs for the VPC")
    val vpcConfigSecurityGroupIds = settingKey[Option[String]]("Comma separated list of security group IDs for the VPC")
    val environment = settingKey[Seq[(String, String)]]("A sequence of environment keys and values")
    val lambdaRuntime = settingKey[String](s"""The Lambda Runtime. Type 'supportedRuntimes' to confirming supported runtimes""")
    val packageLambda = taskKey[File]("The action to package the lambda jar file")
    private[lambda] val resolveRuntime = taskKey[Runtime]("Resolve lambda runtime")
    private[lambda] val supportedLambdaRuntimes = settingKey[List[String]]("Supported runtime")
  }

  import autoImport._

  override def requires = sbtassembly.AssemblyPlugin

  override lazy val projectSettings = Seq(
    configureLambda := doConfigureLambda(
      lambdaName = lambdaName.value,
      region = region.value,
      handlerName = handlerName.value,
      lambdaHandlers = lambdaHandlers.value,
      roleArn = roleArn.value,
      timeout = awsLambdaTimeout.value,
      memory = awsLambdaMemory.value,
      deadLetterArn = deadLetterArn.value,
      vpcConfigSubnetIds = vpcConfigSubnetIds.value,
      vpcConfigSecurityGroupIds = vpcConfigSecurityGroupIds.value,
      environment = environment.value.toMap,
      lambdaRuntime = lambdaRuntime.value,
      deployMethod = deployMethod.value,
      jar = packageLambda.value,
      s3Bucket = s3Bucket.value,
      s3KeyPrefix = s3KeyPrefix.?.value,
      version = version.value,
      resolvedRuntime = resolveRuntime.value
    ),
    deployLambda := doDeployLambda(
      deployMethod = deployMethod.value,
      region = region.value,
      jar = packageLambda.value,
      s3Bucket = s3Bucket.value,
      s3KeyPrefix = s3KeyPrefix.?.value,
      lambdaName = lambdaName.value,
      handlerName = handlerName.value,
      lambdaHandlers = lambdaHandlers.value,
      version = version.value
    ),
    createLambda := doCreateLambda(
      deployMethod = deployMethod.value,
      region = region.value,
      jar = packageLambda.value,
      s3Bucket = s3Bucket.value,
      s3KeyPrefix = s3KeyPrefix.?.value,
      lambdaName = lambdaName.value,
      handlerName = handlerName.value,
      lambdaHandlers = lambdaHandlers.value,
      roleArn = roleArn.value,
      timeout = awsLambdaTimeout.value,
      memory = awsLambdaMemory.value,
      deadLetterArn = deadLetterArn.value,
      vpcConfigSubnetIds = vpcConfigSubnetIds.value,
      vpcConfigSecurityGroupIds = vpcConfigSecurityGroupIds.value,
      environment = environment.value.toMap,
      lambdaRuntime = lambdaRuntime.value,
      version = version.value,
      resolvedRuntime = resolveRuntime.value
    ),
    updateLambda := doDeployLambda(
      deployMethod = deployMethod.value,
      region = region.value,
      jar = packageLambda.value,
      s3Bucket = s3Bucket.value,
      s3KeyPrefix = s3KeyPrefix.?.value,
      lambdaName = lambdaName.value,
      handlerName = handlerName.value,
      lambdaHandlers = lambdaHandlers.value,
      version = version.value
    ),
    resolveRuntime := Def.task {
      val resolvedRuntime = lambdaRuntime.value
      val resolvedSupportedRuntime = supportedLambdaRuntimes.value
        resolveLambdaRuntime(promptUserForLambdaRuntime(resolvedRuntime, resolvedSupportedRuntime), resolvedSupportedRuntime)
    }.value,
    s3Bucket := None,
    lambdaName := Some(sbt.Keys.name.value),
    handlerName := None,
    lambdaHandlers := List.empty[(String, String)],
    roleArn := None,
    region := None,
    deployMethod := Some("S3"),
    awsLambdaMemory := None,
    awsLambdaTimeout := None,
    deadLetterArn := None,
    vpcConfigSubnetIds := None,
    vpcConfigSecurityGroupIds := None,
    environment := Nil,
    supportedLambdaRuntimes := List(
      Runtime.Java8,
      Runtime.Java11
    ).map(_.toString),
    lambdaRuntime := supportedLambdaRuntimes.value.head,
    packageLambda := sbtassembly.AssemblyKeys.assembly.value
  )

  private def doDeployLambda(
    deployMethod: Option[String],
    region: Option[String],
    jar: File, s3Bucket: Option[String],
    s3KeyPrefix: Option[String],
    lambdaName: Option[String],
    handlerName: Option[String],
    lambdaHandlers: Seq[(String, String)],
    version: String,
  ): Map[String, LambdaARN] = {
    val resolvedDeployMethod = resolveDeployMethod(deployMethod)
    val resolvedRegion = resolveRegion(region)
    val resolvedLambdaHandlers = resolveLambdaHandlers(lambdaName, handlerName, lambdaHandlers)

    val s3Client = new AwsS3(wrapper.AmazonS3.instance(resolvedRegion))

    if (resolvedDeployMethod.value == "S3") {
      val resolvedBucketId = resolveBucketId(resolvedRegion, s3Bucket)
      val resolvedS3KeyPrefix = resolveS3KeyPrefix(s3KeyPrefix)

      s3Client.pushJarToS3(jar, resolvedBucketId, resolvedS3KeyPrefix) match {
        case Success(s3Key) =>
          (for (resolvedLambdaName <- resolvedLambdaHandlers.keys) yield {
            val updateFunctionCodeRequest = new UpdateFunctionCodeRequest()
              .withFunctionName(resolvedLambdaName.value)
              .withS3Bucket(resolvedBucketId.value)
              .withS3Key(s3Key.value)

            updateFunctionCode(resolvedRegion, resolvedLambdaName, updateFunctionCodeRequest, version)
          }).toMap
        case Failure(exception) =>
          sys.error(s"Error uploading jar to S3 lambda: ${formatException(exception)}")
      }
    } else if (resolvedDeployMethod.value == "DIRECT") {
      (for (resolvedLambdaName <- resolvedLambdaHandlers.keys) yield {
        val updateFunctionCodeRequest = new UpdateFunctionCodeRequest()
          .withFunctionName(resolvedLambdaName.value)
          .withZipFile(FileOps.fileToBuffer(jar))

        updateFunctionCode(resolvedRegion, resolvedLambdaName, updateFunctionCodeRequest, version)
      }).toMap
    } else
      sys.error(s"Unsupported deploy method: ${resolvedDeployMethod.value}")
  }

  def updateFunctionCode(
    resolvedRegion: Region,
    resolvedLambdaName: LambdaName,
    updateFunctionCodeRequest: UpdateFunctionCodeRequest,
    version: String,
  ): (String, LambdaARN) = {
    val lambdaClient = new AwsLambda(wrapper.AwsLambda.instance(resolvedRegion))
    lambdaClient.updateLambdaWithFunctionCodeRequest(updateFunctionCodeRequest, version) match {
      case Success(updateFunctionCodeResult) =>
        lambdaClient.tagLambda(updateFunctionCodeResult.getFunctionArn, version)
        resolvedLambdaName.value -> LambdaARN(updateFunctionCodeResult.getFunctionArn)
      case Failure(exception) =>
        sys.error(s"Error updating lambda: ${formatException(exception)}")
    }
  }

  def computeEnvironment(
    current_ : JMap[String, String],
    desired: Map[String, String],
  ): (Boolean, Environment) = {
    val current = Option(current_).fold(Map.empty[String, String])(_.asScala.toMap)

    // We want to keep env vars that are set in the lambda but missing in the settings, as they may have been added manually (eg. encrypted env vars)
    val currentWithoutUnlisted = current -- (current.keySet -- desired.keySet)
    val needsUpdate = currentWithoutUnlisted != desired
    needsUpdate -> new Environment().withVariables((current ++ desired).asJava)
  }

  private def doConfigureLambda(
    lambdaName: Option[String],
    region: Option[String],
    handlerName: Option[String],
    lambdaHandlers: Seq[(String, String)],
    roleArn: Option[String],
    timeout: Option[Int],
    memory: Option[Int],
    deadLetterArn: Option[String],
    vpcConfigSubnetIds: Option[String],
    vpcConfigSecurityGroupIds: Option[String],
    environment: Map[String, String],
    lambdaRuntime: String,
    deployMethod: Option[String],
    jar: File, s3Bucket: Option[String],
    s3KeyPrefix: Option[String],
    version: String,
    resolvedRuntime: Runtime
  ): Map[String, LambdaARN] = {
    val resolvedLambdaHandlers = resolveLambdaHandlers(lambdaName, handlerName, lambdaHandlers)
    val resolvedRegion = resolveRegion(region)
    val resolvedRoleName = resolveRoleARN(roleArn)
    val resolvedTimeout = resolveTimeout(timeout)
    val resolvedMemory = resolveMemory(memory)
    val resolvedDeadLetterArn = resolveDeadLetterARN(deadLetterArn)
    val resolvedVpcConfigSubnetIds = resolveVpcConfigSubnetIds(vpcConfigSubnetIds)
    val resolvedVpcConfigSecurityGroupIds = resolveVpcConfigSecurityGroupIds(vpcConfigSecurityGroupIds)

    val resolvedVpcConfig = {
      val config_ = resolvedVpcConfigSubnetIds.map(ids => new VpcConfig().withSubnetIds(ids.value.split(",") :_*))
      resolvedVpcConfigSecurityGroupIds.fold(config_)(ids => Some(config_.getOrElse(new VpcConfig()).withSecurityGroupIds(ids.value.split(",") :_*)))
    }

    val lambdaClient = new AwsLambda(wrapper.AwsLambda.instance(resolvedRegion))
    val s3Client = new AwsS3(wrapper.AmazonS3.instance(resolvedRegion))

    for ((resolvedLambdaName, resolvedHandlerName) <- resolvedLambdaHandlers) yield {
      lambdaClient.getLambdaConfig(resolvedLambdaName).flatMap { configOpt =>
        configOpt.fold {
          println(s"Creating new lambda: ${resolvedLambdaName.value}")
          val (_, resolvedEnvironment) = computeEnvironment(Collections.emptyMap(), environment)
          val functionCode = {
            val resolvedDeployMethod = resolveDeployMethod(deployMethod).value
            if (resolvedDeployMethod == "S3") {
              val resolvedBucketId = resolveBucketId(resolvedRegion, s3Bucket)
              val resolvedS3KeyPrefix = resolveS3KeyPrefix(s3KeyPrefix)
              s3Client.pushJarToS3(jar, resolvedBucketId, resolvedS3KeyPrefix) match {
                case Success(_) =>
                  new FunctionCode().withS3Bucket(resolvedBucketId.value).withS3Key(jar.getName)
                case Failure(exception) =>
                  sys.error(s"Error upload jar to S3 lambda: ${formatException(exception)}")
              }
            } else if (resolvedDeployMethod == "DIRECT") {
              new FunctionCode().withZipFile(FileOps.fileToBuffer(jar))
            } else {
              sys.error(s"Unsupported deploy method: $resolvedDeployMethod")
            }
          }
          lambdaClient.createLambda(
            resolvedLambdaName,
            resolvedHandlerName,
            resolvedRoleName,
            resolvedTimeout,
            resolvedMemory,
            resolvedDeadLetterArn,
            resolvedVpcConfig,
            functionCode,
            resolvedEnvironment,
            resolvedRuntime,
            version,
          ).map(_.getFunctionArn)
        }{ currentConfig =>
          val currentEnv = Option(currentConfig.getEnvironment).fold(Collections.emptyMap[String, String]())(_.getVariables)
          val (envUpdated, resolvedEnvironment) = computeEnvironment(currentEnv, environment)
          if (currentConfig.getHandler != resolvedHandlerName.value ||
              currentConfig.getRole != resolvedRoleName.value ||
              currentConfig.getRuntime != resolvedRuntime.toString ||
              envUpdated ||
              resolvedTimeout.exists(t => Integer.valueOf(t.value) != currentConfig.getTimeout) ||
              resolvedMemory.exists(m => Integer.valueOf(m.value) != currentConfig.getMemorySize) ||
              resolvedVpcConfig.exists(vpn => currentConfig.getVpcConfig == null || vpn.getSecurityGroupIds != currentConfig.getVpcConfig.getSecurityGroupIds || vpn.getSubnetIds != currentConfig.getVpcConfig.getSubnetIds)
          ) {
            println(s"Updating existing lambda: ${resolvedLambdaName.value}")
            lambdaClient.updateLambdaConfig(
              resolvedLambdaName,
              resolvedHandlerName,
              resolvedRoleName,
              resolvedTimeout,
              resolvedMemory,
              resolvedDeadLetterArn,
              resolvedVpcConfig,
              resolvedEnvironment,
              resolvedRuntime,
              version,
            ).map(_.getFunctionArn)
          } else {
            println(s"Skipping unchanged lambda: ${resolvedLambdaName.value}")
            Success(currentConfig.getFunctionArn)
          }
        }
      } match {
        case Success(functionArn) =>
          resolvedLambdaName.value -> LambdaARN(functionArn)
        case Failure(exception) =>
          sys.error(s"Failed to create or update lambda function ${resolvedLambdaName.value}: ${formatException(exception)}")
      }
    }
  }

  private def doCreateLambda(
    deployMethod: Option[String],
    region: Option[String],
    jar: File,
    s3Bucket: Option[String],
    s3KeyPrefix: Option[String],
    lambdaName: Option[String],
    handlerName: Option[String],
    lambdaHandlers: Seq[(String, String)],
    roleArn: Option[String],
    timeout: Option[Int],
    memory: Option[Int],
    deadLetterArn: Option[String],
    vpcConfigSubnetIds: Option[String],
    vpcConfigSecurityGroupIds: Option[String],
    environment: Map[String, String],
    lambdaRuntime: String,
    version: String,
    resolvedRuntime: Runtime
  ): Map[String, LambdaARN] = {
    val resolvedDeployMethod = resolveDeployMethod(deployMethod).value
    val resolvedRegion = resolveRegion(region)
    val resolvedLambdaHandlers = resolveLambdaHandlers(lambdaName, handlerName, lambdaHandlers)
    val resolvedRoleName = resolveRoleARN(roleArn)
    val resolvedTimeout = resolveTimeout(timeout)
    val resolvedMemory = resolveMemory(memory)
    val resolvedDeadLetterArn = resolveDeadLetterARN(deadLetterArn)
    val resolvedVpcConfigSubnetIds = resolveVpcConfigSubnetIds(vpcConfigSubnetIds)
    val resolvedVpcConfigSecurityGroupIds = resolveVpcConfigSecurityGroupIds(vpcConfigSecurityGroupIds)
    val resolvedVpcConfig = {
      val config_ = resolvedVpcConfigSubnetIds.map(ids => new VpcConfig().withSubnetIds(ids.value.split(",") :_*))
      resolvedVpcConfigSecurityGroupIds.fold(config_)(ids => Some(config_.getOrElse(new VpcConfig()).withSecurityGroupIds(ids.value.split(",") :_*)))
    }
    val (_, resolvedEnvironment) = computeEnvironment(Collections.emptyMap(), environment)

    val s3Client = new AwsS3(wrapper.AmazonS3.instance(resolvedRegion))

    if (resolvedDeployMethod == "S3") {
      val resolvedBucketId = resolveBucketId(resolvedRegion, s3Bucket)
      val resolvedS3KeyPrefix = resolveS3KeyPrefix(s3KeyPrefix)
      s3Client.pushJarToS3(jar, resolvedBucketId, resolvedS3KeyPrefix) match {
        case Success(_) =>
          for ((resolvedLambdaName, resolvedHandlerName) <- resolvedLambdaHandlers) yield {
            val functionCode = new FunctionCode().withS3Bucket(resolvedBucketId.value).withS3Key(jar.getName)

            createLambdaWithFunctionCode(
              resolvedRegion,
              resolvedRoleName,
              resolvedTimeout,
              resolvedMemory,
              resolvedLambdaName,
              resolvedHandlerName,
              resolvedDeadLetterArn,
              resolvedVpcConfig,
              functionCode,
              resolvedEnvironment,
              resolvedRuntime,
              version,
            )
          }
        case Failure(exception) =>
          sys.error(s"Error upload jar to S3 lambda: ${formatException(exception)}")
      }
    } else if (resolvedDeployMethod == "DIRECT") {
      for ((resolvedLambdaName, resolvedHandlerName) <- resolvedLambdaHandlers) yield {
        val functionCode = new FunctionCode().withZipFile(FileOps.fileToBuffer(jar))

        createLambdaWithFunctionCode(
          resolvedRegion,
          resolvedRoleName,
          resolvedTimeout,
          resolvedMemory,
          resolvedLambdaName,
          resolvedHandlerName,
          resolvedDeadLetterArn,
          resolvedVpcConfig,
          functionCode,
          resolvedEnvironment,
          resolvedRuntime,
          version,
        )
      }
    } else {
      sys.error(s"Unsupported deploy method: $resolvedDeployMethod")
    }
  }

  def createLambdaWithFunctionCode(
    resolvedRegion: Region,
    resolvedRoleName: RoleARN,
    resolvedTimeout: Option[Timeout],
    resolvedMemory: Option[Memory],
    resolvedLambdaName: LambdaName,
    resolvedHandlerName: HandlerName,
    resolvedDeadLetterArn: Option[DeadLetterARN],
    vpcConfig: Option[VpcConfig],
    functionCode: FunctionCode,
    environment: Environment,
    resolvedRuntime: Runtime,
    version: String,
  ): (String, LambdaARN) = {
    val lambdaClient = new AwsLambda(wrapper.AwsLambda.instance(resolvedRegion))
    lambdaClient.createLambda(
      resolvedLambdaName,
      resolvedHandlerName,
      resolvedRoleName,
      resolvedTimeout,
      resolvedMemory,
      resolvedDeadLetterArn,
      vpcConfig,
      functionCode,
      environment,
      resolvedRuntime,
      version,
    ) match {
      case Success(createFunctionCodeResult) =>
        resolvedLambdaName.value -> LambdaARN(createFunctionCodeResult.getFunctionArn)
      case Failure(exception) =>
        sys.error(s"Failed to create lambda function: ${formatException(exception)}")
    }
  }

  private def resolveRegion(
    sbtSettingValueOpt: Option[String],
  ): Region = {
    sbtSettingValueOpt.orElse(sys.env.get(EnvironmentVariables.region)).map(Region).getOrElse(promptUserForRegion())
  }

  private def resolveDeployMethod(
    sbtSettingValueOpt: Option[String],
  ): DeployMethod = {
    sbtSettingValueOpt.orElse(sys.env.get(EnvironmentVariables.deployMethod)).map(DeployMethod).getOrElse(promptUserForDeployMethod())
  }

  private def resolveBucketId(
    region: Region,
    sbtSettingValueOpt: Option[String],
  ): S3BucketId = {
    sbtSettingValueOpt.orElse(sys.env.get(EnvironmentVariables.bucketId)).map(S3BucketId).getOrElse(promptUserForS3BucketId(region))
  }

  private def resolveS3KeyPrefix(
    sbtSettingValueOpt: Option[String],
  ): String = {
    sbtSettingValueOpt.orElse(sys.env.get(EnvironmentVariables.s3KeyPrefix)).getOrElse("")
  }

  private def resolveLambdaHandlers(
    lambdaName: Option[String],
    handlerName: Option[String],
    lambdaHandlers: Seq[(String, String)],
  ): Map[LambdaName, HandlerName] = {
    val lhs = if (lambdaHandlers.nonEmpty) lambdaHandlers.iterator else {
      val l = lambdaName.getOrElse(sys.env.getOrElse(EnvironmentVariables.lambdaName, promptUserForFunctionName()))
      val h = handlerName.getOrElse(sys.env.getOrElse(EnvironmentVariables.handlerName, promptUserForHandlerName()))
      Iterator(l -> h)
    }
    lhs.map { case (l, h) => LambdaName(l) -> HandlerName(h) }.toMap
  }

  private def resolveRoleARN(
    sbtSettingValueOpt: Option[String],
  ): RoleARN = {
    sbtSettingValueOpt.orElse(sys.env.get(EnvironmentVariables.roleArn)).map(RoleARN(_)).getOrElse(promptUserForRoleARN())
  }

  private def resolveTimeout(
    sbtSettingValueOpt: Option[Int],
  ): Option[Timeout] = {
    sbtSettingValueOpt.orElse(sys.env.get(EnvironmentVariables.timeout).map(_.toInt)).map(Timeout(_))
  }

  private def resolveMemory(
    sbtSettingValueOpt: Option[Int],
  ): Option[Memory] = {
    sbtSettingValueOpt.orElse(sys.env.get(EnvironmentVariables.memory).map(_.toInt)).map(Memory(_))
  }

  private def resolveDeadLetterARN(
    sbtSettingValueOpt: Option[String],
  ): Option[DeadLetterARN] = {
    sbtSettingValueOpt.orElse(sys.env.get(EnvironmentVariables.deadLetterArn)).map(DeadLetterARN(_))
  }

  private def resolveVpcConfigSubnetIds(
    sbtSettingValueOpt: Option[String],
  ): Option[VpcConfigSubnetIds] = {
    sbtSettingValueOpt.orElse(sys.env.get(EnvironmentVariables.vpcConfigSubnetIds)).map(VpcConfigSubnetIds(_))
  }

  private def resolveVpcConfigSecurityGroupIds(
    sbtSettingValueOpt: Option[String],
  ): Option[VpcConfigSecurityGroupIds] = {
    sbtSettingValueOpt.orElse(sys.env.get(EnvironmentVariables.vpcConfigSecurityGroupIds)).map(VpcConfigSecurityGroupIds(_))
  }

  def resolveLambdaRuntime(
    lambdaRuntime: String, supportedLambdaRuntimes: List[String]
  ): Runtime = {
    if (!supportedLambdaRuntimes.contains(lambdaRuntime)) resolveLambdaRuntime(promptUserForLambdaRuntime(lambdaRuntime, supportedLambdaRuntimes), supportedLambdaRuntimes)
    else {
      Try(Runtime.fromValue(lambdaRuntime)).toOption.getOrElse(
        resolveLambdaRuntime(promptUserForLambdaRuntime(lambdaRuntime, supportedLambdaRuntimes), supportedLambdaRuntimes)
      )
    }
  }

  private def promptUserForRegion(
  ): Region = {
    val inputValue = readInput(s"Enter the name of the AWS region to connect to. (You also could have set the environment variable: ${EnvironmentVariables.region} or the sbt setting: region)")

    Region(inputValue)
  }

  private def promptUserForDeployMethod(
  ): DeployMethod = {
    val inputValue = readInput(s"Enter the method of deploy you want to use (S3 or DIRECT). (You also could have set the environment variable: ${EnvironmentVariables.deployMethod} or the sbt setting: deployMethod)")

    DeployMethod(inputValue)
  }

  private def promptUserForS3BucketId(
    resolvedRegion: Region,
  ): S3BucketId = {
    val inputValue = readInput(s"Enter the AWS S3 bucket where the lambda jar will be stored. (You also could have set the environment variable: ${EnvironmentVariables.bucketId} or the sbt setting: s3Bucket)")
    val bucketId = S3BucketId(inputValue)

    val s3Client = new AwsS3(wrapper.AmazonS3.instance(resolvedRegion))

    s3Client.getBucket(bucketId).map (_ => bucketId).getOrElse {
      val createBucket = readInput(s"Bucket $inputValue does not exist. Create it now? (y/n)")

      if(createBucket == "y") {
        s3Client.createBucket(bucketId) match {
          case Success(createdBucketId) =>
            createdBucketId
          case Failure(th) =>
            println(s"Failed to create S3 bucket: ${th.getLocalizedMessage}")
            promptUserForS3BucketId(resolvedRegion)
        }
      }
      else promptUserForS3BucketId(resolvedRegion)
    }
  }

  private def promptUserForFunctionName(
  ): String = {
    readInput(s"Enter the name of the AWS Lambda. (You also could have set the environment variable: ${EnvironmentVariables.lambdaName} or the sbt setting: lambdaName)")
  }

  private def promptUserForHandlerName(
  ): String = {
    readInput(s"Enter the name of the AWS Lambda handler. (You also could have set the environment variable: ${EnvironmentVariables.handlerName} or the sbt setting: handlerName)")
  }

  private def promptUserForRoleARN(
  ): RoleARN = {
    val iamClient = new AwsIAM(wrapper.AmazonIdentityManagement.instance)
    iamClient.basicLambdaRole() match {
      case Some(basicRole) =>
        val reuseBasicRole = readInput(s"IAM role '${AwsIAM.BasicLambdaRoleName}' already exists. Reuse this role? (y/n)")

        if(reuseBasicRole == "y") RoleARN(basicRole.getArn)
        else readRoleARN()
      case None =>
        val createDefaultRole = readInput(s"Default IAM role for AWS Lambda has not been created yet. Create this role now? (y/n)")

        if(createDefaultRole == "y") {
          iamClient.createBasicLambdaRole() match {
            case Success(createdRole) =>
              createdRole
            case Failure(th) =>
              println(s"Failed to create role: ${th.getLocalizedMessage}")
              promptUserForRoleARN()
          }
        } else readRoleARN()
    }
  }

  private def promptUserForLambdaRuntime(
    invalidRuntime: String, supportedLambdaRuntimes: List[String]
  ): String = {
    readInput(s"Invalid runtime [$invalidRuntime]. Enter the name of the AWS Lambda runtime. Currently supported values are [${supportedLambdaRuntimes.mkString(",")}]")
  }

  private def readRoleARN(
  ): RoleARN = {
    val inputValue = readInput(s"Enter the ARN of the IAM role for the Lambda. (You also could have set the environment variable: ${EnvironmentVariables.roleArn} or the sbt setting: roleArn)")
    RoleARN(inputValue)
  }

  private def readInput(
    prompt: String,
  ): String = {
    SimpleReader.readLine(s"$prompt\n").getOrElse {
      val badInputMessage = "Unable to read input"

      val updatedPrompt = if(prompt.startsWith(badInputMessage)) {
        prompt
      } else {
        s"$badInputMessage\n$prompt"
      }

      readInput(updatedPrompt)
    }
  }

  private def formatException(
    t: Throwable,
  ) = {
    val msg = Option(t.getLocalizedMessage).getOrElse(t.toString)
    s"$msg\n${t.getStackTrace.mkString("","\n","\n")}"
  }

}
