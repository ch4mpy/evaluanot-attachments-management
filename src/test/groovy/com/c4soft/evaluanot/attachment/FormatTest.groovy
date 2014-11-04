package com.c4soft.evaluanot.attachment;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import org.junit.Test;

class FormatTest {

	@Test
	public void testCompareTo() {
		Format format = new Format('format', 300, 400);
		assertEquals(0, format.compareTo(new Format('format', 300, 400)));
		assertEquals(0, format.compareTo(new Format('format')));
		assertThat(format.compareTo(null), is(greaterThan(0)));
		assertThat(format.compareTo(new Format('a')), is(greaterThan(0)));
		assertThat(format.compareTo(new Format('z')), is(lessThan(0)));
	}

	@Test
	public void testEqualsObject() {
		Format format = new Format('format', 300, 400);
		assertTrue(format.equals(new Format('format', 300, 400)));
		assertTrue(format.equals(new Format('format')));
		assertFalse(format.equals(null));
		assertFalse(format.equals(new Format('a')));
		assertFalse(format.equals(new Format('z')));
	}

}
