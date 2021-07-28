package com.mycompany.mediacodec;
import java.io.*;

public class MyLog
{
	private static FileWriter wr;
	public static void openLogFile(String path) {
		try {
			wr = new FileWriter(path);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}
	public static void i(String key,String msg) {
		try {
			wr.append("E " + key + ": " + msg + "\n");
		} catch (IOException e) {}
	}
	public static void close() {
		try {
			wr.close();
		} catch (IOException e) {}
	}
}
