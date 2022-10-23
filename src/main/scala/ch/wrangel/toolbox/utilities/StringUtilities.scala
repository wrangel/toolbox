package ch.wrangel.toolbox.utilities

/* Utilities for [[String]] manipulation */
object StringUtilities {

  /** Prepares the output of the ExifTool for further processing
   *
   * @param command ExifTool command
   * @return [[Array]] of [[String]] [[Array]]s in a processable format
   */
  def prepareExifToolOutput(command: String): Array[Array[String]] = {
    println(command)
    MiscUtilities.getProcessOutput(command)
      .get
      .split("\n")
      .map(_.split(" : ").map(_.trim))
      .filter {
        element: Array[String] =>
          !element.head.contains("scanned") & !element.head.contains("read")
      }
  }

}