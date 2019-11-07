import mill._, scalalib._, scalajslib._, publish._

object castor extends Cross[CastorModule]("2.12.10", "2.13.1")
class CastorModule(crossVersion: String) extends Module {
  trait ActorModule extends CrossScalaModule with PublishModule {
    def publishVersion = "0.1.0"
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

    def ivyDeps = Agg(ivy"com.lihaoyi::sourcecode::0.1.8")

    trait ActorTestModule extends Tests {
      def sources = T.sources(
        millSourcePath / "src",
        millSourcePath / s"src-$platformSegment"
      )
      def testFrameworks = Seq("utest.runner.Framework")
      def ivyDeps = Agg(ivy"com.lihaoyi::utest::0.7.1")
    }
  }

  object js extends ActorModule with ScalaJSModule{
    def platformSegment = "js"
    def scalaJSVersion = "0.6.29"

    object test extends ActorTestModule with Tests
  }
  object jvm extends ActorModule{
    def platformSegment = "jvm"

    object test extends ActorTestModule with Tests{
      def ivyDeps = super.ivyDeps() ++ Agg(
        ivy"com.lihaoyi::os-lib:0.4.2"
      )
    }
  }

}
