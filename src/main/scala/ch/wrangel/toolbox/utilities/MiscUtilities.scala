package ch.wrangel.toolbox.utilities

import java.nio.file.{Files, Path, Paths}
import java.time.LocalDateTime

import ch.wrangel.toolbox.Constants

import wvlet.log.LogSupport

import scala.collection.mutable.ListBuffer
import scala.io.StdIn
import scala.sys.process.{Process, ProcessLogger}

/* Utilities for miscellaneous functionality */
object MiscUtilities extends LogSupport {

  /** Prepares an element of the treated files map
    *
    * @param filePath      [[Path]] to the file
    * @param ldt           [[LocalDateTime]] of the file, needed for renaming
    * @param needsRenaming Flag indicating whether renaming is necessary
    * @return [[Tuple2]] holding both [[Path]] to the renamed file and the file's [[LocalDateTime]]
    */
  def prepareFile(filePath: Path,
                  ldt: LocalDateTime,
                  needsRenaming: Boolean): (Path, LocalDateTime) = {
    info("    Adding file to list of files which need manipulation")
    (
      if (needsRenaming)
        TimestampUtilities.renameFileWithTimestamp(filePath, ldt)
      else
        filePath,
      ldt
    )
  }

  /** Checks for zero byte size files
    *
    * @param directory [[String]] representation of directory path
    */
  def handleZeroByteLengthFiles(directory: String): Unit = {
    val zeroByteFiles: ListBuffer[Path] = ListBuffer()
    FileUtilities
      .iterateFiles(directory)
      .foreach { filePath: Path =>
        if (Files.size(filePath) == 0) {
          info(s"   Please check $filePath, since it has byte size of 0")
          zeroByteFiles += filePath
        }
      }
    FileUtilities.moveFiles(zeroByteFiles,
                            Paths.get(directory, Constants.ZeroByteFolder))
  }

  /** Splits a collection into multiple subsets, according to the [[Seq]] of indices provided
    *
    * @param splitPoints [[Seq]] of indices
    * @param s [[String]] to split
    * @param result [[ListBuffer]] containing the subsets
    */
  def splitCollection(splitPoints: Seq[Int],
                      s: String,
                      result: ListBuffer[String]): Unit = {
    val (element, rest) = s.splitAt(splitPoints.head)
    if (rest.nonEmpty)
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
    val procLogger: ProcessLogger = ProcessLogger { line: String =>
      stdout.append(line + "\n")
      stderr.append(line + "\n")
    }
    Process(command.stripMargin).!(procLogger) match {
      case 0 =>
        Some(stdout.toString.trim)
      case _ =>
        info("    Not treated: " + stderr.toString.trim)
        None
    }
  }

  /** Requests correct user input
    *
    * @param message Message to the user
    * @param validRange [[Seq]] of valid values
    * @return Correct user input
    */
  @scala.annotation.tailrec
  def getFeedback(message: String = "", validRange: Seq[String]): String = {
    val addendum: String =
      s"Please select one of (${validRange.mkString(", ")})\n"
    val feedback: String = StdIn.readLine(message.trim + "\n" + addendum)
    if (validRange.contains(feedback))
      feedback
    else
      getFeedback(validRange = validRange)
  }

}
