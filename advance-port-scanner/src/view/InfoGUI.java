package view;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class InfoGUI {
	private static InfoGUI self;
	private static JFrame frame;
	private JTextPane info_pane;
	private JPanel base_panel;
	private JButton ctrl_btn;
	private JPanel ctrl_panel;

	InfoGUI() {
		self = this;
		ctrl_btn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				frame.dispose();
			}
		});
	}

	public static void show(String str) {
		frame = new JFrame("myChenJiaoJiao");
		frame.setContentPane(new InfoGUI().base_panel);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
		self.info_pane.setText(str);
	}

	public static void main(String[] args) {
		show("Test");
	}
}
