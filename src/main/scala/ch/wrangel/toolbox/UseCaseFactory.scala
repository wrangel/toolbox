package ch.wrangel.toolbox

import java.nio.file.{Path, Paths}
import java.time.LocalDateTime

import ch.wrangel.toolbox.utilities.{FileUtilities, MiscUtilities, StringUtilities, TimestampUtilities}

import scala.io.StdIn
import scala.util.{Failure, Success, Try}


/** Protective object around use case singletons
 * https://alvinalexander.com/scala/factory-pattern-in-scala-design-patterns/
 */
object UseCaseFactory {

  /* Renames files & changes mac timestamp according to the either DateTimeOriginal or CreateDate */
  private object ExifAsReference extends UseCase {

    /** Runs the process
     *
     * @param directory     [[String]] representation of directory path
     * @param needsRenaming Flag indicating whether file should be renamed
     */
    def run(directory: String, needsRenaming: Boolean): Unit = {
      FileUtilities.iterateFiles(directory)
        .foreach {
          filePath: Path =>
            TimestampUtilities.readExifTimestamps(filePath)
              .filter {
                case (tag: String, _: Option[LocalDateTime]) =>
                  Constants.ReferenceExifTimestamps
                    .contains(tag)
              }
              .values
              .flatten
              .headOption
            match {
              case Some(ldt: LocalDateTime) =>
                treatedFiles += MiscUtilities.prepareFile(filePath, ldt, needsRenaming = needsRenaming)
              case None =>
            }
        }
      TimestampUtilities.writeTimestamps(treatedFiles.toMap, Some(Constants.ReferenceExifTimestamps))
    }

  }

  /** Gets all the exif timestamps. The user is asked to pick one or none of the timestamps.
   * If some timestamp is chosen, the file is renamed accordingly, and exif and mac timestamps are rewritten
   */
  private object ExifAsPotentialReference extends UseCase {

    /** Runs the process
     *
     * @param directory     [[String]] representation of directory path
     * @param needsRenaming Flag indicating whether file should be renamed
     */
    def run(directory: String, needsRenaming: Boolean): Unit = {
      Try {
        implicit val localDateOrdering: Ordering[LocalDateTime] = _ compareTo _
        FileUtilities.iterateFiles(directory)
          .foreach {
            filePath: Path =>
              val coincidingExifTimestamps: Seq[LocalDateTime] =
                TimestampUtilities.readExifTimestamps(filePath)
                  .values
                  .flatten
                  .toSeq
                  .sorted
              if (coincidingExifTimestamps.nonEmpty) {
                val feedback: Int = StdIn.readLine(
                  filePath + ":\n" +
                    coincidingExifTimestamps.zipWithIndex.mkString("\n") +
                    "\nNone of those: -1\n"
                )
                  .toInt
                if (feedback > -1) {
                  treatedFiles += MiscUtilities.prepareFile(
                    filePath, coincidingExifTimestamps(feedback), needsRenaming = needsRenaming
                  )
                }
              }
              else
                println(s"No valid timestamps found for $filePath")
          }
        TimestampUtilities.writeTimestamps(treatedFiles.toMap)
        Validate.run(directory, needsRenaming)
      }
    }
  }

  /* Renames files & changes mac & exif timestamp according to the valid timestamp detected in the file name */
  private object FileNameAsReference extends UseCase {

    /** Runs the process
     *
     * @param directory     [[String]] representation of directory path
     * @param needsRenaming Flag indicating whether file should be renamed
     */
    def run(directory: String, needsRenaming: Boolean): Unit = {
      TimestampUtilities.detectHiddenTimestampsOrDates(directory)
        .foreach {
          case (filePath: Path, ldt: LocalDateTime) =>
            treatedFiles += MiscUtilities.prepareFile(filePath, ldt, needsRenaming = needsRenaming)
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
     */
    def run(directory: String, needsRenaming: Boolean = false): Unit = {
      FileUtilities.iterateFiles(directory)
        .foreach {
          filePath: Path =>
            val fileName: String = FileUtilities.splitExtension(filePath, isPathNeeded = false)
              .head
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
            match {
              case Some(extractedFileTimestamp: LocalDateTime) =>
                Constants.ReferenceExifTimestamps
                  .foreach {
                    tag: String =>
                      StringUtilities.prepareExiftoolOutput(
                        s"""${Constants.ExiftoolBinary} -$tag "$filePath""""
                      )
                        .headOption match {
                        case Some(element: Array[String]) =>
                          TimestampUtilities.convertStringToTimestamp(
                            element.last, Constants.TimestampFormatters("exif")
                          ) match {
                            case Some(ldt: LocalDateTime) =>
                              println(
                                s"Looking at $filePath with file timestamp $extractedFileTimestamp" +
                                  s" and $ldt ($tag)"
                              )
                              if (!extractedFileTimestamp.equals(ldt))
                                treatedFiles += ((filePath, LocalDateTime.now))
                            case None =>
                              treatedFiles += ((filePath, LocalDateTime.now))
                          }
                        case None =>
                          treatedFiles += ((filePath, LocalDateTime.now))
                      }
                  }
              case None =>
                treatedFiles += ((filePath, LocalDateTime.now))
            }
        }
      FileUtilities.moveFailedFiles(
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
      case "potentialExif" =>
        ExifAsPotentialReference
      case "file" =>
        FileNameAsReference
      case "validate" =>
        Validate
    }
  }

}
