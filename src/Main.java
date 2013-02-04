import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;

import java.io.*;
import java.util.*;

import org.ini4j.Ini;
import org.ini4j.InvalidFileFormatException;

/********************************************************************
 * Class: Main
 * Purpose: runs main morse network application
/*******************************************************************/
public class Main implements Runnable, SerialPortEventListener {
    
    	static CommPortIdentifier portId;
	InputStream inputStream;
	SerialPort serialPort;
	Thread readThread;
	byte[] readBuffer;
	static Main reader;
	static List <KeyPress> messages;
	static Facebook facebook;
	static int WORD_SPACE;
	static int LETTER_SPACE;
	
	public static void main(String[] args) throws NoSuchPortException, InvalidFileFormatException, FileNotFoundException, IOException {
		
	    Ini ini = new Ini();
	    ini.load(new FileReader("morse_config.txt"));
	
	    
	    Ini.Section misc = ini.get("Misc");
	    Ini.Section user = ini.get("User");
	    Ini.Section speeds = ini.get("Speeds");
	    
	    // Params
	    String port = misc.get("port");
	    String email = user.get("email");
	    String password = user.get("password");
	    
	    // Timing settings
	    WORD_SPACE = Integer.parseInt(speeds.get("wordSpace"));
	    LETTER_SPACE = Integer.parseInt(speeds.get("letterSpace"));
		
	    facebook = new Facebook();
	    facebook.setUsername(email);
	    facebook.setPassword(password);
	    
	    // Login ?
	    if(facebook.loginFacebook()){
	    	
	    	messages = new ArrayList<KeyPress>();
	    	portId = CommPortIdentifier.getPortIdentifier(port);
	    
	    	System.out.println(portId.getName());
	    	reader = new Main();
	    }
	}
	
	/********************************************************************
	 * Constructor: Main
	 * Purpose: makes the necessary connections to the ports
	/*******************************************************************/
	public Main() {
	    
	    try {
	        System.out.println("In Main() contructor");
	        serialPort = (SerialPort) portId.open("MainApp1111",500);
	        System.out.println(" Serial Port.. " + serialPort);
	    } 
	    
	    catch (PortInUseException e) { System.out.println("Port in use Exception"); }
	    
	    try {
	        inputStream = serialPort.getInputStream();
	        System.out.println(" Input Stream... " + inputStream);
	    } 
	    
	    catch (IOException e) { System.out.println("IO Exception"); }
	    
	    try {
	        serialPort.addEventListener(this);
	
	    } 
	    
	    catch (TooManyListenersException e) { System.out.println("Tooo many Listener exception"); }
	    
	    serialPort.notifyOnDataAvailable(true);
	    
	    try {
	
	        serialPort.setSerialPortParams(9600, SerialPort.DATABITS_8,
	             SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
	
	        // no handshaking or other flow control
	        serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);
	
	        // timer on any read of the serial port
	        serialPort.enableReceiveTimeout(500);
	
	        System.out.println("................");
	
	    } 
	    
	    catch (UnsupportedCommOperationException e) { System.out.println("UnSupported comm operation");}
	    
	    readThread = new Thread(this);
	    readThread.start();
	}
	
	/********************************************************************
	 * Method: run
	 * Purpose: waits for arduino input
	/*******************************************************************/
	public void run() {
	    try {
	        System.out.println("In run() function ");
	        Thread.sleep(500);
	    } 
	    
	    catch (InterruptedException e) { System.out.println("Interrupted Exception in run() method"); }
	
	}
	
	/********************************************************************
	 * Method: serialEvent
	 * Purpose: takes in and handles a event on the Arduino
	/*******************************************************************/
	@Override
	public void serialEvent(SerialPortEvent event) {
	
	    switch (event.getEventType()) {
	     
	     case SerialPortEvent.DATA_AVAILABLE:
	        
	        // Clear previous byte
	        readBuffer = new byte[1];
	
	        try {
		    
		    // Data in buffer
	            while (inputStream.available()>0) { inputStream.read(readBuffer); }
	            
	            // Params
	            String press = new String(readBuffer);
	            long time = Calendar.getInstance().getTime().getTime();
	            
	            // Post message
	            if(press.equals("3")) { processMorseCode(); }
	            
	            // Arduino error
	            else if(!(press.equals("0") || press.equals("1") || press.equals("3"))) Main.messages = new ArrayList<KeyPress>();
	            
	            // Add new keypress
	            else { 
	            	Main.messages.add(new KeyPress(press, time));
	            	System.out.println(press + " Time=" + time);
	            }
	            
	
	        } 
	        
	        catch (IOException e) { System.out.println("IO Exception in SerialEvent()"); }
	        
	        break;
	    }
	}
	
