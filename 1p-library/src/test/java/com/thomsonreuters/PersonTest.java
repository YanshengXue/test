package com.thomsonreuters;

import org.junit.Test;

import com.thomsonreuters.Person;

import static org.junit.Assert.*;

public class PersonTest {
    @Test
    public void canConstructAPersonWithAName() {
        Person person = new Person("Larry");
        assertEquals("Larry", person.getName());
    }
}
