name := "flink-wordcount"
version := "1.0"
scalaVersion := "2.12.17"

val flinkVersion = "1.15.2"

libraryDependencies ++= Seq(
  "org.apache.flink" %% "flink-scala" % flinkVersion % "provided",
  "org.apache.flink" %% "flink-streaming-scala" % flinkVersion % "provided",
  "org.apache.flink" % "flink-connector-kafka" % "3.1.0-1.17",
  // "org.apache.flink" %% "flink-clients" % flinkVersion % "provided"
)

inTask(assembly)(Seq(
  assemblyMergeStrategy := {
    case PathList("META-INF", _@_*) => MergeStrategy.discard
    case _ => MergeStrategy.first
  },
  mainClass := Some("com.example.WordCount"),
  assemblyShadeRules := Seq(
    ShadeRule.rename("org.apache.flink.connector.base.source.**" -> "shaded.org.apache.flink.connector.base.source.@1")
      .inAll
  ),
  assemblyOption ~= {
    _.withIncludeScala(false)
  }
))
