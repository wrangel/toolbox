package ch.wrangel.toolbox

import java.nio.file.{Path, Paths}
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import scala.util.matching.Regex

/* Holds project wide constants */
object Constants {

  /** Allowed argument space */
  val ParameterSpace: Map[Seq[String], Seq[String]] = Map(
    Seq("-e", "-r", "-s") -> Seq("exif", "true", "true"),
    Seq("-e", "-r") -> Seq("exif", "true"),
    Seq("-e") -> Seq("exif", "false", "true"),
    Seq("-f", "-r") -> Seq("file", "true"),
    Seq("-f") -> Seq("file"),
    Seq("-v") -> Seq("validate")
  )

  /** Welcome screen */
  val WelcomeText: String = {
      """Welcome to the photo and video timestamp toolbox.
        |Parameters:
        |   A) Mandatory (either or):
        |     -e : Use exif timestamps to rename file (if desired, see "-r") and adapt Mac timestamps. Default:
        |          Use principal exif timestamps (CreateDate and DateTimeOriginal). See "-s" if using both
        |          principal and secondary exif timestamps (all the rest of the exif timestamps).
        |     -f : Detect a valid timestamp in the file name and apply them to file name (if desired, see "-r"),
        |          Mac timestamps, and all exif timestamps.
        |     -v : Validate if the timestamp in file name and principal exif timestamps
        |         (DateTimeOriginal / Create Date) coincide. Move the file to a sub folder otherwise.
        |   B) Secondary (both, one alone, or none):
        |     -r : Rename file
        |     -s : Use secondary exif timestamps as well, Only applicable to -e
        |   C) Directory String
        |""".stripMargin
  }

  /* Key for expressing not applicable timestamp while checking secondary timestamps */
  final val NonApplicableKey: String = "-"

  /* Collection of reference exif timestamps */
  final val ReferenceExifTimestamps: Seq[String] = Seq(
    "DateTimeOriginal",
    "CreateDate"
  )

  /* Conversion from full name to setFile identifier */
  final val MacOsTimestampTags: Map[String, Seq[String]] = Map(
    "create" -> Seq("d"),
    "modify" -> Seq("m")
  )

  /* Collection of [[DateTimeFormatter]] patterns for relevant timestamp groups */
  final val TimestampFormatters: Map[String, DateTimeFormatter] = Map(
    "exif" -> DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss"),
    "exif2" -> DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
    "zonedExif" -> DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ssz"),
    "mac" -> DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss"),
    "file" -> DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
  )

  /* String indicating a partition */
  final val PartitionString: String = "__"

  /** Patterns to detect timestamps or dates hidden in file names */
  final val TimestampAndDatePatterns: Seq[Regex] = Seq(
    // Timestamps
    "[0-9]{4}[-,_,/,., ][0-9]{2}[-,_,/,., ][0-9]{2}[-,_,/,., ][0-9]{2}[-,_,/,., ][0-9]{2}[-,_,/,., ][0-9]{2}",
    "[0-9]{4}[-,_,/,., ][0-9]{2}[-,_,/,., ][0-9]{2} at [0-9]{2}[-,_,/,., ][0-9]{2}[-,_,/,., ][0-9]{2}",
    "[0-9]{4}[-,_,/,., ][0-9]{2}[-,_,/,., ][0-9]{2} um [0-9]{2}[-,_,/,., ][0-9]{2}[-,_,/,., ][0-9]{2}",
    "[0-9]{4}[-,_,/,., ][0-9]{2}[-,_,/,., ][0-9]{2}T[0-9]{2}[-,_,/,., ][0-9]{2}[-,_,/,., ][0-9]{2}",
    "[0-9]{8}[-,_,/,., ][0-9]{6}",
    "[0-9]{14}",
    // Dates
    "[0-9]{4}[-,_,/,., ][0-9]{2}[-,_,/,., ][0-9]{2}",
    "[0-9]{8}" ,
    // Partial dates (year and month known, imputation of day of month)
    "[0-9]{4}[-,_,/,., ][0-9]{2}",
    "[0-9]{6}"
  )
    .map(_.r)

  /** Allowed ranges for each element of a timestamp (except ms) */
  final val TimestampRanges: Seq[Range] = {
    val today: LocalDateTime = LocalDateTime.now
    Seq(
      today.minusYears(100).getYear to today.getYear,
      1 to 12,
      1 to 31,  // placeholder
      0 to 23,
      0 to 59,
      0 to 59
    )
  }

  /** Default [[String]] for imputing as time into dates */
  final val DefaultTime: String = "_000100"

  /** Default [[String]] for imputing as day of month into dates */
  final val DefaultDay: String = "01"

  /** [[Path]] to exif config file */
  final val ExifToolConfigFilePath: Path = Paths.get(System.getProperty("user.dir"), "exif.config")

  /** Content of exif config file */
  final val ExifToolConfigFileContent: String =
    "%Image::ExifTool::UserDefined::Options = (\n\tLargeFileSupport => 1,\n);"

  /** Exif tool base command */
  final val ExifToolBaseCommand: String = s"/usr/local/bin/exiftool -config $ExifToolConfigFilePath"

  /** Folder name for files having undergone unsuccessful exif manipulation */
  final val UnsuccessfulFolder: String = "_unsuccessful"

  /* Folder name for files having zero byte size */
  final val ZeroByteFolder: String = "__zeroByte"

  /** Excluded file types */
  final val ExcludedFileTypes: Seq[String] = Seq(".txt")

}