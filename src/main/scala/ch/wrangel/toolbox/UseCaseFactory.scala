package ch.wrangel.toolbox

import java.nio.file.{Path, Paths}
import java.time.LocalDateTime

import ch.wrangel.toolbox.utilities.{FileUtilities, MiscUtilities, StringUtilities, TimestampUtilities}

import scala.util.{Failure, Success, Try}

/** Protective object around use case singletons
  * https://alvinalexander.com/scala/factory-pattern-in-scala-design-patterns/
  */
object UseCaseFactory {

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
      * @param treatSecondaryTimestamps Flag indicating whether to treat secondary timestamps
      */
    def run(directory: String,
            needsRenaming: Boolean,
            treatSecondaryTimestamps: Boolean): Unit = {

      FileUtilities
        .iterateFiles(directory)
        .foreach { filePath: Path =>
          info(s"Treating $filePath")
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
          else if (treatSecondaryTimestamps)
            handleSecondaryTimestamps(secondaryTimestamps,
                                      filePath,
                                      needsRenaming)
          else
            info(s"Omitting file")
        }
      TimestampUtilities.writeTimestamps(treatedFiles.toMap)
      TimestampUtilities.writeTimestamps(treatedFiles2.toMap)
      Validate.run(directory, needsRenaming)

      if (treatSecondaryTimestamps)
        MiscUtilities.getProcessOutput("""osascript -e 'quit app "Preview"'""")
    }

    /** Handles principal timestamps
      *
      * @param principalTimestamps [[Map]] with exif timestamp ids as keys, and optional [[LocalDateTime]] as values
      * @param filePath [[Path]] to file
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
          treatedFiles += MiscUtilities.prepareFile(filePath,
                                                    ldt,
                                                    needsRenaming =
                                                      needsRenaming)
        case None =>
      }
    }

    /** Handles secondary timestamps
      *
      * @param secondaryTimestamps [[Map]] with exif timestamp ids as keys, and optional [[LocalDateTime]] as values
      * @param filePath [[Path]] to file
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
          treatedFiles2 += MiscUtilities.prepareFile(
            filePath,
            candidateTimestamps(feedback.toInt),
            needsRenaming = needsRenaming
          )
      } else
        info("No valid timestamps found")
      MiscUtilities.getProcessOutput(
        """osascript -e 'tell application "Preview" to close first window'""")
    }

  }

  /* Renames files & changes mac & exif timestamp according to the valid timestamp detected in the file name */
  private object FileNameAsReference extends UseCase {

    /** Runs the process
      *
      * @param directory     [[String]] representation of directory path
      * @param needsRenaming Flag indicating whether file should be renamed
      * @param treatSecondaryTimestamps Flag indicating whether to treat secondary timestamps
      */
    def run(directory: String,
            needsRenaming: Boolean,
            treatSecondaryTimestamps: Boolean): Unit = {
      TimestampUtilities
        .detectHiddenTimestampsOrDates(directory)
        .foreach {
          case (filePath: Path, ldt: LocalDateTime) =>
            info(s"Treating $filePath")
            treatedFiles += MiscUtilities.prepareFile(filePath,
                                                      ldt,
                                                      needsRenaming =
                                                        needsRenaming)
        }
      TimestampUtilities.writeTimestamps(treatedFiles.toMap)
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
      * @param treatSecondaryTimestamps Flag indicating whether to treat secondary timestamps
      */
    def run(directory: String,
            needsRenaming: Boolean,
            treatSecondaryTimestamps: Boolean): Unit = {
      FileUtilities
        .iterateFiles(directory)
        .foreach { filePath: Path =>
          val fileName: String =
            FileUtilities.splitExtension(filePath, isPathNeeded = false).head
          // 1) Check if file has valid timestamp
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
          ) match {
            case Some(extractedFileTimestamp: LocalDateTime) =>
              val tag: String = Constants.ReferenceExifTimestamps(1) // Create date time matters
              // 2) Check if shell command returns valid stdout
              StringUtilities
                .prepareExifToolOutput(
                  s"""${Constants.ExifToolBaseCommand} -s -$tag "$filePath""""
                )
                .headOption match {
                case Some(element: Array[String]) =>
                  // 3) Check if String exif timestamp can be converted to a real timestamp
                  Constants.TimestampFormatters
                    .flatMap(
                      ts =>
                        TimestampUtilities.convertStringToTimestamp(
                          element.last,
                          ts._2
                      ))
                    .headOption match {
                    case Some(ldt: LocalDateTime) =>
                      info(
                        s"Looking at $filePath with file timestamp $extractedFileTimestamp" +
                          s" and $tag $ldt"
                      )
                      if (!extractedFileTimestamp.equals(ldt))
                        treatedFiles += ((filePath, LocalDateTime.now))
                    case None =>
                      info(
                        s"   Exif timestamp for $filePath cannot be converted to proper timestamp")
                      treatedFiles += ((filePath, LocalDateTime.now))
                  }
                case None =>
                  info(
                    s"   Shell command returns no valid output for $filePath")
                  treatedFiles += ((filePath, LocalDateTime.now))
              }
            case None =>
              info(s"   Filename of $filePath contains no valid timestamp")
              treatedFiles += ((filePath, LocalDateTime.now))
          }
        }
      FileUtilities.moveFiles(
        treatedFiles.map(_._1),
        Paths.get(directory, Constants.UnsuccessfulFolder)
      )
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
