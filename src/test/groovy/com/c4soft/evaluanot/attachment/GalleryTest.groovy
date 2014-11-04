package com.c4soft.evaluanot.attachment;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import org.junit.Test;

class GalleryTest {

	@Test
	public void testCompareTo() {
		Gallery a = new Gallery('ab', (Set<Format>)[new Format('fullsize')]);
		assertEquals(0, a.compareTo(a));
		assertEquals(0, a.compareTo(new Gallery('ab', (Set<Format>)[new Format('fullsize')])));
		assertThat(a.compareTo(new Gallery('aa', (Set<Format>)[new Format('fullsize')])), is(greaterThan(0)));
		assertThat(a.compareTo(new Gallery('ac', (Set<Format>)[new Format('fullsize')])), is(lessThan(0)));
		assertThat(a.compareTo(new Gallery('ab', (Set<Format>)[])), is(greaterThan(0)));
		assertThat(a.compareTo(new Gallery('ab', (Set<Format>)[new Format('fullsize'), new Format('grid')])), is(lessThan(0)));
		assertThat(a.compareTo(null), is(greaterThan(0)));
	}

	@Test
	public void testEqualsObject() {
		Gallery a = new Gallery('ab', (Set<Format>)[new Format('fullsize')]);
		assertTrue(a.equals(a));
		assertTrue(a.equals(new Gallery('ab', (Set<Format>)[new Format('fullsize')])));
		assertFalse(a.equals(new Gallery('aa', (Set<Format>)[new Format('fullsize')])));
		assertFalse(a.equals(new Gallery('ac', (Set<Format>)[new Format('fullsize')])));
		assertFalse(a.equals(new Gallery('ac', (Set<Format>)[new Format('fullsize'), new Format('grid')])));
		assertFalse(a.equals(new Gallery('ac', (Set<Format>)[])));
		assertFalse(a.equals(null));
	}

}
