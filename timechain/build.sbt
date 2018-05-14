import java.nio.file.Files

val `tendermint-version` = "0.16.0"

name := "timechain"

version := "0.1"

scalaVersion := "2.12.4"

fork in run := true

connectInput in run := true

scalacOptions ++= Seq(
  "-Xmacro-settings:materialize-derivations"
  , "-Ypartial-unification"
//  , "-Xlog-implicits"
)

outputStrategy in run := Some(OutputStrategy.StdoutOutput)

resolvers += "jitpack" at "https://jitpack.io"

enablePlugins(JavaAppPackaging)

testFrameworks += new TestFramework("utest.runner.Framework")
libraryDependencies ++= Seq(
  // Networking
  "com.typesafe.akka" %% "akka-actor" % "2.5.8",
  "com.typesafe.akka" %% "akka-stream" % "2.5.8",
  "com.typesafe.akka" %% "akka-http" % "10.1.0-RC1",
  // UI
  "com.github.fomkin" %% "korolev-server-akkahttp" % "0.7.0",
  // Other
  "org.rudogma" %% "supertagged" % "1.4",
  "io.mytc" %% "scala-abci-server" % "0.9.0",
  "com.github.pureconfig" %% "pureconfig" % "0.9.0",
  "org.typelevel" %% "cats-core" % "1.0.1",
  // Cryptography
  "org.whispersystems" % "curve25519-java" % "0.4.1",
  // Marshalling
  "com.tethys-json" %% "tethys" % "0.6.2",
  "org.json4s" %%	"json4s-ast" % "3.5.3",
  "io.suzaku" %% "boopickle" % "1.2.6",
  "com.lightbend.akka"    %% "akka-stream-alpakka-unix-domain-socket" % "0.17",
  "name.pellet.jp" %% "bsonpickle" % "0.4.4.2",


  "com.chuusai" %% "shapeless" % "2.3.3",

  // Tests
  "com.lihaoyi" %% "utest" % "0.6.3" % "test"
)


addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)

// Download tendermint

resourceGenerators in Compile += Def.task {
  def download(version: String, ghSuffix: String, resSuffix: String) = {
    val targetFile = (resourceManaged in Compile).value / s"tendermint_$resSuffix"
    if (!targetFile.exists()) {
      if (!targetFile.getParentFile.exists()) {
        targetFile.getParentFile.mkdirs()
      }
      val url = s"https://github.com/tendermint/tendermint/releases/download/v$version/tendermint_${version}_$ghSuffix.zip"
      sLog.value.info(s"downloading $url")
      val dir = Files.createTempDirectory("tmsbt").toFile
      val unzipped = IO.unzipURL(new URL(url), dir)
      Files.move(unzipped.head.toPath, targetFile.toPath).toFile
    } else {
      targetFile
    }
  }
  Seq(
    download(`tendermint-version`, "darwin_amd64", "macos_x86_64"),
    download(`tendermint-version`, "linux_amd64", "linux_x86_64"),
    download(`tendermint-version`, "windows_amd64", "windows_x86_64.exe")
  )
}.taskValue


lazy val keyvalue = project
lazy val root = (project in file("."))
  .aggregate(keyvalue)
  .dependsOn(keyvalue)