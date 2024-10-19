package ch.wrangel.toolbox.utilities

import ch.wrangel.toolbox.Constants
import ch.wrangel.toolbox.Constants.ExifToolWebsite
import java.io.InputStream
import java.net.URI
import java.net.URL
import java.nio.file.Paths
import org.apache.commons.text.StringEscapeUtils
import org.htmlcleaner.{HtmlCleaner, TagNode}
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



  /** Installs a new or updated version of ExifTool */
  def handleExifTool(): Unit = {
    try {
      // Get the name of the dmg
      val dmgSB: StringBuilder = new StringBuilder()
      println("here")
      println(dmgSB)
  /*

      (new HtmlCleaner).clean(URL(Constants.ExifToolWebsite)).getElementsByName("a", true) // Root node
        .foreach {
          (element: TagNode) =>
            val text = StringEscapeUtils.unescapeHtml4(element.getText.toString)
            dmgSB.append(
              Try {
                text.substring(
                  text.indexOf(Constants.ImageIdentifiers.head),
                  text.indexOf(Constants.ImageIdentifiers.last)
                )
              }.getOrElse("")
            )
        }
      val dmg: String = dmgSB.toString
      // Compare present and newest versions
      val newestVersion: Double = dmg.substring(Constants.ImageIdentifiers.head.length + 1, dmg.length).toDouble
      val presentVersion: Double = getPresentExifToolVersion
      // Download if present version is older than newest version, or there is no present version
      if(presentVersion < newestVersion) {
        val downloadPath: String = Paths.get(Constants.DownloadFolder, dmg + Constants.ImageIdentifiers.last).toString
        Try {
          FileUtilities.download(Constants.ExifToolWebsite + "/" + dmg + Constants.ImageIdentifiers.last, downloadPath)
        }
        FileUtilities.handleImage(downloadPath, dmg)
        // Check if newest version is present
        if(getPresentExifToolVersion == newestVersion)
          info(s"Newest ExifTool version ($newestVersion) is now / or has already been installed")
        else
          warn(s"Newest ExifTool version ($newestVersion) could not be installed")
      }*/
    } catch {
      case _: java.net.UnknownHostException =>
        warn("You are offline. No attempt to install newest ExifTool version")
      case _:  java.io.FileNotFoundException =>
        warn(s"$ExifToolWebsite is offline")
    }
  }


}
