import java.util.ArrayList;
import java.util.List;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


/********************************************************************
 * Class: Facebook
 * Purpose: Hold all facebook data and connections
/*******************************************************************/
public class Facebook {
	
	// Properties
	private String username;
	private String password;
	private boolean sessionSet;
	private String fb_dtsg;
	private String action;
	private String update;
	private String charset;
	private DefaultHttpClient clientFacebook;
	
	/********************************************************************
	 * Constructor: Facebook()
	 * Purpose: create default facebook object
	/*******************************************************************/
	public Facebook(){
		
		// Initialize
		this.username = "";
		this.password = "";
		this.fb_dtsg = "";
		this.update = "";
		this.action = "";
		this.charset = "";
		this.clientFacebook = new DefaultHttpClient();
	}
	
	
	
	/********************************************************************
	 * Accessors
	 * Purpose: get the corresponding data
	/*******************************************************************/
	public String getUsername() { return this.username; }
	public String getPassword(){ return this.password; }
	public boolean getLoggedIn(){ return this.sessionSet; }
	
	
	/********************************************************************
	 * Mutators: setUsername(), setPassword()
	 * Purpose: set the corresponding data
	/*******************************************************************/
	public void setUsername(String username) { this.username = username; }
	public void setPassword(String password) { this.password = password; }
	
	
	/********************************************************************
	 * Method: loginFacebook()
	 * Purpose: logs user into facebook
	/*******************************************************************/
	public boolean loginFacebook(){
		
		try{
			
			HttpGet initialLoad = new HttpGet("https://www.facebook.com");
			HttpPost login = new HttpPost("https://www.facebook.com/login.php?login_attempt=1");
			
			// Initial load
			HttpResponse response = this.clientFacebook.execute(initialLoad);
	        	HttpEntity entity = response.getEntity();
	        	if (entity != null) EntityUtils.consume(entity);	
	        
	        	// Parameters
		        List <NameValuePair> parameters = new ArrayList <NameValuePair>();
		        parameters.add(new BasicNameValuePair("lsd", ""));
		        parameters.add(new BasicNameValuePair("email", this.username));
		        parameters.add(new BasicNameValuePair("pass", this.password));
		        parameters.add(new BasicNameValuePair("default_persistent", "0"));
		        parameters.add(new BasicNameValuePair("charset_test", ""));
		        parameters.add(new BasicNameValuePair("timezone", "300"));
		        parameters.add(new BasicNameValuePair("lgnrnd", ""));
		        parameters.add(new BasicNameValuePair("lgnjs", ""));
		        parameters.add(new BasicNameValuePair("locale", "en_US"));
		        login.setEntity(new UrlEncodedFormEntity(parameters));
		        
		        // Login
		        response = this.clientFacebook.execute(login);
		        entity = response.getEntity();
		        if (entity != null) EntityUtils.consume(entity);
		        
				
		        List<Cookie> cookies = this.clientFacebook.getCookieStore().getCookies();
		        
		        // Check cookies
		        for (int i = 0; i < cookies.size(); i++) if (cookies.get(i).getName().equals("c_user") && !cookies.get(i).getValue().equals("")) this.sessionSet = true;
		        
		        // Success
		        if (sessionSet){ System.out.println("Successfully logged in."); getMainPage(); return true;  }
		        
		        // Failure
		        else { System.out.println("Login failed."); return false; }
		        
		}
			
		catch(Exception e){ e.printStackTrace(); this.sessionSet = false; return false; }
	}
	
	/********************************************************************
	 * Method: getMainPage
	 * Purpose: get main facebook page to retrieve post function
	/*******************************************************************/
	public void getMainPage(){
		
		try{
			
			// Connect
			HttpGet mainPage = new HttpGet("http://m.facebook.com");
			HttpResponse response = this.clientFacebook.execute(mainPage);
			
			String html = HTMLParser.parse(response);
			
			Document doc = Jsoup.parse(html);
			Element composer = doc.getElementById("composer_form");
			Elements compChildren = composer.children();
			
			Elements nameList = composer.getElementsByAttribute("name");
			
			// Get update value
			for(Element name : nameList) if(name.attr("name").equals("update")) this.update = name.val();
			
			// Action link and other parameters
			this.action = composer.attr("action");
			this.fb_dtsg = compChildren.get(0).val();
			this.charset = compChildren.get(1).val();
			
		}
		
		catch(Exception e){ e.printStackTrace(); }
		
	}
	
	/********************************************************************
	 * Method: PostWall
	 * Purpose: posts a string message to current facebook
	/*******************************************************************/
	@SuppressWarnings("deprecation")
	public void postWall(String message){
		
		try{
			
			// Connect
			HttpPost postMessage = new HttpPost("https://m.facebook.com" + this.action);

			// Parameters
		        List <NameValuePair> parameters = new ArrayList <NameValuePair>();
		        parameters.add(new BasicNameValuePair("fb_dtsg", this.fb_dtsg));
		        parameters.add(new BasicNameValuePair("charset_test", this.charset));
		        parameters.add(new BasicNameValuePair("status", message));
		        parameters.add(new BasicNameValuePair("update", this.update));
		        parameters.add(new BasicNameValuePair("target", ""));
		        postMessage.setEntity(new UrlEncodedFormEntity(parameters));
				
			// Execute			
			HttpResponse response = clientFacebook.execute(postMessage);
			response.getEntity().consumeContent();
			
		}
		
		catch(Exception e){ e.printStackTrace(); }
		
	}

}
