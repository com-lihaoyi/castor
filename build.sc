import mill._, scalalib._, scalajslib._, scalanativelib._, publish._

val scalaVersions = Seq("2.12.10", "2.13.1")
val scalaJsVersions = Seq("0.6.32", "1.0.0")
val scalaNativeVersions = Seq("0.3.9", "0.4.0-M2")

object castor extends Module {
  abstract class ActorModule(crossVersion: String) extends CrossScalaModule with PublishModule {
    def publishVersion = "0.1.2"
    def crossScalaVersion = crossVersion
    def pomSettings = PomSettings(
      description = artifactName(),
      organization = "com.lihaoyi",
      url = "https://github.com/lihaoyi/castor",
      licenses = Seq(License.MIT),
      versionControl = VersionControl.github("lihaoyi", "castor"),
      developers = Seq(
        Developer("lihaoyi", "Li Haoyi","https://github.com/lihaoyi")
      )
    )

    def artifactName = "castor"
    def platformSegment: String
    def millSourcePath = super.millSourcePath / os.up

    def sources = T.sources(
      millSourcePath / "src",
      millSourcePath / s"src-$platformSegment"
    )

    def ivyDeps = Agg(ivy"com.lihaoyi::sourcecode::0.2.1")
  }
  trait ActorTestModule extends ScalaModule with TestModule {
    def platformSegment: String
    def sources = T.sources(
      millSourcePath / "src",
      millSourcePath / s"src-$platformSegment"
    )
    def testFrameworks = Seq("utest.runner.Framework")
    def ivyDeps = Agg(ivy"com.lihaoyi::utest::0.7.4")
  }

  val jsVersions = for {
    sv <- scalaVersions
    sjsv <- scalaJsVersions
  } yield (sv, sjsv)
  object js extends Cross[ActorJsModule](jsVersions:_*)
  class ActorJsModule(crossScalaVersion: String, crossScalaJsVersion: String) extends ActorModule(crossScalaVersion) with ScalaJSModule {
    def platformSegment = "js"
    def scalaJSVersion = crossScalaJsVersion
    object test extends ActorTestModule {
      def platformSegment = "js"
      def scalaVersion = crossScalaVersion
    }
  }
  object jvm extends Cross[ActorJvmModule](scalaVersions:_*)
  class ActorJvmModule(crossScalaVersion: String) extends ActorModule(crossScalaVersion) {
    def platformSegment = "jvm"
    object test extends Tests with ActorTestModule{
      def platformSegment: String = "jvm"
      def ivyDeps = super.ivyDeps() ++ Agg(
        ivy"com.lihaoyi::os-lib:0.4.2"
      )
    }
  }
  object native extends Cross[ActorNativeModule](scalaNativeVersions.map("2.11.12" -> _):_*)
  class ActorNativeModule(crossScalaVersion: String, crossScalaNativeVersion: String) extends ActorModule(crossScalaVersion) with ScalaNativeModule {
    def platformSegment = "native"
    def scalaNativeVersion = crossScalaNativeVersion

    object test extends Tests with ActorTestModule {
      def platformSegment = "native"
    }
  }
}
