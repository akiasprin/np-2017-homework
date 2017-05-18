package view;

import handlers.ICMPScanner;
import handlers.SSHScanner;
import handlers.TCPScanner;
import services.PortScannerPDD;
import services.PortScannerPolicies;
import utils.FileUtils;
import utils.IPUtils;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class MainGUI {

	static private MainGUI self;
	static private String[] dict;
	static private int tcp_isRunning = -1;
	static private int icmp_isRunning = -1;
	static private int ssh_isRunning = -1;
	static private Thread tcp_worker;
	static private Thread icmp_worker;
	static private Thread ssh_worker;
	static private Map tcp_ip_resp_map = new HashMap<String, String>();
	static private Map tcp_ip_row_resp_map = new HashMap<String, Integer>();
	static private Map tcp_ip_tn_map = new HashMap<String, DefaultMutableTreeNode>();
	static private Map icmp_ip_row_hostname_mac_map = new HashMap<String, Integer>();
	private JPanel base_panel;
	private JPanel tcp_info_panel;
	private JPanel tcp_ctrl_panel;
	private JButton tcp_start_btn;
	private JPanel tcp_result_panel;
	private JTable tcp_result_table;
	private JScrollPane tcp_result_scrollpane;
	private JTextField tcp_port_start_text;
	private JTextField tcp_port_stop_text;
	private JTextField tcp_ip_start_text;
	private JTextField tcp_ip_stop_text;
	private JTextPane tcp_status_text;
	private JProgressBar tcp_progressBar;
	private JPanel tcp_progress_panel;
	private JTabbedPane tcp_tabbed_panel;
	private JTree tcp_analyse_tree;
	private JPanel tcp_analyse_panel;
	private JScrollPane tcp_analyse_scrollpane;
	private JPanel icmp_panel;
	private JPanel tcp_panel;
	private JTextField icmp_ip_start;
	private JTextField icmp_ip_stop;
	private JTabbedPane icmp_tabbed_panel;
	private JTable icmp_result_table;
	private JScrollPane icmp_result_scrollpane;
	private JPanel icmp_result_panel;
	private JPanel icmp_progress_panel;
	private JProgressBar icmp_progressBar;
	private JPanel icmp_ctrl_panel;
	private JButton icmp_start_btn;
	private JTextPane icmp_status_text;
	private JPanel ssh_panel;
	private JTextField ssh_port_text;
	private JPanel ssh_info_panel;
	private JTextField ssh_ip_text;
	private JTable ssh_dict_table;
	private JTextPane ssh_status_text;
	private JProgressBar ssh_progressBar;
	private JButton ssh_start_btn;
	private JTable ssh_result_table;
	private DefaultTableModel icmp_result_dtm;
	private DefaultTableModel tcp_result_dtm;
	private DefaultTableModel ssh_result_dtm;
	private DefaultTableModel ssh_dict_dtm;
	private DefaultTreeModel tcp_analyse_dtm;
	private DefaultMutableTreeNode tcp_root;
	private DefaultMutableTreeNode tcp_fakeroot = new DefaultMutableTreeNode("myChenJiaoJiao");

	private int tcp_port_start = 0;
	private int tcp_port_stop = 0;

	public static class TCPTab {

		public static void addResult(String ip, int port) {
			int postion = self.tcp_result_dtm.getRowCount();
			String kv = ip + ":" + port;
			String portocol = (String) PortScannerPDD.getInstance().db.get(port);
			if (portocol == null) {
				portocol = "";
			}
			self.tcp_result_dtm.addRow(new Object[]{postion + 1, kv, portocol});
			tcp_ip_row_resp_map.put(kv, postion);
			DefaultMutableTreeNode ip_tn = (DefaultMutableTreeNode) tcp_ip_tn_map.get(ip);
			if (ip_tn == null) {
				ip_tn = new DefaultMutableTreeNode(ip);
				tcp_ip_tn_map.put(ip, ip_tn);
				self.tcp_analyse_dtm.insertNodeInto(ip_tn, self.tcp_root, self.tcp_analyse_dtm.getChildCount(self.tcp_root));
				self.tcp_analyse_dtm.insertNodeInto(new DefaultMutableTreeNode("Ports"), ip_tn, 0);
			}
			String desc = (String) PortScannerPDD.getInstance().db.get(port);
			if (desc == null) {
				desc = "???";
			}
			DefaultMutableTreeNode port_tn = new DefaultMutableTreeNode(port +
					" | " + desc);
			self.tcp_analyse_dtm.insertNodeInto(port_tn, (DefaultMutableTreeNode) ip_tn.getChildAt(0), 0);
		}

		public static void setStatus(String status) {
			self.tcp_status_text.setText(status);
		}

		public static void setResp(String ip, int port, String v) {
			String k = ip + ":" + port;
			tcp_ip_resp_map.put(k, v);
			String[] policy = (String[]) PortScannerPolicies.getInstance().
					db.get(port);
			if (policy != null) {
				Pattern pattern = Pattern.compile(policy[1], Pattern.MULTILINE);
				Matcher matcher = pattern.matcher(v);
				if (matcher.find())
					v = "*" + matcher.group(0);
				else v = "#" + v;
			} else v = '#' + v;
			self.tcp_result_dtm.setValueAt(v, (int) tcp_ip_row_resp_map.get(k), 3);
		}

		public static void setProgressBar(int n) {
			self.tcp_progressBar.setValue(n);
		}

		public static void setDistrbutedFinish() {
			tcp_isRunning = 2;
			self.tcp_start_btn.setText("Stopp");
		}

		public static void setTasksFinish() {
			tcp_isRunning = -1;
			self.tcp_start_btn.setText("Start");
			controlBtn(true);
		}

		public static void controlBtn(boolean status) {
			self.tcp_ip_start_text.setEnabled(status);
			self.tcp_ip_stop_text.setEnabled(status);
			self.tcp_port_start_text.setEnabled(status);
			self.tcp_port_stop_text.setEnabled(status);
		}

	}

	public static class ICMPTab {

		public static void addResult(String ip) {
			int postion = self.icmp_result_dtm.getRowCount();
			self.icmp_result_dtm.addRow(new Object[]{postion + 1, ip});
			icmp_ip_row_hostname_mac_map.put(ip, postion);
		}

		public static void setHostname(String ip, String v) {
			self.icmp_result_dtm.setValueAt(v, (int) icmp_ip_row_hostname_mac_map.get(ip), 2);
		}

		public static void setMAC(String ip, String v) {
			self.icmp_result_dtm.setValueAt(v, (int) icmp_ip_row_hostname_mac_map.get(ip), 3);
		}

		public static void setStatus(String status) {
			self.icmp_status_text.setText(status);
		}

		public static void setProgressBar(int n) {
			self.icmp_progressBar.setValue(n);
		}

		public static void setDistrbutedFinish() {
			icmp_isRunning = 2;
			self.icmp_start_btn.setText("Stopp");
		}

		public static void setTasksFinish() {
			icmp_isRunning = -1;
			self.icmp_start_btn.setText("Start");
			controlBtn(true);
		}

		public static void controlBtn(boolean status) {
			self.icmp_ip_stop.setEnabled(status);
			self.icmp_ip_start.setEnabled(status);
		}

	}

	public static class SSHTab {

		public static void addResult(String ip, String port, String username, String password) {
			int postion = self.ssh_result_dtm.getRowCount();
			self.ssh_result_dtm.addRow(new Object[]{postion + 1, ip, port, username, password});
		}

		public static void setStatus(String status) {
			self.ssh_status_text.setText(status);
		}

		public static void setProgressBar(int n) {
			self.ssh_progressBar.setValue(n);
		}

		public static void setDistrbutedFinish() {
			ssh_isRunning = 2;
			self.ssh_start_btn.setText("Stopp");
		}

		public static void setTasksFinish() {
			ssh_isRunning = -1;
			self.ssh_start_btn.setText("Start");
			controlBtn(true);
		}

		public static void controlBtn(boolean status) {
			self.ssh_ip_text.setEnabled(status);
			self.ssh_port_text.setEnabled(status);
		}

	}

	MainGUI() {
		self = this;

		icmp_start_btn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {

				if (icmp_isRunning == -1) {
					icmp_worker = new Thread(new Runnable() {
						public void run() {
							ICMPScanner.createTask(icmp_ip_start.getText(), icmp_ip_stop.getText());
						}
					});
					icmp_worker.start();
					icmp_isRunning = 1;
					icmp_start_btn.setText("Pause");
					ICMPTab.controlBtn(false);
				} else if (icmp_isRunning == 1) {
					icmp_isRunning = 0;
					ICMPScanner.suspendService();
					icmp_start_btn.setText("Start");
				} else if (icmp_isRunning == 0) {
					icmp_isRunning = 1;
					ICMPScanner.suspendService();
					icmp_start_btn.setText("Pause");
					ICMPTab.controlBtn(false);
				} else {
					icmp_isRunning = -1;
					ICMPScanner.stopAndCleanService();
					icmp_worker.interrupt();
					icmp_start_btn.setText("Start");
					ICMPTab.controlBtn(true);
				}

			}
		});


		tcp_analyse_dtm = new DefaultTreeModel(tcp_fakeroot);
		tcp_analyse_tree.setModel(tcp_analyse_dtm);

		tcp_start_btn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (tcp_isRunning == -1) {
					tcp_port_start = Math.max(0, Math.min(65535, Integer.parseInt(tcp_port_start_text.getText())));
					tcp_port_stop = Math.max(0, Math.min(65535, Integer.parseInt(tcp_port_stop_text.getText())));
					tcp_worker = new Thread(new Runnable() {
						public void run() {
							TCPScanner.createTask(tcp_ip_start_text.getText(), tcp_ip_stop_text.getText(),
									tcp_port_start, tcp_port_stop);
						}
					});

					tcp_root = new DefaultMutableTreeNode(
							"Task:" + tcp_ip_start_text.getText() + " - " + tcp_ip_stop_text.getText());
					tcp_analyse_dtm.insertNodeInto(tcp_root, tcp_fakeroot, 0);
					tcp_analyse_tree.expandRow(0);
					tcp_worker.start();
					tcp_isRunning = 1;
					tcp_start_btn.setText("Pause");
					TCPTab.controlBtn(false);
				} else if (tcp_isRunning == 1) {
					tcp_isRunning = 0;
					TCPScanner.suspendService();
					tcp_start_btn.setText("Start");
					System.out.println(tcp_worker.getState());
				} else if (tcp_isRunning == 0) {
					tcp_isRunning = 1;
					TCPScanner.suspendService();
					tcp_start_btn.setText("Pause");
					System.out.println(tcp_worker.getState());
					TCPTab.controlBtn(false);
				} else {
					tcp_isRunning = -1;
					TCPScanner.stopAndCleanService();
					tcp_worker.interrupt();
					tcp_start_btn.setText("Start");
					System.out.println(tcp_worker.getState());
					TCPTab.controlBtn(true);
				}
			}
		});


		ssh_start_btn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {

				if (ssh_isRunning == -1) {
					ssh_worker = new Thread(new Runnable() {
						public void run() {
							SSHScanner.setDict(dict);
							SSHScanner.createTask(ssh_ip_text.getText(), Integer.parseInt(ssh_port_text.getText()), "root");
						}
					});
					ssh_worker.start();
					ssh_isRunning = 1;
					ssh_start_btn.setText("Pause");
					SSHTab.controlBtn(false);
				} else if (ssh_isRunning == 1) {
					ssh_isRunning = 0;
					SSHScanner.suspendService();
					ssh_start_btn.setText("Start");
				} else if (ssh_isRunning == 0) {
					ssh_isRunning = 1;
					SSHScanner.suspendService();
					ssh_start_btn.setText("Pause");
					SSHTab.controlBtn(false);
				} else {
					ssh_isRunning = -1;
					SSHScanner.stopAndCleanService();
					ssh_worker.interrupt();
					ssh_start_btn.setText("Start");
					SSHTab.controlBtn(true);
				}
			}
		});

		tcp_analyse_tree.addTreeSelectionListener(new TreeSelectionListener() {
			@Override
			public void valueChanged(TreeSelectionEvent e) {
				//System.out.println(e.getPath().getParentPath().getLastPathComponent());
				try {
					if (e.getPath().getParentPath().getLastPathComponent().toString().equals("Ports")) {
						String _ip = e.getPath().getParentPath().getParentPath().getLastPathComponent().toString();
						String _port = e.getPath().getLastPathComponent().toString().split(" ")[0];
						InfoGUI.show((String) tcp_ip_resp_map.get(_ip + ":" + _port));
					}
				} catch (Exception err) {
				}
			}
		});

		String[] tcp_column_names = {"No.", "Addr.", "Prot.", "Resp."};
		tcp_result_dtm = new DefaultTableModel(tcp_column_names, 0);
		tcp_result_table.setModel(tcp_result_dtm);
		tcp_result_table.addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e) {
				tcp_result_table.changeSelection(tcp_result_table.getRowCount() - 1, 0, false, false);
			}
		});

		String[] icmp_column_names = {"No.", "Addr.", "Host.", "MAC"};
		icmp_result_dtm = new DefaultTableModel(icmp_column_names, 0);
		icmp_result_table.setModel(icmp_result_dtm);
		icmp_result_table.addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e) {
				icmp_result_table.changeSelection(icmp_result_table.getRowCount() - 1, 0, false, false);
			}
		});

		//Fuck
		tcp_result_table.getColumnModel().getColumn(0).setPreferredWidth(40);
		tcp_result_table.getColumnModel().getColumn(1).setPreferredWidth(120);
		tcp_result_table.getColumnModel().getColumn(2).setPreferredWidth(60);
		tcp_result_table.getColumnModel().getColumn(3).setPreferredWidth(200);


		icmp_result_table.getColumnModel().getColumn(0).setPreferredWidth(40);
		icmp_result_table.getColumnModel().getColumn(1).setPreferredWidth(120);
		icmp_result_table.getColumnModel().getColumn(2).setPreferredWidth(120);
		icmp_result_table.getColumnModel().getColumn(3).setPreferredWidth(120);


		String[] ssh_dict_column_names = {"No.", "Pwd."};
		ssh_dict_dtm = new DefaultTableModel(ssh_dict_column_names, 0);
		ssh_dict_table.setModel(ssh_dict_dtm);
		for (int i = 0; i < dict.length; i++) {
			ssh_dict_dtm.addRow(new String[]{ String.valueOf(i + 1), dict[i] });
		}
		ssh_dict_table.getColumnModel().getColumn(0).setPreferredWidth(10);
		ssh_dict_table.getColumnModel().getColumn(1).setPreferredWidth(240);

		String[] ssh_column_names = {"No.", "Addr.", "Port", "User", "Pwd."};
		ssh_result_dtm = new DefaultTableModel(ssh_column_names, 0);
		ssh_result_table.setModel(ssh_result_dtm);
		ssh_result_table.getColumnModel().getColumn(0).setPreferredWidth(40);
		ssh_result_table.getColumnModel().getColumn(1).setPreferredWidth(120);
		ssh_result_table.getColumnModel().getColumn(2).setPreferredWidth(120);
		ssh_result_table.getColumnModel().getColumn(3).setPreferredWidth(120);
		ssh_result_table.getColumnModel().getColumn(4).setPreferredWidth(120);

	}

	public static void main(String[] args) {
		dict = FileUtils.Reader.read("dict/dict.txt").split("\n");
		System.out.println(dict.length);
		JFrame frame = new JFrame("瓜皮端口扫描程序");
		frame.setContentPane(new MainGUI().base_panel);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
	}

}