	/********************************************************************
	 * Method: processMorseCode
 	 * Purpose: post-processing for morse code timestamps
	/*******************************************************************/
	public void processMorseCode(){
		
		// Helpful print
		System.out.println("Message processing...");
		System.out.println("Total messages: " + Main.messages.size());
		
		// Params
		List<String> letter = new ArrayList<String>();
		String currentWord = "";
		String message = "";
		
		// Add first
		letter.add(messages.get(0).getPress());
		KeyPress latestKey = messages.get(0);

		
		for (int i = 1; i < messages.size(); i++){
			
			// Time difference
			float timeDif = messages.get(i).getTime() - latestKey.getTime();
			
			// New word ?
			if(timeDif > WORD_SPACE){
				
				currentWord += convertLetter(letter);
				message += currentWord + " ";
				letter = new ArrayList<String>();
				letter.add(messages.get(i).getPress());
				currentWord = "";

			}
			
			// New letter?
			else if(timeDif> LETTER_SPACE){ 
				
				currentWord += convertLetter(letter);
				letter = new ArrayList<String>();
				letter.add(messages.get(i).getPress());
			}
			
			// Part of current letter
			else letter.add(messages.get(i).getPress());
			
			latestKey = messages.get(i);
		}
		
		message += currentWord + convertLetter(letter);
		facebook.postWall(message);
		System.out.println("Message: " + message);
		
		Main.messages = new ArrayList<KeyPress>();
		
	}
	
	/********************************************************************
	 * Method: convertLetter
	 * Purpose: returns a converted letter of morse code to string
	/*******************************************************************/
	public String convertLetter(List<String> keys){
		
		String retStr = "";
		
		for (String key : keys) retStr += key;
		
		return getKey(retStr);
	}
	
	/********************************************************************
 	 * Method: getKey
 	 * Purpose: returns key for a single morse code letter
	/*******************************************************************/
	public String getKey(String key){
		
		if(key.equals("01")) return "A";
		else if(key.equals("1000")) return "B";
		else if(key.equals("1010")) return "C";
		else if(key.equals("100")) return "D";
		else if(key.equals("0")) return "E";
		else if(key.equals("0010")) return "F";
		else if(key.equals("110")) return "G";
		else if(key.equals("0000")) return "H";
		else if(key.equals("00")) return "I";
		else if(key.equals("0111")) return "J";
		else if(key.equals("101")) return "K";
		else if(key.equals("0100")) return "L";
		else if(key.equals("11")) return "M";
		else if(key.equals("10")) return "N";
		else if(key.equals("111")) return "O";
		else if(key.equals("0110")) return "P";
		else if(key.equals("1101")) return "Q";
		else if(key.equals("010")) return "R";
		else if(key.equals("000")) return "S";
		else if(key.equals("1")) return "T";
		else if(key.equals("001")) return "U";
		else if(key.equals("0001")) return "V";
		else if(key.equals("011")) return "W";
		else if(key.equals("1001")) return "X";
		else if(key.equals("1011")) return "Y";
		else if(key.equals("1100")) return "Z";
		else if(key.equals("01111")) return "1";
		else if(key.equals("00111")) return "2";
		else if(key.equals("00011")) return "3";
		else if(key.equals("00001")) return "4";
		else if(key.equals("00000")) return "5";
		else if(key.equals("10000")) return "6";
		else if(key.equals("11000")) return "7";
		else if(key.equals("11100")) return "8";
		else if(key.equals("11110")) return "9";
		else if(key.equals("11111")) return "6";
		else if(key.equals("001100")) return ".";
		else return "?";
	}
}
