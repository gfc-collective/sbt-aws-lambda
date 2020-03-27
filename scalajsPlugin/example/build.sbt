lazy val root =
  project.in(file("."))
    .settings(
      name := "example",
      organization := "example.com",
      version := "1.0",
      scalaJSLinkerConfig ~= {
        _.withModuleKind(ModuleKind.CommonJSModule)
      },
      lambdaHandlers := Seq(
        "foo"                 -> "handler.foo",
        "bar"                 -> "handler.bar"
      )
    )
    .enablePlugins(
      ScalaJSPlugin,
      AwsLambdaPlugin,
      AwsLambdaScalaJSPlugin)
