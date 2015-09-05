/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.za.gearman.worker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author phamdung
 */
public class UrlReferalUtil {

	private static final List<String> listSEngine = new ArrayList<>();

	private static Pattern pattern = Pattern.compile("//([\\w\\.]+)/");

	public UrlReferalUtil() {

	}

	static {
		listSEngine.add("www.google.com");
		listSEngine.add("www.bing.com");
		listSEngine.add("vn.yahoo.com");

	}

	public static String[] referalParse(String URL) {
		String[] returns = { "null", "0" };
		if ("null".equals(URL) || URL == null) {
			return returns;
		}
		Matcher matcher = pattern.matcher(URL);
		if (matcher.find()) {
			String s = matcher.group(1);
			if (listSEngine.contains(s)) {
				returns[0] = s;
				returns[1] = "2";
				return returns;
			} else {
				returns[0] = s;
				returns[1] = "1";
				return returns;
			}
		}
		return returns;

	}

	public static void main(String[] args) {
		System.out.println(Arrays.toString(referalParse("null")));
	}
}
