package ch.wrangel.toolbox

import java.time.format.DateTimeFormatter

import scala.util.matching.Regex

/* Holds project wide constants */
object Constants {

  /* Collection of relevant timestamps within exiftool timestamp collection */
  final val RelevantTimestamps: Seq[String] = Seq(
    "DateTimeOriginal",
    "CreateDate",
    "ModifyDate",
    "%SB",
    "%Sm"
  )

  /* Conversion from full name to setfile identifier */
  final val MacOsTimestampTags: Map[String, Seq[String]] = Map(
    "create" -> Seq("d"),
    "modify" -> Seq("m")
  )

  /* Collection of [[DateTimeFormatter]] patterns for relevant timestamp groups */
  final val TimestampFormatters: Map[String, DateTimeFormatter] = Map(
    "exif" -> DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss"),
    "zonedExif" -> DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ssz"),
    "mac" -> DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss"),
    "file" -> DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
  )

  /* String indicating a partition */
  final val PartitionString: String = "__"

  /** Patterns to detect timestamps or dates hidden in filenames */
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
    "[0-9]{8}"
  )
    .map(_.r)

  /** Allowed ranges for each element of a timestamp (except ms) */
  final val TimestampRanges: Seq[Range] = Seq(
    1900 to 2100,
    1 to 12,
    1 to 31,
    0 to 23,
    0 to 59,
    0 to 59
  )

  /** Default [[String]] for inputing as time into dates */
  final val DefaultTime: String = "_000100"

  /** Problematic video formats */
  final val ProblematicVideoFormats: Seq[String] = Seq(".mpg")

  /** Target video format */
  final val TargetVideoFormat: String = ".mp4"

  /** Exif tool binary */
  final val ExiftoolBinary: String = "/usr/local/bin/exiftool"

  /** Ffmpeg binary */
  final val FfmpegBinary: String = "/usr/local/bin/ffmpeg"

  /** folder name for files having undergone unsuccessful exif manipulation */
  final val UnsuccessfulFolder: String = "_not_validated"

}