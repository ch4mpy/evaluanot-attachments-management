package com.c4soft.evaluanot.attachment

import groovy.json.JsonSlurper

import java.util.regex.Matcher
import java.util.regex.Pattern;


class MandateMetaData implements Serializable {

	Attachment cover

	public static MandateMetaData parseMetaDataFile(File metaDataFile) {
		MandateMetaData metaData;

		if(metaDataFile.isFile()) {
			Attachment cover = parseCover(new JsonSlurper().parse(metaDataFile));
			metaData = new MandateMetaData(cover: cover);
		} else {
			metaData = new MandateMetaData();
		}

		return metaData;
	}

	public static Attachment parseCover(Object raughData) {
		Attachment cover;
		try {
			Collection<Map> formats = raughData.cover.gallery.formats;
			Set<Format> formatsSet = new HashSet<Format>(formats.size());
			for(Map o : formats) {
				formatsSet << new Format(o.name, o.maxWidth, o.maxHeight);
			}
			String id = raughData.cover?.id
			if(!id) {
				id = UUID.randomUUID().toString();
			}
			cover = new Attachment(
					raughData.cover.officeId,
					raughData.cover.mandateId,
					raughData.cover.bienId,
					new Gallery(raughData.cover.gallery.name, formatsSet),
					id,
					raughData.cover.label,
					raughData.cover.displayColumn,
					raughData.cover.displayRow,
					raughData.cover.fileExtension);
		} catch(Throwable t) {
			cover = null;
		}

		return cover;
	}
}
