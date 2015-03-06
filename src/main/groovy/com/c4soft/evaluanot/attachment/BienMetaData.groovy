package com.c4soft.evaluanot.attachment

import groovy.json.JsonSlurper

import java.util.regex.Matcher
import java.util.regex.Pattern;


class BienMetaData extends MandateMetaData {
	
	final Map<String, Map<Integer, Map<Integer, String>>> labelsByGalleryNameAndColumnAndRow = [:];
	
	public String getLabel(String galleryName, Integer column, Integer row) {
		String label = labelsByGalleryNameAndColumnAndRow.get(galleryName)?.get(column)?.get(row);
		if(!label) {
			label = column + '_' + row;
		}
		return label;
	}
	
	public void setLabel(String galleryName, int column, int row, String label) {
		if(!galleryName) {
			throw new IllegalArgumentException('gallery must be not null and not empty');
		}
		
		if(labelsByGalleryNameAndColumnAndRow[galleryName] == null) {
			labelsByGalleryNameAndColumnAndRow[galleryName] = [:];
		}
		if(labelsByGalleryNameAndColumnAndRow[galleryName][column] == null) {
			labelsByGalleryNameAndColumnAndRow[galleryName][column] = [:];
		}
		
		if(label == null) {
			labelsByGalleryNameAndColumnAndRow[galleryName][column].remove(row);
		} else {
			labelsByGalleryNameAndColumnAndRow[galleryName][column][row] = label;
		}
	}

	public static BienMetaData parseMetaDataFile(File metaDataFile) {
		BienMetaData metaData;

		if(metaDataFile.isFile()) {
			Object raughData = new JsonSlurper().parse(metaDataFile);
			Attachment cover = MandateMetaData.parseCover(raughData);
			metaData = new BienMetaData(cover: cover);
			raughData.labelsByGalleryNameAndColumnAndRow?.each { galleryName, cols ->
				cols?.each { col, rows ->
					rows?.each { row, label ->
						if(galleryName != null && col != null && row != null) {
							metaData.setLabel((String)galleryName, Integer.valueOf(col), Integer.valueOf(row), (String)label);
						}
					}
				}
			}
		} else {
			metaData = new BienMetaData();
		}

		return metaData;
	}
}
