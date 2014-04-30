import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.regex.Pattern;


public class imp2 {
	
	private static final String DEFAULT_TEST_CONFIG_FILE = "test.cfg";
	
	private static final int TOKEN_STARTING_BRACES = 1;
	private static final int TOKEN_START = 8;
	private static final int TOKEN_GLOBAL_CONFIG = 9;
	private static final int TOKEN_ATTRIBUTE_KEY = 10;
	private static final int TOKEN_ATTRIBUTE_EQUALS = 11;
	private static final int TOKEN_ATTRIBUTE_VALUE = 12;
	private static final int TOKEN_ATTRIBUTE_END = 13;
	private static final int TOKEN_CONFIG_END = 14;
	private static final int TOKEN_HOST_CONFIG = 15;
	private static final int TOKEN_HOST_HOSTNAME = 16;
	private static final int TOKEN_CONFIG_END_SEMICOLON = 17;
	private static final int TOKEN_QUOTED_ATTRIBUTE_VALUE_STARTED = 18;
	
	private static final int LEXICAL_ERROR = 10;
	private static final int PARSER_ERROR = 11;
	
	private static final String COMMENT = "#";
	private static final String STARTING_BRACE = "{";
	private static final String CLOSING_BRACE = "}";
	private static final String QUOTE = "\"";
	//private static final String CLOSING_BRACE_SEMICOLON = "};";
	private static final String GLOBAL = "global";
	private static final String EQUALS = "=";
	private static final String NEWLINE = "\n";
	private static final String SEMICOLON = ";";
	private static final String HOST = "host";
	
	private int currentToken = TOKEN_START;			// Keeps track of the last token seen
	
	private HashMap<String,Attribute> viewedAttrMap = new HashMap<String,Attribute>();	// Global attribute map to determine overridden flag
	private ArrayList<Config> configList = new ArrayList<Config>();	// list of configurations
	private Config currentConfig = null;		// Configuration object of the current run
	private int lineCount = 0;					// current line number
	
	Attribute currentAttribute = new Attribute();	
	String currentAttributeKey = "";
	String currentAttributeValue = "";
	String currentAttributeType = "";
	boolean currentOverridenFlag = false;
	
	public Status tokenize(String fileName){
		Status status = new Status();
		status.setResponseCode(0);
		status.setDescription("");
		try {
			BufferedReader br = new BufferedReader(new FileReader(fileName));
			String nextLine = "";
			while((nextLine = br.readLine())!=null){
				lineCount++;
				if(nextLine.trim().length()==0 || nextLine.trim().charAt(0)=='#')	// ignore empty lines and comments
					continue;
				nextLine = nextLine.replaceAll("[{]", " { ");	// as we are separating the tokens by space, need tokens around these characters so that we can separate them
				nextLine = nextLine.replaceAll("[}]", " } ");
				nextLine = nextLine.replaceAll("[=]", " = ");
				nextLine = nextLine.replaceAll("[;]", " ; ");
				nextLine = nextLine.trim() + " \n";
				if(nextLine.contains(COMMENT)){		// ignore comment within a sentence
					if(nextLine.contains(EQUALS)){
						String []split = nextLine.split(EQUALS);
						if(split[1].trim().charAt(0)=='\"'){
							int indexQuote1 = nextLine.indexOf(QUOTE);
							int indexQuote2 = nextLine.indexOf(QUOTE, indexQuote1+1);
							int indexComment = nextLine.indexOf(COMMENT);
							if(indexQuote1 >=0 && indexQuote2 >= 0){
								while(indexComment >= 0){
									if(indexComment < indexQuote1){
										nextLine = nextLine.substring(0,indexComment)+" \n";
										break;
									}
									else if(indexComment > indexQuote1 && indexComment < indexQuote2)
										indexComment = nextLine.indexOf(COMMENT, indexComment+1);
									else if(indexComment > indexQuote2){
										nextLine = nextLine.substring(0, indexComment)+" \n";
										break;
									}
								}
							}
						}
						else{
							int commentIndex = nextLine.indexOf(COMMENT);
							nextLine = nextLine.substring(0, commentIndex)+" \n";
						}
					}
					else{
						int commentIndex = nextLine.indexOf(COMMENT);
						nextLine = nextLine.substring(0, commentIndex)+" \n";
					}
				}
				String []token = nextLine.split(" +");		// split the line into words (tokens)
				for(int i=0;i<token.length;i++){
					int res = parse(token[i]);
					if(res != 0){
						br.close();
						status.setResponseCode(res);
						if(res == PARSER_ERROR)
							status.setDescription("Err:P:"+lineCount);
						else if(res == LEXICAL_ERROR)
							status.setDescription("Err:L:"+lineCount);
						return status;
					}
					
					/*
					if(res == PARSER_ERROR){
						System.out.println("Err:P:"+lineCount);
						br.close();
						return res;
					}
					else if(res == LEXICAL_ERROR){
						System.out.println("Err:L:"+lineCount);
						br.close();
						return res;
					}*/
				}
			}
			br.close();
			if(currentToken != TOKEN_CONFIG_END && currentToken != TOKEN_CONFIG_END_SEMICOLON){		// if the current token is not TOKEN_CONFIG_END, the config didn't end with }
				//System.out.println("Error:P:"+lineCount);
				status.setResponseCode(PARSER_ERROR);
				status.setDescription("Err:P:"+lineCount);
				return status;
			}
			
			
		} catch (FileNotFoundException e) {
			System.out.println("Error: File Not Found\n"+e.getLocalizedMessage());
		} catch (IOException e) {
			System.out.println(e.getLocalizedMessage());
		}
		return status;
	}
	
