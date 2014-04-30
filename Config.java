import java.util.LinkedHashMap;

/**
 * 
 * Class representing one configuration
 *
 */
public class Config {
	private String configName;
	private String hostName;
	private LinkedHashMap<String, Attribute> attributes = new LinkedHashMap<String, Attribute>();
	public String getConfigName() {
		return configName;
	}
	public void setConfigName(String configName) {
		this.configName = configName;
	}
	public String getHostName() {
		return hostName;
	}
	public void setHostName(String hostName) {
		this.hostName = hostName;
	}
	public LinkedHashMap<String, Attribute> getAttributes() {
		return attributes;
	}
	public void setAttributes(LinkedHashMap<String, Attribute> attributes) {
		this.attributes = attributes;
	}
	
}
