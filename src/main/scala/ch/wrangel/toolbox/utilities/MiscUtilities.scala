package ch.wrangel.toolbox.utilities

import java.nio.file.{Path, Paths}
import java.time.LocalDateTime

import ch.wrangel.toolbox.Constants


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
            StringUtilities.cleanCommand(
              // mpg to mp4
              s"""${Constants.FfmpegBinary} -i "$filePath" -c:v libx264 -preset veryslow
                 |-crf 0 -y -c:a copy "$outputFilePath"""".stripMargin
            )
            StringUtilities.cleanCommand(s"rm $filePath")
          }
      }
  }

}