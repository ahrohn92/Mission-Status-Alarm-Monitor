import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import com.toedter.calendar.JCalendar;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/*
 * File: 		GUI.java
 * Author: 		Andrew H. Rohn
 * Date: 		16 November 2019
 * Purpose: 	This class constructs the GUI for the program and contains the main method.
 * 				There are also action listeners for the buttons and error handling.
 */

public class GUI extends JFrame {

	// GUI Components
	private JLabel primeLabel = new JLabel("Are we prime?");
	private JLabel statusLabel = new JLabel("Status:");
	private JLabel statusText = new JLabel("");
	private JLabel packetLossLabel = new JLabel("Packet Loss:");
	private JLabel packetLossText = new JLabel("");
	private JLabel consistencyThresholdLabel = new JLabel("Consistency Threshold (min)");
	private JLabel primeReportTimeLabel = new JLabel("Primary Reportable Time (min)");
	private JLabel altReportTimeLabel = new JLabel("Alternate Reportable Time (min)");
	private JLabel downtimeCounter = new JLabel("");
	private JRadioButton yesRadioButton = new JRadioButton("Yes");
	private JRadioButton noRadioButton = new JRadioButton("No");
	private ButtonGroup buttonGroup = new ButtonGroup();
	private JTextField statusColor = new JTextField("",10);
	private JTextField consistencyThresholdTextField = new JTextField("5",10);
	private JTextField primeReportTimeTextField = new JTextField("15",10);
	private JTextField altReportTimeTextField = new JTextField("60",10);
	private JButton startButton = new JButton("Start");
	private JButton stopButton = new JButton("Stop");
	private JButton searchLogButton = new JButton("Search Log");
	private JButton acknowledgeButton = new JButton("Acknowledge");
	private JButton testButton = new JButton("Test");
	private JButton clearButton = new JButton("Clear");
	private JTextArea textArea = new JTextArea();
	private JScrollPane scrollPane = new JScrollPane(textArea);
	
	// Local Variables
	private boolean isPrime;
	private boolean isPrevPrime;
	private double consistencyThreshold = 0;
	private double primeReportTime = 0;
	private double altReportTime = 0;
	private boolean hasAlreadyBeenStarted = false;
	private boolean isMonitorRestart = false;
	private boolean run = false;
	private long startTime;
	private int monitorRunTime = 0;
	private static Monitor monitor;
	private Status lastStatus;
	
