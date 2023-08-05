package ch.wrangel.toolbox

import java.nio.file.{Path, Paths}
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import scala.util.matching.Regex

/** Holds project wide constants */
object Constants {

  /** Representation of blank split character */
  final val BlankSplitter: String = " "

  /** Representation of caffeinate identifier */
  final val CaffeinateIdentifier = "caffeinate"

  /** Default [[String]] for imputing as day of month into dates */
  final val DefaultDay: String = "01"

  /** Default [[String]] for imputing as time into dates */
  final val DefaultTime: String = "_000100"

  /** Location of download folder */
  val DownloadFolder: String = Paths.get(System.getProperty("user.home"), "/Downloads/").toString

  /** Excluded file types */
  final val ExcludedFileTypes: Seq[String] = Seq(".txt")

  /** Content of exif config file */
  final val ExifToolConfigFileContent: String =
    "%Image::ExifTool::UserDefined::Options = (\n\tLargeFileSupport => 1,\n);"

  /** [[Path]] to exif config file */
  final val ExifToolConfigFilePath: Path =
    Paths.get(System.getProperty("user.dir"), "exif.config")

  /** Exif tool base command */
  final val ExifToolBaseCommand: String =
    s"/usr/local/bin/exiftool -config $ExifToolConfigFilePath"
  /** ExifTool main website */
  final val ExifToolWebsite: String = "https://exiftool.org"

  /** Identifying string for hdutil command line tool */
  final val HdiUtilIdentifier: String = "hdiutil"

  /** Set of dmg image identifiers */
  final val ImageIdentifiers = Seq("ExifTool", ".dmg")

  /** Identifier for exiftool temp files */
  final val isNotExiftoolTmpFile: String => Boolean =
    (filename: String) => !filename.endsWith("exiftool_tmp")

  /** Conversion from full name to setFile identifier */
  final val MacOsTimestampTags: Map[String, Seq[String]] = Map(
    "create" -> Seq("d"),
    "modify" -> Seq("m")
  )

  /** Key for expressing not applicable timestamp while checking secondary timestamps */
  final val NonApplicableKey: String = "-"

  /** Allowed argument space */
  final val ParameterSpace: Map[Seq[String], Seq[String]] = Map(
    Seq("-e", "-r", "-s") -> Seq("exif", "true", "true"),
    Seq("-e", "-r") -> Seq("exif", "true"),
    Seq("-e") -> Seq("exif", "false", "true"),
    Seq("-f", "-r", "-e") -> Seq("file", "true", "true"),
    Seq("-f", "-r") -> Seq("file", "true"),
    Seq("-f", "-e") -> Seq("file", "false", "true"),
    Seq("-f") -> Seq("file"),
    Seq("-v") -> Seq("validate")
  )

  /** String indicating a partition */
  final val PartitionString: String = "__"

  /** Collection of reference exif timestamps */
  final val ReferenceExifTimestamps: Seq[String] = Seq(
    "DateTimeOriginal",
    "CreateDate"
  )

  /** End screen */
  val TextEnd: String = {
    """The procedure ran through. You may close all associated Terminal windows now.
      |""".stripMargin
  }

  /** Welcome screen */
  val TextWelcome: String = {
    """Welcome to the photo and video timestamp toolbox.
      |This Scala tool only works on Mac.
      |It makes use of ExifTool (https://exiftool.org), installs it or updates it whenever necessary,
      |and an internet connection is available.
      |Parameters:
      |   -e -r -s <directory string>
      |       Primary exif timestamps as reference (CreateDate and DateTimeOriginal)
      |       Rename the file with a prepending timestamp
      |       Treat secondary timestamps as well (not CreateDate or DateTimeOriginal)
      |   -e -r <directory string>
      |       Primary exif timestamps as reference (CreateDate and DateTimeOriginal)
      |       Rename the file with a prepending timestamp
      |   -e <directory string>
      |       Primary exif timestamps as reference (CreateDate and DateTimeOriginal)
      |   -f -r -e <directory string>
      |       Valid timestamp contained in filename as reference
      |       Rename the file with a prepending timestamp
      |       Treat exif timestamps
      |   -f -r <directory string>
      |       Valid timestamp contained in filename as reference
      |       Rename the file with a prepending timestamp
      |   -f -e <directory string>
      |       Valid timestamp contained in filename as reference
      |       Treat exif timestamps
      |   -f <directory string>
      |       Valid timestamp contained in filename as reference
      |   -v <directory string>
      |       Validate if the timestamp in file name and principal exif timestamps
      |       (DateTimeOriginal / Create Date) coincide. Move the file to a sub folder otherwise
      |""".stripMargin
  }

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
    "[0-9]{8}",
    // Partial dates (year and month known, imputation of day of month)
    "[0-9]{4}[-,_,/,., ][0-9]{2}",
    "[0-9]{6}"
  ).map(_.r)

  /** Collection of [[DateTimeFormatter]] patterns for relevant timestamp groups */
  final val TimestampFormatters: Map[String, DateTimeFormatter] = Map(
    "exif" -> DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss"),
    "exif2" -> DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
    "zonedExif" -> DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ssz"),
    "mac" -> DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss"),
    "file" -> DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
  )

  /** Allowed ranges for each element of a timestamp (except ms) */
  final val TimestampRanges: Seq[Range] = {
    val today: LocalDateTime = LocalDateTime.now
    Seq(
      today.minusYears(100).getYear to today.getYear,
      1 to 12,
      1 to 31, // placeholder
      0 to 23,
      0 to 59,
      0 to 59
    )
  }

  /** Folder name for files having undergone unsuccessful exif manipulation */
  final val UnsuccessfulFolder: String = "_unsuccessful"

  /** Folder name for files having zero byte size */
  final val ZeroByteFolder: String = "_zeroByte"

}
