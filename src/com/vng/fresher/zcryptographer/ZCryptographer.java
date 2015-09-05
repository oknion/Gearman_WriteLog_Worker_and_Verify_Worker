package com.vng.fresher.zcryptographer;

public class ZCryptographer {
	private static final String key = "az";

	public static String tranform(String message) {
		if (message == null)
			return null;
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < message.length(); i++) {
			result.append((char) (message.charAt(i) ^ key.charAt(i % key.length())));
		}
		return result.toString();
	}
}
