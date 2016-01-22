package com.company;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class Main extends JFrame {

    public static Env globalEnv;
    private static JTabbedPane tabbedPane;
    private static Color backgroundIn;

    Main() {
        super("Liscript REPL v.0.1");
        JToolBar buttonsPanel = new JToolBar(SwingConstants.HORIZONTAL);
        JButton button;

        backgroundIn = new Color(255, 237, 197);

        Action loadFileAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                WorkPanel pane = (WorkPanel)tabbedPane.getSelectedComponent();
                if (pane == null) return;
                if (pane.thread != null) return;
                //if (pane.isCin) return;

                JFileChooser fileopen = new JFileChooser();
                fileopen.setCurrentDirectory(new File(CurrentDir()));
                fileopen.setFileFilter(new FileNameExtensionFilter("TXT files", "txt"));
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
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
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

                Object lv = Read.tokens2LispVal(Read.tokens(expression));
                //if (showEcho)
                //pane.out(true, lv.toString());
                pane.out(true, Eval.eval(0, true, pane, globalEnv, lv).toString());
            } catch (Throwable e) {
                Thread.currentThread().interrupt();
                pane.out(true, e.toString());
            }

            //pane.out("hash map not thread safe!!! need hash table!!!\n");
            //pane.out("icons\n");
            //pane.out("Lisp demo\n");

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
                            pane.textAreaIn.setBackground(Color.white);
                        } else {
                            label.setOpaque(true);

                            if (state == 2) {
                                label.setBackground(Color.yellow);
                                pane.textAreaIn.setBackground(backgroundIn);
                                pane.textAreaIn.requestFocus();
                            } else {
                                label.setBackground(Color.gray);
                                pane.textAreaIn.setBackground(Color.white);
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

    public static String CurrentDir(){
        //String path = System.getProperty("java.class.path");
        //String FileSeparator = (String)System.getProperty("file.separator");
        //return path.substring(0, path.lastIndexOf(FileSeparator)+1);

        return "C:\\Users\\Ivana\\Java_1\\txt\\";
        //return "C:\\Users\\Ivana\\Java_1\\src\\com\\company\\txt\\";
        //return "C:\\Users\\Ivana\\Java_1\\out\\artifacts\\Java_1_jar\\";
    }

    public static String readFileToString (String fileAbsolutePath) throws IOException {
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

    public static void loadFile (String fileName) {

        Runnable doIt = new Runnable() {
            public void run() {
                WorkPanel pane =
                        (WorkPanel)tabbedPane.getSelectedComponent();
                if (pane == null) return;
                //if (pane.thread != null) return;

                //pane.out(true, "Вернуть CurrentDir() !");
                String fileAbsolutePath = CurrentDir() + fileName + ".txt";
                try {
                    String s = readFileToString(fileAbsolutePath);
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
        globalEnv = new Env(new HashMap <String, Object> (), null);

        loadFile("lib");
        //loadFile("test");

        //loadFile("Demo");
        //pane.out("Lets begin\n");

        WorkPanel pane = (WorkPanel) tabbedPane.getSelectedComponent();
        if (pane != null) pane.textAreaIn.requestFocus();
    }

    public static void main(String[] args) {
        //try {UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());}
        //catch (Throwable e) {}

        Main application = new Main();
        application.setVisible(true);
        application.pack();
        application.run();
    }
}
