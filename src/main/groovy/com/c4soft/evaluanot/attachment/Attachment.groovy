package com.c4soft.evaluanot.attachment

import groovy.json.JsonBuilder

/**
 * @author Ch4mp
 * Evalu@not attached document properties
 */
class Attachment implements Comparable<Attachment> {
	
	final long officeId;
	
	final long missionId;
	
	final long bienId;

	final String collectionType;
	
	final int displayRow;
	
	final int displayColumn;
	
	final String label;
	
	final String fileExtension;

	public Attachment(long officeId, long missionId, long bienId, String collectionType, String label, int displayColumn, int displayRow, String fileExtension) {
	    super();
	    this.officeId = officeId;
	    this.missionId = missionId;
	    this.bienId = bienId;
	    this.collectionType = collectionType;
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
	    result = prime * result + missionId;
	    result = prime * result + bienId;
	    result = prime * result + ((collectionType == null) ? 0 : collectionType.hashCode());
	    result = prime * result + displayRow;
	    result = prime * result + displayColumn;
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
		if(missionId > o.missionId) {
			return 1;
		}
		if(missionId < o.missionId) {
			return -1;
		}
		if(bienId > o.bienId) {
			return 1;
		}
		if(bienId < o.bienId) {
			return -1;
		}
		if(collectionType != o.collectionType) {
			if(collectionType) {
				return collectionType.compareTo(o.collectionType);
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
