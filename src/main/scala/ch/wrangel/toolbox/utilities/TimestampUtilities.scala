package ch.wrangel.toolbox.utilities

import java.nio.file.{Files, Path}
import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, LocalTime, ZonedDateTime}

import ch.wrangel.toolbox.Constants

import scala.collection.immutable.ArraySeq
import scala.collection.mutable.ListBuffer
import scala.util.{Failure, Success, Try}


/** Holds a collection of timestamp utilities */
object TimestampUtilities {

  /** Adjusts both Mac OS and exif timestamps
   *
   * @param fileToDateMap [[Map]] from file [[Path]] to the file's [[LocalDateTime]]
   * @param excludedExifTags Optional [[Seq]] of exif timestamps which should not be written. Default is None
   */
  def writeTimestamps(fileToDateMap: Map[Path, LocalDateTime], excludedExifTags: Option[Seq[String]] = None): Unit = {
    if (fileToDateMap.nonEmpty) {
      writeExifTimestamps(fileToDateMap, excludedExifTags)
      writeMacTimestamps(fileToDateMap)
    }
  }

  /** Rewrites the Mac OS creation and modification timestamps
   *
   * @param fileNameToTimestampMap [[Map]] containing the file [[Path]] as well as the file's [[LocalDateTime]]
   */
  def writeMacTimestamps(fileNameToTimestampMap: Map[Path, LocalDateTime]): Unit = {
    fileNameToTimestampMap.keys
      .foreach {
        filePath: Path =>
          Constants.MacOsTimestampTags
            .values
            .map(_.head)
            .foreach {
              macTag: String =>
                val newDate: String = fileNameToTimestampMap(filePath).format(Constants.TimestampFormatters("mac"))
                FileUtilities.handleManipulation(
                  s"""SetFile -$macTag "$newDate" "${filePath.toString}"""",
                  macTag,
                  "Mac",
                  filePath,
                  newDate
                )
            }
      }
  }

  /** Newly writes exif timestamps
   *
   * @param fileNameToTimestampMap [[Map]] containing the file [[Path]] as well as the file's [[LocalDateTime]]
   * @param optionalExcludedExifTags Optional [[Seq]] of exif timestamps which should not be written. Default is None
   */
  private def writeExifTimestamps(
                                   fileNameToTimestampMap: Map[Path, LocalDateTime],
                                   optionalExcludedExifTags: Option[Seq[String]] = None
                                 ): Unit = {
    fileNameToTimestampMap.foreach {
      case (filePath: Path, ldt: LocalDateTime) =>
        StringUtilities.getAllExifTimestampTags(filePath)._2
          .filter {
            exifTag: String =>
              optionalExcludedExifTags match {
                case Some(excludedExifTags: Seq[String]) =>
                  !excludedExifTags.contains(exifTag)
                case None =>
                  true
              }
          }
          .map {
            exifTag: String =>
              val newDate: String = ldt.format(Constants.TimestampFormatters("exif"))
              FileUtilities.handleManipulation(
                s"""${Constants.ExiftoolBinary} -m -EXIF:ExifIFD:$exifTag="$newDate"
                   | -overwrite_original "$filePath"""".stripMargin,
                exifTag,
                "Exif",
                filePath,
                newDate
              )
          }
    }
  }

  /** Extracts the exif timestamp from exiftool output, if possible
   *
   * @param filePath [[Path]] to the file
   * @return [[Map]] of tag identifier and the optional corresponding [[LocalDateTime]]
   */
  def readExifTimestamps(filePath: Path): Map[String, Option[LocalDateTime]] = {
    StringUtilities.prepareExiftoolOutput(
      s"""${Constants.ExiftoolBinary} -time:all -m -s "${filePath.toString}""""
    )
      .map {
        descriptorAndTimestamp: Array[String] =>
          descriptorAndTimestamp.head ->
            convertStringToTimestamp(descriptorAndTimestamp.last,
              Constants.TimestampFormatters("exif")
            )
      }
      .toMap
  }

  /** Converts [[String]] to [[LocalDateTime]], if possible (core method)
   *
   * @param timestamp [[String]] potentially containing the [[LocalDateTime]]
   * @param dtf       [[DateTimeFormatter]] used to convert [[String]] to [[LocalDateTime]]
   * @return Optional [[LocalDateTime]]
   */
  def convertStringToTimestamp(timestamp: String, dtf: DateTimeFormatter): Option[LocalDateTime] = {
    Try {
      LocalDateTime.parse(timestamp, dtf)
    }
    match {
      case Success(ldt: LocalDateTime) =>
        if (ldt.getYear == 1970 & ldt.getMonth == 1 & ldt.getDayOfMonth == 1)
          None
        else
          Some(ldt)
      case Failure(_) =>
        Try {
          Some(ZonedDateTime.parse(timestamp, Constants.TimestampFormatters("zonedExif"))
            .toLocalDateTime
          )
        }
          .getOrElse(None)
    }
  }

