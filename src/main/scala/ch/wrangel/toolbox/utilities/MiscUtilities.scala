// utilities/MiscUtilities.scala
package ch.wrangel.toolbox.utilities

import ch.wrangel.toolbox.Constants
import ch.wrangel.toolbox.Constants.ExifToolWebsite
import java.net.URL
import java.nio.file.Paths
import org.apache.commons.text.StringEscapeUtils
import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import scala.io.StdIn
import scala.sys.process.{Process, ProcessLogger}
import scala.util.Try
import wvlet.log.LogSupport

/* Utilities for miscellaneous functionality */
object MiscUtilities extends LogSupport {

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
    val procLogger: ProcessLogger = ProcessLogger { 
      line => 
        stdout.append(line + "\n")
        stderr.append(line + "\n")
    }
    Process(command.stripMargin).!(procLogger) match {
      case 0 =>
        Some(stdout.toString.trim)
      case _ =>
        info(stderr.toString.trim)
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

  /** Currying function returning the ExifTool version present on the local computer
   *
   * @return Current ExifTool version in Double format
   */
  def getPresentExifToolVersion: Double = getProcessOutput(
    s"${Constants.ExifToolBaseCommand.split(Constants.BlankSplitter).head.trim} -ver"
  ).getOrElse("-1").toDouble

}
