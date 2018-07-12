package com.bitiknow.indyservice.utils;

import org.apache.commons.io.FileUtils;

public class EnvironmentUtils {
	static String getTestPoolIP() {
		String testPoolIp = System.getenv("TEST_POOL_IP");
		return testPoolIp != null ? testPoolIp : "192.168.99.100";
	}

	public static String getIndyHomePath() {
		return FileUtils.getUserDirectoryPath() + "/.indy_client/";
	}

	public static String getIndyHomePath(String filename) {
		return getIndyHomePath() + filename;
	}

	static String getTmpPath() {
		return FileUtils.getTempDirectoryPath() + "/indy/";
	}

	static String getTmpPath(String filename) {
		return getTmpPath() + filename;
	}
}