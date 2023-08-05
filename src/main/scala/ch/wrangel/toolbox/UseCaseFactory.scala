package ch.wrangel.toolbox

import ch.wrangel.toolbox.utilities.{FileUtilities, MiscUtilities, StringUtilities, TimestampUtilities}
import java.nio.file.{Path, Paths}
import java.time.LocalDateTime
import scala.util.{Failure, Success, Try}
import wvlet.log.LogSupport

/** Protective object around use case singletons
  * https://alvinalexander.com/scala/factory-pattern-in-scala-design-patterns/
  */
object UseCaseFactory extends LogSupport {

  /* Renames files & changes mac timestamp according to exif timestamps.
   *
   * These are either one of the principal timestamps DateTimeOriginal or CreateDate, in which case the timestamp
   * is taken as-is and applied to file name and mac timestamps.
   * If no principal timestamp exists for the file, but further exif timestamps ("secondary timestamps") are
   * available, the user is asked to pick one or none of the timestamps. If some timestamp is chosen,
   * the file is renamed accordingly, and exif and mac timestamps are rewritten
   */
  private object ExifAsReference extends UseCase {

    /** Runs the process
      *
      * @param directory     [[String]] representation of directory path
      * @param needsRenaming Flag indicating whether file should be renamed
      * @param treatExifTimestamps Flag indicating whether to treat secondary timestamps

      */
    def run(directory: String,
            needsRenaming: Boolean,
            treatExifTimestamps: Boolean): Unit = {

      FileUtilities
        .iterateFiles(directory)
        .foreach {
            (filePath: Path) =>
              val (
                principalTimestamps: Map[String, Option[LocalDateTime]],
                secondaryTimestamps: Map[String, Option[LocalDateTime]]
              ) = TimestampUtilities
                .readExifTimestamps(filePath)
                .partition {
                  case (tag: String, _: Option[LocalDateTime]) =>
                    Constants.ReferenceExifTimestamps
                      .contains(tag)
                }
              if (principalTimestamps.nonEmpty & principalTimestamps.forall {
                    _._2.isDefined
                  })
                handlePrincipalTimestamps(principalTimestamps,
                                          filePath,
                                          needsRenaming)
              else if (treatExifTimestamps)
                handleSecondaryTimestamps(secondaryTimestamps,
                                          filePath,
                                          needsRenaming)
              else
                warn(s"======== Omitting $filePath")
        }
      TimestampUtilities.writeTimestamps(treatedFiles.toMap)
      TimestampUtilities.writeTimestamps(treatedFiles2.toMap)
      Validate.run(directory, needsRenaming)

      if (treatExifTimestamps)
        MiscUtilities.getProcessOutput("""osascript -e 'quit app "Preview"'""")
    }

    /** Handles principal timestamps
      *
      * @param principalTimestamps [[Map]] with exif timestamp ids as keys, and optional [[LocalDateTime]] as values
      * @param filePath [[Path]] to the file
      * @param needsRenaming Flag indicating whether to rename the file
      */
    private def handlePrincipalTimestamps(
        principalTimestamps: Map[String, Option[LocalDateTime]],
        filePath: Path,
        needsRenaming: Boolean
    ): Unit = {
      info("Handling principal timestamps")
      TimestampUtilities
        .getExifTimestamps(principalTimestamps)
        .headOption match {
        case Some(ldt: LocalDateTime) =>
          treatedFiles += FileUtilities.prepareFile(filePath,
                                                    ldt,
                                                    needsRenaming =
                                                      needsRenaming)
        case None =>
      }
    }

    /** Handles secondary timestamps
      *
      * @param secondaryTimestamps [[Map]] with exif timestamp ids as keys, and optional [[LocalDateTime]] as values
      * @param filePath [[Path]] to the file
      * @param needsRenaming Flag indicating whether to rename the file
      */
    private def handleSecondaryTimestamps(
        secondaryTimestamps: Map[String, Option[LocalDateTime]],
        filePath: Path,
        needsRenaming: Boolean
    ): Unit = {
      info("Handling secondary timestamps")
      MiscUtilities.getProcessOutput(
        s"""open -a Preview ${filePath.toString}""")
      val candidateTimestamps: Seq[LocalDateTime] =
        TimestampUtilities.getExifTimestamps(secondaryTimestamps).toSeq.sorted
      val options: Seq[(LocalDateTime, Int)] = candidateTimestamps.zipWithIndex
      if (candidateTimestamps.nonEmpty) {
        val feedback: String = MiscUtilities
          .getFeedback(
            options.mkString("\n") + "\nNone of those: -\n",
            Constants.NonApplicableKey +: options.map(_._2.toString)
          )
        if (feedback != Constants.NonApplicableKey)
          treatedFiles2 += FileUtilities.prepareFile(
            filePath,
            candidateTimestamps(feedback.toInt),
            needsRenaming = needsRenaming
          )
      } else
        warn("No valid timestamps found")
      MiscUtilities.getProcessOutput(
        """osascript -e 'tell application "Preview" to close first window'""")
    }

  }

  /* Renames files & changes mac & exif timestamp (if desired)
  according to the valid timestamp detected in the file name
   */
  private object FileNameAsReference extends UseCase {