  /** Renames a file, if necessary (i.e. the file does not already bear exactly the same filename)
   *
   * @param filePath [[Path]] to the file
   * @param ldt      [[LocalDateTime]] to be used for renaming
   * @return [[Path]] to the renamed filename
   */
  def renameFileWithTimestamp(filePath: Path, ldt: LocalDateTime): Path = {
    val filePathComponents: Seq[String] = FileUtilities.splitExtension(filePath, isPathNeeded = false)
    val oldFileName: String = filePathComponents.mkString("")
    val timestamp: String = ldt.format(Constants.TimestampFormatters("file"))
    if (!filePathComponents.head.equals(timestamp) & Files.isRegularFile(filePath)) {
      val newFileName: String =
        timestamp + Constants.PartitionString + oldFileName
      println(s"Renaming $oldFileName to $newFileName")
      val newPath: Path = filePath.resolveSibling(newFileName)
      Files.move(filePath, newPath)
      newPath
    }
    else {
      println(s"No need to rename $oldFileName")
      filePath
    }
  }

  /** Detects timestamps / dates in filenames. Filenames without valid timestamps / dates are omitted
   *
   * @param directory [[String]] representation of path of directory
   * @return [[Map]] containing file [[Path]]s as keys, and valid [[LocalDateTime]]s as values.
   *         For dates, the time component is set to "00:01:00"
   */
  def detectHiddenTimestampsOrDates(directory: String): Map[Path, LocalDateTime] = {
    identifyCandidates(directory).map {
      case (filePath: Path, value: String) =>
        filePath -> {
          val (date: String, time: String) = value.splitAt(8)
          if (time.nonEmpty)
            date + "_" + time
          else
            date
        } ->
          isValidCandidate(value)
    }
      .filter(_._2 == true)
      .keys
      .map {
        case (filePath: Path, element: String) =>
          filePath -> {
            if (element.length > 8)
              TimestampUtilities.convertStringToTimestamp(element, Constants.TimestampFormatters("file")).get
            else
              addTimeToDate(filePath, element)
          }
      }
      .toMap
  }

  /** Filters out [[Path]]s to files whose file names might include a valid timestamp / date
   *
   * @param directory [[String]] representation of directory path
   * @return [[Map]] containing the candidate files' [[Path]] as keys, and the timestamp / date candidate [[String]]
   */
  def identifyCandidates(directory: String): Map[Path, String] = {
    FileUtilities.iterateFiles(directory).map {
      filePath: Path =>
        filePath -> {
          val rawSeq: Seq[String] = Constants.TimestampAndDatePatterns
            .map(_.findFirstIn(filePath.getFileName.toString).getOrElse(""))
          rawSeq.maxBy(_.length)
            .replaceAll("[^0-9]", "")
        }
    }
      .filter(_._2.length > 0)
      .toMap
  }

  /** Determines whether a candidate [[String]] is a valid timestamp / date
   *
   * @param candidate [[String]] potentially representing a valid timestamp / date
   * @return Flag indicating whether candidate [[String]] is a valid timestamp / date
   */
  def isValidCandidate(candidate: String): Boolean = {
    val slidedDigits = candidate.map(_.asDigit)
      .sliding(2, 2)
      .zipWithIndex
      .toSeq
    slidedDigits.map {
      case (slide: ArraySeq[Int], idx: Int) =>
        idx match {
          case 0 =>
            slide.concat(slidedDigits(idx + 1)._1)
              .mkString
              .toInt
          case x if x > 1 =>
            slide.mkString
              .toInt
          case _ =>
            -1
        }
    }
      .filter(_ != -1)
      .zipWithIndex
      .map {
        case (timestampElement: Int, idx: Int) =>
          Constants.TimestampRanges(idx)
            .contains(timestampElement)
      }
      .forall(_ == true)
  }

  /** Adds [[LocalTime]] to a date
   * If there is at least one exif timestamp supporting the date present in the file name, the former's time portion
   * is added to the date. If not, a default time is added
   *
   * @param filePath [[Path]] to the file
   * @param date     [[String]] representation of a potentially valid date
   * @return Potentially valid [[LocalDateTime]]
   */
  def addTimeToDate(filePath: Path, date: String): LocalDateTime = {
    val coincidingExifTimestamps: ListBuffer[LocalDateTime] = ListBuffer[LocalDateTime]()
    readExifTimestamps(filePath)
      .foreach {
        case (_: String, oldt: Option[LocalDateTime]) =>
          oldt match {
            case Some(ldt: LocalDateTime) =>
              if (
                Try {
                  ldt.getYear == date.substring(0, 4).toInt &
                    ldt.getMonthValue == date.substring(4, 6).toInt &
                    ldt.getDayOfMonth == date.substring(6, 8).toInt
                }
                  .getOrElse(false)
              )
                coincidingExifTimestamps += ldt
            case None =>
          }
      }
    Try {
      coincidingExifTimestamps.min
    }
      .getOrElse(
        convertStringToTimestamp(date + Constants.DefaultTime, Constants.TimestampFormatters("file")).get
      )
  }

}