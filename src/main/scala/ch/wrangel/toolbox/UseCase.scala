// UseCase.scala
package ch.wrangel.toolbox

import java.nio.file.Path
import java.time.LocalDateTime
import scala.collection.mutable.ListBuffer
import wvlet.log.LogSupport

/** Holds common tools for manipulation */
trait UseCase extends LogSupport {

  /** Instantiates a [[List]] of files which are to be treated */
  val treatedFiles: ListBuffer[(Path, LocalDateTime)] = ListBuffer[(Path, LocalDateTime)]()

  /** Instantiates a second [[List]] of files which are to be treated */
  val treatedFiles2: ListBuffer[(Path, LocalDateTime)] = ListBuffer[(Path, LocalDateTime)]()

  /** Runs the task
   *
   * @param directory     [[String]] representation of directory path
   * @param needsRenaming Flag indicating whether file should be renamed
   * @param treatExifTimestamps Flag indicating whether to treat secondary timestamps
   */
  def run(directory: String, needsRenaming: Boolean, treatExifTimestamps: Boolean): Unit

}
