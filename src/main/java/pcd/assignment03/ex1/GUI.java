package pcd.assignment03.ex1;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.util.Collection;

import akka.actor.ActorSystem;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;


public class GUI {

    private final static Border border = BorderFactory.createEmptyBorder(10, 20, 10, 20);
    private final JFrame frame = new JFrame("Assignment#03");
    private final DefaultListModel<String> countingListModel = new DefaultListModel<>();
    private final DefaultListModel<String> longestFilesModel = new DefaultListModel<>();
    private final ListView counting = new ListView(countingListModel);
    private final ListView leaderboard = new ListView(longestFilesModel);
    private final NumericBox totalFilesBox = new NumericBox("Files counted:");
    private final NumericBox durationBox = new NumericBox("Duration (ms):");
    private final JTextField directory = new JTextField("./", 20);
    private final JButton startButton = new JButton("START");
    private final JButton stopButton = new JButton("STOP");

    public GUI(Class<?> sourceAnalyzer) {
        frame.setSize(800, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Container panel = frame.getContentPane();

        Box inputsPanel = Box.createVerticalBox();
        Box inputPanelRow0 = Box.createHorizontalBox();
        NumericInputBox maxLinesBox = new NumericInputBox("Max. lines", 1000);
        NumericInputBox intervalsBox = new NumericInputBox("N. Intervals", 5);
        NumericInputBox longestFilesBox = new NumericInputBox("# of longestFiles", 5);

        inputPanelRow0.add(maxLinesBox);
        inputPanelRow0.add(Box.createGlue());
        inputPanelRow0.add(intervalsBox);
        inputPanelRow0.add(Box.createGlue());
        inputPanelRow0.add(longestFilesBox);
        inputPanelRow0.setBorder(border);

        Box inputPanelRow1 = Box.createHorizontalBox();
        inputPanelRow1.add(Box.createGlue());
        var chooseDirButton = new JButton("Choose directory");
        inputPanelRow1.add(chooseDirButton);
        chooseDirButton.addActionListener(e -> {
            var chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            var returnVal = chooser.showSaveDialog(new JFrame());
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                directory.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });
        inputPanelRow1.add(Box.createGlue());
        inputPanelRow1.setBorder(border);

        Box inputPanelRow2 = Box.createHorizontalBox();
        inputPanelRow2.add(directory);
        inputPanelRow2.setBorder(border);

        inputsPanel.add(inputPanelRow0);
        inputsPanel.add(inputPanelRow1);
        inputsPanel.add(inputPanelRow2);

        Box mainPanel = Box.createHorizontalBox();
        mainPanel.add(counting);
        mainPanel.add(Box.createGlue());
        mainPanel.add(leaderboard);
        mainPanel.setBorder(border);

        Box controlsPanel = Box.createHorizontalBox();
        NumericInputBox workersInput = new NumericInputBox("# Workers", 1);
        controlsPanel.add(totalFilesBox);
        controlsPanel.add(Box.createGlue());
        controlsPanel.add(durationBox);
        controlsPanel.add(Box.createGlue());
        controlsPanel.add(startButton);
        controlsPanel.add(Box.createRigidArea(new Dimension(20, 0)));
        controlsPanel.add(stopButton);
        controlsPanel.add(Box.createGlue());
        controlsPanel.add(workersInput);
        stopButton.setEnabled(false);
        stopButton.addActionListener(e ->  {
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
            // TODO: stop actor system
        });
        startButton.addActionListener(e -> {
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
            totalFilesBox.reset();
            durationBox.reset();
            int maxLines = (int) maxLinesBox.spinner.getValue();
            int intervals = (int) intervalsBox.spinner.getValue();
            int longestFiles = (int) longestFilesBox.spinner.getValue();

            // TODO: start actor system
            ActorSystem system = ActorSystem.create("test-system");
            //var as = akka.actor.typed.ActorSystem.create("my actor system");
            //var actorSystem = new ActorSystem<Object>()
                    
            // long startTime = System.currentTimeMillis();
            // durationBox.textField.setText(String.valueOf(System.currentTimeMillis() - startTime));

        });
        controlsPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        controlsPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(inputsPanel, BorderLayout.NORTH);
        panel.add(controlsPanel, BorderLayout.PAGE_END);
        panel.add(mainPanel, BorderLayout.CENTER);
        frame.setVisible(true);
    }

    public void updateReport(Report report) {
        SwingUtilities.invokeLater(() -> {
            countingListModel.clear();
            countingListModel.addAll(
                toJavaCollection(report.ranges()).stream()
                        .map(r -> "[" + r.head() + "; " + r.last() + "] => " + report.filesInRange(r))
                        .toList()
            );
        });
    }

    public void updateLeaderboard(Leaderboard leaderboard) {
        SwingUtilities.invokeLater(() -> {
            longestFilesModel.clear();
            longestFilesModel.addAll(toJavaCollection(leaderboard.toList().map(p -> p._1)));
        });
    }

    private <T> Collection<T> toJavaCollection(scala.collection.Iterable<T> iterable) {
        return scala.jdk.javaapi.CollectionConverters.asJavaCollection(iterable);
    }

    private static void setSizeForText(JComponent component) {
        component.setPreferredSize(new Dimension(100, 40));
        component.setMaximumSize(component.getPreferredSize());
        component.setAlignmentX(Component.RIGHT_ALIGNMENT);
    }

    private static class NumericInputBox extends Box {

        private final JSpinner spinner;

        public NumericInputBox(String prompt, int initialValue) {
            super(BoxLayout.Y_AXIS);
            spinner = new JSpinner(new SpinnerNumberModel(initialValue, 0, Integer.MAX_VALUE, 1));
            add(new JLabel(prompt) {{
                setAlignmentX(Component.RIGHT_ALIGNMENT);
            }});
            add(spinner);
            setSizeForText(spinner);
        }
    }

    private static class NumericBox extends Box {

        private final JTextField textField;

        public NumericBox(String label) {
            super(BoxLayout.Y_AXIS);
            textField = new JTextField();
            add(new JLabel(label) {{
                setAlignmentX(Component.RIGHT_ALIGNMENT);
            }});
            add(textField);
            setSizeForText(textField);
            textField.setEditable(false);
        }

        public void reset() {
            update(0);
        }

        public void update(int value) {
            textField.setText(String.valueOf(value));
        }
    }

    private static class ListView extends JScrollPane {
        public ListView(ListModel<String> listModel) {
            super(new JList<>(listModel) {{
                setMinimumSize(new Dimension(300, 400));
                setCellRenderer(new DefaultListCellRenderer());
            }});
        }
    }
}