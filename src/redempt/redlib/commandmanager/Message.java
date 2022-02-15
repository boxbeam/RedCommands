package redempt.redlib.commandmanager;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a single entry in a Messages manager
 * @author Redempt
 */
public class Message {
	
	private String value;
	private String defaultValue;
	private String formatted;
	private List<String> placeholders = new ArrayList<>();
	
	protected Message(String value, String defaultValue, Pattern placeholderPattern) {
		this.value = value == null ? defaultValue : value;
		this.defaultValue = defaultValue;
		Matcher matcher = placeholderPattern.matcher(defaultValue);
		while (matcher.find()) {
			placeholders.add(matcher.group());
		}
	}
	
	protected void setFormatted(String formatted) {
		this.formatted = formatted;
	}
	
	/**
	 * @return The formatted value of this Message
	 */
	public String getFormattedValue() {
		return formatted;
	}
	
	/**
	 * @return The unformatted value of this Message
	 */
	public String getValue() {
		return value;
	}
	
	/**
	 * @return The default unformatted value of this Message
	 */
	public String getDefaultValue() {
		return defaultValue;
	}
	
	/**
	 * @return All placeholders that were found in this Message
	 */
	public List<String> getPlaceholders() {
		return placeholders;
	}
	
}
