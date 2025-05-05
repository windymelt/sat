scalaVersion := "3.3.3" // A Long Term Support version.

enablePlugins(ScalaNativePlugin)

// set to Debug for compilation details (Info is default)
logLevel := Level.Info

// import to add Scala Native options
import scala.scalanative.build._

// defaults set with common options shown
nativeConfig ~= { c =>
  c.withLTO(LTO.none) // thin
    .withMode(Mode.debug) // releaseFast
    .withGC(GC.immix) // commix
}

lazy val root = (project in file("."))
  .settings(
    name := "cats",
    version := "0.1.0-SNAPSHOT",
    libraryDependencies += "com.lihaoyi" %%% "mainargs" % "0.7.6"
  )
