import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations._
import sbtrelease.ReleaseStateTransformations.commitNextVersion
import sbtrelease.Version.Bump.Bugfix

name         := "rdr-svc-template"
organization := "com.radioretail"
scalaVersion := "2.11.12"

val scalacOpts = Seq(
  "-Ywarn-unused:implicits",
  "-Ywarn-unused:imports",
  "-Ywarn-unused:locals",
  "-Ywarn-unused:params",
  "-Ywarn-unused:patvars",
  "-Ywarn-unused:privates",
  "-deprecation",
  "-encoding", "UTF-8",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-unchecked",
  "-Xlint",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard"
)

scalacOptions ++= scalacOpts

addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")

resolvers ++= {
  Seq(
    "jitpack" at "https://jitpack.io",
    Resolver.sonatypeRepo("releases"),
    Resolver.sonatypeRepo("snapshots"),
    Resolver.githubPackages("fullfacing")
  )
}

resolvers ++= {
  Seq(
    "jitpack" at "https://jitpack.io",
    Resolver.sonatypeRepo("releases"),
    Resolver.sonatypeRepo("snapshots"),
    Resolver.githubPackages("fullfacing")
  )
}

val SKYHOOK_VERSION = "4.5.2"
val COMMON_VERSION  = "7.29.0"

val skyhook = Seq(
  "com.fullfacing" %% "skyhook-json"   % SKYHOOK_VERSION,
  "com.fullfacing" %% "skyhook-mongo"  % SKYHOOK_VERSION,
  "com.fullfacing" %% "skyhook-rabbit" % SKYHOOK_VERSION,
  "com.fullfacing" %% "skyhook-rest"   % SKYHOOK_VERSION,
  "com.fullfacing" %% "skyhook-saga"   % SKYHOOK_VERSION
)

val common = Seq(
  "com.radioretail" %% "common-rdr"        % COMMON_VERSION,
  "com.radioretail" %% "common-rdr-utils"  % COMMON_VERSION,
  "com.radioretail" %% "common-rdr-sync"   % COMMON_VERSION,
  "com.radioretail" %% "common-client"     % COMMON_VERSION,
  "com.radioretail" %% "common-media"      % COMMON_VERSION,
  "com.radioretail" %% "common-product"    % COMMON_VERSION,
  "com.radioretail" %% "common-template"   % COMMON_VERSION,
  "com.radioretail" %% "common-production" % COMMON_VERSION
)

val csv = Seq(
  "com.github.tototoshi" %% "scala-csv" % "1.3.7"
)

val `tracing-utils` = Seq(
  "org.mdedetrich"         %% "monix-opentracing"        % "0.1.0-SNAPSHOT",
  "io.opentracing.contrib" %  "opentracing-mongo-driver" % "0.1.5"
)

val beard = Seq(
  "com.fullfacing" %% "beard" % "1.0.0"
)

libraryDependencies ++= skyhook ++ common ++ csv ++ `tracing-utils` ++ beard

githubTokenSource := TokenSource.GitConfig("github.token") || TokenSource.Environment("GITHUB_TOKEN")

assemblyOutputPath in assembly := file(s"${name.value}-${git.gitHeadCommit.value.fold("")(_.substring(0, 7))}.jar")
assemblyMergeStrategy in assembly := {
  case "META-INF/io.netty.versions.properties" => MergeStrategy.first
  case "module-info.class"                     => MergeStrategy.first
  case x                                       => (assemblyMergeStrategy in assembly).value(x)
}

releaseIgnoreUntrackedFiles := true
releaseVersionBump := Bugfix
releaseProcess := Seq[ReleaseStep](
  inquireVersions,
  runClean,
  releaseStepCommand("assembly"),
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  setNextVersion,
  commitNextVersion,
  pushChanges
)
