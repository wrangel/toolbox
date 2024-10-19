// Main.scala
package ch.wrangel.toolbox

import ch.wrangel.toolbox.utilities.{FileUtilities, MiscUtilities}
import scala.util.Try
import wvlet.log.LogSupport

/** Command line tool to synchronize photo file names, exif and mac timestamps */
object Main extends LogSupport {
	def main(args: Array[String]): Unit = {
    val relevantParameters = args.slice(0, args.length - 1).toSeq
    if (Constants.ParameterSpace.keys.toSeq.contains(relevantParameters)) {
      // Prevent mac from going to sleep
	    // TODO implement MiscUtilities.caffeinate()
      // Install or update ExifTool, if necessary
      MiscUtilities.handleExifTool()
      // Handle the ExifTool config file
      FileUtilities.createOrAdaptExifConfigFile()
      // Handle zero bytes files
      val arguments: Seq[String] = Constants.ParameterSpace(relevantParameters) :+ args.last
      FileUtilities.handleZeroByteLengthFiles(arguments.last)
      UseCaseFactory(arguments.head).run(
        arguments.last,
        Try{arguments(1).toBoolean}.getOrElse(false),
        Try{arguments(2).toBoolean}.getOrElse(false)
      )
    } else 
      info(Constants.TextWelcome)
  }
}