package view;

import controller.executors.ExecutorSourceAnalyzer;
import controller.utils.SearchConfiguration;
import controller.SourceAnalyzer;
import controller.virtual_threads.VTSourceAnalyzer;
import model.resources.Directory;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

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
    private final Class<?> sourceAnalyzer;

    public GUI(Class<?> sourceAnalyzer) {
        this.sourceAnalyzer = sourceAnalyzer;
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
        startButton.addActionListener(e -> {
            totalFilesBox.reset();
            durationBox.reset();
            int maxLines = (int) maxLinesBox.spinner.getValue();
            int intervals = (int) intervalsBox.spinner.getValue();
            int longestFiles = (int) longestFilesBox.spinner.getValue();
            SearchConfiguration searchConfiguration = new SearchConfiguration(intervals, maxLines, longestFiles);
            SourceAnalyzer sourceAnalyzerInstance = getSourceAnalyzerInstance(this.sourceAnalyzer, searchConfiguration);
            long startTime = System.currentTimeMillis();
            var report = sourceAnalyzerInstance.analyzeSources(new Directory(new File(directory.getText())));
            report.addUpdateHandler((counter, longestFilesList) -> {
                SwingUtilities.invokeLater(() -> {
                    countingListModel.clear();
                    countingListModel.addAll(counter.stream().map(Object::toString).toList());
                    longestFilesModel.clear();
                    longestFilesModel.addAll(longestFilesList);
                });
            });
            report.addOnCompleteHandler(() -> {
                SwingUtilities.invokeLater(() -> {
                    totalFilesBox.textField.setText(
                            report.getIntervals().stream()
                                    .map(report::filesCount)
                                    .reduce(0, Integer::sum)
                                    .toString()
                    );
                    durationBox.textField.setText(String.valueOf(System.currentTimeMillis() - startTime));
                    startButton.setEnabled(true);
                    stopButton.setEnabled(false);
                });

            });
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
            ActionListener al = (s) -> {
                report.abort();
                SwingUtilities.invokeLater(() -> {
                    startButton.setEnabled(true);
                    stopButton.setEnabled(false);
                });
            };
            for(var l : stopButton.getActionListeners()) {
                stopButton.removeActionListener(l);
            }
            stopButton.addActionListener(al);
        });
        controlsPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        controlsPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(inputsPanel, BorderLayout.NORTH);
        panel.add(controlsPanel, BorderLayout.PAGE_END);
        panel.add(mainPanel, BorderLayout.CENTER);
        frame.setVisible(true);
    }

    public GUI() {
        this(ExecutorSourceAnalyzer.class);
    }

    private SourceAnalyzer getSourceAnalyzerInstance(Class<?> sourceAnalyzer, SearchConfiguration searchConfiguration) {
        try {
            return (SourceAnalyzer) sourceAnalyzer.getConstructor(SearchConfiguration.class)
                    .newInstance(searchConfiguration);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
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