	/**
	 * Parses the tokens, the logic is separated into difference cases which represent the last token seen.
	 * This method determines which token is valid with last seen token and accordingly updates the current token.
	 * 
	 * @param token
	 * @return
	 */
	private int parse(String token){
		switch(currentToken){
		
			case TOKEN_START:{				// starting, we did not see anything previously yet
				if(token.equals(GLOBAL)){	// we expect global
					currentConfig = new Config();
					currentConfig.setAttributes(new LinkedHashMap<String,Attribute>());
					currentConfig.setConfigName(GLOBAL);
					currentConfig.setHostName("");
					currentToken = TOKEN_GLOBAL_CONFIG;
				}
				else 						// didn't find global, throw error
					return PARSER_ERROR;
				break;
			}
			
			case TOKEN_GLOBAL_CONFIG:{		// we have seen global previously
				if(token.equals(NEWLINE)){	// ignore new lines
					break;
					//do nothing
				}
				else if(token.equals(STARTING_BRACE))	// found starting brace, update the current token
					currentToken = TOKEN_STARTING_BRACES;
				else
					return PARSER_ERROR;			// anything else is invalid, throw error
				break;
			}
			
			case TOKEN_STARTING_BRACES:{	// we have seen starting braces previously
				if(token.equals(NEWLINE)){	// ignore new lines
					break;
				}
				if(token.equals(CLOSING_BRACE)){		// found closing braces, save the config read till now in the config list and update current token
					configList.add(currentConfig);
					currentConfig = null;
					currentToken = TOKEN_CONFIG_END;
					break;
				}
				// to do, check if the start character is a number, return error in that case
				currentAttributeKey = token;			// found attribute key, have to validate it
				if(currentAttributeKey.charAt(0) >= '0' && currentAttributeKey.charAt(0) <= '9')
					return PARSER_ERROR;				// wrong attribute key, throw error
				if(isValidAttributeKey(token)==false)
					return PARSER_ERROR;
				
				currentToken = TOKEN_ATTRIBUTE_KEY;
				break;
			}
			
			case TOKEN_ATTRIBUTE_KEY:{		// we have seen attribute key previously
				if(token.equals(EQUALS))	// next token has to be =
					currentToken = TOKEN_ATTRIBUTE_EQUALS;
				else						// anything else is invalid, throw error
					return PARSER_ERROR;
				break;
			}
			
			case TOKEN_ATTRIBUTE_EQUALS:{	// we have seen = previously
				//to do, check for valid attribute value
				currentAttributeValue = token;
				
				if(currentAttributeValue.matches("[+-]?\\d+"))		// determine type of attribute value
					currentAttributeType = "I";
				else if(Pattern.matches("([+-]?[0-9]*)\\.([0-9]*)", currentAttributeValue)){
					if(currentAttributeValue.charAt(0)=='.')
						return PARSER_ERROR;
					currentAttributeType = "F";
				}
				else if(currentAttributeValue.length()> 2 && currentAttributeValue.charAt(0)=='\"' ){
					currentAttributeType = "Q";
					int lastCharIndex = currentAttributeValue.length()-1;
					if(currentAttributeValue.charAt(lastCharIndex)!='\"'){	// the token doesn't end with "
						currentToken = TOKEN_QUOTED_ATTRIBUTE_VALUE_STARTED;
					}
					else{										// the token ends with a "
						if(currentAttributeValue.charAt(lastCharIndex-1)=='\\' && currentAttributeValue.charAt(lastCharIndex-2)!='\\')
							return LEXICAL_ERROR;
						int innerQuoteIndex = currentAttributeValue.indexOf('\"', 1);
						if(innerQuoteIndex > 0 && innerQuoteIndex < lastCharIndex)
							if(currentAttributeValue.charAt(innerQuoteIndex-1)!='\\')
								return PARSER_ERROR;
						if(currentAttributeValue.charAt(lastCharIndex-1)=='\\'){
							currentToken = TOKEN_QUOTED_ATTRIBUTE_VALUE_STARTED;
						}
						else
							currentToken = TOKEN_ATTRIBUTE_VALUE;
					}
					break;
				}
				else{
					if(isValidUnquotedString(token)==false)
						return PARSER_ERROR;
					currentAttributeType = "S";
				}
				
				currentToken = TOKEN_ATTRIBUTE_VALUE;
				
				break;
			}
			
			case TOKEN_QUOTED_ATTRIBUTE_VALUE_STARTED:{
				if(token.equals(NEWLINE))
					return PARSER_ERROR;
				if(token.equals(QUOTE)){
					currentToken = TOKEN_ATTRIBUTE_VALUE;
					break;
				}
				else{
					if(token.charAt(token.length()-2)=='\\' && token.charAt(token.length()-3)!='\\')
						return LEXICAL_ERROR;
					if(token.charAt(0)=='\"' && token.length()>1)
						return PARSER_ERROR;
					currentAttributeValue = currentAttributeValue + " " + token;
					int lastCharIndex = token.length()-1;
					int innerQuoteIndex = token.indexOf('\"',1);
					if(innerQuoteIndex > 0 && innerQuoteIndex<lastCharIndex)
						if(token.charAt(innerQuoteIndex-1)!='\\')
							return PARSER_ERROR;
					if(token.charAt(lastCharIndex) == '\"'){
						if(token.charAt(lastCharIndex-1)!='\\')
							currentToken = TOKEN_ATTRIBUTE_VALUE;
					}
					break;
				}
			}
			case TOKEN_ATTRIBUTE_VALUE:{		// we have seen attribute value previously
				if(token.equals(NEWLINE) ){		// new line marks end of current attribute, save in the attribute list
					currentAttribute = new Attribute();
					currentAttribute.setValue(currentAttributeValue);
					currentAttribute.setType(currentAttributeType);
					
					
					if(viewedAttrMap.get(currentAttributeKey)!=null){	// check for overriding
						if(viewedAttrMap.get(currentAttributeKey).getType().equals(currentAttribute.getType()))
							currentAttribute.setOverriden(true);
					}
					
					currentConfig.getAttributes().put(currentAttributeKey, currentAttribute);
					
					viewedAttrMap.put(currentAttributeKey, currentAttribute);
					currentToken = TOKEN_ATTRIBUTE_END;
				}
				else if(token.equals(CLOSING_BRACE) ){		// found }, need to save current attribute, and current config and mark end of config
					currentAttribute = new Attribute();
					currentAttribute.setValue(currentAttributeValue);
					currentAttribute.setType(currentAttributeType);
					
					
					if(viewedAttrMap.get(currentAttributeKey)!=null){
						if(viewedAttrMap.get(currentAttributeKey).getType().equals(currentAttribute.getType()))
							currentAttribute.setOverriden(true);
					}
					
					currentConfig.getAttributes().put(currentAttributeKey, currentAttribute);
					
					viewedAttrMap.put(currentAttributeKey, currentAttribute);
					configList.add(currentConfig);
					currentConfig = null;
					currentToken = TOKEN_CONFIG_END;
				}
				else
					return PARSER_ERROR;
				break;
			}
			
			case TOKEN_ATTRIBUTE_END:{		// we have seen end of an attribute
				if(token.equals(NEWLINE))	// ignore new line
					break;
				if(token.equals(CLOSING_BRACE)){	// check for end of config
					configList.add(currentConfig);
					currentConfig = null;
					currentAttribute = null;
					currentAttributeKey = "";
					currentAttributeValue = "";
					currentOverridenFlag = false;
					currentToken = TOKEN_CONFIG_END;
					break;
				}
				currentAttributeKey = token;
				if(currentAttributeKey.charAt(0)>='0' && currentAttributeKey.charAt(0)<='9')	// new attribute found
					return PARSER_ERROR;
				currentToken = TOKEN_ATTRIBUTE_KEY;
				break;
			}
			
			case TOKEN_CONFIG_END:{		// we have seen end of config
				if(token.equals(NEWLINE))	// ignore new line
					break;
				if(token.equals(SEMICOLON)){
					currentToken = TOKEN_CONFIG_END_SEMICOLON;
					break;
				}
				if(token.equals(HOST)){		// new config found, start again
					currentConfig = new Config();
					currentConfig.setConfigName(HOST);
					currentToken = TOKEN_HOST_CONFIG;
				}
				else
					return PARSER_ERROR;
				break;
			}
			
			case TOKEN_CONFIG_END_SEMICOLON:{
				if(token.equals(NEWLINE))
					break;
				if(token.equals(HOST)){		// new config found, start again
					currentConfig = new Config();
					currentConfig.setConfigName(HOST);
					currentToken = TOKEN_HOST_CONFIG;
				}
				else
					return PARSER_ERROR;
				break;
			}
			
			case TOKEN_HOST_CONFIG:{	// host type of config found
				if(token.equals(NEWLINE)){	// ignore new line
					break;
					//do nothing
				}
				else if(token.equals(STARTING_BRACE)){	// starting brace found, no hostname for this one
					currentConfig.setHostName("");
					currentToken = TOKEN_STARTING_BRACES;
				}
				else{
					//check for valid hostname
					if(isValidHostname(token))
						currentConfig.setHostName(token);	// hostname found
					else
						return PARSER_ERROR;
					currentToken = TOKEN_HOST_HOSTNAME;
				}
				break;
			}
			
			case TOKEN_HOST_HOSTNAME:{	// we have seen hostname prevously
				if(token.equals(NEWLINE)){	// ignore new lines
					break;
					//do nothing
				}
				else if(token.equals(STARTING_BRACE))	// starting braces found
					currentToken = TOKEN_STARTING_BRACES;
				else
					return PARSER_ERROR;
				break;
			}
		}
		return 0;
	}
	