	// Constructor
	public GUI() {
		
		 /*
		  * JPanels for GUI Layout
		  */
		
		JPanel primePanel = new JPanel();
		primePanel.add(primeLabel);
		primePanel.add(yesRadioButton);
		primePanel.add(noRadioButton);
		buttonGroup.add(yesRadioButton);
		buttonGroup.add(noRadioButton);
		noRadioButton.setSelected(true);
		
		JPanel buttonPanel = new JPanel();
		buttonPanel.add(startButton);
		startButton.setBackground(Color.LIGHT_GRAY);
		buttonPanel.add(stopButton);
		stopButton.setBackground(Color.LIGHT_GRAY);
		buttonPanel.add(searchLogButton);
		searchLogButton.setBackground(Color.LIGHT_GRAY);
		
		JPanel upperLeftPanel = new JPanel();
		upperLeftPanel.setLayout(new BoxLayout(upperLeftPanel, BoxLayout.PAGE_AXIS));
		upperLeftPanel.add(primePanel);
		upperLeftPanel.add(buttonPanel);
		
		JPanel consistencyPanel = new JPanel();
		consistencyPanel.setLayout(new GridLayout(1,2,5,0));
		consistencyPanel.add(consistencyThresholdLabel);
		consistencyPanel.add(consistencyThresholdTextField);
		
		JPanel primeTimePanel = new JPanel();
		primeTimePanel.setLayout(new GridLayout(1,2,5,0));
		primeTimePanel.add(primeReportTimeLabel);
		primeTimePanel.add(primeReportTimeTextField);
		
		JPanel altTimePanel = new JPanel();
		altTimePanel.setLayout(new GridLayout(1,2,5,0));
		altTimePanel.add(altReportTimeLabel);
		altTimePanel.add(altReportTimeTextField);
		
		JPanel alarmButtonPanel = new JPanel();
		alarmButtonPanel.setLayout(new GridLayout(1,3,5,0));
		alarmButtonPanel.add(acknowledgeButton);
		alarmButtonPanel.add(testButton);
		alarmButtonPanel.add(clearButton);
		acknowledgeButton.setBackground(Color.LIGHT_GRAY);
		testButton.setBackground(Color.LIGHT_GRAY);
		clearButton.setBackground(Color.LIGHT_GRAY);
		
		JPanel settingsPanel = new JPanel();
		settingsPanel.setBorder(new TitledBorder("Alarm Settings"));
		settingsPanel.setLayout(new GridLayout(4,1,0,5));
		settingsPanel.add(consistencyPanel);
		settingsPanel.add(primeTimePanel);
		settingsPanel.add(altTimePanel);
		settingsPanel.add(alarmButtonPanel);

		JPanel labelPanel = new JPanel();
		labelPanel.setLayout(new GridLayout(2,1));
		labelPanel.add(statusLabel);
		labelPanel.add(packetLossLabel);
		statusLabel.setHorizontalAlignment(JLabel.RIGHT);
		packetLossLabel.setHorizontalAlignment(JLabel.RIGHT);
		
		JPanel textPanel = new JPanel();
		textPanel.setLayout(new GridLayout(2,1));
		textPanel.add(statusText);
		textPanel.add(packetLossText);
		statusText.setHorizontalAlignment(JLabel.CENTER);
		packetLossText.setHorizontalAlignment(JLabel.CENTER);
		
		JPanel statusPanel = new JPanel();
		statusPanel.setLayout(new GridLayout(1,3));
		statusPanel.setBorder(new TitledBorder("Mission Status"));
		statusPanel.add(labelPanel);
		statusPanel.add(textPanel);
		statusPanel.add(statusColor);
		statusColor.setBackground(Color.WHITE);
		statusColor.setEditable(false);
		
		JPanel leftPanel = new JPanel();
		leftPanel.setLayout(new GridLayout(2,1));
		leftPanel.add(upperLeftPanel);
		leftPanel.add(statusPanel);
		
		JPanel upperPanel = new JPanel();
		upperPanel.setLayout(new GridLayout(1,2));
		upperPanel.add(leftPanel);
		upperPanel.add(settingsPanel);
		
		JPanel scrollPanel = new JPanel();
		scrollPanel.setLayout(new BorderLayout());
		scrollPanel.add(scrollPane, BorderLayout.CENTER);
		scrollPanel.add(downtimeCounter, BorderLayout.SOUTH);
		scrollPane.setBorder(new TitledBorder("System Log"));
		downtimeCounter.setHorizontalAlignment(JLabel.CENTER);
		textArea.setEditable(false);
		
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BorderLayout());
		mainPanel.add(upperPanel, BorderLayout.NORTH);
		mainPanel.add(scrollPanel, BorderLayout.CENTER);
		add(mainPanel);
		
		/*
		 * Action Listeners for Buttons
		 */
        
