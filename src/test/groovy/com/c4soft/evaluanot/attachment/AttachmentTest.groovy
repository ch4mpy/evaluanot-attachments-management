package com.c4soft.evaluanot.attachment;

import static org.hamcrest.Matchers.*
import static org.junit.Assert.*;
import groovy.json.JsonSlurper;

import org.junit.Test;

class AttachmentTest {

	private static final Gallery PHOTOS = new Gallery('photos');

	@Test
	public void testHashCode() {
		Attachment attachment = new Attachment(4001L, 51L, 69L, PHOTOS, 'image', 0, 1, 'jpg');
		assertThat(attachment.hashCode(), is(-1239557292));
	}

	@Test
	public void testEquals() {
		Attachment attachment = new Attachment(4001L, 51L, 69L, PHOTOS, 'image', 0, 1, 'jpg');
		assertThat(attachment.equals(null), is(false));
		
		JsonSlurper json = new JsonSlurper();
		assertThat(attachment.equals(json.parseText('{"officeId": 4001, "missionId": 51, "bienId": 69, "collectionType": "photos", "label": "image", "displayRow": 0, "displayColumn": 1, "fileExtension": "jpg"}')), is(false));
		
		assertThat(attachment.equals(new Attachment(4001L, 51L, 69L, PHOTOS, 'image', 0, 1, 'jpg')), is(true));
	}
	
	@Test
	public void testCompareTo() {
		Attachment attachment = new Attachment(4001L, 51L, 69L, PHOTOS, 'image', 1, 2, 'jpg');
		
		assertThat(attachment.compareTo(null), is(greaterThan(0)));
		
		assertThat(attachment.compareTo(new Attachment(4001L, 51L, 69L, PHOTOS, 'image', 1, 2, 'jpg')), is(0));
		
		assertThat(attachment.compareTo(new Attachment(4000L, 99L, 99L, new Gallery('z'), 'A', 99, 99, 'A')), is(greaterThan(0)));
		assertThat(attachment.compareTo(new Attachment(4002L, 0L, 0L, new Gallery('a'), '0', 0, 0, '0')), is(lessThan(0)));
		
		assertThat(attachment.compareTo(new Attachment(4001L, 50L, 99L, new Gallery('z'), 'A', 99, 99, 'A')), is(greaterThan(0)));
		assertThat(attachment.compareTo(new Attachment(4001L, 52L, 0L, new Gallery('a'), '0', 0, 0, '0')), is(lessThan(0)));
		
		assertThat(attachment.compareTo(new Attachment(4001L, 51L, 0L, new Gallery('z'), 'A', 99, 99, 'A')), is(greaterThan(0)));
		assertThat(attachment.compareTo(new Attachment(4001L, 51L, 99L, new Gallery('a'), '0', 0, 0, '0')), is(lessThan(0)));
		
		assertThat(attachment.compareTo(new Attachment(4001L, 51L, 69L, new Gallery('photo'), 'A', 99, 99, 'A')), is(greaterThan(0)));
		assertThat(attachment.compareTo(new Attachment(4001L, 51L, 69L, new Gallery('z'), '0', 0, 0, '0')), is(lessThan(0)));
		
		assertThat(attachment.compareTo(new Attachment(4001L, 51L, 69L, PHOTOS, 'image', 0, 99, 'A')), is(greaterThan(0)));
		assertThat(attachment.compareTo(new Attachment(4001L, 51L, 69L, PHOTOS, 'image', 99, 0, '0')), is(lessThan(0)));
		
		assertThat(attachment.compareTo(new Attachment(4001L, 51L, 69L, PHOTOS, 'image', 1, 0, 'A')), is(greaterThan(0)));
		assertThat(attachment.compareTo(new Attachment(4001L, 51L, 69L, PHOTOS, 'image', 1, 99, '0')), is(lessThan(0)));
		
		assertThat(attachment.compareTo(new Attachment(4001L, 51L, 69L, PHOTOS, 'a', 1, 2, 'A')), is(greaterThan(0)));
		assertThat(attachment.compareTo(new Attachment(4001L, 51L, 69L, PHOTOS, 'z', 1, 2, '0')), is(lessThan(0)));
		
		assertThat(attachment.compareTo(new Attachment(4001L, 51L, 69L, PHOTOS, 'image', 1, 2, 'a')), is(greaterThan(0)));
		assertThat(attachment.compareTo(new Attachment(4001L, 51L, 69L, PHOTOS, 'image', 1, 2, 'z')), is(lessThan(0)));
	}
	
	@Test
	public void testToString() {
		assertThat(new Attachment(4001L, 51L, 69L, PHOTOS, 'image', 1, 2, 'jpg').toString(), is(/{
    "bienId": 69,
    "officeId": 4001,
    "displayRow": 2,
    "gallery": {
        "name": "photos"
    },
    "label": "image",
    "displayColumn": 1,
    "missionId": 51,
    "fileExtension": "jpg"
}/));
	}
}