	public void printConfiguration(){
		for(int i=0;i<configList.size();i++){
			System.out.print(configList.get(i).getConfigName().toUpperCase());
			if(configList.get(i).getHostName().length()>0)
				System.out.print(" "+configList.get(i).getHostName());
			System.out.println(":");
			Iterator<Entry<String, Attribute>> attrIterator = configList.get(i).getAttributes().entrySet().iterator();
			while(attrIterator.hasNext()){
				Entry<String,Attribute> entry = attrIterator.next();
				System.out.print("    "+entry.getValue().getType()+":");
				if(entry.getValue().isOverriden())
					System.out.print("O");
				//Matcher.quoteReplacement(s)
				/*String val = entry.getValue().getValue().replaceAll("\\\\\\\\", Matcher.quoteReplacement("\\"));
				if(entry.getValue().getType().equals("Q")){
					val = "\"\"" + val;
					val = val + "\"\"";
				}*/
				String val = entry.getValue().getValue();
				if(entry.getValue().getType().equals("Q"))
					val = processQuotedString(val);
				System.out.println(":"+entry.getKey()+":"+val);
				//System.out.println(":"+entry.getKey()+":"+entry.getValue().getValue());
			}
		}
	}
	
	public static void main(String []args){
		imp2 tokenizer = new imp2();
		Status status = tokenizer.tokenize(DEFAULT_TEST_CONFIG_FILE);
		tokenizer.printConfiguration();
		if(status.getResponseCode()!=0)
			System.out.println(status.getDescription());
	}
	