        // Start Button
        startButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!run) {
					if (consistencyThresholdTextField.getText().equals("")) {
						JOptionPane.showMessageDialog(null, "Please set a threshold for consistent downtime.", "ERROR", JOptionPane.ERROR_MESSAGE);
					} else if (primeReportTimeTextField.getText().equals("")) {
						JOptionPane.showMessageDialog(null, "Please set a reportable time for PRIME.", "ERROR", JOptionPane.ERROR_MESSAGE);
					} else if (altReportTimeTextField.getText().equals("")) {
						JOptionPane.showMessageDialog(null, "Please set a reportable time for ALTERNATE.", "ERROR", JOptionPane.ERROR_MESSAGE);
					} else {
						try {
							consistencyThreshold = Double.parseDouble(consistencyThresholdTextField.getText());
							primeReportTime = Double.parseDouble(primeReportTimeTextField.getText());
							altReportTime = Double.parseDouble(altReportTimeTextField.getText());
							if (consistencyThreshold <= 0 || primeReportTime <= 0 || altReportTime <= 0 ) {
								JOptionPane.showMessageDialog(null, "Alarm times must be greater than 0.", "ERROR", JOptionPane.ERROR_MESSAGE);
							} else {
								if (yesRadioButton.isSelected()) {
									isPrime = true;
								} else {
									isPrime = false;
								}
								if (!hasAlreadyBeenStarted) {
									isPrevPrime = isPrime;
								}
								try {
									startMonitor(isPrime, isPrevPrime, consistencyThreshold, primeReportTime, altReportTime, hasAlreadyBeenStarted);
									run = true;
									hasAlreadyBeenStarted = true;
									setTitle("Mission Status Alarm Monitor (Running)");
									consistencyThresholdTextField.setEditable(false);
									primeReportTimeTextField.setEditable(false);
									altReportTimeTextField.setEditable(false);
									yesRadioButton.setEnabled(false);
									noRadioButton.setEnabled(false);
								} catch (Exception e1) {
									JOptionPane.showMessageDialog(null, "Mission Status Alarm Monitor was unable to start.", "ERROR", JOptionPane.ERROR_MESSAGE);
								}
							}
						} catch (NumberFormatException nfe) {
							JOptionPane.showMessageDialog(null, "Invalid User Input\nPlease enter a valid number (ex: 35).", "ERROR", JOptionPane.ERROR_MESSAGE);
						}
					}
				} else {
					JOptionPane.showMessageDialog(null, "The monitor is already running.", "ERROR", JOptionPane.ERROR_MESSAGE);
				}
			}
        });
        
        // Stop Button
        stopButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				stopMonitor();
			}
        });
        
        // Search Log Button
        searchLogButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				boolean datesAreValid = false;
				while (!datesAreValid) {
					JLabel startDateLabel = new JLabel("Start Date:");
					JCalendar startDateCalendar = new JCalendar();
					JLabel endDateLabel = new JLabel("End Date:");
					JCalendar endDateCalendar = new JCalendar();
					SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-YYYY  HH:mm:ss'Z'", Locale.ENGLISH);
					dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
					Object[] dateRange = {
							startDateLabel, startDateCalendar,
							endDateLabel, endDateCalendar,
							"OK", "Cancel"
					};
					int option = JOptionPane.showOptionDialog(null, "Select Date Range:", "Search Log Entries", 
							JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, dateRange, null);
					if (option == 4) {
						Calendar startDate = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
						startDate.setTime(startDateCalendar.getCalendar().getTime());
						startDate.set(Calendar.HOUR_OF_DAY, 0);
						startDate.set(Calendar.MINUTE, 0);
						startDate.set(Calendar.SECOND, 0);
						Calendar endDate = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
						endDate.setTime(endDateCalendar.getCalendar().getTime());
						endDate.set(Calendar.HOUR_OF_DAY, 23);
						endDate.set(Calendar.MINUTE, 59);
						endDate.set(Calendar.SECOND, 59);
						
						if (startDate.after(endDate)) {
							JOptionPane.showMessageDialog(null, "The start date cannot be after the end date.", "ERROR", JOptionPane.ERROR_MESSAGE);
						} else {
							datesAreValid = true;
							try {
								FileReader reader = new FileReader("C:/Users/Matthew/Desktop/Work Projects/Mission Status Alarm Monitor/FAM/log.txt");
								BufferedReader bufferedReader = new BufferedReader(reader);
								String logEntries = "";
								String line;
								int lineNumber = 1;
								int lastLineNumber = 1;
								
								while ((line = bufferedReader.readLine()) != null) {
									if (!line.equals("") && Character.isDigit(line.charAt(0))) {
										Calendar entryDate = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
										entryDate.set(Calendar.DAY_OF_MONTH, Integer.parseInt(line.substring(0,2)));
										entryDate.set(Calendar.MONTH, determineMonth(line.substring(3,6)));
										entryDate.set(Calendar.YEAR, Integer.parseInt(line.substring(7,11)));
										entryDate.set(Calendar.HOUR_OF_DAY, Integer.parseInt(line.substring(13,15)));
										entryDate.set(Calendar.MINUTE, Integer.parseInt(line.substring(16,18)));
										entryDate.set(Calendar.SECOND, Integer.parseInt(line.substring(19,21)));
	
										if (entryDate.after(startDate) && !entryDate.after(endDate)) {
											logEntries += line+"\n";
											lastLineNumber = lineNumber;
										}
									} else {
										if ((lineNumber - lastLineNumber) == 1) {
											logEntries += line+"\n";
										}
									}
									lineNumber++;
								}
								bufferedReader.close();
								reader.close();
								JTextArea logResults = new JTextArea();
								JScrollPane logScrollPane = new JScrollPane(logResults);
								logScrollPane.setPreferredSize(new Dimension(400,400));
								logResults.setText(logEntries);
								logResults.setEditable(false);
								logResults.setCaretPosition(0);
								JOptionPane.showMessageDialog(null, logScrollPane, "Log Entries", JOptionPane.PLAIN_MESSAGE);
							} catch (IOException e1) {
								JOptionPane.showMessageDialog(null, "Log file could not be searched.", "ERROR", JOptionPane.ERROR_MESSAGE);
								break;
							}
						}
					} else {
						break;
					}
				}
			}
        });
        
        // Acknowledge Alarm Button
        acknowledgeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (monitor != null) {
					monitor.acknowledgeAlarm();
				}
			}
        });
        
        // Test Alarm Button
        testButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (monitor != null) {
					monitor.testAlarm("alarm1.wav");
				} else {
					JOptionPane.showMessageDialog(null, "Monitor must be running to test alarm.", null, JOptionPane.PLAIN_MESSAGE);
				}
			}
        });
        
        // Clear Settings Button
        clearButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!run) {
					consistencyThresholdTextField.setText("");
					primeReportTimeTextField.setText("");
					altReportTimeTextField.setText("");
				} else {
					JOptionPane.showMessageDialog(null, "Alarm settings cannot be changed while monitor is running."
							+ "\nIf you would like to make changes, the monitor must be stopped first.", "ERROR", JOptionPane.ERROR_MESSAGE);
				}
			}
        });
        
        // Fills System Log w/ Prior Log Entries in log.txt File
        try {
			textArea.setText(new String(Files.readAllBytes(Paths.get("C:/Users/Matthew/Desktop/Work Projects/Mission Status Alarm Monitor/FAM/log.txt")), StandardCharsets.UTF_8));
		} catch (IOException e1) {
			JOptionPane.showMessageDialog(null, "Prior log entries could not be read from log file.", "ERROR", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	/*
	 * Methods
	 */
	
	// Starts Mission Status Alarm Monitor
	private void startMonitor(boolean isPrime, boolean isPrevPrime, double downtimeThreshold,
			double primeReportTime, double altReportTime, boolean hasAlreadyBeenStarted) throws Exception {
		SwingWorker<Void, String> worker = new SwingWorker<Void, String>() {

			long downtime;
			
			// Retrieves Data from Monitor Class
			protected Void doInBackground() throws Exception {
				textArea.setCaretPosition(textArea.getDocument().getLength());
				monitor = new Monitor(isPrime, isPrevPrime, downtimeThreshold, primeReportTime, altReportTime, hasAlreadyBeenStarted);
				if (hasAlreadyBeenStarted) {
					monitor.setStatus(lastStatus);
					monitor.setStartTime(startTime);
				}
				TimeUnit.MILLISECONDS.sleep(15000);
				while (run) {
					if (monitorRunTime >= 604800) {
						isMonitorRestart = true;
						stopMonitor();
					} else {
						TimeUnit.MILLISECONDS.sleep(1000);
						if (monitor.getStatus() == Status.DOWN) {
							downtime = (System.nanoTime() - monitor.getStartTime()) / 1000000000;
						} else {
							downtime = 0;
						}
						publish(monitor.getLogUpdate());
						monitorRunTime++;
					}
				}
				return null;
			}
			
			// Updates GUI Values
			protected void process(List<String> logUpdates) {
				for (String logUpdate : logUpdates) {
					textArea.append(logUpdate);
					if (logUpdate != "") {
						updateLog(logUpdate);
						textArea.setCaretPosition(textArea.getDocument().getLength());
					}
				}
				if (run) {
					if (monitor.getStatus() == Status.NORMAL) {
						statusText.setText("NORMAL");
						statusColor.setBackground(Color.GREEN);
					} else if (monitor.getStatus() == Status.DOWN) {
						statusText.setText("DOWN");
						statusColor.setBackground(Color.RED);
					} else if (monitor.getStatus() == Status.DEGRADED) {
						statusText.setText("DEGRADED");
						statusColor.setBackground(Color.YELLOW);
					} else {
						statusText.setText("");
						statusColor.setBackground(Color.WHITE);
					}
					packetLossText.setText(monitor.getPacketLoss());
					if (downtime == 0) {
						downtimeCounter.setText("");
					} else {
						downtimeCounter.setText("Current Downtime:    "+monitor.formatDowntime(downtime));
					}
				} else {
					statusText.setText("");
					statusColor.setBackground(Color.WHITE);
					packetLossText.setText("");
				}
			}

			// Message Appears if Monitor is Stopped
			protected void done() {
				if (!isMonitorRestart) {
					JOptionPane.showMessageDialog(null, "The Mission Status Alarm Monitor has stopped.", null, JOptionPane.PLAIN_MESSAGE);
				}
			}
		};
		worker.execute();
	}
	
	// Stops Mission Status Alarm Monitor
	private void stopMonitor() {
		if (run) {
			run = false;
			try {
				TimeUnit.MILLISECONDS.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (monitor != null) {
				isPrevPrime = monitor.isPrime();
				lastStatus = monitor.getStatus();
				startTime = monitor.getStartTime();
				monitor.setRun(false);
				if (!isMonitorRestart) {
					monitor.testAlarm("alarm3.wav");
				}
//				monitor.getDriver().close();
			}
			if (!isMonitorRestart) {
				setTitle("Mission Status Alarm Monitor (Stopped)");
				consistencyThresholdTextField.setEditable(true);
				primeReportTimeTextField.setEditable(true);
				altReportTimeTextField.setEditable(true);
				yesRadioButton.setEnabled(true);
				noRadioButton.setEnabled(true);
			} else {
				try {
					startMonitor(isPrime, isPrevPrime, consistencyThreshold, primeReportTime, altReportTime, hasAlreadyBeenStarted);
					run = true;
				} catch (Exception e1) {
					JOptionPane.showMessageDialog(null, "Mission Status Alarm Monitor was unable to start.", "ERROR", JOptionPane.ERROR_MESSAGE);
				}
				isMonitorRestart = false;
			}
			monitorRunTime = 0;
		}
	}
	
	// Updates log.txt File w/ New Entries
	private void updateLog(String logUpdate) {
		try {
			FileWriter writer = new FileWriter("C:/Users/Matthew/Desktop/Work Projects/Mission Status Alarm Monitor/FAM/log.txt", true);
			writer.append(logUpdate);
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	// Determines Number of Month in Log Entry
	private int determineMonth(String month) {
		int monthNumber;
		switch (month.toUpperCase()) {
			case "JAN": monthNumber = 0; break;
			case "FEB": monthNumber = 1; break;
			case "MAR": monthNumber = 2; break;
			case "APR": monthNumber = 3; break;
			case "MAY": monthNumber = 4; break;
			case "JUN": monthNumber = 5; break;
			case "JUL": monthNumber = 6; break;
			case "AUG": monthNumber = 7; break;
			case "SEP": monthNumber = 8; break;
			case "OCT": monthNumber = 9; break;
			case "NOV": monthNumber = 10; break;
			case "DEC": monthNumber = 11; break;
		default: monthNumber = 0; break;
		}
		return monthNumber;
	}
	
	// Main Method
	public static void main(String args[]) throws ClassNotFoundException,
	InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException {
		
		GUI main = new GUI();
		
        // GUI Parameters
		UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        main.setVisible(true);
        main.setTitle("Mission Status Alarm Monitor");
        main.setSize(850,600);
        main.setResizable(true);
        main.setLocationRelativeTo(null);
        main.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        // Code Executes at Exit of Program
        Runtime.getRuntime().addShutdownHook(new Thread() { 
        	public void run() { 
        		if (monitor != null && monitor.getDriver() != null) {
        			monitor.getDriver().close();
        			try {
						Process exit = Runtime.getRuntime().exec("taskkill /F /IM IEDriverServer.exe");
						exit.waitFor();
					} catch (IOException | InterruptedException e) {
						e.printStackTrace();
					}
        		}
        	} 
        }); 
	}
}
