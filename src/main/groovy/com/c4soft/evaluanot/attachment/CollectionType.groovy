package com.c4soft.evaluanot.attachment;

/**
 * @author Ch4mp
 *
 */
enum CollectionType {
	MAP('maps'),
	PICTURE('pictures'),
	REPORT('reports')
	
	final String value;
	
	public CollectionType(String value) {
		this.value = value;
	}
}
