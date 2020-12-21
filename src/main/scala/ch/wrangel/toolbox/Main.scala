package ch.wrangel.toolbox

import ch.wrangel.toolbox.utilities.{FileUtilities, MiscUtilities}

import scala.util.Try

/** Command line tool to synchronize photo file names, exif and mac timestamps */
object Main extends App {

  val relevantParameters = args.slice(0, args.length - 1).toSeq
  if (Constants.ParameterSpace.keys.toSeq.contains(relevantParameters)) {
    val arguments
      : Seq[String] = Constants.ParameterSpace(relevantParameters) :+ args.last
    FileUtilities.createOrAdaptExifConfigFile()
    MiscUtilities.handleZeroByteLengthFiles(arguments.last)
    UseCaseFactory(arguments.head).run( // Use case
      arguments.last, // Directory
      Try {
        arguments(1).toBoolean // Rename true false needsRenaming
      }.getOrElse(false),
      Try {
        arguments(2).toBoolean // Handle secondary timestamps
      }.getOrElse(false)
    )
  } else
    System.out.println(Constants.WelcomeText)

}
