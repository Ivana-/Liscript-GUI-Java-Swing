package ivana.liscript.gui.swing;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.*;
import java.util.Enumeration;
import java.util.HashMap;

public class SettingsDialog extends JDialog {

    public static void write(Object f, String filename) throws Exception{
        XMLEncoder encoder =
                new XMLEncoder(
                        new BufferedOutputStream(
                                new FileOutputStream(filename)));
        encoder.writeObject(f);
        encoder.close();
    }
    public static Object read(String filename) throws Exception {
        XMLDecoder decoder =
                new XMLDecoder(new BufferedInputStream(
                        new FileInputStream(filename)));
        Object o = decoder.readObject();
        decoder.close();
        return o;
    }

    private static class FontChooser extends JDialog implements ActionListener {

        private JComboBox nameComboBox, sizeComboBox;
        private JCheckBox boldBox, italicBox;
        private JLabel textLabel;
        private JButton okButton, cancelButton;
        private Font currentFont, returnFont;

        //public FontChooser(JFrame owner, Font inputFont) {
        public FontChooser(JDialog owner, Font inputFont) {
            super(owner, "Выберите шрифт", true);

            setVisible(false);
            try {UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());}
            catch (Throwable e) {}

            setLayout(new BorderLayout());

            JPanel buttonPanel = new JPanel();
            buttonPanel.setLayout(new FlowLayout());
            okButton = new JButton("Ok");
            buttonPanel.add(okButton);
            cancelButton = new JButton("Cancel");
            buttonPanel.add(cancelButton);
            add(buttonPanel, BorderLayout.SOUTH);

            JPanel controlPanel = new JPanel();
            controlPanel.setLayout(new FlowLayout());

            String[] fontName =
                    GraphicsEnvironment.getLocalGraphicsEnvironment()
                            .getAvailableFontFamilyNames();

            int from = 5, to = 40;
            String[] fontSize = new String[to - from + 1];
            for(int i = from; i <= to; i++) fontSize[i - from] = String.valueOf(i);

            nameComboBox = new JComboBox(fontName);
            controlPanel.add(nameComboBox);

            sizeComboBox = new JComboBox(fontSize);
            controlPanel.add(sizeComboBox);

            boldBox = new JCheckBox("Жирный");
            controlPanel.add(boldBox);

            italicBox = new JCheckBox("Наклонный");
            controlPanel.add(italicBox);

            add(controlPanel, BorderLayout.NORTH);

            textLabel = new JLabel("English / Русский", SwingConstants.CENTER);
            add(textLabel, BorderLayout.CENTER);

            //pack();
            setMinimumSize(new Dimension(500, 150));
            //setMaximumSize(new Dimension(500, 150));
            //setPreferredSize(new Dimension(500, 150));
            setResizable(false);

            // и только здесь привязываем локейшен, после выяснения размеров
            setLocationRelativeTo(owner);

            if (inputFont != null){
                currentFont = inputFont;
                nameComboBox.setSelectedItem(currentFont.getName());
                sizeComboBox.setSelectedItem(String.valueOf(currentFont.getSize()));
                boldBox.setSelected(currentFont.isBold());
                italicBox.setSelected(currentFont.isItalic());
                textLabel.setFont(currentFont);
            }

            // и только здесь назначаем листенеры, потому что иначе
            // они отрабатывают при командах выше (setSelectedItem)
            // и получается фигня
            okButton.addActionListener(this);
            cancelButton.addActionListener(this);
            nameComboBox.addActionListener(this);
            sizeComboBox.addActionListener(this);
            boldBox.addActionListener(this);
            italicBox.addActionListener(this);
        }

        public void actionPerformed(ActionEvent e) {
            setProperties();
            if(e.getSource() == okButton) {
                returnFont = currentFont;
                setVisible(false);
            } else if(e.getSource() == cancelButton) {
                setVisible(false);
            }
        }

        private void setProperties() {
            int style = 0;
            if(boldBox.isSelected()) style += Font.BOLD;
            if(italicBox.isSelected()) style += Font.ITALIC;
            currentFont = new Font(nameComboBox.getSelectedItem().toString(), style,
                    Integer.parseInt(sizeComboBox.getSelectedItem().toString()));
            textLabel.setFont(currentFont);
        }

        public void showDialog() { setVisible(true); }

