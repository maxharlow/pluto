name := "party-funding-loan-grapher"

scalaVersion := "2.10.3"

resolvers ++= Seq(
  "Anormcypher Repository" at "http://repo.anormcypher.org/"
)

libraryDependencies ++= Seq(
  "com.github.tototoshi" %% "scala-csv" % "1.0.0",
  "org.anormcypher" %% "anormcypher" % "0.4.4"
)