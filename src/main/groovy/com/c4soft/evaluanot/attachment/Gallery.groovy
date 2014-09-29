package com.c4soft.evaluanot.attachment

class Gallery implements Comparable<Gallery>{
	final String name;
	
	public Gallery(String name) {
		if(!name) {
			throw new AttachmentPersistenceException("name can't be null or empty");
		}
		this.name = name;
	}

	@Override
	public int hashCode() {
		return name?.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if(!obj) {
			return false;
		}
		return name == obj.toString();
	}

	@Override
	public String toString() {
		return name;
	}
	
	@Override
	public int compareTo(Gallery other) {
		if(!other) {
			return 1;
		}
		if(!name && other.name) {
			return -1;
		}
		return name.compareTo(other.name);
	}
	
}
