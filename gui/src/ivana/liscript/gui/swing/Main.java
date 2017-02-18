package ivana.liscript.gui.swing;

import ivana.liscript.core.*;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.Document;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Main extends JFrame {

    public static Env globalEnv;
    public static JTabbedPane tabbedPane;
    public static Main application;
    private static JCheckBox checkBoxEvalIter, checkBoxShowEvalTime;
    public static int maxstacksize;

    Main() {
        super("Liscript REPL v.0.1");
        this.setIconImage(createImageIcon("images/Lambda.gif", "").getImage());

        WorkPanel.setDefaultSettings();
        SyntaxHighlighter.setDefaultSettings();
        SettingsDialog.readSettingFromFile("settings.xml");

        globalEnv = new Env();

        JToolBar buttonsPanel = new JToolBar(SwingConstants.HORIZONTAL);
        JButton button;

        Action loadFileAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                WorkPanel pane = (WorkPanel)tabbedPane.getSelectedComponent();
                if (pane == null) return;
                if (pane.thread != null) return;
                //if (pane.isCin) return;

                JFileChooser fileopen = new JFileChooser();
                fileopen.setCurrentDirectory(new File("demo\\"));
                fileopen.setFileFilter(new FileNameExtensionFilter("liscript files", "liscript"));
                int ret = fileopen.showDialog(getParent(), "Выберите файл скрипта");
                if (ret == JFileChooser.APPROVE_OPTION) {
                    String fileAbsolutePath = fileopen.getSelectedFile().getAbsolutePath();
                    try {
                        String s = readFileToString(fileAbsolutePath);
                        pane.lastLoadFileNameLabel.setText(fileAbsolutePath);
                        startNewThread(pane, false, s);
                    } catch (IOException ex) {
                        pane.out(true, ex.getLocalizedMessage());
                    }
                }
            }
        };
        button = new JButton();
        button.setAction(loadFileAction);
        //int buttonSize = 32;
        //button.setPreferredSize(new Dimension(buttonSize, buttonSize));
        //button.setIcon(UIManager.getIcon("OptionPane.informationIcon"));
        button.setToolTipText("load file");
        button.setIcon(createImageIcon("images/Download.png", button.getToolTipText()));
        buttonsPanel.add(button);

        Action reloadFileAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                WorkPanel pane = (WorkPanel)tabbedPane.getSelectedComponent();
                if (pane == null) return;
                if (pane.thread != null) return;
                //if (pane.isCin) return;

                String fileAbsolutePath = pane.lastLoadFileNameLabel.getText();
                try {
                    String s = readFileToString(fileAbsolutePath);
                    startNewThread(pane, false, s);
                } catch (IOException ex) {
                    pane.out(true, ex.getLocalizedMessage());
                }
            }
        };
        button = new JButton();
        button.setAction(reloadFileAction);
        button.setToolTipText("reload file");
        //button.setIcon(UIManager.getIcon("OptionPane.warningIcon"));
        button.setIcon(createImageIcon("images/Refresh.png", button.getToolTipText()));
        buttonsPanel.add(button);
        buttonsPanel.addSeparator();

        Action addNewTabAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {addNewTab();}
        };
        button = new JButton();
        button.setAction(addNewTabAction);
        //button.setIcon(UIManager.getIcon("OptionPane.errorIcon"));
        button.setToolTipText("add new tab");
        button.setIcon(createImageIcon("images/Create.png", button.getToolTipText()));
        buttonsPanel.add(button);

        //Action showSettingsDialogAction = new AbstractAction() {
        ActionListener showSettingsDialogAction = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SettingsDialog sd = new SettingsDialog(null);
                sd.setLocationRelativeTo(null);
                sd.showDialog();
            }
        };
        button = new JButton();
        //button.setAction(showSettingsDialogAction);
        button.addActionListener(showSettingsDialogAction);
        button.setToolTipText("settings");
        button.setIcon(createImageIcon("images/Settings.png", button.getToolTipText()));
        buttonsPanel.add(button);
        buttonsPanel.addSeparator();

        JTextArea activeThreads = new JTextArea();
        Action showActiveThreadsAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ThreadGroup threadGroup = Thread.currentThread().getThreadGroup();
                ThreadGroup parent;
                String s = "";
                while ((parent = threadGroup.getParent()) != null) {
                    threadGroup = parent;
                    Thread[] threadList = new Thread[threadGroup.activeCount()];
                    threadGroup.enumerate(threadList);
                    for (Thread thread : threadList) {
                        s = s + thread.getThreadGroup().getName()
                                + " " + thread.getPriority()
                                + " " + thread.getName()
                                + "\n";
                    }
                    activeThreads.setText(s);
                }
            }
        };
        //JButton showActiveThreads = new JButton();
        //showActiveThreads.setAction(showActiveThreadsAction);
        //showActiveThreads.setText("active threads");
        //showActiveThreads.setIcon(UIManager.getIcon("OptionPane.questionIcon"));

        Action interruptAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                WorkPanel pane = (WorkPanel)tabbedPane.getSelectedComponent();
                pane.interruptAction.actionPerformed(e);
            }
        };
        button = new JButton();
        button.setAction(interruptAction);
        button.setToolTipText("interrupt (Esc)");
        button.setIcon(createImageIcon("images/Cancel.png", button.getToolTipText()));
        buttonsPanel.add(button);

        Action sendInputPaneInputAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                WorkPanel pane = (WorkPanel)tabbedPane.getSelectedComponent();
                pane.sendUserInputAction.actionPerformed(e);
            }
        };
        button = new JButton();
        button.setAction(sendInputPaneInputAction);
        //button.setIcon(UIManager.getIcon("OptionPane.questionIcon"));
        button.setToolTipText("run input pane (Ctrl+Enter)");
        button.setIcon(createImageIcon("images/Ok.png", button.getToolTipText()));
        buttonsPanel.add(button);

        Action restoreInputPaneAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                WorkPanel pane = (WorkPanel)tabbedPane.getSelectedComponent();
                pane.restoreUserInputAction.actionPerformed(e);
            }
        };
        button = new JButton();
        button.setAction(restoreInputPaneAction);
        //button.setIcon(UIManager.getIcon("OptionPane.questionIcon"));
        button.setToolTipText("restore input pane (Ctrl+=)");
        button.setIcon(createImageIcon("images/Flip.png", button.getToolTipText()));
        buttonsPanel.add(button);
        buttonsPanel.addSeparator();

        Action openFileAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                WorkPanel pane = (WorkPanel)tabbedPane.getSelectedComponent();
                if (pane == null) return;
                //if (pane.thread != null) return;
                //if (pane.isCin) return;

                JFileChooser fileopen = new JFileChooser();
                fileopen.setCurrentDirectory(new File("demo\\"));
                fileopen.setFileFilter(new FileNameExtensionFilter("liscript files", "liscript"));
                int ret = fileopen.showDialog(getParent(), "Выберите файл скрипта");
                if (ret == JFileChooser.APPROVE_OPTION) {
                    String fileAbsolutePath = fileopen.getSelectedFile().getAbsolutePath();
                    try {
                        String s = readFileToString(fileAbsolutePath);
                        //pane.editPane.setText(s);
                        pane.editPane.setText("");
                        Document doc = pane.editPane.getDocument();
                        doc.insertString(doc.getLength(), s, null);
                        pane.lastOpenFileNameLabel.setText(fileAbsolutePath);
                    } catch (Throwable ex) { //(IOException ex) { (BadLocationException exc) {
                        pane.out(true, ex.getLocalizedMessage());
                    }
                }
            }
        };
        button = new JButton();
        button.setAction(openFileAction);
        button.setToolTipText("open file");
        button.setIcon(createImageIcon("images/Open.png", button.getToolTipText()));
        buttonsPanel.add(button);

        Action saveFileAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                WorkPanel pane = (WorkPanel)tabbedPane.getSelectedComponent();
                if (pane == null) return;
                //if (pane.thread != null) return;
                //if (pane.isCin) return;

                String fileName = pane.lastOpenFileNameLabel.getText();
                //if (!fileName.endsWith(".liscript")) fileName = fileName + ".liscript";
                if (fileName.isEmpty()) {
                    JOptionPane.showMessageDialog(application, "Не выбран файл",
                            "Ошибка", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                String s = pane.editPane.getText();
                if (s.isEmpty()) {
                    JOptionPane.showMessageDialog(application, "Текст пуст",
                            "Ошибка", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                try {
                    writeStringToFile(s, fileName);
                } catch (IOException ex) {
                    pane.out(true, ex.getLocalizedMessage());
                }
            }
        };
        button = new JButton();
        button.setAction(saveFileAction);
        button.setToolTipText("save file");
        button.setIcon(createImageIcon("images/Save.png", button.getToolTipText()));
        buttonsPanel.add(button);

        Action saveFileAsAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                WorkPanel pane = (WorkPanel)tabbedPane.getSelectedComponent();
                if (pane == null) return;
                //if (pane.thread != null) return;
                //if (pane.isCin) return;

                String s = pane.editPane.getText();
                if (s.isEmpty()) {
                    JOptionPane.showMessageDialog(application, "Текст пуст",
                            "Ошибка", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                JFileChooser fileopen = new JFileChooser();
                fileopen.setCurrentDirectory(new File("demo\\"));
                fileopen.setFileFilter(new FileNameExtensionFilter("liscript files", "liscript"));
                int ret = fileopen.showDialog(getParent(), "Выберите файл скрипта");
                if (ret == JFileChooser.APPROVE_OPTION) {
                    String fileName = fileopen.getSelectedFile().getAbsolutePath();
                    if (fileName.isEmpty()) {
                        JOptionPane.showMessageDialog(application, "Не выбран файл",
                                "Ошибка", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    if (!fileName.endsWith(".liscript")) fileName = fileName + ".liscript";
                    try {
                        writeStringToFile(s, fileName);
                        pane.lastOpenFileNameLabel.setText(fileName);
                    } catch (IOException ex) {
                        pane.out(true, ex.getLocalizedMessage());
                    }
                }
            }
        };
        button = new JButton();
        button.setAction(saveFileAsAction);
        button.setToolTipText("save file as");
        button.setIcon(createImageIcon("images/Save as.png", button.getToolTipText()));
        buttonsPanel.add(button);

        Action sendEditPaneInputAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                WorkPanel pane = (WorkPanel)tabbedPane.getSelectedComponent();
                pane.sendEditPaneInputAction.actionPerformed(e);
            }
        };
        button = new JButton();
        button.setAction(sendEditPaneInputAction);
        button.setToolTipText("run edit pane (Ctrl+F1)");
        button.setIcon(createImageIcon("images/Play.png", button.getToolTipText()));
        buttonsPanel.add(button);
        buttonsPanel.addSeparator();

        checkBoxEvalIter = new JCheckBox("Итеративный эвалюатор");
        //checkBoxBold.setSelected((boolean) attr.getAttribute(StyleConstants.Bold));
        buttonsPanel.add(checkBoxEvalIter);

        checkBoxShowEvalTime = new JCheckBox("Замерять время");
        buttonsPanel.add(checkBoxShowEvalTime);

        getContentPane().setLayout(new BorderLayout());
        tabbedPane = new JTabbedPane();
        //tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        addNewTab();
        addNewTab();
        addNewTab();
        getContentPane().add(tabbedPane, BorderLayout.CENTER);
        //sendUserInput.setDefaultCapable(true);
        getContentPane().add(buttonsPanel, BorderLayout.NORTH);

        //this.setBounds(100, 100, 500, 400);
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }

    protected ImageIcon createImageIcon(String path, String description) {
        java.net.URL imgURL = getClass().getResource(path);
        if (imgURL != null) {
            return new ImageIcon(imgURL, description);
        } else {
            System.err.println("Couldn't find file: " + path);
            return null;
        }
    }

    private class ClosableTabTitle extends JPanel {
        public JLabel label;

        public ClosableTabTitle(final String title) {
            super(new BorderLayout(5, 0));
            //super(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            Component buttonTabComponent = this;
            setOpaque(false);
            label = new JLabel(title);
            label.setOpaque(false);
            //label.setIcon(createImageIcon("images/folder_page.png", "reload file"));

            Action closeTabAction = new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    int i = tabbedPane.indexOfTabComponent(buttonTabComponent);
                    if (i != -1) {
                        WorkPanel p =
                                (WorkPanel)tabbedPane.getComponentAt(i);
                        if (p.thread != null) p.thread.interrupt();
                        tabbedPane.remove(i);
                    }
                }
            };
            JButton button = new JButton();
            button.setAction(closeTabAction);
            //button.setIcon(UIManager.getIcon("InternalFrame.paletteCloseIcon"));
            Icon icon = UIManager.getIcon("InternalFrame.closeIcon");
            button.setIcon(icon);
            button.setPreferredSize(new Dimension(icon.getIconWidth(), icon.getIconHeight()));
            button.setToolTipText("close this tab");
            //Make the button looks the same for all Laf's
        //    button.setUI(new BasicButtonUI());
        //    button.setContentAreaFilled(false);

            add(label, BorderLayout.CENTER);
            add(button, BorderLayout.EAST);
            //label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
            setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
        }
    }

    private void addNewTab() {
        tabbedPane.addTab(null, new WorkPanel());
        tabbedPane.setTabComponentAt(
                tabbedPane.getTabCount() - 1, new ClosableTabTitle("      "));
    }

    public static class InterThread extends Thread {
        public String expression;
        public boolean showEcho;
        private WorkPanel pane;

        InterThread(WorkPanel _pane, boolean _showEcho, String _exp) {
            pane = _pane; showEcho = _showEcho; expression = _exp; }

        public void run() {
            try {
                setPaneTabState(pane, 1);

                //pane.out(true, Read.tokens_(expression).toString());
                //pane.out(true, Read.tokens(expression).toString());

                Object lv = Read.string2LispVal(expression);
                    //tokens2LispVal(Read.tokens(expression));
                //if (showEcho) pane.out(true, lv.toString());

                pane.out(false, "=> ");
        //        pane.out(true, Eval.eval(-1, true, pane, globalEnv, lv).toString());
        //        pane.out(true, Eval.evalIter(pane, globalEnv, lv).toString());

                long time;
                time = System.nanoTime();

                if (checkBoxEvalIter.isSelected()) {
                    pane.out(true, Eval.evalIter(pane, globalEnv, lv).toString());
                    pane.out(true, "max стек: " + maxstacksize);
                } else {
                    pane.out(true, Eval.eval(-1, true, pane, globalEnv, lv).toString());
                }

                time = System.nanoTime() - time;
                if (checkBoxShowEvalTime.isSelected())
                    pane.out(true, "" + String.format("%.5f", time/1.E9) + " сек");

            } catch (Throwable e) {
                Thread.currentThread().interrupt();
                //pane.out(true, e.getLocalizedMessage());
                pane.out(true, e.toString());
            }
            pane.thread = null;
            setPaneTabState(pane, 0);
        }
    }

    public static void startNewThread(WorkPanel pane, boolean showEcho, String exp) {
        if (exp == null || exp.trim().isEmpty()) return;

        pane.thread = new InterThread(pane, showEcho, exp);
        pane.thread.start();
    }

    public static void setPaneTabState(WorkPanel pane, int state) {

        Runnable doIt = new Runnable() {
            public void run() {
                int i = tabbedPane.indexOfComponent(pane);
                if (i != -1) {
                    Component c = tabbedPane.getTabComponentAt(i);
                    if (c instanceof ClosableTabTitle) {
                        JLabel label = ((ClosableTabTitle)c).label;
                        if (state == 0) {
                            label.setOpaque(false);
                            label.setBackground(Color.white);
                            pane.textAreaIn.setBackground(
                                    (Color) WorkPanel.styleTextAreaIn.get("background"));
                        } else {
                            label.setOpaque(true);

                            if (state == 2) {
                                label.setBackground(Color.yellow);
                                pane.textAreaIn.setBackground(WorkPanel.textAreaIn_BackgroundIn);
                                pane.textAreaIn.requestFocus();
                            } else {
                                label.setBackground(Color.gray);
                                pane.textAreaIn.setBackground(
                                        (Color) WorkPanel.styleTextAreaIn.get("background"));
                            }
                        }
                    }
                }
                //tabbedPane.setForegroundAt(i, color);
            }
        };

        if (SwingUtilities.isEventDispatchThread()) doIt.run();
        else SwingUtilities.invokeLater(doIt);
    }

    public static void paneout(boolean newLine, String s) {
        WorkPanel pane =
                (WorkPanel)tabbedPane.getSelectedComponent();
        if (pane == null) return;
        pane.out(newLine, s);
    }

    //--------------------------------- MAIN -----------------------------

    //public static String DemoDir(){
        //String path = System.getProperty("java.class.path");
        //String FileSeparator = (String)System.getProperty("file.separator");
        //return path.substring(0, path.lastIndexOf(FileSeparator)+1);

    //    return "demo\\";
        //return "C:\\Users\\Ivana\\Java_1\\txt\\";
        //return "C:\\Users\\Ivana\\Java_1\\src\\com\\company\\txt\\";
        //return "C:\\Users\\Ivana\\Java_1\\out\\artifacts\\Java_1_jar\\";
    //}

    public static String readFileToString_ (String fileAbsolutePath) throws IOException {
        File file = new File(fileAbsolutePath);
        String fileContents = "";
        try (InputStream fileStream = new FileInputStream(file);
             InputStream bufStream = new BufferedInputStream(fileStream);
             Reader reader = new InputStreamReader(bufStream, StandardCharsets.UTF_8)) {

            StringBuilder fileContentsBuilder = new StringBuilder();
            char[] buffer = new char[1024];
            int charsRead;
            while ((charsRead = reader.read(buffer)) != -1) {
                fileContentsBuilder.append(buffer, 0, charsRead);
            }
            fileContents = fileContentsBuilder.toString();
        } catch (IOException e) {
            //throw new RuntimeException(e.getMessage(), e);
            throw e;
        }
        return fileContents;
    }

    private static String readFileToString (String fileAbsolutePath) throws IOException {
        byte[] fileBytes = Files.readAllBytes(Paths.get(fileAbsolutePath));
        return new String(fileBytes, StandardCharsets.UTF_8);
    }

    private static void writeStringToFile (String s, String fileAbsolutePath) throws
            IOException {
        byte[] stringBytes = s.getBytes(StandardCharsets.UTF_8);
        Files.write(Paths.get(fileAbsolutePath), stringBytes);
    }

    private static void loadFile (String fileName) {

        Runnable doIt = new Runnable() {
            public void run() {
                WorkPanel pane =
                        (WorkPanel)tabbedPane.getSelectedComponent();
                if (pane == null) return;
                //if (pane.thread != null) return;
                try {
                    String s = readFileToString(fileName);
                    startNewThread(pane, false, s);
                    //pane.out(true, s);
                } catch (IOException ex) {
                    pane.out(true, ex.getLocalizedMessage());
                }
            }
        };

        //if (SwingUtilities.isEventDispatchThread()) doIt.run();
        //else
        //SwingUtilities.invokeLater(doIt);
        try {
            SwingUtilities.invokeAndWait(doIt);
        } catch(Throwable ex) {
            paneout(true, ex.getLocalizedMessage());
        }
    }

    private void run() {
        loadFile("standard_library.liscript");
        //loadFile("test");
        //loadFile("Demo");
        //pane.out("Lets begin\n");

        WorkPanel pane = (WorkPanel) tabbedPane.getSelectedComponent();
        if (pane != null) pane.textAreaIn.requestFocus();
    }

    public static void main(String[] args) {
        try {UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());}
        catch (Throwable e) {}

        //Main
        application = new Main();
        application.setVisible(true);
        //application.pack();
        Rectangle wa = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        application.setSize(wa.width, wa.height);
        application.setResizable(true);
        application.setExtendedState(JFrame.MAXIMIZED_BOTH);
        application.run();
    }
}
