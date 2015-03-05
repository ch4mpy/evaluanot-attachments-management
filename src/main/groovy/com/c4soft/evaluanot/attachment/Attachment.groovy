package com.c4soft.evaluanot.attachment

import groovy.json.JsonBuilder

/**
 * @author Ch4mp
 * Evalu@not attached document properties
 */
class Attachment implements Comparable<Attachment>, Serializable {
	
	final String id;

	final long officeId;

	final long mandateId;

	final long bienId;

	final Gallery gallery;

	final int displayRow;

	final int displayColumn;

	final String label;

	final String fileExtension;

	public Attachment(long officeId, long mandateId, long bienId, Gallery gallery, String id, String label, int displayColumn, int displayRow, String fileExtension) {
		super();
		this.id = id;
		this.officeId = officeId;
		this.mandateId = mandateId;
		this.bienId = bienId;
		this.gallery = gallery;
		this.label = label;
		this.displayColumn = displayColumn;
		this.displayRow = displayRow;
		this.fileExtension = fileExtension;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + officeId;
		result = prime * result + mandateId;
		result = prime * result + bienId;
		result = prime * result + ((gallery == null) ? 0 : gallery.hashCode());
		result = prime * result + displayRow;
		result = prime * result + displayColumn;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((label == null) ? 0 : label.hashCode());
		result = prime * result + ((fileExtension == null) ? 0 : fileExtension.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		return compareTo((Attachment) obj) == 0;
	}

	@Override
	public String toString() {
		return new JsonBuilder(this).toPrettyString();
	}

	@Override
	public int compareTo(Attachment o) {
		if(!o) {
			return 1;
		}
		if(officeId > o.officeId) {
			return 1;
		}
		if(officeId < o.officeId) {
			return -1;
		}
		if(mandateId > o.mandateId) {
			return 1;
		}
		if(mandateId < o.mandateId) {
			return -1;
		}
		if(bienId > o.bienId) {
			return 1;
		}
		if(bienId < o.bienId) {
			return -1;
		}
		if(gallery != o.gallery) {
			if(gallery) {
				return gallery.compareTo(o.gallery);
			} else {
				return -1;
			}
		}
		if(displayColumn > o.displayColumn) {
			return 1;
		}
		if(displayColumn < o.displayColumn) {
			return -1;
		}
		if(displayRow > o.displayRow) {
			return 1;
		}
		if(displayRow < o.displayRow) {
			return -1;
		}
		if(id != o.id) {
			if(id) {
				return id.compareTo(o.id);
			} else {
				return -1;
			}
		}
		if(label != o.label) {
			if(label) {
				return label.compareTo(o.label);
			} else {
				return -1;
			}
		}
		if(fileExtension != o.fileExtension) {
			if(fileExtension) {
				return fileExtension.compareTo(o.fileExtension);
			} else {
				return -1;
			}
		}
		return 0;
	}
}
