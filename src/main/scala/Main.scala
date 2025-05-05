import scala.scalanative.unsafe
import scala.scalanative.unsigned.* // for .toCSize
import scala.scalanative.libc.stdio.FILE
import scala.scalanative.posix.string.strlen
import scala.scalanative.unsafe.Zone
import scala.scalanative.unsafe.CQuote
import scala.util.boundary

import mainargs.{main, arg, ParserForMethods, Flag, Leftover}
import scala.scalanative.unsafe.Tag

object Main {
  def main(args: Array[String]): Unit =
    ParserForMethods(this).runOrExit(args)
    sys.exit(0)

  @main
  def run(
      @arg(name = "squeeze-blank", short = 's') squeeze: Flag,
      @arg(short = 'S') separator: String = "",
      @arg(positional = true) infiles: Leftover[String]
  ): Unit =
    infiles.value match
      case Seq() =>
        cats("-" :: Nil, squeeze.value, separator)
      case fs =>
        cats(fs.toList, squeeze.value, separator)

  def usage(): Unit = println("not implemented yet")

  def cats(
      infiles: List[String],
      squeeze: Boolean,
      separator: String = ""
  ): Unit =
    var copiedFileCount = 0
    val fileCount = infiles.length
    Zone {
      val separatorCStr = unsafe.toCString(separator)
      extractEscapeSequence(separatorCStr, strlen(separatorCStr))

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
              Console.err.println(s"cannot open file: $inFile")
              usage()
              boundary.break()

            val bufSize = 1024.toCSize
            val buf = unsafe.alloc[unsafe.CChar](bufSize)

            copy(inStream, stdout, buf, bufSize, squeeze)

            scalanative.libc.stdio.fclose(inStream)

            if (copiedFileCount < fileCount - 1) then
              scalanative.posix.stdio.fprintf(
                stdout,
                c"%s",
                separatorCStr
              )

            copiedFileCount += 1
        }

        scalanative.libc.stdio.fclose(stdout)
      }
    }

  def copy(
      inStream: unsafe.Ptr[FILE],
      outStream: unsafe.Ptr[FILE],
      buf: unsafe.CString,
      bufSize: unsafe.CSize,
      squeeze: Boolean
  ) =
    var nRead = 1.toCSize

    while (nRead > 0.toCSize) do
      nRead = scalanative.libc.stdio.fread(
        buf,
        1.toCSize,
        bufSize,
        inStream
      )

      if squeeze then nRead = this.squeeze(buf, nRead)

      scalanative.libc.stdio.fwrite(
        buf,
        nRead,
        1.toCSize,
        outStream
      )

  def squeeze(buf: unsafe.CString, bufSize: unsafe.CSize): unsafe.CSize =
    Zone {
      var writePos = 0.toCSize
      var readPos = 0.toCSize
      var consecutiveNewlines = 0
      val lf = '\n'

      while (readPos < bufSize) do
        val currentChar = buf(readPos)

        if (currentChar == lf) then
          consecutiveNewlines += 1
          if (consecutiveNewlines <= 2) then
            buf(writePos) = currentChar
            writePos += 1.toCSize
        else
          consecutiveNewlines = 0
          buf(writePos) = currentChar
          writePos += 1.toCSize

        readPos += 1.toCSize

      writePos
    }

    /** Extracts the escape sequence from the buffer. For example, "foo\n"
      * (literally) will be converted to "foo(line break)". This is used to
      * convert the escape sequence to a string. XXX: If we use BSD, we can use
      * strunvis() to convert the escape
      *
      * @param buf
      * @param bufSize
      * @return
      */
  def extractEscapeSequence(
      buf: unsafe.CString,
      bufSize: unsafe.CSize
  ): unsafe.CSize =
    val map = Map(
      'n'.toByte -> '\n',
      't'.toByte -> '\t',
      'r'.toByte -> '\r'
    )
    // scan escape sequence and replace
    var writePos = 0.toCSize
    var readPos = 0.toCSize

    while (readPos < bufSize) do
      val currentChar = buf(readPos)

      if (currentChar == '\\') then
        readPos += 1.toCSize
        val escapeChar = buf(readPos)
        map.get(escapeChar) match
          case Some(replacement) =>
            buf(writePos) = replacement.toByte
            writePos += 1.toCSize
          case None =>
            buf(writePos) = currentChar
            writePos += 1.toCSize
      else
        buf(writePos) = currentChar
        writePos += 1.toCSize

      readPos += 1.toCSize

    // null terminate the string
    buf(writePos) = 0.toByte
    writePos
}
