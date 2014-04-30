
/**
 * 
 * Class representing an attribute
 *
 */
public class Attribute {
	String value;
	String type;
	boolean isOverriden;
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public boolean isOverriden() {
		return isOverriden;
	}
	public void setOverriden(boolean isOverriden) {
		this.isOverriden = isOverriden;
	}
	
}
