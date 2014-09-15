package com.c4soft.evaluanot.attachment

import groovy.json.JsonBuilder;

/**
 * @author Ch4mp
 * Evalu@not attached document properties
 */
class Attachment {
	
	final long officeId;
	
	final long missionId;
	
	final long bienId;

	final CollectionType collectionType;
	
	final String label;
	
	final int displayRow;
	
	final int displayColumn;
	
	final String fileExtension;

	public Attachment(long officeId, long missionId, long bienId, CollectionType collectionType, String label, int displayColumn, int displayRow, String fileExtension) {
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
	    result = prime * result + displayColumn;
	    result = prime * result + displayRow;
	    result = prime * result + ((fileExtension == null) ? 0 : fileExtension.hashCode());
	    result = prime * result + ((label == null) ? 0 : label.hashCode());
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
	    Attachment other = (Attachment) obj;
	    if (officeId != other.officeId) {
		    return false;
	    }
	    if (missionId != other.missionId) {
		    return false;
	    }
	    if (bienId != other.bienId) {
		    return false;
	    }
	    if (collectionType != other.collectionType) {
		    return false;
	    }
		if (displayColumn != other.displayColumn) {
		    return false;
		}
		if (displayRow != other.displayRow) {
		    return false;
		}
		if (fileExtension == null) {
		    if (other.fileExtension != null) {
			    return false;
		    }
	    } else if (!fileExtension.equals(other.fileExtension)) {
		    return false;
	    }
	    if (label == null) {
		    if (other.label != null) {
			    return false;
		    }
	    } else if (!label.equals(other.label)) {
		    return false;
	    }
	    return true;
    }

	@Override
	public String toString() {
		return new JsonBuilder(this).toPrettyString();
	}
	
}
