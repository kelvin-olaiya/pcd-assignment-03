package pcd.assignment03.ex1

import java.io.File

object Utils:
  case class SearchConfiguration(maxLines: Int, numIntervals: Int, numLongestFile: Int)

  extension (path: String) { // non matcha la regex https://regex101.com prova così
    def freshLabel(prefix: String) = s"$prefix-${File(path).getAbsolutePath.split("[|{}%$!@#&()\"\\-`.+,/\\s\\t\\n~^:]").mkString("-")}"
  }