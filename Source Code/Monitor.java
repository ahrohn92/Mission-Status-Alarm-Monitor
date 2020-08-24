import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.JOptionPane;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.ie.InternetExplorerDriver;

/*
 * File: 		Monitor.java
 * Author: 		Andrew H. Rohn
 * Date: 		16 November 2019
 * Purpose: 	This class uses Selenium to control an Internet Explorer instance that
 * 				pings the IOPlex at XXX to determine the status of the mission. This class
 * 				also handles alarm functionality and log entries.
 */

public class Monitor implements Runnable {
	
	// Local Variables
	private Thread thread = new Thread(this);
	private String prevPacketLoss = "";
	private String currentPacketLoss = "";
	private String logUpdate = "";
	private String currentAlarm = "";
	private double consistencyThreshold, primeReportTime, altReportTime;
	private long startTime, downtime;
	private boolean isPrime, hasAlreadyBeenStarted;
	private boolean run = true;
	private boolean newAlarm = false;
	private boolean alarmIsActive = false;
	private boolean isInitialScan = true;
	public boolean pageHasChanged = false;
	private Status prevStatus, currentStatus;
	private SimpleDateFormat dateFormat;
	private Clip clip;
	private WebDriver driver;

	// Monitor Constructor
	public Monitor(boolean isPrime, boolean isPrevPrime, double consistencyThreshold, double primeReportTime,
			double altReportTime, boolean hasAlreadyBeenStarted) throws InterruptedException, AWTException {
		dateFormat = new SimpleDateFormat("dd-MMM-YYYY  HH:mm:ss'Z'");
		dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		if (isPrime != isPrevPrime) {
			if (isPrime) {
				logUpdate += dateFormat.format(new Date())+"    We are now PRIME\r\n";
			} else {
				logUpdate += dateFormat.format(new Date())+"    We are now ALTERNATE\r\n";
			}
		}
		this.isPrime = isPrime;
		this.consistencyThreshold = consistencyThreshold * 60;
		this.primeReportTime = primeReportTime * 60;
		this.altReportTime = altReportTime * 60;
		this.hasAlreadyBeenStarted = hasAlreadyBeenStarted;
		
		startWebDriver();
		thread.start();
	}
	
	// Starts Selenium IE Web Driver
	private void startWebDriver() throws InterruptedException, AWTException {
		if (driver == null) {
			Robot robot = new Robot();
			System.setProperty("webdriver.ie.driver", "C:/Users/Andrew Rohn/Desktop/Projects/Work Projects/Mission Status Alarm Monitor/FAM/IEDriverServer.exe");
			driver = new InternetExplorerDriver();
			driver.get("https://192.168.1.51/index.html");
			driver.findElement(By.id("overridelink")).click();
			robot.delay(1000);
			robot.keyPress(KeyEvent.VK_ENTER);
			robot.keyRelease(KeyEvent.VK_ENTER);
			TimeUnit.SECONDS.sleep(1);
			driver.findElement(By.id("bannerAccept")).click();
			WebElement userElement = driver.findElement(By.id("user_name"));
			userElement.click();
			userElement.sendKeys("ioplex");
			WebElement passElement = driver.findElement(By.id("password"));
			passElement.click();
			passElement.sendKeys("ioplex");
			driver.findElement(By.id("Submit_")).click();
			TimeUnit.SECONDS.sleep(1);
			driver.findElement(By.linkText("Diagnostic")).click();
			TimeUnit.SECONDS.sleep(1);
			driver.findElement(By.linkText("Ping")).click();
			WebElement ipAddrElement = driver.findElement(By.id("ipAddr"));
			ipAddrElement.click();
			ipAddrElement.sendKeys("172.16.11.65");
		}
	}
	
	@Override
	// Monitor Thread
	public void run() {
		String receivedData = "";
		while (run) {
			receivedData = interfaceWithSelenium();			
			currentStatus = determineStatus(receivedData);
			if (isInitialScan) {
				prevStatus = currentStatus;
				prevPacketLoss = currentPacketLoss;
				checkStatusChange();
			}
			isInitialScan = false;
		}
	}
	
