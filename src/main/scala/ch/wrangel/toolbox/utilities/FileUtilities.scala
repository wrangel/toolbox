package ch.wrangel.toolbox.utilities

import java.io.{BufferedWriter, File, FileWriter}
import java.nio.file._

import ch.wrangel.toolbox.Constants

import scala.collection.View
import scala.collection.mutable.ListBuffer
import scala.io.{BufferedSource, Source}
import scala.jdk.StreamConverters._

/*Utilities for file manipulation */
object FileUtilities {

  /** Iterates through a directory [[String]]
   *
   * @param directory             [[String]] representation of directory path
   * @param walk                  [[Boolean]] indicating whether the iteration will be recursive
   * @return [[Seq]] of file [[Path]]s within the directory
   */
  def iterateFiles(directory: String, walk: Boolean = false): View[Path] = {
    (
      if (walk) {
        Files.walk(Paths.get(directory))
      } else
        Files.list(Paths.get(directory))
      )
      .toScala(Seq)
      .view
      .filter(Files.isRegularFile(_))
      .filter {
        filePath: Path =>
          val filePathString: String = filePath.toString
          !Files.isHidden(filePath) & { // !filePathString.contains("@") &
            Constants.ExcludedFileTypes
              .map {
                fileType: String =>
                  !filePathString.endsWith(fileType)
              }
              .forall(_ == true)
          }
      }
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

  /** Creates an exif config file, or adds content to it.
   *
   * - Support for large files
   */
  def createOrAdaptExifConfigFile(): Unit = {
    if(!Files.exists(Constants.ExifToolConfigFilePath))
      writeToFile(Constants.ExifToolConfigFileContent)
    else {
      val bufferedSource: BufferedSource = Source.fromFile(Constants.ExifToolConfigFilePath.toString)
      val content: String = bufferedSource.getLines.mkString("\n")
      bufferedSource.close
      if(!content.contains(Constants.ExifToolConfigFileContent)) {
        // Append content to existing content
        writeToFile(content + "\n\n" + Constants.ExifToolConfigFileContent)
      }
    }

    /** Writes content to a file
     *
     * @param content [[String]] representing content to be written to file
     */
    def writeToFile(content: String): Unit = {
      val bw: BufferedWriter = new BufferedWriter(new FileWriter(new File(Constants.ExifToolConfigFilePath.toString)))
      bw.write(content)
      bw.close()
    }
  }

}