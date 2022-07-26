name := "programs-as-values"
version := "1.0.0"
scalaVersion := "2.13.8"

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-core" % "2.8.0",
  "org.typelevel" %% "cats-effect" % "3.3.14"
)

run / fork := true
