package com.c4soft.evaluanot.attachment



class Gallery implements Comparable<Gallery>, Serializable {
	final String name;

	final Set<Format> formats;

	public Gallery(String name, Set<Format> formats) {
		if(!name) {
			throw new AttachmentPersistenceException("name can't be null or empty");
		}
		this.name = name;
		this.formats = Collections.unmodifiableSet(formats);
	}

	public Set<Format> getFormats() {
		return formats;
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
		int nameComp = name.compareTo(other.name);
		if(nameComp != 0) {
			return nameComp;
		}

		if(!formats && other.formats) {
			return -1;
		} else if(formats) {
			if(!other.formats) {
				return 1;
			}
			if(formats.size() > other.formats.size()) {
				return 1;
			} else if(formats.size() < other.formats.size()) {
				return -1;
			} else {
				for(Format f : formats) {
					if(!other.formats.contains(f)) {
						return 1;
					}
				}
			}
		}

		return 0;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		for(Format f : formats) {
			result = prime * result + ((f == null) ? 0 : f.hashCode());
		}
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if(!obj || !Gallery.isAssignableFrom(obj.class)) {
			return false;
		}
		return compareTo(obj) != 0;
	}
}
