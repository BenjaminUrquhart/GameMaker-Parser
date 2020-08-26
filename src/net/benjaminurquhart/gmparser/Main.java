package net.benjaminurquhart.gmparser;

import java.io.File;

public class Main {

	public static void main(String[] args) {
		GMDataFile data = new GMDataFile(new File(args[0]));
		System.out.println(data);
		
		data.getObjects().forEach(System.out::println);
	}
}
