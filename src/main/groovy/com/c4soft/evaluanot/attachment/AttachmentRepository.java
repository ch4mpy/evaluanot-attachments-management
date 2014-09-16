package com.c4soft.evaluanot.attachment;

import java.io.File;
import java.util.Map;

/**
 * @author Ch4mp
 * Handle Evalu@not attached documents on a file system
 */
public interface AttachmentRepository {
	
	/**
	 * Retrieves all attachments from a specific collection
	 * @param officeId
	 * @param missionId
	 * @param bienId
	 * @param collectionType
	 * @return all attachments from specified collection mapped by display column and display row
	 */
	Map<Integer, Map<Integer, Attachment>> findByOfficeIdAndMissionIdAndBienIdAndCollectionTypeMapByColumnAndRow(long officeId, long missionId, long bienId, CollectionType collectionType) throws IllegalArgumentException;
	
	/**
	 * Adds an attachment to the repository
	 * @param file
	 * @param officeId
	 * @param missionId
	 * @param bienId
	 * @param collectionType
	 * @param label
	 * @param column
	 * @param row
	 * @return updated collection of attachments
	 */
	Map<Integer, Map<Integer, Attachment>> create(File file, long officeId, long missionId, long bienId, CollectionType collectionType, String label, int column, int row) throws IllegalArgumentException, AttachmentPersistenceException;
	
	/**
	 * Removes an attachment from the repository
	 * @param attachment
	 * @return updated collection of attachments
	 */
	Map<Integer, Map<Integer, Attachment>> delete(Attachment attachment) throws IllegalArgumentException;
	
	/**
	 * Removes an attachment from current position and inserts it at new position (other attachments from the same collection will be moved accordingly)
	 * @param attachment
	 * @param newColumn
	 * @param newRow
	 * @return reordered collection of attachments
	 */
	Map<Integer, Map<Integer, Attachment>> move(Attachment attachment, int newColumn, int newRow) throws IllegalArgumentException, AttachmentPersistenceException;
	
	/**
	 * Modify the label of an attachment
	 * @param attachment
	 * @param newLabel
	 * @return renamed attachment
	 */
	Attachment rename(Attachment attachment, String newLabel) throws IllegalArgumentException, AttachmentPersistenceException;
	
	/**
	 * Retrieve the actual attached file from the file system
	 * @param attachment
	 * @return actual attached file from the file system
	 */
	File getContent(Attachment attachment) throws IllegalArgumentException, AttachmentPersistenceException;
}
