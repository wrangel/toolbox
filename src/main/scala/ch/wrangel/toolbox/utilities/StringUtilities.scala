package ch.wrangel.toolbox.utilities

import java.nio.file.Path

import ch.wrangel.toolbox.Constants

import scala.sys.process.Process

/* Utilities for [[String]] manipulation */
object StringUtilities {

  /** Standardizes the process output
   * Fails for 0 bytes files
   *
   * @param command Command
   * @return Result
   */
  def cleanCommand(command: String): String = {
    Process(command.stripMargin).!!
      .trim
  }

  /** Prepares the output of the exiftool for further processing
   *
   * @param command Exiftool command
   * @return [[Array]] of [[String]] [[Array]]s in a processable format
   */
  def prepareExiftoolOutput(command: String): Array[Array[String]] = {
    cleanCommand(command).split("\n")
      .map(_.split(" : ").map(_.trim))
      .filter {
        element: Array[String] =>
          !element.head.contains("scanned") & !element.head.contains("read")
      }
  }

  /** Gets all exif timestamp tags available for the file
   *
   * @param filePath [[Path]] to the file
   * @return Mapping from file [[Path]] to the file's exif timestamp tags
   */
  def getExifTimestampTags(filePath: Path): (Path, Seq[String]) = {
    filePath -> Constants.ReferenceExifTimestamps
      .concat(
        prepareExiftoolOutput(s"""${Constants.ExiftoolBinary} -time:all -m -s "$filePath"""")
          .map(_.head)
      )
      .distinct
  }

}