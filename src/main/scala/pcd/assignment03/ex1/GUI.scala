package pcd.assignment03.ex1

import java.awt.*
import java.awt.event.ActionEvent
import java.util
import javax.swing.*
import javax.swing.border.Border

object GUI {
  private val border = BorderFactory.createEmptyBorder(10, 20, 10, 20)

  private def setSizeForText(component: JComponent): Unit = {
    component.setPreferredSize(new Dimension(100, 40))
    component.setMaximumSize(component.getPreferredSize)
    component.setAlignmentX(Component.RIGHT_ALIGNMENT)
  }

  class NumericInputBox(prompt: String, initialValue: Int) extends Box(BoxLayout.Y_AXIS) {
    val spinner = new JSpinner(new SpinnerNumberModel(initialValue, 0, Integer.MAX_VALUE, 1))
    add(new JLabel(prompt) {})
    add(spinner)
    setSizeForText(spinner)
  }

  class NumericBox(label: String) extends Box(BoxLayout.Y_AXIS) {
    val textField = new JTextField
    add(new JLabel(label) {})
    add(textField)
    setSizeForText(textField)
    textField.setEditable(false)

    def reset(): Unit = {
      update(0)
    }

    def update(value: Int): Unit = {
      textField.setText(String.valueOf(value))
    }
  }

  private class ListView(listModel: ListModel[String]) extends JScrollPane(new JList[String](listModel) {}) {}
}

class GUI() {
  final private val frame = new JFrame("Assignment#03")
  final private val countingListModel = new DefaultListModel[String]
  final private val longestFilesModel = new DefaultListModel[String]
  final private val counting = new GUI.ListView(countingListModel)
  final private val leaderboard = new GUI.ListView(longestFilesModel)
  final private val totalFilesBox = new GUI.NumericBox("Files counted:")
  final private val durationBox = new GUI.NumericBox("Duration (ms):")
  final private val directory = new JTextField("./", 20)
  final private val startButton = new JButton("START")
  final private val stopButton = new JButton("STOP")

  frame.setSize(800, 600)
  frame.setDefaultCloseOperation(3 /*JFrame.EXIT_ON_CLOSE*/)
  val panel: Container = frame.getContentPane
  val inputsPanel: Box = Box.createVerticalBox
  val inputPanelRow0: Box = Box.createHorizontalBox
  private val maxLinesBox: GUI.NumericInputBox = GUI.NumericInputBox("Max. lines", 1000)
  private val intervalsBox = GUI.NumericInputBox("N. Intervals", 5)
  private val longestFilesBox = GUI.NumericInputBox("# of longestFiles", 5)
  inputPanelRow0.add(maxLinesBox)
  inputPanelRow0.add(Box.createGlue)
  inputPanelRow0.add(intervalsBox)
  inputPanelRow0.add(Box.createGlue)
  inputPanelRow0.add(longestFilesBox)
  inputPanelRow0.setBorder(GUI.border)
  val inputPanelRow1: Box = Box.createHorizontalBox
  inputPanelRow1.add(Box.createGlue)
  val chooseDirButton = new JButton("Choose directory")
  inputPanelRow1.add(chooseDirButton)
  chooseDirButton.addActionListener { _ =>
    val chooser = new JFileChooser
    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY)
    val returnVal = chooser.showSaveDialog(new JFrame)
    if (returnVal == JFileChooser.APPROVE_OPTION) directory.setText(chooser.getSelectedFile.getAbsolutePath)
  }
  inputPanelRow1.add(Box.createGlue)
  inputPanelRow1.setBorder(GUI.border)
  val inputPanelRow2: Box = Box.createHorizontalBox
  inputPanelRow2.add(directory)
  inputPanelRow2.setBorder(GUI.border)
  inputsPanel.add(inputPanelRow0)
  inputsPanel.add(inputPanelRow1)
  inputsPanel.add(inputPanelRow2)
  val mainPanel: Box = Box.createHorizontalBox
  mainPanel.add(counting)
  mainPanel.add(Box.createGlue)
  mainPanel.add(leaderboard)
  mainPanel.setBorder(GUI.border)
  val controlsPanel: Box = Box.createHorizontalBox
  val workersInput = new GUI.NumericInputBox("# Workers", 1)
  controlsPanel.add(totalFilesBox)
  controlsPanel.add(Box.createGlue)
  controlsPanel.add(durationBox)
  controlsPanel.add(Box.createGlue)
  controlsPanel.add(startButton)
  controlsPanel.add(Box.createRigidArea(new Dimension(20, 0)))
  controlsPanel.add(stopButton)
  controlsPanel.add(Box.createGlue)
  controlsPanel.add(workersInput)
  stopButton.setEnabled(false)
  stopButton.addActionListener((e: ActionEvent) => {
    startButton.setEnabled(true)
    stopButton.setEnabled(false)
  })
  startButton.addActionListener((e: ActionEvent) => {
    startButton.setEnabled(false)
    stopButton.setEnabled(true)
    totalFilesBox.reset()
    durationBox.reset()
    val maxLines = maxLinesBox.spinner.getValue.asInstanceOf[Int]
    val intervals = intervalsBox.spinner.getValue.asInstanceOf[Int]
    val longestFiles = longestFilesBox.spinner.getValue.asInstanceOf[Int]
  })
  controlsPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10))
  controlsPanel.setAlignmentX(Component.CENTER_ALIGNMENT)
  panel.add(inputsPanel, BorderLayout.NORTH)
  panel.add(controlsPanel, BorderLayout.PAGE_END)
  panel.add(mainPanel, BorderLayout.CENTER)
  frame.setVisible(true)


  def updateReport(report: Report): Unit = {
    SwingUtilities.invokeLater(() => {
      countingListModel.clear()
      countingListModel.addAll(toJavaCollection(report.ranges).stream.map((r: Range) => "[" + r.head + "; " + r.last + "] => " + report.filesInRange(r)).toList)
    })
  }

  def updateLeaderboard(leaderboard: Leaderboard): Unit = {
    SwingUtilities.invokeLater(() => {
      longestFilesModel.clear()
      longestFilesModel.addAll(toJavaCollection(leaderboard.toList.map(p => s"${p._1}")))
    })
  }

  private def toJavaCollection[T](iterable: Iterable[T]) = scala.jdk.javaapi.CollectionConverters.asJavaCollection(iterable)
}