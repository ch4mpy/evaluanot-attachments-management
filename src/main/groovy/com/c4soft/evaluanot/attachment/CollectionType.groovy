package com.c4soft.evaluanot.attachment;

/**
 * @author Ch4mp
 *
 */
enum CollectionType {
	PHOTO('photo'),
	CADASTRE('cadastre'),
	IGN('ign'),
	GEOFONCIER('geofoncier'),
	AUTRE('autre')
	
	final String value;
	
	public CollectionType(String value) {
		this.value = value;
	}
}
