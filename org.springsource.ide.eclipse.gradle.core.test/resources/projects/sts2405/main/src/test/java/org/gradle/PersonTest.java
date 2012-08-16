package org.gradle;

import org.junit.Test;
import static org.junit.Assert.*;

public class PersonTest {
    @Test
    public void canConstructAPersonWithAName() {
    	CoolClass c = new CoolClass();
        Person person = new Person("Larry");
        assertEquals("Larry", person.getName());
    }
}
