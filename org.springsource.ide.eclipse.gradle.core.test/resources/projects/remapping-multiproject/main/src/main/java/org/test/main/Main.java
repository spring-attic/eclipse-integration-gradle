package org.test.main;

import org.apache.commons.collections.buffer.CircularFifoBuffer;
import org.test.lib.Hello;
import org.test.sublib.SubHello;

public class Main {
	
	public static void main(String[] args) {
		new CircularFifoBuffer();
		Hello.sayHello("Kris");
		SubHello.hello("Subby");
	}

}