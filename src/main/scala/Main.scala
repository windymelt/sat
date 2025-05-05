import scala.scalanative.unsafe
import scala.scalanative.unsigned.* // for .toCSize
import scala.scalanative.libc.stdio.FILE
import scala.scalanative.unsafe.Zone
import scala.scalanative.unsafe.CQuote
import scala.util.boundary

import mainargs.{main, arg, ParserForMethods, Flag}

object Main {
  @main
  def run(
      @arg(positional = true) infiles: List[String]
  ): Unit =
    infiles match
      case Nil =>
        cats("-" :: Nil)
      case _ =>
        cats(infiles)

  def main(args: Array[String]): Unit =
    ParserForMethods(this).runOrExit(args)
    sys.exit(0)

  def usage(): Unit = println("not implemented yet")

  def cats(infiles: List[String]): Unit =
    Zone {
      boundary {
        val stdout = scalanative.libc.stdio.stdout
        if (stdout == null) then
          Console.err.println("stdout is null")
          boundary.break()

        boundary {
          for inFile <- infiles do
            val inStream = inFile match
              case "-" => scalanative.libc.stdio.stdin
              case _ =>
                scalanative.libc.stdio.fopen(
                  unsafe.toCString(inFile),
                  c"rb"
                )

            if (inStream == null) then
              usage()
              boundary.break()

            val bufSize = 1024.toCSize
            val buf = unsafe.alloc[unsafe.CVoidPtr](bufSize)

            copy(inStream, stdout, buf, bufSize)

            scalanative.libc.stdio.fclose(inStream)
        }

        scalanative.libc.stdio.fclose(stdout)
      }
    }

  def copy(
      inStream: unsafe.Ptr[FILE],
      outStream: unsafe.Ptr[FILE],
      buf: unsafe.CVoidPtr,
      bufSize: unsafe.CSize
  ) =
    var nRead = 1.toCSize

    while (nRead > 0.toCSize) do
      nRead = scalanative.libc.stdio.fread(
        buf,
        1.toCSize,
        bufSize,
        inStream
      )
      scalanative.libc.stdio.fwrite(
        buf,
        nRead,
        1.toCSize,
        outStream
      )
}
