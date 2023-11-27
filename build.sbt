
ThisBuild / tlBaseVersion := "0.2"

ThisBuild / organization := "gmkumar2005"
ThisBuild / organizationName := "gmkumar2005"
ThisBuild / startYear := Some(2023)
ThisBuild / developers := List(
  tlGitHubDev("gmkumar2005", "Kiran Kumar")
)

ThisBuild / tlCiReleaseBranches ++= Seq("series/0.1")
ThisBuild / tlSitePublishBranch := Some("main")
ThisBuild / tlSonatypeUseLegacyHost := false

ThisBuild / crossScalaVersions := Seq("3.3.1")
ThisBuild / scalacOptions ++= Seq("-new-syntax", "-indent", "-source:future")

ThisBuild / githubWorkflowJavaVersions := Seq(JavaSpec.temurin("21"))

val CatsVersion = "2.10.0"
val CatsEffectVersion = "3.5.2"
val Fs2Version = "3.9.3"
val Fs2DomVersion = "0.2.1"
val Http4sVersion = "0.23.23"
val Http4sDomVersion = "0.2.10"
val MonocleVersion = "3.2.0"
val calicoVersion = "0.2.1"
val ScalaJsStubs = "1.1.0"
val Log4Cats = "2.6.0"
val Logback = "1.4.11"
val Phobos = "0.21.0"

lazy val root =
  tlCrossRootProject.aggregate(docs)
    .settings(noPublish)

lazy val shared = crossProject(JSPlatform, JVMPlatform)
  .in(file("./shared"))
  .enablePlugins(BuildInfoPlugin)
  .settings(commonSettings)
  .settings(
    // sbt-BuildInfo plugin can write any (simple) data available in sbt at
    // compile time to a `case class BuildInfo` that it makes available at runtime.
    buildInfoKeys := Seq[BuildInfoKey](scalaVersion, sbtVersion, BuildInfoKey("calicoVersion" -> calicoVersion)),
    // The BuildInfo case class is located in target/scala<version>/src_managed,
    // and with this setting, you'll need to `import com.raquo.buildinfo.BuildInfo`
    // to use it.
    buildInfoPackage := "gmkumar2005.buildinfo"
    // Because we add BuildInfo to the `shared` project, this will be available
    // on both the client and the server, but you can also make it e.g. server-only.
  )
  .settings(
    libraryDependencies ++= List(
      // JSON codec
    )
  )
  .jvmSettings(
    libraryDependencies ++= List(
      // This dependency lets us put @JSExportAll and similar Scala.js
      // annotations on data structures shared between JS and JVM.
      // With this library, on the JVM, these annotations compile to
      // no-op, which is exactly what we need.
      "org.scala-js" %% "scalajs-stubs" % ScalaJsStubs
    )
  )


lazy val server = project
  .in(file("./server"))
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= List(
      // Effect library providing the IO type, used as a better alternative to scala.Future
      "org.typelevel" %% "cats-effect" % CatsEffectVersion,
      // Http4s web server framework
      "org.http4s" %% "http4s-ember-server" % Http4sVersion,
      "org.http4s" %% "http4s-dsl" % Http4sVersion,
      // Logging
      "org.typelevel" %% "log4cats-slf4j" % Log4Cats,
      "ch.qos.logback" % "logback-classic" % Logback,
      // Http4s HTTP client to fetch data from the weather API
      "org.http4s" %% "http4s-ember-client" % Http4sVersion,
      // XML decoder (to parse weather API XMLs)
      "ru.tinkoff" %% "phobos-core" % Phobos,
    )
  )
  .settings(
    assembly / mainClass := Some("com.raquo.server.Server"),
    assembly / assemblyJarName := "app.jar",

    // Gets rid of "(server / assembly) deduplicate: different file contents found in the following" errors
    // https://stackoverflow.com/questions/54834125/sbt-assembly-deduplicate-module-info-class
    assembly / assemblyMergeStrategy := {
      case path if path.endsWith("module-info.class") => MergeStrategy.discard
      case path =>
        val oldStrategy = (assembly / assemblyMergeStrategy).value
        oldStrategy(path)
    }
  )
  .dependsOn(shared.jvm)

lazy val client = project
  .in(file("./client"))
  .enablePlugins(ScalaJSPlugin)
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= List(
      "org.http4s" %%% "http4s-dom" % Http4sDomVersion,
      "org.http4s" %%% "http4s-circe" % Http4sVersion,
      "com.armanbilge" %%% "calico" % calicoVersion,
      "com.armanbilge" %%% "calico-router" % calicoVersion
    ),
    scalaJSLinkerConfig ~= {
      _.withModuleKind(ModuleKind.ESModule)
      // .withModuleSplitStyle(
      //   ModuleSplitStyle.SmallModulesFor(List("com.raquo.app")))
    },
    // Generated scala.js output will call your main() method to start your app.
    scalaJSUseMainModuleInitializer := true
  )
  .settings(
    // Ignore changes to .less and .css files when watching files with sbt.
    // With the suggested build configuration and usage patterns, these files are
    // not included in the scala.js output, so there is no need for sbt to watch
    // their contents. If sbt was also watching those files, editing them would
    // cause the entire Scala.js app to do a full reload, whereas right now we
    // have Vite watching those files, and it is able to hot-reload them without
    // reloading the entire application – much faster and smoother.
    watchSources := watchSources.value.filterNot { source =>
      source.base.getName.endsWith(".less") || source.base.getName.endsWith(".css")
    }
  )
  .dependsOn(shared.js)

lazy val jsdocs = project
  .settings(
    scalacOptions ~= (_.filterNot(_.startsWith("-Wunused"))),
    libraryDependencies ++= Seq(
      "org.http4s" %%% "http4s-dom" % Http4sDomVersion,
      "org.http4s" %%% "http4s-circe" % Http4sVersion,
      "com.armanbilge" %%% "calico" % calicoVersion,
      "com.armanbilge" %%% "calico-router" % calicoVersion
    )
  )
//  .dependsOn(calico, router)
  .enablePlugins(ScalaJSPlugin)

lazy val docs = project
  .in(file("site"))
  .enablePlugins(TypelevelSitePlugin)

  .settings(
    tlSiteApiPackage := Some("calico"),
    mdocJS := Some(jsdocs),
    laikaConfig ~= { _.withRawContent},

    tlSiteHelium ~= {
      import laika.helium.config.*
      _.site
        .mainNavigation(appendLinks = Seq(
        ThemeNavigationSection(
          "Related Projects",
          TextLink.external("https://www.armanbilge.com/calico/", "Calico"),
          TextLink.external("https://typelevel.org/cats-effect/", "Cats Effect"),
          TextLink.external("https://fs2.io/", "FS2"),
          TextLink.external("https://github.com/armanbilge/fs2-dom/", "fs2-dom"),
          TextLink.external("https://http4s.github.io/http4s-dom/", "http4s-dom")
        )
      ))
    },
    mdocVariables += ("HTTP4S_DOM_VERSION" -> Http4sDomVersion)
  )

lazy val noPublish = Seq(
  publishLocal / skip := true,
  publish / skip := true
)

lazy val commonSettings = Seq(
  scalacOptions ++= Seq(
    "-deprecation",
    //"-feature",
    "-language:implicitConversions"
  )
)