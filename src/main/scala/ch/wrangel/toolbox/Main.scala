package ch.wrangel.toolbox

import ch.wrangel.toolbox.utilities.MiscUtilities

import scala.util.Try

/** Command line tool to synchronize photo file names, exif and mac timestamps
 *
 * Example arguments:
 * <PathToFiles> file false
 * <PathToFiles> exif true
 * <PathToFiles> validate
 *
 * Typical procedure will be:
 * - 1) exif    -- YELLOW tag --
 *      To apply a valid DateTimeOriginal / CreateDate to file name,
 *      mac timestamps, and the rest of the exif timestamps
 * - 2) file    -- GREY tag --
 *      To detect a valid timestamp in the file name and apply them to file name,
 *      mac timestamps, and exif timestamps.
 *      If you know an approximate or accurate date or event timestamp of your photo, and there are no accurate
 *      timestamps available for neither exif nor mac, then add the date / timestamp to your photo's name.
 *      The procedure takes it from there
 * - 3) potentialExif -- ORANGE tag --
 *      To detect potentially valid exif dates outside DateTimeOriginal / CreateDate, ask for user's choice,
 *      and apply choice (if any) to file name, mac timestamps, and the rest of the exif timestamps
 * - 4) validate
 *      To check if the timestamp in file name and DateTimeOriginal / Create Date coincide. Move to sub folder
 *      "_unsuccessful" otherwise
 *
 * TODO video
 * Establish which create dates are used by photo programs - adapt "Validate" and create consistent framework
 * AVI (convert with ffmpeg, since exiftool cannot write tags)
 * MiscUtilities.convertVideo(directory) a) as part of a use case, b) extended to other conversions
 */
object Main {

  def main(args: Array[String]): Unit = {
    MiscUtilities.checkForZeroByteLengthFiles(args.head)
    UseCaseFactory(args(1)).run(
      args.head,
      Try { args(2).toBoolean }
        .getOrElse(false)
    )
  }

}
