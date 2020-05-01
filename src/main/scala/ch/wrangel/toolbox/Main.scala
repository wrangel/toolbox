package ch.wrangel.toolbox

import scala.util.Try

/** Entry point to a set of use case driven manipulation methods
 *
 * Typical procedure will be:
 * - exif
 * - file
 * - potentialExif
 * - (validate)
 *
 * Example arguments:
 * <PathToYourFiles> file false
 * <PathToYourFiles> exif true
 *
 * TODO picture
 *- detect partial dates (impute random days and day time)
 * "[0-9]{4}[-,_,/,., ][0-9]{2}", // random? day of month imputation
 * "[0-9]{6}, // random? day of month imputation
 *
 * TODO video
 * Establish which create dates are used by photo programs - adapt "Validate" and create consistent framework
 * AVI (convert with ffmpeg, since exiftool cannot write tags)
 * MiscUtilities.convertVideo(directory) a) as part of a use case, b) extended to other conversions
 */
object Main {

  def main(args: Array[String]): Unit = {
    UseCaseFactory(args(1)).run(
      args.head,
      Try {
        args(2).toBoolean
      }
        .getOrElse(false)
    )
  }

}
