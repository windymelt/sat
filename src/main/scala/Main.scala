import scala.scalanative.unsafe
import scala.scalanative.unsigned.* // for .toCSize
import scala.scalanative.libc.stdio.FILE
import scala.scalanative.unsafe.Zone
import scala.scalanative.unsafe.CQuote
import scala.util.boundary

object Main {
  def main(args: Array[String]): Unit =
    cats(
      args.toList.init, // TODO: stdin
      args.last // TODO: stdout
    )

  def usage(): Unit = println("not implemented yet")

  def cats(infiles: List[String], outfile: String): Unit =
    Zone {
      boundary {
        val outStream = scalanative.libc.stdio.fopen(
          unsafe.toCString(outfile),
          c"wb"
        )
        if (outStream == null) then
          usage()
          boundary.break()

        boundary {
          for inFile <- infiles do
            val inStream = scalanative.libc.stdio.fopen(
              unsafe.toCString(inFile),
              c"rb"
            )
            if (inStream == null) then
              usage()
              boundary.break()

            val bufSize = 1024.toCSize
            val buf = unsafe.alloc[unsafe.CVoidPtr](bufSize)

            simpleCat(inStream, outStream, buf, bufSize)

            scalanative.libc.stdio.fclose(inStream)
        }

        scalanative.libc.stdio.fclose(outStream)
      }
    }

  def simpleCat(
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
