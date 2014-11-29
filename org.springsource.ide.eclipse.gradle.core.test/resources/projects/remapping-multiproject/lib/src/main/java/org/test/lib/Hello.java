package org.test.lib;

import org.test.sublib.SubHello;

public class Hello {
	
	public static void sayHello(String name) {
		SubHello.hello(name);
	}

}
