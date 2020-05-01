package ch.wrangel.toolbox

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
 *      mac timestamps, and exif timestamps
 * - 3) potentialExif
 *      To detect potentially valid exif dates outside DateTimeOriginal / CreateDate, ask for user's choice,
 *      and and apply them to file name, mac timestamps, and the rest of the exif timestamps
 * - 4) validate
 *      To check if the timestamp in file name and DateTimeOriginal / Create Date coincide. Move to sub folder
 *      "_unsuccessful" otherwise
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
