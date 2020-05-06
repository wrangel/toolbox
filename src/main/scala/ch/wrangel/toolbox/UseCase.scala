package ch.wrangel.toolbox

import java.nio.file.Path
import java.time.LocalDateTime

import scala.collection.mutable.ListBuffer

/** Holds common tools for manipulation */
trait UseCase {

  /** Instantiates a [[List]] of files which are to be treated */
  val treatedFiles: ListBuffer[(Path, LocalDateTime)] = ListBuffer[(Path, LocalDateTime)]()

  /** Instantiates a second [[List]] of files which are to be treated */
  val treatedFiles2: ListBuffer[(Path, LocalDateTime)] = ListBuffer[(Path, LocalDateTime)]()

  /** Runs the task
   *
   * @param directory     [[String]] representation of directory path
   * @param needsRenaming Flag indicating whether file should be renamed
   */
  def run(directory: String, needsRenaming: Boolean): Unit

}
