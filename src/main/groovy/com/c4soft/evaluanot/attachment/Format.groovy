package com.c4soft.evaluanot.attachment


class Format implements Comparable<Format>, Serializable {
	final String name;
	final Integer maxWidth;
	final Integer maxHeight;

	public Format(String name, Integer maxWidth = null, Integer maxHeight = null) {
		if(!name) {
			throw new AttachmentPersistenceException("name can't be null or empty");
		}
		this.name = name;
		this.maxWidth = maxWidth;
		this.maxHeight = maxHeight;
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public int compareTo(Format other) {
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

		if(!maxWidth && other.maxWidth) {
			return -1;
		} else if(maxWidth) {
			int widthComp = maxWidth.compareTo(other.maxWidth);
			if(widthComp != 0) {
				return widthComp;
			}
		}

		if(!maxHeight && other.maxHeight) {
			return -1;
		} else if(maxHeight) {
			return maxHeight.compareTo(other.maxHeight);
		}
		return 0;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((maxHeight == null) ? 0 : maxHeight.hashCode());
		result = prime * result + ((maxWidth == null) ? 0 : maxWidth.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Format other = (Format) obj;
		return compareTo(other) == 0;
	}
}