        public Font getFont() { return returnFont; }
    }

    public JPanel chooseTextPanePanel(HashMap<String, Object> attr, String panelText) {

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panel.add(new JLabel(panelText, SwingConstants.CENTER));

        JButton button = new JButton("Шрифт");
        button.setToolTipText("Выбор шрифта");
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Font font = (Font) attr.get("font");
                FontChooser fontChooser = new FontChooser(SettingsDialog.this, font);
                fontChooser.showDialog();
                font = fontChooser.getFont();
                if (font != null) {
                    attr.put("font", font);
                    applySettings();
                }
            }
        });
        panel.add(button);

        button = new JButton("Цвет текста");
        button.setToolTipText("Выбор цвета");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Color color = (Color) attr.get("foreground");
                color = JColorChooser.showDialog(SettingsDialog.this, "Выберите цвет", color);
                if (color != null) {
                    attr.put("foreground", color);
                    applySettings();
                }
            }
        });
        panel.add(button);

        button = new JButton("Цвет фона");
        button.setToolTipText("Выбор цвета");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Color color = (Color) attr.get("background");
                color = JColorChooser.showDialog(SettingsDialog.this, "Выберите цвет", color);
                if (color != null) {
                    attr.put("background", color);
                    applySettings();
                }
            }
        });
        panel.add(button);

        button = new JButton("Цвет курсора");
        button.setToolTipText("Выбор цвета");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Color color = (Color) attr.get("caretColor");
                color = JColorChooser.showDialog(SettingsDialog.this, "Выберите цвет", color);
                if (color != null) {
                    attr.put("caretColor", color);
                    applySettings();
                }
            }
        });
        panel.add(button);

        return panel;
    }

    public JPanel chooseStylePanel(SimpleAttributeSet attr, String panelText) {

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panel.add(new JLabel(panelText, SwingConstants.CENTER));

        JButton button = new JButton("Цвет");
        button.setToolTipText("Выбор цвета");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Color color = (Color) attr.getAttribute(StyleConstants.Foreground);
                color = JColorChooser.showDialog(SettingsDialog.this, "Выберите цвет", color);
                if (color != null) {
                    attr.addAttribute(StyleConstants.Foreground, color);
                    applySettings();
                }
            }
        });
        panel.add(button);

        JCheckBox checkBoxBold = new JCheckBox("Жирный");
        checkBoxBold.setSelected((boolean) attr.getAttribute(StyleConstants.Bold));
        checkBoxBold.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                attr.addAttribute(StyleConstants.Bold, checkBoxBold.isSelected());
                applySettings();
            }
        });
        checkBoxBold.invalidate();
        panel.add(checkBoxBold);

        JCheckBox checkBoxUnderline = new JCheckBox("Подчеркнутый");
        checkBoxUnderline.setSelected((boolean) attr.getAttribute(StyleConstants.Underline));
        checkBoxUnderline.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                attr.addAttribute(StyleConstants.Underline, checkBoxUnderline.isSelected());
                applySettings();
            }
        });
        panel.add(checkBoxUnderline);

        JCheckBox checkBoxItalic = new JCheckBox("Наклонный");
        checkBoxItalic.setSelected((boolean) attr.getAttribute(StyleConstants.Italic));
        checkBoxItalic.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                attr.addAttribute(StyleConstants.Italic, checkBoxItalic.isSelected());
                applySettings();
            }
        });
        panel.add(checkBoxItalic);

        return panel;
    }

    public SettingsDialog(JFrame owner) {
        super(owner, "Настройки", true);

        //setVisible(false);

        try {UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());}
        catch (Throwable e) {}

        setLayout(new GridLayout(0, 1));

        add(chooseTextPanePanel(WorkPanel.styleTextArea, "Окно вывода :"));
        add(chooseTextPanePanel(WorkPanel.styleEditPane, "Окно редактирования :"));
        add(chooseTextPanePanel(WorkPanel.styleTextAreaIn, "Окно ввода :"));

        JButton button;
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panel.add(new JLabel("Цвет фона окна ввода при ожидании ввода: ", SwingConstants.CENTER));

        button = new JButton("Цвет фона");
        button.setToolTipText("Выбор цвета");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Color color = WorkPanel.textAreaIn_BackgroundIn;
                color = JColorChooser.showDialog(SettingsDialog.this, "Выберите цвет", color);
                if (color != null) {
                    WorkPanel.textAreaIn_BackgroundIn = color;
                    applySettings();
                }
            }
        });
        panel.add(button);
        add(panel);

        add(chooseStylePanel(SyntaxHighlighter.styleKeyWord, "Ключевые слова :"));
        add(chooseStylePanel(SyntaxHighlighter.styleEnvBounds,
                "Связанные в глобальном окружении имена :"));
        add(chooseStylePanel(SyntaxHighlighter.styleNumber, "Числа :"));
        add(chooseStylePanel(SyntaxHighlighter.styleBoolean, "Булевы константы :"));
        add(chooseStylePanel(SyntaxHighlighter.styleString, "Строки :"));
        add(chooseStylePanel(SyntaxHighlighter.styleStringError, "Незакрытые строки :"));
        add(chooseStylePanel(SyntaxHighlighter.styleComment, "Комментарии :"));
        add(chooseStylePanel(SyntaxHighlighter.styleUtil, "Скобки и т.п. :"));

        panel = new JPanel(new FlowLayout(FlowLayout.CENTER));

        button = new JButton("Сохранить настройки");
        button.setToolTipText("Будут использованы как настройки по-умолчанию");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    write(getHashMapSettingsToSave(), "settings.xml");
                } catch (Exception ex) {
                    //throw new RuntimeException(e.getMessage(), e);
                    //throw ex;
                }
                setVisible(false);
            }
        });
        panel.add(button);

        button = new JButton("Восстановить базовые настройки");
        button.setToolTipText("Восстановить настройки приложения");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                File file = new File("settings.xml");
                if (file.exists()) {
                    int reply = JOptionPane.showConfirmDialog(null,
                            "Удалить сохраненный файл настроек по умолчанию" +
                                    " и восстановить базовые настройки?",
                            null, JOptionPane.YES_NO_OPTION);
                    if (reply != JOptionPane.YES_OPTION) return;
                }
                if (!file.delete()) return;
                WorkPanel.setDefaultSettings();
                SyntaxHighlighter.setDefaultSettings();
                applySettings();
            }
        });
        panel.add(button);

        button = new JButton("Загрузить тему");
        button.setToolTipText("Загрузить настройки из файла");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fc = new JFileChooser();
                fc.setCurrentDirectory(new File("themes\\"));
                fc.setFileFilter(new FileNameExtensionFilter("xml files", "xml"));
                int ret = fc.showDialog(getParent(), "Выберите файл настройки");
                if (ret == JFileChooser.APPROVE_OPTION) {
                    readSettingFromFile (fc.getSelectedFile().getAbsolutePath());
                    applySettings();
                }
            }
        });
        panel.add(button);

        button = new JButton("Сохранить тему как");
        button.setToolTipText("Сохранить настройки в файл");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                File dir = new File("themes");
                //dir.exists() && dir.isDirectory()
                if (!dir.exists()) {
                    if (!dir.mkdir()) {
                        JOptionPane.showMessageDialog(null,
                            "Не удалось создать каталог настроек //themes",
                            "Ошибка!", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }
                //dir.mkdirs();

                JFileChooser fc = new JFileChooser();
                fc.setCurrentDirectory(new File("themes\\"));
                fc.setFileFilter(new FileNameExtensionFilter("xml files", "xml"));
                int ret = fc.showSaveDialog(null);
                if (ret == JFileChooser.APPROVE_OPTION) {
                    String fileName = fc.getSelectedFile().getAbsolutePath();
                    if (!fileName.endsWith(".xml")) fileName = fileName + ".xml";
                    try {
                        write(getHashMapSettingsToSave(), fileName);
                    } catch ( Exception ex ) {}
                }
            }
        });
        panel.add(button);

        add(panel);

        pack();
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }

    private HashMap<Object, Object> sAS2HM(SimpleAttributeSet attr) {
        HashMap<Object, Object> r = new HashMap<>();
        Enumeration<?> en = attr.getAttributeNames();
        while (en.hasMoreElements()) {
            Object name = en.nextElement();
            r.put(name.toString(), attr.getAttribute(name));
        }
        return r;
    }

    public void showDialog() {setVisible(true);}

    private void applySettings() {
        for (int i = 0; i < Main.tabbedPane.getTabCount(); i++)
            ((WorkPanel)Main.tabbedPane.getComponentAt(i)).applySettings();
    }

    private HashMap<String, Object> getHashMapSettingsToSave() {
        HashMap<String, Object> settings = new HashMap<>();
        settings.put("styleTextArea", WorkPanel.styleTextArea);
        settings.put("styleTextAreaIn", WorkPanel.styleTextAreaIn);
        settings.put("styleEditPane", WorkPanel.styleEditPane);
        settings.put("textAreaIn_BackgroundIn", WorkPanel.textAreaIn_BackgroundIn);
        settings.put("styleUtil", sAS2HM(SyntaxHighlighter.styleUtil));
        settings.put("styleNumber", sAS2HM(SyntaxHighlighter.styleNumber));
        settings.put("styleBoolean", sAS2HM(SyntaxHighlighter.styleBoolean));
        settings.put("styleString", sAS2HM(SyntaxHighlighter.styleString));
        settings.put("styleStringError", sAS2HM(SyntaxHighlighter.styleStringError));
        settings.put("styleEnvBounds", sAS2HM(SyntaxHighlighter.styleEnvBounds));
        settings.put("styleComment", sAS2HM(SyntaxHighlighter.styleComment));
        settings.put("styleKeyWord", sAS2HM(SyntaxHighlighter.styleKeyWord));
        return settings;
    }

    public static void readAttributes (SimpleAttributeSet attr, Object o) {
        if (!(o instanceof HashMap) || attr==null) return;
        HashMap<String, Object> attrFrom = (HashMap<String, Object>) o;

        Enumeration en = attr.getAttributeNames();
        while (en.hasMoreElements()) {
            Object name = en.nextElement();
            String s = name.toString();
            if (attrFrom.containsKey(s)) attr.addAttribute(name, attrFrom.get(s));
        }
    }

    public static void readHM (HashMap<String, Object> attr, Object o) {
        if (!(o instanceof HashMap) || attr==null) return;
        HashMap<String, Object> attrFrom = (HashMap<String, Object>) o;

        for (String s : attr.keySet()) {
            if (attrFrom.containsKey(s)) attr.put(s, attrFrom.get(s));
        }
    }

    public static void readSettingFromFile (String fileName) { //throws IOException {

        try (InputStream fileStream = new FileInputStream(fileName);
             InputStream bufStream = new BufferedInputStream(fileStream);
             XMLDecoder decoder = new XMLDecoder(bufStream)) {

            Object o = decoder.readObject();
            decoder.close();

            if (!(o instanceof HashMap)) return;
            HashMap<String, Object> settings = (HashMap<String, Object>) o;
            for (HashMap.Entry<String, Object> entry : settings.entrySet()) {
                String id = entry.getKey();
                Object v = entry.getValue();
                //System.out.println(id + " = " + v.toString());

                if (id.equals("styleTextArea")) readHM(WorkPanel.styleTextArea, v);
                else if (id.equals("styleTextAreaIn")) readHM(WorkPanel.styleTextAreaIn, v);
                else if (id.equals("styleEditPane")) readHM(WorkPanel.styleEditPane, v);
                else if (id.equals("textAreaIn_BackgroundIn")) {
                    if (v instanceof Color) WorkPanel.textAreaIn_BackgroundIn = (Color)v;
                }
                else if (id.equals("styleUtil")) readAttributes(SyntaxHighlighter.styleUtil, v);
                else if (id.equals("styleNumber")) readAttributes(SyntaxHighlighter.styleNumber, v);
                else if (id.equals("styleBoolean")) readAttributes(SyntaxHighlighter.styleBoolean, v);
                else if (id.equals("styleString")) readAttributes(SyntaxHighlighter.styleString, v);
                else if (id.equals("styleStringError")) readAttributes(SyntaxHighlighter.styleStringError, v);
                else if (id.equals("styleEnvBounds")) readAttributes(SyntaxHighlighter.styleEnvBounds, v);
                else if (id.equals("styleComment")) readAttributes(SyntaxHighlighter.styleComment, v);
                else if (id.equals("styleKeyWord")) readAttributes(SyntaxHighlighter.styleKeyWord, v);
            }
        } catch (Exception e) {
            //throw new RuntimeException(e.getMessage(), e);
            //throw e;
        }
    }

}
