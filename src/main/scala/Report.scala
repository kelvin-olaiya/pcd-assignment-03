trait Report:
  def ranges: Iterable[Range]
  def filesInRange(range: Range): Int
  def merge(report: Report): Report

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

  private class ReportImpl(private val counter: Map[Range, Int]) extends Report:
    def ranges: Iterable[Range] = counter.keys
    def filesInRange(range: Range): Int = counter.getOrElse(range, 0)
    def merge(report: Report): Report =
      ReportImpl(this.ranges.map(r => (r, report.filesInRange(r) + this.filesInRange(r))).toMap)