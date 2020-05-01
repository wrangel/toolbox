package ch.wrangel.toolbox.utilities

import java.nio.file._

import scala.collection.mutable.ListBuffer
import scala.jdk.StreamConverters._
import scala.util.{Failure, Success, Try}

/*Utilities for file manipulation */
object FileUtilities {

  /** Iterates through a directory [[String]]
   *
   * @param directory             [[String]] representation of directory path
   * @param walk                  [[Boolean]] indicating whether the iteration will be recursive
   * @param admissibleFileFormats Optional [[Seq]] of relevant file formats
   * @return [[Seq]] of file [[Path]]s within the directory
   */
  def iterateFiles(
                    directory: String,
                    walk: Boolean = false,
                    admissibleFileFormats: Option[Seq[String]] = None
                  ): Seq[Path] = {
    (
      if (walk) {
        Files.walk(Paths.get(directory))
      } else
        Files.list(Paths.get(directory))
      )
      .filter(Files.isRegularFile(_))
      .filter {
        filePath: Path =>
          !Files.isHidden(filePath) &
            !filePath.toString.contains("@") & {
            admissibleFileFormats match {
              case Some(fileExtensions: Seq[String]) =>
                fileExtensions.exists {
                  fileExtension: String =>
                    FileSystems.getDefault
                      .getPathMatcher(s"glob:**.$fileExtension")
                      .matches(filePath)
                }
              case None =>
                true
            }
          }
      }
      .toScala(Seq)
  }

  /** Splits a filename into body and extension
   *
   * @param filePath [[Path]] to the file whose trailer is to be removed
   * @return [[String]] with removed trailers
   */
  def splitExtension(filePath: Path, isPathNeeded: Boolean): Seq[String] = {
    val relevantPathPortion: String = if (isPathNeeded)
      filePath.toString
    else
      filePath.getFileName.toString
    val dotPosition: Int = relevantPathPortion.reverse.indexOf(".") + 1
    val length: Int = relevantPathPortion.length
    Seq(
      relevantPathPortion.substring(0, length - dotPosition),
      relevantPathPortion.substring(length - dotPosition, length)
    )
  }

  /** Convenience method for treating timestamp manipulations
   *
   * @param command  [[String]] command of manipulation process
   * @param tag      Tag to be treated
   * @param filePath [[Path]] to the file
   * @return Flag indicating whether manipulation failed
   */
  def handleManipulation(
                          command: String,
                          tag: String,
                          manipulationType: String,
                          filePath: Path,
                          newDate: String
                        ): Unit = {
    Try {
      StringUtilities.cleanCommand(command)
    } match {
      case Success(output: String) =>
        println(s"$manipulationType: Successfully changed $tag of $filePath to $newDate")
      case Failure(t: Throwable) =>
        println(s"===>>> NOT TREATED: $tag of $filePath")
    }
  }

  /** Handles failed files
   *
   * @param unsuccessfulFiles          [[ListBuffer]] containing failed files
   * @param unsuccessfulFileParentPath [[Path]] to destination folder
   */
  def moveFailedFiles(unsuccessfulFiles: ListBuffer[Path], unsuccessfulFileParentPath: Path): Unit = {
    if (unsuccessfulFiles.nonEmpty) {
      Files.createDirectories(unsuccessfulFileParentPath)
      unsuccessfulFiles.foreach {
        filePath: Path =>
          try {
            Files.move(filePath, Paths.get(unsuccessfulFileParentPath.toString, filePath.getFileName.toString))
          } catch {
            case _: java.nio.file.NoSuchFileException =>
          }
      }

    }
  }

}