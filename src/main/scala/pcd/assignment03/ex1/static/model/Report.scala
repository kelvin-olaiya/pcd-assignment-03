package pcd.assignment03.ex1.static.model

import pcd.assignment03.ex1.Report
import pcd.assignment03.ex1.dynamic.Report
import pcd.assignment03.ex1.dynamic.Utils.SearchConfiguration

import java.io.File
import java.nio.file.Files

trait Report:
  def ranges: Iterable[Range]
  def filesInRange(range: Range): Int
  def merge(report: Report): Report
  def rangeOf(lines: Int): Range
  def submit(lines: Int): Report


object Report:
  def apply(counter: Map[Range, Int]): Report = ReportImpl(counter)
  def apply(maxLines: Int, numIntervals: Int): Report =
    val intervalSize = maxLines / (numIntervals - 1)
    var i = 0
    var counter = Map.empty[Range, Int]
    while (i < maxLines) {
      counter = counter + ((i until (i + intervalSize)) -> 0)
      i += intervalSize
    }
    counter = counter + ((i until Int.MaxValue) -> 0)
    ReportImpl(counter)
  def apply(lines: Int, searchConfiguration: SearchConfiguration) = Report.empty(searchConfiguration).submit(lines)
  def empty(searchConfiguration: SearchConfiguration): Report = Report(searchConfiguration.maxLines, searchConfiguration.numIntervals)

  private class ReportImpl(private val counter: Map[Range, Int]) extends Report:
    override def ranges: Iterable[Range] = counter.keys
    override def filesInRange(range: Range): Int = counter.getOrElse(range, 0)
    override def merge(report: Report): Report =
      ReportImpl(this.ranges.map(r => (r, report.filesInRange(r) + this.filesInRange(r))).toMap)
    override def rangeOf(lines: Int): Range = ranges.find(_ contains lines).getOrElse(Int.MinValue until Int.MaxValue)
    override def submit(lines: Int): Report = merge(Report(Map(rangeOf(lines) -> 1)))
    override def toString: String =
      val builder = StringBuilder()
      ranges.toList
        .sorted(_.head - _.head)
        .map(r => s"[${r.start}; ${r.end}) => ${filesInRange(r)}\n")
        .foreach(builder.append)
      builder.toString()

