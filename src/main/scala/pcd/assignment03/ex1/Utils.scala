package pcd.assignment03.ex1

import java.io.File

object Utils:
  case class SearchConfiguration(maxLines: Int, numIntervals: Int, numLongestFile: Int)

  extension (path: String) { // [^a-zA-Z0-9]
    def freshLabel(prefix: String) = s"$prefix-${File(path).getAbsolutePath.split("[|à{}%$!@#&()\"\\-`.+,/\\s\\t\\n~^:]").mkString("-")}"
  }