import sbt.*

object AppDependencies {

  private val bootstrapVersion = "10.7.0"

  val compile = Seq(
    play.sbt.PlayImport.ws,
    "uk.gov.hmrc"       %% "play-frontend-hmrc-play-30"    % "12.32.0",
    "uk.gov.hmrc"       %% "bootstrap-frontend-play-30"    % bootstrapVersion,
    "org.typelevel"     %% "cats-core"                     % "2.13.0"
  )

  val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-30"  % bootstrapVersion,
    "org.scalatestplus"       %% "scalacheck-1-18"         % "3.2.19.0"
  ).map(_ % Test)

  def apply(): Seq[ModuleID] = compile ++ test
}