    /** Runs the process
      *
      * @param directory     [[String]] representation of directory path
      * @param needsRenaming Flag indicating whether file should be renamed
      * @param treatExifTimestamps Flag indicating whether to treat exif timestamps
      */
    def run(directory: String,
            needsRenaming: Boolean,
            treatExifTimestamps: Boolean): Unit = {
      TimestampUtilities
        .detectHiddenTimestampsOrDates(directory)
        .foreach {
          case (filePath: Path, ldt: LocalDateTime) =>
            treatedFiles += FileUtilities.prepareFile(filePath,
                                                      ldt,
                                                      needsRenaming =
                                                        needsRenaming)
        }
      TimestampUtilities.writeTimestamps(treatedFiles.toMap,
                                         treatExifTimestamps)
      Validate.run(directory)
    }

  }

  /** Checks if all relevant EXIF dates are the same as the file name's timestamp
    * Only runs with already renamed file names (e.g. containing YYYYMMDD_hhmmss)
    */
  private object Validate extends UseCase {

    /** Moves files with invalid relevant EXIF timestamps to a specific subfolder
      *
      * @param directory     [[String]] representation of directory path
      * @param needsRenaming Flag indicating whether file should be renamed
      * @param treatExifTimestamps Flag indicating whether to treat secondary timestamps
      */
    def run(directory: String,
            needsRenaming: Boolean = false,
            treatExifTimestamps: Boolean = false,
    ): Unit = {
      FileUtilities
        .iterateFiles(directory)
        .foreach { 
          (filePath: Path) =>
            info(s"======== Validating $filePath")
            // 0) Check if file is not an exiftool temp file
            if (Constants.isNotExiftoolTmpFile(filePath.getFileName.toString)) {
              // 1) Check if file has valid timestamp
              checkFileTimestamp(filePath: Path) match {
                case Some(filenameTimestamp: LocalDateTime) =>
                  // Check for each relevant exif timestamp
                  Constants.ReferenceExifTimestamps
                    .foreach { 
                      (tag: String) =>
                        // 2) Check if shell command returns valid stdout
                        checkValidity(filePath, tag) match {
                          case Some(element: Array[String]) =>
                            // 3) Check if String exif timestamp can be converted to a real timestamp
                            convertExifTimestamp(element) match {
                              case Some(exifTimestamp: LocalDateTime) =>
                                compareTimestamps(filePath,
                                                  filenameTimestamp,
                                                  exifTimestamp,
                                                  tag)
                              case None =>
                                warn(s"$tag cannot be converted properly")
                                treatedFiles += ((filePath, LocalDateTime.now))
                            }
                          case None =>
                            warn(s"Shell command returns no valid output")
                            treatedFiles += ((filePath, LocalDateTime.now))
                        }
                    }
                case None =>
                  warn(s"File timestamp contains no valid timestamp")
                  treatedFiles += ((filePath, LocalDateTime.now))
              }
            } else {
              warn(s"File is a remnant exiftool temp file")
              treatedFiles += ((filePath, LocalDateTime.now))
            }
        }
      FileUtilities.moveFiles(
        treatedFiles.map(_._1),
        Paths.get(directory, Constants.UnsuccessfulFolder)
      )
    }

    /** Attempts to extract a [[LocalDateTime]] from the file name
      *
      * @param filePath [[Path]] to the file
      * @return Optional [[LocalDateTime]] extracted from file name
      */
    private def checkFileTimestamp(filePath: Path): Option[LocalDateTime] = {
      val fileName: String =
        FileUtilities.splitExtension(filePath, isPathNeeded = false).head
      TimestampUtilities.convertStringToTimestamp(
        Try {
          fileName.substring(0, fileName.indexOf(Constants.PartitionString))
        } match {
          case Success(s: String) =>
            s
          case Failure(_) =>
            fileName
        },
        Constants.TimestampFormatters("file")
      )
    }

    /** Checks if shell command returns valid stdout
      *
      * @param filePath [[Path]] to the file
      * @param tag Relevant exif timestamp tag
      * @return Optional [[Array[String]]] containing the stdout
      */
    private def checkValidity(filePath: Path,
                              tag: String): Option[Array[String]] = {
      StringUtilities
        .prepareExifToolOutput(
          s"""${Constants.ExifToolBaseCommand} -s -$tag "$filePath""""
        )
        .headOption
    }

    /** Attempts to convert the exif timestamps to [[LocalDateTime]]
      *
      * @param element Output of exiftool stdout
      * @return Valid [[LocalDateTime]]
      */
    private def convertExifTimestamp(
        element: Array[String]): Option[LocalDateTime] = {
      Constants.TimestampFormatters
        .flatMap(
          ts =>
            TimestampUtilities.convertStringToTimestamp(
              element.last,
              ts._2
          ))
        .headOption
    }

    /** Compares exif timestamps with timestamp extracted from file name
      *
      * @param filePath [[Path]] to the file
      * @param filenameTimestamp [[LocalDateTime]] representing the timestamp extracted from the file name
      * @param exifTimestamp [[LocalDateTime]] representing the exif timestamp
      * @param tag Tag of the exif timestamp
      */
    private def compareTimestamps(filePath: Path,
                                  filenameTimestamp: LocalDateTime,
                                  exifTimestamp: LocalDateTime,
                                  tag: String): Unit = {
      info(
        s"Comparing file timestamp $filenameTimestamp" +
          s" with $tag $exifTimestamp"
      )
      if (!filenameTimestamp.equals(exifTimestamp)) {
        warn(s"Timestamps do not match")
        treatedFiles += ((filePath, LocalDateTime.now))
      } else
        info(s"Timestamps match")
    }
  }

  /** Factory method
    *
    * @param useCase Applicable use case
    * @return Use case singleton
    */
  def apply(useCase: String): UseCase = {
    useCase match {
      case "exif" =>
        ExifAsReference
      case "file" =>
        FileNameAsReference
      case "validate" =>
        Validate
    }
  }

}
