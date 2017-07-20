/*
 * NotePadView.java
 */
package notepad;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.print.PageFormat;
import java.awt.print.PrinterJob;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EventObject;
import java.util.List;
import java.util.TooManyListenersException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JFormattedTextField.AbstractFormatter;
import javax.swing.JFormattedTextField.AbstractFormatterFactory;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.EditorKit;
import javax.swing.undo.UndoManager;
import org.jdesktop.application.Action;
import org.jdesktop.application.Application.ExitListener;
import org.jdesktop.application.FrameView;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.SingleFrameApplication;
import org.jdesktop.application.TaskMonitor;
import org.jvnet.substance.skin.SubstanceCremeCoffeeLookAndFeel;

/**
 * The application's main frame.
 */
public class NotePadView extends FrameView {

    private PageFormat page;
    private Document document;
    private int findIndex = 0;
    private String currentFile;
    private PrinterJob printer;
    private String oldText = "";
    private EditorKit editorKit;
    private boolean upDownFlag = false;
    private String findNextText = "";
    private String currentLook = metal;
    private final DefaultComboBoxModel findModel;
    private final DefaultComboBoxModel replaceModel;
    private DropTarget dropTarget = new DropTarget();
    private JFileChooser chooser = new JFileChooser();
    private UndoManager undoManager = new UndoManager();
    private DefaultListModel fontNameModel = new DefaultListModel();
    private DefaultListModel fontSizeModel = new DefaultListModel();
    private static final String metal = "org.jvnet.substance.skin.SubstanceCremeCoffeeLookAndFeel";
    private static final String motif = "org.jvnet.substance.skin.SubstanceOfficeBlue2007LookAndFeel";
    private static final String windows = "org.jvnet.substance.skin.SubstanceNebulaBrickWallLookAndFeel";
    private Font fontList[] = GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();

