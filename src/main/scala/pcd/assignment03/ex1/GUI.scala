package pcd.assignment03.ex1

import javax.swing.{Box, BoxLayout, JLabel, JSpinner, SpinnerNumberModel}
import java.awt.Component

import javax.swing.{Box, BoxLayout, DefaultListCellRenderer, Dimension, JLabel, JList, JScrollPane, ListModel, JTextField}
import java.awt.Component

class NumericBox(label: String) extends Box(BoxLayout.Y_AXIS) {
  private val textField = new JTextField()
  add(new JLabel(label) {
    setAlignmentX(Component.RIGHT_ALIGNMENT)
  })
  add(textField)
  setSizeForText(textField)
  textField.setEditable(false)

  def reset(): Unit = {
    update(0)
  }

  def update(value: Int): Unit = {
    textField.setText(value.toString)
  }
}

class ListView(listModel: ListModel[String]) extends JScrollPane(new JListString {
  setMinimumSize(new Dimension(300, 400))
  setCellRenderer(new DefaultListCellRenderer)
})

class NumericInputBox(prompt: String, initialValue: Int) extends Box(BoxLayout.Y_AXIS) {
  private val spinner = new JSpinner(new SpinnerNumberModel(initialValue, 0, Integer.MAX_VALUE, 1))
  add(new JLabel(prompt) {
    setAlignmentX(Component.RIGHT_ALIGNMENT)
  })
  add(spinner)
  setSizeForText(spinner)
}
