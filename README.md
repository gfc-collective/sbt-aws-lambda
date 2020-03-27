# sbt-aws-lambda [![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.gfccollective/sbt-aws-lambda/badge.svg?style=plastic)](https://maven-badges.herokuapp.com/maven-central/org.gfccollective/sbt-aws-lambda) [![Build Status](https://github.com/gfc-collective/sbt-aws-lambda/workflows/Scala%20CI/badge.svg)](https://github.com/gfc-collective/sbt-aws-lambda/actions) [![Coverage Status](https://coveralls.io/repos/gfc-collective/sbt-aws-lambda/badge.svg?branch=master&service=github)](https://coveralls.io/github/gfc-collective/sbt-aws-lambda?branch=master)

An sbt plugin to deploy code to AWS Lambda.
A fork and new home of the unmaintained Gilt sbt plugin, now under the [GFC Collective](https://github.com/gfc-collective) umbrella, maintained by some of the original authors.

This plugin is available for sbt 1.x. The latest version (1.2.0) was released on 27/Mar/2020.

Installation
------------

Add the following to your `project/plugins.sbt` file:

```scala
addSbtPlugin("org.gfccollective" % "sbt-aws-lambda" % "1.2.0")
```

Add the `AwsLambdaPlugin` auto-plugin to your build.sbt:

```scala
enablePlugins(AwsLambdaPlugin)
```



Usage
-------------

`sbt configureLambda` Creates a new AWS Lambda if it doesn't exist yet, or updates the Lambda configuration, if it has changed.

`sbt deployLambda` Packages and deploys the current project to an existing AWS Lambda.

Deprecated Usage
-------------

The plugin also has the following deprecated tasks:

`sbt createLambda` creates a new AWS Lambda function from the current project.

`sbt updateLambda` updates an existing AWS Lambda function with the current project.


Configuration
-------------

sbt-aws-lambda can be configured using sbt settings, environment variables or by reading user input at deploy time

| sbt setting   | Environment variable      |  Description |
|:----------|:----------|:---------------|
| s3Bucket |  AWS_LAMBDA_BUCKET_ID | The name of an S3 bucket where the lambda code will be stored |
| s3KeyPrefix | AWS_LAMBDA_S3_KEY_PREFIX | The prefix to the S3 key where the jar will be uploaded |
| lambdaName |    AWS_LAMBDA_NAME   |   The name to use for this AWS Lambda function. Defaults to the project name |
| handlerName | AWS_LAMBDA_HANDLER_NAME |    Java class name and method to be executed, e.g. `com.example.Lambda::myMethod` |
| roleArn | AWS_LAMBDA_IAM_ROLE_ARN |The [ARN](http://docs.aws.amazon.com/general/latest/gr/aws-arns-and-namespaces.html "AWS ARN documentation") of an [IAM](https://aws.amazon.com/iam/ "AWS IAM documentation") role to use when creating a new Lambda |
| region |  AWS_REGION | The name of the AWS region to connect to. Defaults to `us-east-1` |
| awsLambdaTimeout | AWS_LAMBDA_TIMEOUT | The Lambda timeout in seconds (1-900). Defaults to AWS default. |
| awsLambdaMemory | AWS_LAMBDA_MEMORY | The amount of memory in MB for the Lambda function (128-1536, multiple of 64). Defaults to AWS default. |
| lambdaHandlers |              | Sequence of Lambda names to handler functions (for multiple lambda methods per project). Overrides `lambdaName` and `handlerName` if present. | 
| deployMethod | AWS_LAMBDA_DEPLOY_METHOD | The preferred method for uploading the jar, either `S3` for uploading to AWS S3 or `DIRECT` for direct upload to AWS Lambda |
| deadLetterArn | AWS_LAMBDA_DEAD_LETTER_ARN | The [ARN](http://docs.aws.amazon.com/general/latest/gr/aws-arns-and-namespaces.html "AWS ARN documentation") of the Lambda function's dead letter SQS queue or SNS topic, to receive unprocessed messages |
| vpcConfigSubnetIds | AWS_LAMBDA_VPC_CONFIG_SUBNET_IDS | Comma separated list of subnet IDs for the VPC |
| vpcConfigSecurityGroupIds | AWS_LAMBDA_VPC_CONFIG_SECURITY_GROUP_IDS | Comma separated list of security group IDs for the VPC |
| environment  |                | Seq[(String, String)] of environment variables to set in the lambda function |
| lambdaRuntime | | The [Lambda Runtime](https://docs.aws.amazon.com/lambda/latest/dg/lambda-runtimes.html#w501aac27c33 "Runtime documentation") to use. Currently supported values are "java8" and "java11". Default is "java8" |

An example configuration might look like this:


```scala
retrieveManaged := true

enablePlugins(AwsLambdaPlugin)

lambdaHandlers := Seq(
  "function1"                 -> "com.example.Lambda::handleRequest1",
  "function2"                 -> "com.example.Lambda::handleRequest2",
  "function3"                 -> "com.example.OtherLambda::handleRequest3"
)

// or, instead of the above, for just one function/handler
//
// lambdaName := Some("function1")
//
// handlerName := Some("com.example.Lambda::handleRequest1")

s3Bucket := Some("lambda-jars")

awsLambdaMemory := Some(192)

awsLambdaTimeout := Some(30)

roleArn := Some("arn:aws:iam::123456789000:role/lambda_basic_execution")

```
(note that you will need to use a real ARN for your role rather than copying this one).


Customizing the packaging step
------------------------------

By default, sbt-aws-lambda uses [sbt-assembly](https://github.com/sbt/sbt-assembly) to package a fat jar that includes your project and all its dependencies. 
In some cases you may want to override this and provide a different task that sbt-aws-lambda should call to package the jar. To do this, override the `packageLambda` task key with a different task that produces an package file, 
like [sbt-native-package](https://github.com/sbt/sbt-native-packager) or the built-in `packageBin` task:

```scala
// Use sbt-native-packager
packageLambda := (packageBin in Universal).value

``` 
```scala
// Use project jar only
packageLambda := (packageBin in Compile).value

``` 

Scala.js Support
------------------------------
Add the following to your `project/plugins.sbt` file:

```scala
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.0.0")
addSbtPlugin("org.gfccollective" % "sbt-aws-lambda-scalajs" % "1.2.0")
```

Add the following plugins to your build.sbt:

```scala
enablePlugins(ScalaJSPlugin, AwsLambdaPlugin, AwsLambdaScalaJSPlugin)
```

You need to put a javascript to be the handler in the project root. Refer [example project](scalajsPlugin/example).
The runtimes that can be specified in `lambdaRuntime` are` nodejs10.x` and `nodejs12.x`. (Defaults to `nodejs12.x`)
 
Publishing new versions of this plugin
--------------------------------------

This plugin uses [sbt-sonatype](https://github.com/xerial/sbt-sonatype) to publish to maven central

```
sbt publishSigned sonatypeRelease
```
