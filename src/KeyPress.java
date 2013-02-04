/********************************************************************
 * Class: KeyPress
 * Purpose: holds keypress information
/*******************************************************************/
public class KeyPress {
	
	// Properties
	private String press;
	private long time;
	
	/********************************************************************
 	 * Constructor: Keypress
	 * Purpose: constructs default keypress object
	/*******************************************************************/
	public KeyPress(String press, long time){
		this.press = press;
		this.time = time;
		
	}
	
	/********************************************************************
	 * Accessors
	 * Purpose: gets corresponding information for keypress
	/*******************************************************************/
	public String getPress(){return this.press;}
	public long getTime(){return this.time;}
	
}