    public NotePadView(SingleFrameApplication app) {
        super(app);
        getFrame().setUndecorated(true);
        app.addExitListener(new ExitListener() {

            public boolean canExit(EventObject event) {
                return true;
            }

            public void willExit(EventObject event) {
                if (editorPane.getText() == null) {
                    return;
                }
                if (!oldText.equals(editorPane.getText()) && editorPane.getText() != null) {
                    int select = JOptionPane.showConfirmDialog(null, "是否保存当前文档？");
                    if (select == JOptionPane.YES_OPTION) {
                        try {
                            save();
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(null, ex.getMessage());
                            Logger.getLogger(NotePadView.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            }
        });
        initComponents();
        ButtonGroup bg = new ButtonGroup();
        bg.add(windowsLook);
        bg.add(javaLook);
        bg.add(motifLook);
        fontNameList.setModel(fontNameModel);
        fontSizeList.setModel(fontSizeModel);
        for (Font f : fontList) {
            fontNameModel.addElement(f.getName());
        }
        for (int i = 2; i < 73; i++) {
            fontSizeModel.addElement(i);
        }
        Font f = fontList[fontList.length - 5];
        f = new Font(f.getName(), Font.PLAIN, 15);
        editorPane.setFont(f);
        initFontDialog();
        try {
            UIManager.setLookAndFeel(new SubstanceCremeCoffeeLookAndFeel());
            JFrame.setDefaultLookAndFeelDecorated(true);
            JDialog.setDefaultLookAndFeelDecorated(true);
            getRootPane().setWindowDecorationStyle(JRootPane.FRAME);
            //UIManager.setLookAndFeel(currentLook);
            SwingUtilities.updateComponentTreeUI(getFrame());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(editorPane, ex.getMessage());
            Logger.getLogger(NotePadView.class.getName()).log(Level.SEVERE, null, ex);
        }
        dialog.setSize(408, 164);
        toWhere.setSize(202, 150);
        findModel = new DefaultComboBoxModel();
        replaceModel = new DefaultComboBoxModel();
        findBox.setModel(findModel);
        replaceBox.setModel(replaceModel);
        ButtonGroup bg2 = new ButtonGroup();
        bg2.add(encodingGBK);
        bg2.add(encodingISO);
        bg2.add(encodingUTF8);
        chooser.setFileFilter(new FileFilter() {

            @Override
            public boolean accept(File f) {
                return f.getName().endsWith(".TXT") || f.getName().endsWith(".txt") || f.isDirectory();
            }

            @Override
            public String getDescription() {
                return "文本文件（*.txt）";
            }
        });
        document = editorPane.getDocument();
        document.addUndoableEditListener(undoManager);
        editorKit = editorPane.getEditorKit();
        getFrame().setTitle("无标题-Java版记事本");
        editorPane.setDropTarget(dropTarget);
        dropTarget.setActive(true);
        rowTextField.setFormatterFactory(new AbstractFormatterFactory() {

            @Override
            public AbstractFormatter getFormatter(JFormattedTextField tf) {
                return new AbstractFormatter() {

                    @Override
                    public Object stringToValue(String text) throws ParseException {
                        if (text.matches("^[1-9]\\d*$")) {
                            return text;
                        } else {
                            return "";
                        }
                    }

                    @Override
                    public String valueToString(Object value) throws ParseException {
                        return value == null ? "" : value.toString();
                    }
                };
            }
        });
        rowTextField.addCaretListener(new CaretListener() {

            public void caretUpdate(CaretEvent e) {
                try {
                    rowTextField.commitEdit();
                } catch (ParseException ex) {
                    JOptionPane.showMessageDialog(null, ex.getMessage());
                    Logger.getLogger(NotePadView.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
        try {
            dropTarget.addDropTargetListener(new DropTargetAdapter() {

                public void drop(DropTargetDropEvent dtde) {
                    try {
                        if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                            File f;
                            dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
                            List list = (List) (dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor));
                            if (list != null && list.size() > 0) {
                                f = (File) list.get(0);
                                if (f.getName().endsWith(".txt") || f.getName().endsWith(".TXT")) {
                                    if (!oldText.equals(editorPane.getText()) && editorPane.getText() != null) {
                                        int select = JOptionPane.showConfirmDialog(null, "是否保存当前文档？");
                                        if (select == JOptionPane.YES_OPTION) {
                                            save();
                                            loadFile(f);
                                            getFrame().setTitle(f.getAbsolutePath());
                                            dtde.dropComplete(true);
                                        } else {
                                            loadFile(f);
                                            getFrame().setTitle(f.getAbsolutePath());
                                            dtde.dropComplete(true);
                                        }
                                    } else {
                                        loadFile(f);
                                        getFrame().setTitle(f.getAbsolutePath());
                                        dtde.dropComplete(true);
                                    }
                                } else {
                                    Toolkit.getDefaultToolkit().beep();
                                }
                            } else {
                                Toolkit.getDefaultToolkit().beep();
                            }
                        } else {
                            dtde.rejectDrop();
                        }
                    } catch (Exception ioe) {
                        JOptionPane.showMessageDialog(null, ioe.getMessage());
                    }
                }
            });
        } catch (TooManyListenersException ex) {
            JOptionPane.showMessageDialog(null, ex.getMessage());
            Logger.getLogger(NotePadView.class.getName()).log(Level.SEVERE, null, ex);
        }
        // status bar initialization - message timeout, idle icon and busy animation, etc
        ResourceMap resourceMap = getResourceMap();
        int messageTimeout = resourceMap.getInteger("StatusBar.messageTimeout");
        messageTimer = new Timer(messageTimeout, new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                statusMessageLabel.setText("");
            }
        });
        messageTimer.setRepeats(false);
        int busyAnimationRate = resourceMap.getInteger("StatusBar.busyAnimationRate");
        for (int i = 0; i < busyIcons.length; i++) {
            busyIcons[i] = resourceMap.getIcon("StatusBar.busyIcons[" + i + "]");
        }
        busyIconTimer = new Timer(busyAnimationRate, new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                busyIconIndex = (busyIconIndex + 1) % busyIcons.length;
            }
        });
        idleIcon = resourceMap.getIcon("StatusBar.idleIcon");
        // connecting action tasks to status bar via TaskMonitor
        TaskMonitor taskMonitor = new TaskMonitor(getApplication().getContext());
        taskMonitor.addPropertyChangeListener(new java.beans.PropertyChangeListener() {

            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                String propertyName = evt.getPropertyName();
                if ("started".equals(propertyName)) {
                    if (!busyIconTimer.isRunning()) {
                        busyIconIndex = 0;
                        busyIconTimer.start();
                    }
                } else if ("done".equals(propertyName)) {
                    busyIconTimer.stop();
                } else if ("message".equals(propertyName)) {
                    String text = (String) (evt.getNewValue());
                    statusMessageLabel.setText((text == null) ? "" : text);
                    messageTimer.restart();
                } else if ("progress".equals(propertyName)) {
                    int value = (Integer) (evt.getNewValue());
                }
            }
        });
    }

    @Action
    public boolean findText() {
        Object o = findBox.getSelectedItem();
        String findText = o == null ? "" : o.toString();
        String text;
        if (!findText.equals("")) {
            findBox.setSelectedItem(findText);
        }
        if (!"".equals(findText)) {
            text = editorPane.getText().replaceAll("\n", "");
            findIndex = text.indexOf(findText, findIndex);
            findNextText = findText;
            if (findIndex >= 0) {
                editorPane.setSelectionStart(findIndex);
                editorPane.setSelectionEnd(findIndex + findText.length());
                editorPane.setSelectionColor(Color.BLUE);
                editorPane.scrollToReference(findText);
                statusAnimationLabel.setText("");
            } else {
                statusAnimationLabel.setText("已到文档尾！");
                return true;
            }
        } else {
            Toolkit.getDefaultToolkit().beep();
        }
        return false;
    }

    @Action
    public void findNext() {
        findIndex = editorPane.getCaretPosition();
        if (!findNextText.equals("")) {
            String text = editorPane.getText().replaceAll("\n", "");
            findIndex = text.indexOf(findNextText, findIndex);
            if (findIndex >= 0) {
                editorPane.setSelectionStart(findIndex);
                editorPane.setSelectionEnd(findIndex + findNextText.length());
                editorPane.setSelectionColor(Color.BLUE);
                editorPane.scrollToReference(findNextText);
                statusAnimationLabel.setText("");
            } else {
                statusAnimationLabel.setText("已到文档尾！");
            }
        }
    }

    /**
     * 返回光标当前行列
     *
     * @return
     */
    private Point getDot() {
        if (editorPane.getText() == null) {
            return new Point(0, 0);
        }
        String[] text = editorPane.getText().split("\n");
        int hang = 0, lie = 0, dot = editorPane.getCaretPosition();
        for (int i = 0; i < text.length; i++) {
            lie = dot - text[i].length() <= 0 ? dot : dot - text[i].length();
            hang = i;
            dot -= text[i].length();
            if (dot < 0) {
                break;
            } else if (dot == 0 && text[i].endsWith("\r")) {
                hang += 1;
                lie = 0;
                break;
            }
        }
        return new Point(hang + 1, lie + 1);
    }

    @Action
    public void showAboutBox() {
        if (aboutBox == null) {
            JFrame mainFrame = NotePadApp.getApplication().getMainFrame();
            aboutBox = new NotePadAboutBox(mainFrame);
            aboutBox.setLocationRelativeTo(mainFrame);
        }
        NotePadApp.getApplication().show(aboutBox);
    }

    @Action
    public void date() {
        try {
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            document.insertString(editorPane.getCaretPosition(), df.format(new Date()), null);
        } catch (BadLocationException ex) {
            JOptionPane.showMessageDialog(null, ex.getMessage());
            Logger.getLogger(NotePadView.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Action
    public void moveUpRow() {
        upDownFlag = true;
        moveRow();
    }

    @Action
    public void moveDownRow() {
        upDownFlag = false;
        moveRow();
    }

    private void moveRow() {
        String[] rows = editorPane.getText().split("\n");
        Point p = getDot();
        int index = (int) p.getX();
        String temp = "";
        if (upDownFlag) {
            if (index < 2) {
                Toolkit.getDefaultToolkit().beep();
                return;
            }
            temp = rows[index - 2];
            rows[index - 2] = rows[index - 1];
            rows[index - 1] = temp;
        } else {
            if (index >= rows.length) {
                Toolkit.getDefaultToolkit().beep();
                return;
            }
            temp = rows[index];
            rows[index] = rows[index - 1];
            rows[index - 1] = temp;
        }
        temp = "";
        int pos = 0;
        for (int i = 0; i < rows.length; i++) {
            temp += rows[i] + "\n";
            if (upDownFlag ? i < index - 2 : i < index) {
                pos += rows[i].length();
            }
        }
        editorPane.setText(temp);
        editorPane.setCaretPosition(pos);
    }

    @Action
    public void closeFindDialog() {
        dialog.dispose();
    }

    @Action
    public void newEdit() {
        if (!oldText.equals(editorPane.getText()) && editorPane.getText() != null) {
            int select = JOptionPane.showConfirmDialog(null, "是否保存当前文档？");
            if (select == JOptionPane.YES_OPTION) {
                save();
                editorPane.setText("");
                getFrame().setTitle("无标题-Java版记事本");
                currentFile = null;
            } else if (select == JOptionPane.NO_OPTION) {
                editorPane.setText("");
                getFrame().setTitle("无标题-Java版记事本");
                currentFile = null;
            } else {
                return;
            }
        } else {
            editorPane.setText("");
            getFrame().setTitle("无标题-Java版记事本");
            currentFile = null;
        }
        oldText = editorPane.getText();
        undoManager.end();
    }

    @Action
    public void saveOther() {
        currentFile = null;
        save();
    }

    @Action
    public void save() {
        FileWriter fw = null;
        try {
            File file = null;
            if (currentFile == null) {
                if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                    file = chooser.getSelectedFile();
                    currentFile = file.getAbsolutePath();
                    currentFile = !currentFile.contains(".") ? currentFile + ".txt" : currentFile;
                    file = new File(currentFile);
                } else {
                    return;
                }
            } else {
                file = new File(currentFile);
            }
            fw = new FileWriter(file);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(editorPane.getText(), 0, editorPane.getText().length());
            bw.flush();
            fw.flush();
            fw.close();
            bw.close();
            getFrame().setTitle(file.getAbsolutePath() + "-Java版记事本");
            oldText = editorPane.getText();
            statusAnimationLabel.setText("保存完成！");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null, ex.getMessage());
            Logger.getLogger(NotePadView.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                fw.close();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(null, ex.getMessage());
                Logger.getLogger(NotePadView.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Action
    public void undo() {
        if (undoManager.canUndo()) {
            undoManager.undo();
        } else {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    @Action
    public void redo() {
        if (undoManager.canRedo()) {
            undoManager.redo();
        } else {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    @Action
    public void deleted() {
        editorPane.replaceSelection("");
    }

    @Action
    public void font() {
        int x = getFrame().getX() + getFrame().getWidth() - 437, y = getFrame().getY() + getFrame().getHeight() - 324;
        fontDialog.setSize(437, 324);
        fontDialog.setLocation(x / 2, y / 2);
        fontDialog.setVisible(true);
    }

    @Action
    public void setFontAction() {
        editorPane.setFont(getSelectFont());
        fontDialog.setVisible(false);
    }

    private Font getSelectFont() {
        String name = fontNameList.getSelectedValue().toString();
        int size = Integer.parseInt(fontSizeList.getSelectedValue().toString());
        int style = fontStyleList.getSelectedIndex();
        switch (style) {
            case 0://常规
                style = Font.PLAIN;
                break;
            case 1://斜体
                style = Font.BOLD;
                break;
            case 2://粗体
                style = Font.ITALIC;
                break;
            case 3://粗斜体
                style = Font.BOLD + Font.ITALIC;
                break;
        }
        Font f = new Font(name, style, size);
        return f;
    }

    private void initFontDialog() {
        Font f = editorPane.getFont();
        fontNameList.setSelectedValue(f.getName(), true);
        fontSizeList.setSelectedValue(f.getSize(), true);
        fontStyleList.setSelectedIndex(0);
    }

    @Action
    public void cutAction() {
        editorPane.cut();
    }

    @Action
    public void copyAction() {
        editorPane.copy();
    }

    @Action
    public void selectAllAction() {
        editorPane.selectAll();
    }

    @Action
    public void pasteAction() {
        editorPane.paste();
    }

    @Action
    public void find() {
        int x = getFrame().getX() + getFrame().getWidth(), y = getFrame().getY() + getFrame().getHeight();
        dialog.setLocation(x / 2, y / 2);
        dialog.setVisible(true);
        findBox.setSelectedItem(editorPane.getSelectedText());
    }

    @Action
    public void openAction() throws Exception {
        int select;
        if (currentFile != null) {
            chooser.setCurrentDirectory(new File(currentFile));
        }
        if (editorPane.getText() == null) {
            openMethod();
            return;
        }
        if (!oldText.equals(editorPane.getText()) && editorPane.getText() != null) {
            select = JOptionPane.showConfirmDialog(null, "是否保存当前文档？");
            if (select == JOptionPane.YES_OPTION) {
                save();
                openMethod();
            } else if (select == JOptionPane.NO_OPTION) {
                openMethod();
            }
        } else {
            openMethod();
        }

    }

    @Action
    public void toWhere() {
        int x = getFrame().getX() + getFrame().getWidth(), y = getFrame().getY() + getFrame().getHeight();
        toWhere.setLocation(x / 2, y / 2);
        rowTextField.setText("");
        toWhere.setVisible(true);
    }

    @Action
    public void pageSetting() {
        if (printer == null) {
            printer = PrinterJob.getPrinterJob();
            page = printer.defaultPage();
        }
        page = printer.pageDialog(page);
    }

    @Action
    public void printPage() {
        if (printer == null) {
            printer = PrinterJob.getPrinterJob();
            page = printer.defaultPage();
        }
        printer.defaultPage(page);
        if (printer.printDialog()) {
            try {
                printer.print();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, e.getMessage());
            }
        }
    }

    private void openMethod() {
        int select = chooser.showOpenDialog(null);
        if (select == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            getFrame().setTitle(f.getAbsolutePath() + "-Java版记事本");
            loadFile(f);
        }
    }

    private void loadFile(File f) {
        FileReader is = null;
        try {
            is = new FileReader(f);
            currentFile = f.getAbsolutePath();
            editorPane.setText("");
            editorKit.read(is, document, 0);
            oldText = editorPane.getText();
            is.close();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, ex.getMessage());
            Logger.getLogger(NotePadView.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                is.close();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(null, ex.getMessage());
                Logger.getLogger(NotePadView.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        bindingGroup = new org.jdesktop.beansbinding.BindingGroup();

        menuBar = new javax.swing.JMenuBar();
        javax.swing.JMenu fileMenu = new javax.swing.JMenu();
        newMenuItem = new javax.swing.JMenuItem();
        openMenuItem = new javax.swing.JMenuItem();
        saveMenuItem = new javax.swing.JMenuItem();
        saveAsMenuItem = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        settingMenuItem = new javax.swing.JMenuItem();
        printMenuItem = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JPopupMenu.Separator();
        exitMenuItem = new javax.swing.JMenuItem();
        editMenu = new javax.swing.JMenu();
        undoMenuItem = new javax.swing.JMenuItem();
        jMenuItem1 = new javax.swing.JMenuItem();
        jSeparator3 = new javax.swing.JPopupMenu.Separator();
        cutMenuItem = new javax.swing.JMenuItem();
        copyMenuItem = new javax.swing.JMenuItem();
        pasteMenuItem = new javax.swing.JMenuItem();
        deleteMenuItem = new javax.swing.JMenuItem();
        jSeparator4 = new javax.swing.JPopupMenu.Separator();
        findMenuItem = new javax.swing.JMenuItem();
        nextMenuItem = new javax.swing.JMenuItem();
        replaceMenuItem = new javax.swing.JMenuItem();
        gotoMenuItem = new javax.swing.JMenuItem();
        jSeparator5 = new javax.swing.JPopupMenu.Separator();
        selectAllMenuItem = new javax.swing.JMenuItem();
        dateMenuItem = new javax.swing.JMenuItem();
        jSeparator8 = new javax.swing.JPopupMenu.Separator();
        moveUpMenuItem = new javax.swing.JMenuItem();
        moveDownMenuItem = new javax.swing.JMenuItem();
        styleMenu = new javax.swing.JMenu();
        autoEnterMenuItem = new javax.swing.JCheckBoxMenuItem();
        fontMenuItem = new javax.swing.JMenuItem();
        encodingMenu = new javax.swing.JMenu();
        encodingGBK = new javax.swing.JRadioButtonMenuItem();
        encodingUTF8 = new javax.swing.JRadioButtonMenuItem();
        encodingISO = new javax.swing.JRadioButtonMenuItem();
        viewMenu = new javax.swing.JMenu();
        statusMenuItem = new javax.swing.JRadioButtonMenuItem();
        javax.swing.JMenu helpMenu = new javax.swing.JMenu();
        javax.swing.JMenuItem aboutMenuItem = new javax.swing.JMenuItem();
        lookAndViewMenu = new javax.swing.JMenu();
        windowsLook = new javax.swing.JRadioButtonMenuItem();
        javaLook = new javax.swing.JRadioButtonMenuItem();
        motifLook = new javax.swing.JRadioButtonMenuItem();
        mainPanel = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        editorPane = new javax.swing.JEditorPane();
        statusPanel = new javax.swing.JPanel();
        statusMessageLabel = new javax.swing.JLabel();
        statusAnimationLabel = new javax.swing.JLabel();
        dialog = new javax.swing.JDialog(getFrame());
        findLabel = new javax.swing.JLabel();
        replaceLabel = new javax.swing.JLabel();
        findButton = new javax.swing.JButton();
        replaceButton = new javax.swing.JButton();
        replaceAllButton = new javax.swing.JButton();
        closeButton = new javax.swing.JButton();
        findBox = new javax.swing.JComboBox();
        replaceBox = new javax.swing.JComboBox();
        popMenu = new javax.swing.JPopupMenu();
        popUndo = new javax.swing.JMenuItem();
        popRedo = new javax.swing.JMenuItem();
        jSeparator6 = new javax.swing.JPopupMenu.Separator();
        popCopy = new javax.swing.JMenuItem();
        popPaste = new javax.swing.JMenuItem();
        popCut = new javax.swing.JMenuItem();
        popDel = new javax.swing.JMenuItem();
        jSeparator7 = new javax.swing.JPopupMenu.Separator();
        popSelectAll = new javax.swing.JMenuItem();
        fontDialog = new javax.swing.JDialog();
        jLabel1 = new javax.swing.JLabel();
        fontNameTextField = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        fontStyleTextField = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        fontSizeTextField = new javax.swing.JTextField();
        fontConfirmButton = new javax.swing.JButton();
        fontCancelButton = new javax.swing.JButton();
        previewTextField = new javax.swing.JTextField();
        jScrollPane2 = new javax.swing.JScrollPane();
        fontNameList = new javax.swing.JList();
        jScrollPane3 = new javax.swing.JScrollPane();
        fontStyleList = new javax.swing.JList();
        jScrollPane4 = new javax.swing.JScrollPane();
        fontSizeList = new javax.swing.JList();
        toWhere = new javax.swing.JDialog();
        rowTextField = new javax.swing.JFormattedTextField();
        jLabel4 = new javax.swing.JLabel();
        toWhereConfirm = new javax.swing.JButton();
        toWhereCancel = new javax.swing.JButton();

        menuBar.setBorder(null);
        menuBar.setName("menuBar"); // NOI18N

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(notepad.NotePadApp.class).getContext().getResourceMap(NotePadView.class);
        fileMenu.setText(resourceMap.getString("fileMenu.text")); // NOI18N
        fileMenu.setName("fileMenu"); // NOI18N

        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(notepad.NotePadApp.class).getContext().getActionMap(NotePadView.class, this);
        newMenuItem.setAction(actionMap.get("newEdit")); // NOI18N
        newMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_N, java.awt.event.InputEvent.CTRL_MASK));
        newMenuItem.setText(resourceMap.getString("newMenuItem.text")); // NOI18N
        newMenuItem.setName("newMenuItem"); // NOI18N
        fileMenu.add(newMenuItem);

        openMenuItem.setAction(actionMap.get("openAction")); // NOI18N
        openMenuItem.setText(resourceMap.getString("openMenuItem.text")); // NOI18N
        openMenuItem.setName("openMenuItem"); // NOI18N
        fileMenu.add(openMenuItem);

        saveMenuItem.setAction(actionMap.get("save")); // NOI18N
        saveMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_MASK));
        saveMenuItem.setText(resourceMap.getString("saveMenuItem.text")); // NOI18N
        saveMenuItem.setName("saveMenuItem"); // NOI18N
        fileMenu.add(saveMenuItem);

        saveAsMenuItem.setAction(actionMap.get("saveOther")); // NOI18N
        saveAsMenuItem.setText(resourceMap.getString("saveAsMenuItem.text")); // NOI18N
        saveAsMenuItem.setName("saveAsMenuItem"); // NOI18N
        fileMenu.add(saveAsMenuItem);

        jSeparator1.setToolTipText(resourceMap.getString("jSeparator1.toolTipText")); // NOI18N
        jSeparator1.setName("jSeparator1"); // NOI18N
        fileMenu.add(jSeparator1);

        settingMenuItem.setAction(actionMap.get("pageSetting")); // NOI18N
        settingMenuItem.setText(resourceMap.getString("settingMenuItem.text")); // NOI18N
        settingMenuItem.setName("settingMenuItem"); // NOI18N
        fileMenu.add(settingMenuItem);

        printMenuItem.setAction(actionMap.get("printPage")); // NOI18N
        printMenuItem.setText(resourceMap.getString("printMenuItem.text")); // NOI18N
        printMenuItem.setName("printMenuItem"); // NOI18N
        fileMenu.add(printMenuItem);

        jSeparator2.setName("jSeparator2"); // NOI18N
        fileMenu.add(jSeparator2);

        exitMenuItem.setAction(actionMap.get("quit")); // NOI18N
        exitMenuItem.setText(resourceMap.getString("exitMenuItem.text")); // NOI18N
        exitMenuItem.setName("exitMenuItem"); // NOI18N
        fileMenu.add(exitMenuItem);

        menuBar.add(fileMenu);

        editMenu.setText(resourceMap.getString("editMenu.text")); // NOI18N
        editMenu.setName("editMenu"); // NOI18N

        undoMenuItem.setAction(actionMap.get("undo")); // NOI18N
        undoMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Z, java.awt.event.InputEvent.CTRL_MASK));
        undoMenuItem.setText(resourceMap.getString("undoMenuItem.text")); // NOI18N
        undoMenuItem.setName("undoMenuItem"); // NOI18N
        editMenu.add(undoMenuItem);

        jMenuItem1.setAction(actionMap.get("redo")); // NOI18N
        jMenuItem1.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Y, java.awt.event.InputEvent.CTRL_MASK));
        jMenuItem1.setText(resourceMap.getString("jMenuItem1.text")); // NOI18N
        jMenuItem1.setName("jMenuItem1"); // NOI18N
        editMenu.add(jMenuItem1);

        jSeparator3.setName("jSeparator3"); // NOI18N
        editMenu.add(jSeparator3);

        cutMenuItem.setAction(actionMap.get("cutAction")); // NOI18N
        cutMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_X, java.awt.event.InputEvent.CTRL_MASK));
        cutMenuItem.setText(resourceMap.getString("cutMenuItem.text")); // NOI18N
        cutMenuItem.setName("cutMenuItem"); // NOI18N
        editMenu.add(cutMenuItem);

        copyMenuItem.setAction(actionMap.get("copyAction")); // NOI18N
        copyMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C, java.awt.event.InputEvent.CTRL_MASK));
        copyMenuItem.setText(resourceMap.getString("copyMenuItem.text")); // NOI18N
        copyMenuItem.setName("copyMenuItem"); // NOI18N
        editMenu.add(copyMenuItem);

        pasteMenuItem.setAction(actionMap.get("pasteAction")); // NOI18N
        pasteMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_V, java.awt.event.InputEvent.CTRL_MASK));
        pasteMenuItem.setText(resourceMap.getString("pasteMenuItem.text")); // NOI18N
        pasteMenuItem.setName("pasteMenuItem"); // NOI18N
        editMenu.add(pasteMenuItem);

        deleteMenuItem.setAction(actionMap.get("deleted")); // NOI18N
        deleteMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DELETE, 0));
        deleteMenuItem.setText(resourceMap.getString("deleteMenuItem.text")); // NOI18N
        deleteMenuItem.setName("deleteMenuItem"); // NOI18N
        editMenu.add(deleteMenuItem);

        jSeparator4.setName("jSeparator4"); // NOI18N
        editMenu.add(jSeparator4);

        findMenuItem.setAction(actionMap.get("find")); // NOI18N
        findMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F, java.awt.event.InputEvent.CTRL_MASK));
        findMenuItem.setText(resourceMap.getString("findMenuItem.text")); // NOI18N
        findMenuItem.setName("findMenuItem"); // NOI18N
        editMenu.add(findMenuItem);

        nextMenuItem.setAction(actionMap.get("findNext")); // NOI18N
        nextMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F3, 0));
        nextMenuItem.setText(resourceMap.getString("nextMenuItem.text")); // NOI18N
        nextMenuItem.setName("nextMenuItem"); // NOI18N
        editMenu.add(nextMenuItem);

        replaceMenuItem.setAction(actionMap.get("find")); // NOI18N
        replaceMenuItem.setText(resourceMap.getString("replaceMenuItem.text")); // NOI18N
        replaceMenuItem.setName("replaceMenuItem"); // NOI18N
        editMenu.add(replaceMenuItem);

        gotoMenuItem.setAction(actionMap.get("toWhere")); // NOI18N
        gotoMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_G, java.awt.event.InputEvent.CTRL_MASK));
        gotoMenuItem.setText(resourceMap.getString("gotoMenuItem.text")); // NOI18N
        gotoMenuItem.setName("gotoMenuItem"); // NOI18N
        editMenu.add(gotoMenuItem);

        jSeparator5.setName("jSeparator5"); // NOI18N
        editMenu.add(jSeparator5);

        selectAllMenuItem.setAction(actionMap.get("selectAllAction")); // NOI18N
        selectAllMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_A, java.awt.event.InputEvent.CTRL_MASK));
        selectAllMenuItem.setText(resourceMap.getString("selectAllMenuItem.text")); // NOI18N
        selectAllMenuItem.setName("selectAllMenuItem"); // NOI18N
        editMenu.add(selectAllMenuItem);

        dateMenuItem.setAction(actionMap.get("date")); // NOI18N
        dateMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F5, 0));
        dateMenuItem.setText(resourceMap.getString("dateMenuItem.text")); // NOI18N
        dateMenuItem.setName("dateMenuItem"); // NOI18N
        editMenu.add(dateMenuItem);

        jSeparator8.setName("jSeparator8"); // NOI18N
        editMenu.add(jSeparator8);

        moveUpMenuItem.setAction(actionMap.get("moveUpRow")); // NOI18N
        moveUpMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_UP, java.awt.event.InputEvent.ALT_MASK));
        moveUpMenuItem.setText(resourceMap.getString("moveUpMenuItem.text")); // NOI18N
        moveUpMenuItem.setName("moveUpMenuItem"); // NOI18N
        editMenu.add(moveUpMenuItem);

        moveDownMenuItem.setAction(actionMap.get("moveDownRow")); // NOI18N
        moveDownMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DOWN, java.awt.event.InputEvent.ALT_MASK));
        moveDownMenuItem.setText(resourceMap.getString("moveDownMenuItem.text")); // NOI18N
        moveDownMenuItem.setName("moveDownMenuItem"); // NOI18N
        editMenu.add(moveDownMenuItem);

        menuBar.add(editMenu);

        styleMenu.setText(resourceMap.getString("styleMenu.text")); // NOI18N
        styleMenu.setName("styleMenu"); // NOI18N

        autoEnterMenuItem.setText(resourceMap.getString("autoEnterMenuItem.text")); // NOI18N
        autoEnterMenuItem.setToolTipText(resourceMap.getString("autoEnterMenuItem.toolTipText")); // NOI18N
        autoEnterMenuItem.setEnabled(false);
        autoEnterMenuItem.setName("autoEnterMenuItem"); // NOI18N
        autoEnterMenuItem.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                autoEnterMenuItemItemStateChanged(evt);
            }
        });
        styleMenu.add(autoEnterMenuItem);

        fontMenuItem.setAction(actionMap.get("font")); // NOI18N
        fontMenuItem.setText(resourceMap.getString("fontMenuItem.text")); // NOI18N
        fontMenuItem.setName("fontMenuItem"); // NOI18N
        styleMenu.add(fontMenuItem);

        encodingMenu.setText(resourceMap.getString("encodingMenu.text")); // NOI18N
        encodingMenu.setName("encodingMenu"); // NOI18N

        encodingGBK.setSelected(true);
        encodingGBK.setText(resourceMap.getString("encodingGBK.text")); // NOI18N
        encodingGBK.setEnabled(false);
        encodingGBK.setName("encodingGBK"); // NOI18N
        encodingGBK.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                encodingGBKItemStateChanged(evt);
            }
        });
        encodingMenu.add(encodingGBK);

        encodingUTF8.setText(resourceMap.getString("encodingUTF8.text")); // NOI18N
        encodingUTF8.setEnabled(false);
        encodingUTF8.setName("encodingUTF8"); // NOI18N
        encodingUTF8.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                encodingUTF8ItemStateChanged(evt);
            }
        });
        encodingMenu.add(encodingUTF8);

        encodingISO.setText(resourceMap.getString("encodingISO.text")); // NOI18N
        encodingISO.setEnabled(false);
        encodingISO.setName("encodingISO"); // NOI18N
        encodingISO.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                encodingISOItemStateChanged(evt);
            }
        });
        encodingMenu.add(encodingISO);

        styleMenu.add(encodingMenu);

        menuBar.add(styleMenu);

        viewMenu.setText(resourceMap.getString("viewMenu.text")); // NOI18N
        viewMenu.setName("viewMenu"); // NOI18N

        statusMenuItem.setSelected(true);
        statusMenuItem.setText(resourceMap.getString("statusMenuItem.text")); // NOI18N
        statusMenuItem.setName("statusMenuItem"); // NOI18N
        statusMenuItem.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                statusMenuItemItemStateChanged(evt);
            }
        });
        viewMenu.add(statusMenuItem);

        menuBar.add(viewMenu);

        helpMenu.setText(resourceMap.getString("helpMenu.text")); // NOI18N
        helpMenu.setName("helpMenu"); // NOI18N

        aboutMenuItem.setAction(actionMap.get("showAboutBox")); // NOI18N
        aboutMenuItem.setText(resourceMap.getString("aboutMenuItem.text")); // NOI18N
        aboutMenuItem.setName("aboutMenuItem"); // NOI18N
        helpMenu.add(aboutMenuItem);

        menuBar.add(helpMenu);

        lookAndViewMenu.setText(resourceMap.getString("lookAndViewMenu.text")); // NOI18N
        lookAndViewMenu.setName("lookAndViewMenu"); // NOI18N

        windowsLook.setText(resourceMap.getString("windowsLook.text")); // NOI18N
        windowsLook.setName("windowsLook"); // NOI18N
        windowsLook.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                windowsLookItemStateChanged(evt);
            }
        });
        lookAndViewMenu.add(windowsLook);

        javaLook.setSelected(true);
        javaLook.setText(resourceMap.getString("javaLook.text")); // NOI18N
        javaLook.setName("javaLook"); // NOI18N
        javaLook.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                javaLookItemStateChanged(evt);
            }
        });
        lookAndViewMenu.add(javaLook);

        motifLook.setText(resourceMap.getString("motifLook.text")); // NOI18N
        motifLook.setName("motifLook"); // NOI18N
        motifLook.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                motifLookItemStateChanged(evt);
            }
        });
        lookAndViewMenu.add(motifLook);

        menuBar.add(lookAndViewMenu);

        mainPanel.setName("mainPanel"); // NOI18N

        jScrollPane1.setBorder(null);
        jScrollPane1.setName("jScrollPane1"); // NOI18N

        editorPane.setBorder(null);
        editorPane.setContentType(resourceMap.getString("editorPane.contentType")); // NOI18N
        editorPane.setFont(resourceMap.getFont("editorPane.font")); // NOI18N
        editorPane.setComponentPopupMenu(popMenu);
        editorPane.setCursor(new java.awt.Cursor(java.awt.Cursor.TEXT_CURSOR));
        editorPane.setDragEnabled(true);
        editorPane.setDropMode(javax.swing.DropMode.INSERT);
        editorPane.setName("editorPane"); // NOI18N
        editorPane.addCaretListener(new javax.swing.event.CaretListener() {
            public void caretUpdate(javax.swing.event.CaretEvent evt) {
                editorPaneCaretUpdate(evt);
            }
        });
        jScrollPane1.setViewportView(editorPane);
        editorPane.getAccessibleContext().setAccessibleDescription(resourceMap.getString("editorPane.AccessibleContext.accessibleDescription")); // NOI18N

        statusPanel.setName("statusPanel"); // NOI18N

        statusMessageLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        statusMessageLabel.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);
        statusMessageLabel.setAlignmentX(5.0F);
        statusMessageLabel.setAlignmentY(5.0F);
        statusMessageLabel.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        statusMessageLabel.setMaximumSize(new java.awt.Dimension(10, 10));
        statusMessageLabel.setMinimumSize(new java.awt.Dimension(10, 10));
        statusMessageLabel.setName("statusMessageLabel"); // NOI18N

        statusAnimationLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        statusAnimationLabel.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        statusAnimationLabel.setName("statusAnimationLabel"); // NOI18N

        javax.swing.GroupLayout statusPanelLayout = new javax.swing.GroupLayout(statusPanel);
        statusPanel.setLayout(statusPanelLayout);
        statusPanelLayout.setHorizontalGroup(
            statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, statusPanelLayout.createSequentialGroup()
                .addComponent(statusAnimationLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 439, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(statusMessageLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 173, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        statusPanelLayout.setVerticalGroup(
            statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(statusMessageLabel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 20, Short.MAX_VALUE)
            .addComponent(statusAnimationLabel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 20, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout mainPanelLayout = new javax.swing.GroupLayout(mainPanel);
        mainPanel.setLayout(mainPanelLayout);
        mainPanelLayout.setHorizontalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(statusPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 618, Short.MAX_VALUE)
        );
        mainPanelLayout.setVerticalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, mainPanelLayout.createSequentialGroup()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 630, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(statusPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        dialog.setName("dialog"); // NOI18N
        dialog.setResizable(false);
        dialog.getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        findLabel.setText(resourceMap.getString("findLabel.text")); // NOI18N
        findLabel.setName("findLabel"); // NOI18N
        dialog.getContentPane().add(findLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 10, -1, -1));

        replaceLabel.setText(resourceMap.getString("replaceLabel.text")); // NOI18N
        replaceLabel.setName("replaceLabel"); // NOI18N
        dialog.getContentPane().add(replaceLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 40, -1, -1));

        findButton.setText(resourceMap.getString("findButton.text")); // NOI18N
        findButton.setName("findButton"); // NOI18N
        findButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                findButtonMouseClicked(evt);
            }
        });
        dialog.getContentPane().add(findButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(299, 10, 90, -1));

        replaceButton.setText(resourceMap.getString("replaceButton.text")); // NOI18N
        replaceButton.setName("replaceButton"); // NOI18N
        replaceButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                replaceButtonMouseClicked(evt);
            }
        });
        dialog.getContentPane().add(replaceButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(299, 40, 90, -1));

        replaceAllButton.setText(resourceMap.getString("replaceAllButton.text")); // NOI18N
        replaceAllButton.setName("replaceAllButton"); // NOI18N
        replaceAllButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                replaceAllButtonMouseClicked(evt);
            }
        });
        dialog.getContentPane().add(replaceAllButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(300, 70, 90, -1));

        closeButton.setAction(actionMap.get("closeFindDialog")); // NOI18N
        closeButton.setText(resourceMap.getString("closeButton.text")); // NOI18N
        closeButton.setName("closeButton"); // NOI18N
        dialog.getContentPane().add(closeButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(299, 100, 90, -1));

        findBox.setEditable(true);
        findBox.setName("findBox"); // NOI18N
        dialog.getContentPane().add(findBox, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 10, 220, -1));

        replaceBox.setEditable(true);
        replaceBox.setName("replaceBox"); // NOI18N
        dialog.getContentPane().add(replaceBox, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 40, 220, -1));

        popMenu.setMinimumSize(new java.awt.Dimension(100, 100));
        popMenu.setName("popMenu"); // NOI18N

        popUndo.setAction(actionMap.get("undo")); // NOI18N
        popUndo.setText(resourceMap.getString("popUndo.text")); // NOI18N
        popUndo.setMinimumSize(new java.awt.Dimension(4, 4));
        popUndo.setName("popUndo"); // NOI18N
        popMenu.add(popUndo);

        popRedo.setAction(actionMap.get("redo")); // NOI18N
        popRedo.setText(resourceMap.getString("popRedo.text")); // NOI18N
        popRedo.setName("popRedo"); // NOI18N
        popMenu.add(popRedo);

        jSeparator6.setName("jSeparator6"); // NOI18N
        popMenu.add(jSeparator6);

        popCopy.setAction(actionMap.get("copyAction")); // NOI18N
        popCopy.setText(resourceMap.getString("popCopy.text")); // NOI18N
        popCopy.setMinimumSize(new java.awt.Dimension(4, 4));
        popCopy.setName("popCopy"); // NOI18N
        popMenu.add(popCopy);

        popPaste.setAction(actionMap.get("pasteAction")); // NOI18N
        popPaste.setText(resourceMap.getString("popPaste.text")); // NOI18N
        popPaste.setMinimumSize(new java.awt.Dimension(4, 4));
        popPaste.setName("popPaste"); // NOI18N
        popMenu.add(popPaste);

        popCut.setAction(actionMap.get("cutAction")); // NOI18N
        popCut.setText(resourceMap.getString("popCut.text")); // NOI18N
        popCut.setName("popCut"); // NOI18N
        popMenu.add(popCut);

        popDel.setAction(actionMap.get("deleted")); // NOI18N
        popDel.setText(resourceMap.getString("popDel.text")); // NOI18N
        popDel.setName("popDel"); // NOI18N
        popMenu.add(popDel);

        jSeparator7.setName("jSeparator7"); // NOI18N
        popMenu.add(jSeparator7);

        popSelectAll.setAction(actionMap.get("selectAllAction")); // NOI18N
        popSelectAll.setText(resourceMap.getString("popSelectAll.text")); // NOI18N
        popSelectAll.setName("popSelectAll"); // NOI18N
        popMenu.add(popSelectAll);

        fontDialog.setModal(true);
        fontDialog.setName("fontDialog"); // NOI18N
        fontDialog.setResizable(false);
        fontDialog.getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel1.setText(resourceMap.getString("jLabel1.text")); // NOI18N
        jLabel1.setName("jLabel1"); // NOI18N
        fontDialog.getContentPane().add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 10, -1, -1));

        fontNameTextField.setBackground(resourceMap.getColor("fontNameTextField.background")); // NOI18N
        fontNameTextField.setText(resourceMap.getString("fontNameTextField.text")); // NOI18N
        fontNameTextField.setName("fontNameTextField"); // NOI18N
        fontDialog.getContentPane().add(fontNameTextField, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 30, 170, -1));

        jLabel2.setText(resourceMap.getString("jLabel2.text")); // NOI18N
        jLabel2.setName("jLabel2"); // NOI18N
        fontDialog.getContentPane().add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(190, 10, -1, -1));

        fontStyleTextField.setBackground(resourceMap.getColor("fontStyleTextField.background")); // NOI18N
        fontStyleTextField.setName("fontStyleTextField"); // NOI18N
        fontDialog.getContentPane().add(fontStyleTextField, new org.netbeans.lib.awtextra.AbsoluteConstraints(190, 30, 80, -1));

        jLabel3.setText(resourceMap.getString("jLabel3.text")); // NOI18N
        jLabel3.setName("jLabel3"); // NOI18N
        fontDialog.getContentPane().add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(290, 10, -1, -1));

        fontSizeTextField.setBackground(resourceMap.getColor("fontSizeTextField.background")); // NOI18N
        fontSizeTextField.setText(resourceMap.getString("fontSizeTextField.text")); // NOI18N
        fontSizeTextField.setName("fontSizeTextField"); // NOI18N
        fontDialog.getContentPane().add(fontSizeTextField, new org.netbeans.lib.awtextra.AbsoluteConstraints(290, 30, 60, -1));

        fontConfirmButton.setAction(actionMap.get("setFontAction")); // NOI18N
        fontConfirmButton.setText(resourceMap.getString("fontConfirmButton.text")); // NOI18N
        fontConfirmButton.setName("fontConfirmButton"); // NOI18N
        fontDialog.getContentPane().add(fontConfirmButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(360, 10, -1, -1));

        fontCancelButton.setText(resourceMap.getString("fontCancelButton.text")); // NOI18N
        fontCancelButton.setName("fontCancelButton"); // NOI18N
        fontCancelButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                fontCancelButtonMouseClicked(evt);
            }
        });
        fontDialog.getContentPane().add(fontCancelButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(360, 40, -1, -1));

        previewTextField.setBackground(resourceMap.getColor("previewTextField.background")); // NOI18N
        previewTextField.setEditable(false);
        previewTextField.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        previewTextField.setText(resourceMap.getString("previewTextField.text")); // NOI18N
        previewTextField.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        previewTextField.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        previewTextField.setEnabled(false);
        previewTextField.setFocusable(false);
        previewTextField.setName("previewTextField"); // NOI18N
        fontDialog.getContentPane().add(previewTextField, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 210, 330, 70));

        jScrollPane2.setBorder(null);
        jScrollPane2.setName("jScrollPane2"); // NOI18N

        fontNameList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        fontNameList.setName("fontNameList"); // NOI18N

        org.jdesktop.beansbinding.Binding binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, fontNameTextField, org.jdesktop.beansbinding.ELProperty.create("${text}"), fontNameList, org.jdesktop.beansbinding.BeanProperty.create("selectedElement"));
        bindingGroup.addBinding(binding);

        fontNameList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                fontNameListValueChanged(evt);
            }
        });
        jScrollPane2.setViewportView(fontNameList);

        fontDialog.getContentPane().add(jScrollPane2, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 60, 170, -1));

        jScrollPane3.setBorder(null);
        jScrollPane3.setName("jScrollPane3"); // NOI18N

        fontStyleList.setModel(new javax.swing.AbstractListModel() {
            String[] strings = { "常规", "斜体", "粗体", "粗斜体" };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        fontStyleList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        fontStyleList.setName("fontStyleList"); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, fontStyleTextField, org.jdesktop.beansbinding.ELProperty.create("${text}"), fontStyleList, org.jdesktop.beansbinding.BeanProperty.create("selectedElement"));
        bindingGroup.addBinding(binding);

        fontStyleList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                fontStyleListValueChanged(evt);
            }
        });
        jScrollPane3.setViewportView(fontStyleList);

        fontDialog.getContentPane().add(jScrollPane3, new org.netbeans.lib.awtextra.AbsoluteConstraints(190, 60, 80, 130));

        jScrollPane4.setBorder(null);
        jScrollPane4.setName("jScrollPane4"); // NOI18N

        fontSizeList.setName("fontSizeList"); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, fontSizeTextField, org.jdesktop.beansbinding.ELProperty.create("${text}"), fontSizeList, org.jdesktop.beansbinding.BeanProperty.create("selectedElement"));
        bindingGroup.addBinding(binding);

        fontSizeList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                fontSizeListValueChanged(evt);
            }
        });
        jScrollPane4.setViewportView(fontSizeList);

        fontDialog.getContentPane().add(jScrollPane4, new org.netbeans.lib.awtextra.AbsoluteConstraints(290, 60, 60, 130));

        toWhere.setTitle(resourceMap.getString("toWhere.title")); // NOI18N
        toWhere.setModal(true);
        toWhere.setName("toWhere"); // NOI18N
        toWhere.setResizable(false);
        toWhere.getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        rowTextField.setBackground(resourceMap.getColor("rowTextField.background")); // NOI18N
        rowTextField.setText(resourceMap.getString("rowTextField.text")); // NOI18N
        rowTextField.setToolTipText(resourceMap.getString("rowTextField.toolTipText")); // NOI18N
        rowTextField.setAutoscrolls(false);
        rowTextField.setBorder(javax.swing.BorderFactory.createLineBorder(resourceMap.getColor("rowTextField.border.lineColor"), 2)); // NOI18N
        rowTextField.setDropMode(javax.swing.DropMode.INSERT);
        rowTextField.setName("rowTextField"); // NOI18N
        toWhere.getContentPane().add(rowTextField, new org.netbeans.lib.awtextra.AbsoluteConstraints(38, 10, 110, -1));

        jLabel4.setText(resourceMap.getString("jLabel4.text")); // NOI18N
        jLabel4.setName("jLabel4"); // NOI18N
        toWhere.getContentPane().add(jLabel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 13, -1, -1));

        toWhereConfirm.setText(resourceMap.getString("toWhereConfirm.text")); // NOI18N
        toWhereConfirm.setName("toWhereConfirm"); // NOI18N
        toWhereConfirm.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                toWhereConfirmMouseClicked(evt);
            }
        });
        toWhere.getContentPane().add(toWhereConfirm, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 50, -1, -1));

        toWhereCancel.setText(resourceMap.getString("toWhereCancel.text")); // NOI18N
        toWhereCancel.setName("toWhereCancel"); // NOI18N
        toWhereCancel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                toWhereCancelMouseClicked(evt);
            }
        });
        toWhere.getContentPane().add(toWhereCancel, new org.netbeans.lib.awtextra.AbsoluteConstraints(130, 50, -1, -1));

        setComponent(mainPanel);
        setMenuBar(menuBar);

        bindingGroup.bind();
    }// </editor-fold>//GEN-END:initComponents

    private void statusMenuItemItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_statusMenuItemItemStateChanged
        if (statusPanel.isVisible() && !statusMenuItem.isSelected()) {
            statusPanel.setVisible(false);
        } else {
            statusPanel.setVisible(true);
        }
    }//GEN-LAST:event_statusMenuItemItemStateChanged

    private void windowsLookItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_windowsLookItemStateChanged
        if (windowsLook.isSelected()) {
            currentLook = windows;
            try {
                UIManager.setLookAndFeel(currentLook);
                SwingUtilities.updateComponentTreeUI(getFrame());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, ex.getMessage());
                Logger.getLogger(NotePadView.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }//GEN-LAST:event_windowsLookItemStateChanged

    private void javaLookItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_javaLookItemStateChanged
        if (javaLook.isSelected()) {
            currentLook = metal;
            try {
                UIManager.setLookAndFeel(currentLook);
                SwingUtilities.updateComponentTreeUI(getFrame());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, ex.getMessage());
                Logger.getLogger(NotePadView.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }//GEN-LAST:event_javaLookItemStateChanged

    private void motifLookItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_motifLookItemStateChanged
        if (motifLook.isSelected()) {
            try {
                currentLook = motif;
                UIManager.setLookAndFeel(currentLook);
                SwingUtilities.updateComponentTreeUI(getFrame());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, ex.getMessage());
                Logger.getLogger(NotePadView.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }//GEN-LAST:event_motifLookItemStateChanged

    private void editorPaneCaretUpdate(javax.swing.event.CaretEvent evt) {//GEN-FIRST:event_editorPaneCaretUpdate
        Point point = getDot();
        statusMessageLabel.setText("行：" + (int) point.getX() + " 列：" + (int) point.getY());
    }//GEN-LAST:event_editorPaneCaretUpdate

    private void findButtonMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_findButtonMouseClicked
        findIndex = editorPane.getCaretPosition();
        if (findText()) {
            Object o = findBox.getSelectedItem();
            String findText = o == null ? "" : o.toString();
            if (findText.equals("")) {
                Toolkit.getDefaultToolkit().beep();
                return;
            }
            findModel.removeElement(findText);
            findModel.addElement(findText);
            int selected = JOptionPane.showConfirmDialog(dialog, "找不到：" + findText + "\t是否从头开始查找？");
            if (selected == JOptionPane.YES_OPTION) {
                findIndex = 0;
                findText();
            }
        }
    }//GEN-LAST:event_findButtonMouseClicked

    private void replaceButtonMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_replaceButtonMouseClicked
        Object o = replaceBox.getSelectedItem();
        String replaceText = o == null ? "" : o.toString();
        findIndex = editorPane.getCaretPosition();
        if (editorPane.getSelectedText() == null) {
            if (findText()) {
                return;
            }
        }
        editorPane.replaceSelection(replaceText);
        if (!replaceText.equals("")) {
            replaceModel.removeElement(replaceText);
            replaceModel.addElement(replaceText);
            replaceBox.setSelectedItem(replaceText);
        }
        findText();
    }//GEN-LAST:event_replaceButtonMouseClicked

    private void replaceAllButtonMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_replaceAllButtonMouseClicked
        Object o = replaceBox.getSelectedItem();
        String replaceText = o == null ? "" : o.toString();
        Object oo = findBox.getSelectedItem();
        String findText = oo == null ? "" : oo.toString();
        if (findText.equals("")) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }
        findModel.removeElement(findText);
        findModel.addElement(findText);
        if (!replaceText.equals("")) {
            replaceModel.removeElement(replaceText);
            replaceModel.addElement(replaceText);
        }
        editorPane.setText(editorPane.getText().replaceAll(findText, replaceText));
        statusAnimationLabel.setText("完成全部替换！");
    }//GEN-LAST:event_replaceAllButtonMouseClicked

    private void fontCancelButtonMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fontCancelButtonMouseClicked
        fontDialog.setVisible(false);
    }//GEN-LAST:event_fontCancelButtonMouseClicked

    private void fontNameListValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_fontNameListValueChanged
        if (fontDialog.isVisible()) {
            Font f = getSelectFont();
            int code = f.getName().codePointAt(0);
            if ((code < 90 && code > 65) || (code > 97 && code < 122)) {
                previewTextField.setText("AaBbYyZz");
            } else {
                previewTextField.setText("披着羊皮的狼");
            }
            previewTextField.setFont(f);
        }
    }//GEN-LAST:event_fontNameListValueChanged

    private void fontStyleListValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_fontStyleListValueChanged
        fontNameListValueChanged(evt);
    }//GEN-LAST:event_fontStyleListValueChanged

    private void fontSizeListValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_fontSizeListValueChanged
        fontNameListValueChanged(evt);
    }//GEN-LAST:event_fontSizeListValueChanged

    private void autoEnterMenuItemItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_autoEnterMenuItemItemStateChanged
        //TODO AutoLine
    }//GEN-LAST:event_autoEnterMenuItemItemStateChanged

    private void toWhereCancelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_toWhereCancelMouseClicked
        toWhere.setVisible(false);
    }//GEN-LAST:event_toWhereCancelMouseClicked

    private void toWhereConfirmMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_toWhereConfirmMouseClicked
        String rows = rowTextField.getText();
        if (rows == null || "".equals(rows)) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }
        int row = Integer.parseInt(rows);
        String[] text = editorPane.getText().split("\n");
        int totalRows = text.length;
        if (row > totalRows) {
            Toolkit.getDefaultToolkit().beep();
        } else {
            int caret = 0;
            for (int i = 0; i < row - 1; i++) {
                caret += text[i].length() + 1;
            }
            editorPane.setCaretPosition(caret);
            editorPane.requestFocus();
            toWhere.setVisible(false);
        }
    }//GEN-LAST:event_toWhereConfirmMouseClicked

    private void changeEncoding(String textType) {
//        try {
        //editorPane.setContentType("text/html;charset=" + textType);
//            String text = editorPane.getText();
//            String temp = new String(text.getBytes(getEncoding(text)), textType);
//            editorPane.setText(temp);
//        } catch (UnsupportedEncodingException ex) {
//            JOptionPane.showMessageDialog(null, ex.getMessage());
//            Logger.getLogger(NotePadView.class.getName()).log(Level.SEVERE, null, ex);
//        }
    }

    private String getEncoding(String str) {
        String encoding = "gb2312";
        try {
            if (str.equals(new String(str.getBytes(encoding), encoding))) {
                return encoding;
            }
            encoding = "utf-8";
            if (str.equals(new String(str.getBytes(encoding), encoding))) {
                return encoding;
            }
            encoding = "ISO-8859-1";
            if (str.equals(new String(str.getBytes(encoding), encoding))) {
                return encoding;
            }
        } catch (UnsupportedEncodingException ex) {
            JOptionPane.showMessageDialog(null, ex.getMessage());
            Logger.getLogger(NotePadView.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    private void encodingGBKItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_encodingGBKItemStateChanged
        if (encodingGBK.isSelected()) {
            changeEncoding("gb2312");
        }
    }//GEN-LAST:event_encodingGBKItemStateChanged

    private void encodingUTF8ItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_encodingUTF8ItemStateChanged
        if (encodingUTF8.isSelected()) {
            changeEncoding("utf-8");
        }
    }//GEN-LAST:event_encodingUTF8ItemStateChanged

    private void encodingISOItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_encodingISOItemStateChanged
        if (encodingISO.isSelected()) {
            changeEncoding("ISO-8859-1");
        }
    }//GEN-LAST:event_encodingISOItemStateChanged
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBoxMenuItem autoEnterMenuItem;
    private javax.swing.JButton closeButton;
    private javax.swing.JMenuItem copyMenuItem;
    private javax.swing.JMenuItem cutMenuItem;
    private javax.swing.JMenuItem dateMenuItem;
    private javax.swing.JMenuItem deleteMenuItem;
    private javax.swing.JDialog dialog;
    private javax.swing.JMenu editMenu;
    private javax.swing.JEditorPane editorPane;
    private javax.swing.JRadioButtonMenuItem encodingGBK;
    private javax.swing.JRadioButtonMenuItem encodingISO;
    private javax.swing.JMenu encodingMenu;
    private javax.swing.JRadioButtonMenuItem encodingUTF8;
    private javax.swing.JMenuItem exitMenuItem;
    private javax.swing.JComboBox findBox;
    private javax.swing.JButton findButton;
    private javax.swing.JLabel findLabel;
    private javax.swing.JMenuItem findMenuItem;
    private javax.swing.JButton fontCancelButton;
    private javax.swing.JButton fontConfirmButton;
    private javax.swing.JDialog fontDialog;
    private javax.swing.JMenuItem fontMenuItem;
    private javax.swing.JList fontNameList;
    private javax.swing.JTextField fontNameTextField;
    private javax.swing.JList fontSizeList;
    private javax.swing.JTextField fontSizeTextField;
    private javax.swing.JList fontStyleList;
    private javax.swing.JTextField fontStyleTextField;
    private javax.swing.JMenuItem gotoMenuItem;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JPopupMenu.Separator jSeparator2;
    private javax.swing.JPopupMenu.Separator jSeparator3;
    private javax.swing.JPopupMenu.Separator jSeparator4;
    private javax.swing.JPopupMenu.Separator jSeparator5;
    private javax.swing.JPopupMenu.Separator jSeparator6;
    private javax.swing.JPopupMenu.Separator jSeparator7;
    private javax.swing.JPopupMenu.Separator jSeparator8;
    private javax.swing.JRadioButtonMenuItem javaLook;
    private javax.swing.JMenu lookAndViewMenu;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JRadioButtonMenuItem motifLook;
    private javax.swing.JMenuItem moveDownMenuItem;
    private javax.swing.JMenuItem moveUpMenuItem;
    private javax.swing.JMenuItem newMenuItem;
    private javax.swing.JMenuItem nextMenuItem;
    private javax.swing.JMenuItem openMenuItem;
    private javax.swing.JMenuItem pasteMenuItem;
    private javax.swing.JMenuItem popCopy;
    private javax.swing.JMenuItem popCut;
    private javax.swing.JMenuItem popDel;
    private javax.swing.JPopupMenu popMenu;
    private javax.swing.JMenuItem popPaste;
    private javax.swing.JMenuItem popRedo;
    private javax.swing.JMenuItem popSelectAll;
    private javax.swing.JMenuItem popUndo;
    private javax.swing.JTextField previewTextField;
    private javax.swing.JMenuItem printMenuItem;
    private javax.swing.JButton replaceAllButton;
    private javax.swing.JComboBox replaceBox;
    private javax.swing.JButton replaceButton;
    private javax.swing.JLabel replaceLabel;
    private javax.swing.JMenuItem replaceMenuItem;
    private javax.swing.JFormattedTextField rowTextField;
    private javax.swing.JMenuItem saveAsMenuItem;
    private javax.swing.JMenuItem saveMenuItem;
    private javax.swing.JMenuItem selectAllMenuItem;
    private javax.swing.JMenuItem settingMenuItem;
    private javax.swing.JLabel statusAnimationLabel;
    private javax.swing.JRadioButtonMenuItem statusMenuItem;
    private javax.swing.JLabel statusMessageLabel;
    private javax.swing.JPanel statusPanel;
    private javax.swing.JMenu styleMenu;
    private javax.swing.JDialog toWhere;
    private javax.swing.JButton toWhereCancel;
    private javax.swing.JButton toWhereConfirm;
    private javax.swing.JMenuItem undoMenuItem;
    private javax.swing.JMenu viewMenu;
    private javax.swing.JRadioButtonMenuItem windowsLook;
    private org.jdesktop.beansbinding.BindingGroup bindingGroup;
    // End of variables declaration//GEN-END:variables
    private final Timer messageTimer;
    private final Timer busyIconTimer;
    private final Icon idleIcon;
    private final Icon[] busyIcons = new Icon[15];
    private int busyIconIndex = 0;
    private JDialog aboutBox;
}