	public boolean isValidUnquotedString(String str){
		if(Character.isLetter(str.charAt(0)) || str.charAt(0)=='/'){
			String pattern = "[a-zA-Z0-9./-]+";
			if(str.matches(pattern))
				return true;
			else
				return false;
		}
		else
			return false;
	}
	
	public boolean isValidHostname(String hostname){
        if (hostname.matches("[a-zA-Z0-9._-]+"))
        	return true;        
        else 
        	return false;
	}
	
	public boolean isValidAttributeKey(String str){
		if(str.matches("[a-zA-Z0-9_]+"))
			return true;
		else
			return false;
	}
	
	public String processQuotedString(String quotedString){
		String []str = quotedString.split("\\\\");
		quotedString = str[0];
		boolean escapeSeen = true;
		for(int i=1;i<str.length;i++){
			if(str[i].trim().length()==0){
				if(escapeSeen == true){
					quotedString = quotedString + "\\";
					escapeSeen = false;
				}
				else
					escapeSeen = true;
			}
			else if(str[i].charAt(0)=='n'){
				quotedString = quotedString + "\n" +str[i].substring(1);
				escapeSeen = true;
			}
			else if(str[i].charAt(0)=='r'){
				quotedString =  "\"" +str[i].substring(1);
				escapeSeen = true;
			}
			else if(str[i].charAt(0)=='\\'){
				quotedString = quotedString + "\\" +str[i].substring(1);
				escapeSeen = true;
			}
			else{
				quotedString = quotedString + str[i];
				escapeSeen = true;
			}
			
		}
		quotedString = "\"\"" + quotedString + "\"\"";
		return quotedString;
	}
	
	private class Status{
		int responseCode;
		String description;
		public int getResponseCode() {
			return responseCode;
		}
		public void setResponseCode(int responseCode) {
			this.responseCode = responseCode;
		}
		public String getDescription() {
			return description;
		}
		public void setDescription(String description) {
			this.description = description;
		}
		
	}
}
