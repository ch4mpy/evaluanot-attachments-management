package com.c4soft.evaluanot.attachment;

import java.io.File;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * @author Ch4mp Handle Evalu@not attached documents on a file system
 */
public interface AttachmentRepository {

	/**
	 * Retrieves all attachments from a specific collection
	 * 
	 * @param officeId
	 * @param mandateId
	 * @param bienId
	 * @param gallery
	 *            should be an enumerated value
	 * @return all attachments from specified collection mapped by display
	 *         column and display row
	 */
	Map<Integer, Map<Integer, Entry<Attachment, Set<Format>>>> findByOfficeIdAndMissionIdAndBienIdAndGalleryMapByColumnAndRow(long officeId, long mandateId,
			long bienId, Gallery gallery) throws IllegalArgumentException;

	/**
	 * Adds an attachment to the repository. Provided files are copied into the
	 * repo (and not moved)., so you might want to delete input file after
	 * adding it to the repo.
	 * 
	 * @param fileByFormat
	 *            file in different formats (i.e. fullsize, thumbnail, ...)
	 * @param officeId
	 * @param mandateId
	 * @param bienId
	 * @param gallery
	 *            should be an enumerated value
	 * @param label
	 * @param column
	 * @param row
	 * @return updated collection of attachments mapped by display
	 *         column and display row
	 */
	Map<Integer, Map<Integer, Entry<Attachment, Set<Format>>>> create(Map<Format, File> fileByFormat, long officeId, long mandateId, long bienId,
			Gallery gallery, String label, int column, int row) throws IllegalArgumentException, AttachmentPersistenceException;

	/**
	 * Removes an attachment from the repository
	 * 
	 * @param attachment
	 * @return updated collection of attachments
	 */
	Map<Integer, Map<Integer, Entry<Attachment, Set<Format>>>> delete(Attachment attachment) throws IllegalArgumentException;

	/**
	 * Removes an attachment from current position and inserts it at new
	 * position (other attachments from the same collection will be moved
	 * accordingly)
	 * 
	 * @param attachment
	 * @param newColumn
	 * @param newRow
	 * @return reordered collection of attachments mapped by display
	 *         column and display row
	 */
	Map<Integer, Map<Integer, Entry<Attachment, Set<Format>>>> move(Attachment attachment, int newColumn, int newRow) throws IllegalArgumentException,
			AttachmentPersistenceException;

	/**
	 * Modify the label of an attachment
	 * 
	 * @param attachment
	 * @param newLabel
	 * @return renamed attachment
	 */
	Attachment rename(Attachment attachment, String newLabel) throws IllegalArgumentException, AttachmentPersistenceException;

	/**
	 * Retrieve actual attached files from the file system (indexed by format:
	 * fullsize, thumbnail, ...).
	 * 
	 * @param attachment
	 * @return actual attached file from the file system
	 */
	Map<Format, File> getContentByFormat(Attachment attachment) throws IllegalArgumentException, AttachmentPersistenceException;

	/**
	 * Retrieve the servlet path to access the resource (indexed by format:
	 * fullsize, thumbnail, ...).
	 * 
	 * @param attachment
	 * @return actual attached file servlet path
	 */
	Map<Format, String> getServletPathByFormat(Attachment attachment) throws IllegalArgumentException, AttachmentPersistenceException;

	/**
	 * Set which attached document should be the report cover for a bien
	 * 
	 * @param officeId
	 * @param mandateId
	 * @param bienId
	 * @param attachment
	 */
	void setBienCover(long officeId, long mandateId, long bienId, Attachment attachment);

	/**
	 * @param officeId
	 * @param mandateId
	 * @param bienId
	 * @return report cover as previously set for a bien
	 */
	Attachment getBienCover(long officeId, long mandateId, long bienId);

	/**
	 * Set which attached document should be the report cover for a mandate
	 * 
	 * @param officeId
	 * @param mandateId
	 * @param attachment
	 */
	void setMandateCover(long officeId, long mandateId, Attachment attachment);

	/**
	 * @param officeId
	 * @param mandateId
	 * @return report cover as previously set for a mandate
	 */
	Attachment getMandateCover(long officeId, long mandateId);
}
