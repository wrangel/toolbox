package ch.wrangel.toolbox.utilities

import java.nio.file.{Files, Path, Paths}
import java.time.LocalDateTime

import ch.wrangel.toolbox.Constants

import scala.collection.mutable.ListBuffer
import scala.sys.process.{Process, ProcessLogger}


/* Utilities for miscellaneous functionality */
object MiscUtilities {

  /** Prepares an element of the treated files map
   *
   * @param filePath      [[Path]] to the file
   * @param ldt           [[LocalDateTime]] of the file, needed for renaming
   * @param needsRenaming Flag indicating whether renaming is necessary
   * @return [[Tuple2]] holding both [[Path]] to the renamed file and the file's [[LocalDateTime]]
   */
  def prepareFile(filePath: Path, ldt: LocalDateTime, needsRenaming: Boolean): (Path, LocalDateTime) = {
    println(s"Adding $filePath to list of files which need manipulation")
    (
      if (needsRenaming)
        TimestampUtilities.renameFileWithTimestamp(filePath, ldt)
      else
        filePath,
      ldt
    )
  }

  /** Losslessly copies video files to a format whose exif metadata is writable by exiftool
   * https://trac.ffmpeg.org/wiki/Encode/H.264
   *
   * @param directory [[String]] representation of directory path
   */
  def convertVideo(directory: String): Unit = {
    FileUtilities.iterateFiles(directory)
      .foreach {
        filePath: Path =>
          val filePathComponents: Seq[String] = FileUtilities.splitExtension(filePath, isPathNeeded = true)
          val outputFilePath: Path = Paths.get(filePathComponents.head + Constants.TargetVideoFormat)
          val extension: String = filePathComponents.last
            .toLowerCase
          if (Constants.ProblematicVideoFormats.contains(extension)) {
            println(s"Convert $filePath to $outputFilePath")
            getProcessOutput(
              // mpg to mp4
              s"""${Constants.FfmpegBinary} -i "$filePath" -c:v libx264 -preset veryslow
                 |-crf 0 -y -c:a copy "$outputFilePath""""
            )
            getProcessOutput(s"rm $filePath")
          }
      }
  }

  /** Checks for zero byte size files
   *
   * @param directory [[String]] representation of directory path
   */
  def checkForZeroByteLengthFiles(directory: String): Unit = {
    val zeroByteFiles: ListBuffer[String] = new ListBuffer[String]()
    FileUtilities.iterateFiles(directory)
      .foreach {
        filePath: Path =>
          if (Files.size(filePath) == 0) {
            zeroByteFiles += filePath.toString
            println(s"Please remove $filePath, since it has byte size of 0")
          }
      }
    if (zeroByteFiles.nonEmpty)
      System.exit(0)
  }

  /** Splits a collection into multiple subsets, according to the [[Seq]] of indices provided
   *
   * @param splitPoints [[Seq]] of indices
   * @param s [[String]] to split
   * @param result [[ListBuffer]] containing the subsets
   */
  def splitCollection(splitPoints: Seq[Int], s: String, result: ListBuffer[String]): Unit = {
    val (element, rest) = s.splitAt(splitPoints.head)
    if(rest.nonEmpty)
      splitCollection(splitPoints.tail, rest, result)
    element +=: result
  }

  /** Handles shell processes
   * https://stackoverflow.com/questions/29935873/how-to-run-external-process-in-scala-and-get-both-exit-code-and-output
   *
   * @param command [[String]] representing shell command
   * @return Optional Stdout
   */
  def getProcessOutput(command: String): Option[String] = {
    val stdout: StringBuilder = new StringBuilder
    val stderr: StringBuilder = new StringBuilder
    val logger: ProcessLogger = ProcessLogger(stdout.append(_), stderr.append(_))
    val status: Int = Process(command.stripMargin).!(logger)
    status match {
      case 0 =>
        Some(stdout.toString.trim)
      case _ =>
        println(">>> NOT TREATED: " + stderr.toString.trim)
        None
    }
  }

}