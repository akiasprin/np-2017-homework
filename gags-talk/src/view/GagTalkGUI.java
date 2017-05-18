package view;

import handlers.GagTalk;
import utils.GetNetworkInterFace;
import models.Member;
import models.Message;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import java.awt.*;
import java.awt.event.*;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GagTalkGUI {
    private JList member_list;
    private JTextPane talking_area;
    private JEditorPane editor_area;
    private JPanel main_panel;
    private JPanel content_panel;
    private JPanel member_panel;
    private JTextField name_field;
    private JButton login_btn;
    private JButton post_btn;
    private JLabel info_field;
    private JPanel info_panel;
    private JPanel login_panel;
    private JComboBox if_combo;
    private JPanel name_panel;
    private GagTalk gagTalk;
    private DefaultListModel dlm;
    private Boolean isLogin;
    private static Date lastMessageDate = null;
    private static String lastMessageName = null;
    public static GagTalkGUI self;
    private static SimpleAttributeSet date_attrset = new SimpleAttributeSet();
    private static SimpleAttributeSet name_attrset = new SimpleAttributeSet();
    private static SimpleAttributeSet name_self_attrset = new SimpleAttributeSet();
    private static SimpleAttributeSet content_attrset = new SimpleAttributeSet();

    static {
        StyleConstants.setFontSize(date_attrset,13);
        StyleConstants.setForeground(date_attrset, Color.GRAY);
        StyleConstants.setFontSize(name_attrset,13);
        StyleConstants.setForeground(name_attrset, Color.MAGENTA);
        StyleConstants.setFontSize(name_self_attrset,13);
        StyleConstants.setForeground(name_self_attrset, Color.BLUE);
        StyleConstants.setFontSize(content_attrset,13);
    }

    public void setContent(Date date, String name, String content) {
        //talking_area(content + "\n");
        Document docs = talking_area.getDocument();
        try {
            if (lastMessageDate == null ||
                    lastMessageName == null ||
                    date.getTime() - lastMessageDate.getTime() >= 5000 || !name.equals(lastMessageName) ||
                    name.equals("系统消息")) {
                 docs.insertString(docs.getLength(), Message.getFormatDate(date) + "\n", date_attrset);//对文本进行追加
                if (name.equals(Member.localhost.getName())){
                    docs.insertString(docs.getLength(), "[" + "我" + "]: ", name_self_attrset);//对文本进行追加
                } else {
                    docs.insertString(docs.getLength(), "[" + name + "]: ", name_attrset);//对文本进行追加
                }
            }
            docs.insertString(docs.getLength(), content + "\n", content_attrset);//对文本进行追加
            lastMessageDate = date;
            lastMessageName = name;
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    public void addNetIF(String name) {
        if_combo.addItem(name);
    }
    public void addMember(Member member) {
        dlm.add(dlm.size(), member.getName());
    }

    public void removeMember(Member member) {
        dlm.removeElement(member.getName());
    }

    public GagTalkGUI() {
        self = this;
        isLogin = false;
        post_btn.setEnabled(false);
        dlm = new DefaultListModel();
        member_list.setModel(dlm);
        new GetNetworkInterFace();
        login_btn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!isLogin) {
                    gagTalk = new GagTalk(name_field.getText(), (String)if_combo.getSelectedItem());
                    gagTalk.isStop = false;
                    gagTalk.loginGagKing();
                    gagTalk.get();
                    name_field.setEnabled(false);
                    if_combo.setEnabled(false);
                    post_btn.setEnabled(true);
                    login_btn.setText("注销");
                    isLogin = true;
                } else {
                    gagTalk.logoffGagKing();
                    gagTalk.isStop = true;
                    name_field.setEnabled(true);
                    if_combo.setEnabled(true);
                    post_btn.setEnabled(false);
                    login_btn.setText("登录");
                    isLogin = false;
                    dlm.clear();
                }
            }
        });

        talking_area.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                talking_area.setCaretPosition(talking_area.getText().length());
            }
        });

        post_btn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String content = editor_area.getText();
                Pattern pattern = Pattern.compile("^@\\S+");
                Matcher matcher = pattern.matcher(content);
                if (matcher.find()) {
                    gagTalk.postPersonalGag(content, matcher.group().substring(1));
                } else {
                    gagTalk.postMulticastGag(content);
                }
                editor_area.setText("");
            }
        });

        member_list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if (e.getClickCount() == 2) {
                    int index = member_list.locationToIndex(e.getPoint());
                    if (index >= 0) {
                        Object o = member_list.getModel().getElementAt(index);
                        editor_area.setText("@" + o.toString() + " ");
                    }
                }
            }
        });
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("局域网组播讲GAG");
        frame.setContentPane(new GagTalkGUI().main_panel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

}