	// Interfaces w/ Selenium IE Web Driver to Access IOplex Software
	private String interfaceWithSelenium() {
		String receivedData = "";
		
		if (isWebBrowserOpen()) {
			if (driver.getCurrentUrl().contains("https://192.168.1.51/ping.html")) {
				receivedData = pingIOplex();
			} else {
				pageHasChanged = true;
			}
		} else {
			driver = null;
			try {
				startWebDriver();
			} catch (InterruptedException | AWTException e) {
				e.printStackTrace();
			}
		}
		return receivedData;
	}
	
	// Determines If Web Browser is Open
	public boolean isWebBrowserOpen() {
		try {
			driver.getTitle();
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	
	// Pings IOplex
	private String pingIOplex() {
		String receivedData = "";
		driver.findElement(By.id("ViewBut")).click();
		for (int i = 0; i < 15; i++) {
			if (!isInitialScan) {
				checkStatusChange();
			}
			try {
				TimeUnit.MILLISECONDS.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		try {
			receivedData = driver.findElement(By.id("view")).getText();
		} catch (Exception e) {
			receivedData = "";
		}
		return receivedData;
	}
	
	// Determines Status of Mission from Received Data
	private Status determineStatus(String testString) {
		Status status = null;
		if (testString.contains("0%")) {
			currentPacketLoss = "0%";
			status = Status.NORMAL;
		}
		if (testString.contains("20%")) {
			currentPacketLoss = "20%";
			status = Status.DEGRADED;
		}
		if (testString.contains("40%")) {
			currentPacketLoss = "40%";
			status = Status.DEGRADED;
		}
		if (testString.contains("60%")) {
			currentPacketLoss = "60%";
			status = Status.DEGRADED;
		}
		if (testString.contains("80%")) {
			currentPacketLoss = "80%";
			status = Status.DEGRADED;
		}
		if (testString.contains("100%")) {
			currentPacketLoss = "100%";
			status = Status.DOWN;
		}
		if (status != null) {
			return status;
		} else {
			return prevStatus;
		}
	}
	
	// Handles Status Changes
	private void checkStatusChange() {
		if (currentStatus == Status.DOWN) {
			if (prevStatus != Status.DOWN || (isInitialScan && !hasAlreadyBeenStarted))  {
				startTime = System.nanoTime();
				logUpdate += dateFormat.format(new Date())+"    Mission is DOWN ("+currentPacketLoss+" packet loss)\r\n";
				newAlarm = true;
			} else {
				determineAlarmType();
			}
		}
		if (currentStatus != Status.DOWN && currentStatus != null) {
			if (prevStatus == Status.DOWN) {
				downtime = ((System.nanoTime() - startTime) / 1000000000);
				logUpdate += dateFormat.format(new Date())+"    Mission is UP ("+currentPacketLoss+" packet loss)\r\n";
				logUpdate += "Downtime Duration:    "+formatDowntime(downtime)+"\r\n";
				if (clip != null && alarmIsActive) {
					clip.close();
					alarmIsActive = false;
				}
				if ((isPrime && downtime > primeReportTime) || (!isPrime && downtime > altReportTime)) {
					if (clip != null) {
						clip.close();
					}
					alarmIsActive = false;
					testAlarm("alarm4.wav");
					alarmIsActive = false;
					newAlarm = false;
				}
			}
		}
		if (currentStatus == Status.DEGRADED && currentPacketLoss != prevPacketLoss) {
			logUpdate += dateFormat.format(new Date())+"    Mission is DEGRADED ("+currentPacketLoss+" packet loss)\r\n";
		}
		if (currentStatus == Status.NORMAL && prevStatus == Status.DEGRADED) {
			logUpdate += dateFormat.format(new Date())+"    Mission is NORMAL ("+currentPacketLoss+" packet loss)\r\n";
		}
		if (currentStatus != null) {
			prevPacketLoss = currentPacketLoss;
			prevStatus = currentStatus;
		}
	}
	
	// Determines the Type of Alarm to Be Used
	private void determineAlarmType() {
		String alarmType = "";
		downtime = ((System.nanoTime() - startTime) / 1000000000);
		if (isPrime) {
			if (downtime > consistencyThreshold) {
				alarmType = "alarm1.wav";
			}
			if (downtime > primeReportTime) {
				alarmType = "alarm2.wav";
			}
		} else if (downtime > altReportTime) {
			alarmType = "alarm2.wav";
		}
		if (!currentAlarm.equals(alarmType)) {
			if (clip != null) {
				clip.close();
			}
			alarmIsActive = false;
			newAlarm = true;
			currentAlarm = alarmType;
		}
		soundAlarm(alarmType);
	}
	
	// Sounds Alarm
	private void soundAlarm(String alarm) {
		if (!alarm.equals("") && !alarmIsActive && newAlarm) {
			try {
				File musicPath = new File("C:/Users/Matthew/Desktop/Projects/Work Projects/Mission Status Alarm Monitor/FAM/"+alarm);
				AudioInputStream audioInput = AudioSystem.getAudioInputStream(musicPath);
				clip = AudioSystem.getClip();
				clip.open(audioInput);
				if (!alarm.equals("alarm3.wav") && !alarm.equals("alarm4.wav")) {
					clip.loop(Clip.LOOP_CONTINUOUSLY);
				}
				clip.start();
				alarmIsActive = true;
			} catch (Exception e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(null, "Alarm is not working.", "ERROR", JOptionPane.ERROR_MESSAGE);
			}
		}
	}
	
	// Silences Alarm
	public void acknowledgeAlarm() {
		if (clip != null && alarmIsActive) {
			clip.close();
			alarmIsActive = false;
			newAlarm = false;
		}
	}
	
	// Tests Alarm
	public void testAlarm(String alarm) {
		newAlarm = true;
		soundAlarm(alarm);
	}
	
	// Formats Downtime
	public String formatDowntime(long downtime) {
		int numHours = 0;
		int numMinutes = 0;
		int numSeconds = 0;
		while (downtime >= 3600) {
			downtime -= 3600;
			numHours++;
		}
		while (downtime >= 60) {
			downtime -= 60;
			numMinutes++;
		}
		while (downtime >= 1) {
			downtime--;
			numSeconds++;
		}
		String formattedDowntime = "";
		if (numHours > 0) {
			if (numHours == 1) {
				formattedDowntime += numHours+" hr ";
			} else {
				formattedDowntime += numHours+" hrs ";
			}
		}
		if (numMinutes > 0) {
			if (numMinutes == 1) {
				formattedDowntime += numMinutes+" min ";
			} else {
				formattedDowntime += numMinutes+" mins ";
			}
		}
		if (numSeconds > 0) {
			if (numSeconds == 1) {
				formattedDowntime += numSeconds+" sec";
			} else {
				formattedDowntime += numSeconds+" secs";
			}
		}
		return formattedDowntime;
	}
	
	/*
	 * Setter and Getter Methods
	 */
	
	// Sets Run Variable
	public void setRun(boolean run) {
		this.run = run;
	}
	
	// Sets Start Time
	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}
	
	// Sets Status
	public void setStatus(Status status) {
		isInitialScan = false;
		prevStatus = status;
	}
	
	// Returns Run Variable
	public boolean getRun() {
		return run;
	}
	
	// Returns Start Time
	public long getStartTime() {
		return startTime;
	}
	
	// Returns Log Updates
	public String getLogUpdate() {
		String temp = logUpdate;
		logUpdate = "";
		return temp;
	}
	
	// Returns isPrime Variable
	public boolean isPrime() {
		return isPrime;
	}
	
	// Returns Status
	public Status getStatus() {
		return currentStatus;
	}
	
	// Returns Packet Loss
	public String getPacketLoss() {
		return currentPacketLoss;
	}
	
	// Returns Web Driver
	public WebDriver getDriver() {
		return driver;
	}
}
