package com.c4soft.evaluanot.attachment

import groovy.io.FileType;

import java.io.File
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Ch4mp
 *
 */
class AttachmentRepositoryImpl implements AttachmentRepository {
	/*
	 * First group is a column position
	 * Second group is a row position
	 * Third group is attachment label
	 * Last group is file extension
	 */
	private static final Pattern REPO_FILE_NAME_PATTERN = ~/^(\d)+_(\d)+_([\w\séèêàçù_\-\.]+)\.(\w+)$/;
	
	private static final Pattern REPO_FILE_LABEL_PATTERN = ~/^[\w\séèêàçù_\-\.]+$/;
	
	private static final Pattern INPUT_FILE_NAME_PATTERN = ~/^(.+)\.(\w+)$/;
	
	final File rootDirectory;
	
	public AttachmentRepositoryImpl(File rootDirectory) {
		rootDirectory.mkdirs();
		this.rootDirectory = rootDirectory;
	}

	public Map<Integer, Map<Integer, Attachment>> findByOwnerAndCollectionTypeMapByColumnAndRow(long officeId, long missionId, long bienId, CollectionType collectionType) throws IllegalArgumentException {
		Map<Integer, Map<Integer, List<Attachment>>> attachmentColumns = [:];
		File ownerDir = new File(rootDirectory, Long.toString(officeId));
		File missionDir = new File(ownerDir, Long.toString(missionId));
		File propertyDir = new File(missionDir, Long.toString(bienId));
		File collectionDir = new File(propertyDir, collectionType.value);
		
		collectionDir.mkdirs();
		collectionDir.eachFile(FileType.FILES, {
			Matcher matcher = REPO_FILE_NAME_PATTERN.matcher(it.name);
			if(matcher.matches()) {
				Integer column = Integer.valueOf(matcher[0][1]);
				Integer row = Integer.valueOf(matcher[0][2]);
				if(!attachmentColumns[column]) {
					attachmentColumns[column] = [:];
				}
				if(!attachmentColumns[column][row]) {
					attachmentColumns[column][row] = [];
				}
				Attachment attachment = new Attachment(officeId, missionId, bienId, collectionType, matcher[0][3], column, row, matcher[0][4])
				System.out.println('attachmentColumns[' + column + '][' + row + '] << ' + attachment);
				attachmentColumns[column][row] << attachment;
			}
		});
	
		return cleanCollection(attachmentColumns);
	}

	public Map<Integer, Map<Integer, Attachment>> create(File file, long officeId, long missionId, long bienId, CollectionType collectionType, String label, int column, int row) throws IllegalArgumentException, AttachmentPersistenceException {
		if(!file?.isFile()) {
			throw new IllegalArgumentException('provided file ' + file?.name + ' is not valid');
		}
		Matcher inputMatcher = INPUT_FILE_NAME_PATTERN.matcher(file.name);
		if(!inputMatcher.matches()) {
			throw new IllegalArgumentException('provided file name must be composed of a name and an extension');
		}
		if(!REPO_FILE_LABEL_PATTERN.matcher(label).matches()) {
			throw new IllegalArgumentException('label can only be composed of latin1 characters, spaces, digits, underscores, dashes and dots');
		}
		
		Attachment attachment = new Attachment(officeId, missionId, bienId, collectionType, label, column, row, inputMatcher[0][2]);
		return insert(file, attachment);
	}

	public Map<Integer, Map<Integer, Attachment>> delete(Attachment attachment) {
		Map<Integer, Map<Integer, Attachment>> columns = findByOwnerAndCollectionTypeMapByColumnAndRow(attachment.officeId, attachment.missionId, attachment.bienId, attachment.collectionType);
		for(int i = attachment.displayRow; i < columns[attachment.displayColumn].size() - 1; i++) {
			Attachment shifted = columns[attachment.displayColumn][i+1];
			columns[attachment.displayColumn][i] = new Attachment(attachment.officeId, attachment.missionId, attachment.bienId, shifted.collectionType, shifted.label, shifted.displayColumn, i, shifted.fileExtension);
			Files.move(path(shifted), path(columns[attachment.displayColumn][i]));
		}
		columns[attachment.displayColumn].remove(columns[attachment.displayColumn].size() - 1);
		Files.delete(path(attachment));
		return columns;
	}

	public Map<Integer, Map<Integer, Attachment>> move(Attachment attachment, int newColumn, int newRow) throws AttachmentPersistenceException {
		insert(getContent(attachment), new Attachment(attachment.officeId, attachment.missionId, attachment.bienId, attachment.collectionType, attachment.label, newColumn, newRow, attachment.fileExtension));
		delete(attachment);
		
		return findByOwnerAndCollectionTypeMapByColumnAndRow(attachment.officeId, attachment.missionId, attachment.bienId, attachment.collectionType);
	}

	public Attachment rename(Attachment attachment, String newLabel) throws AttachmentPersistenceException {
		if(!newLabel) {
			throw new IllegalArgumentException("new name can't be empty");
		}
		
		Attachment newAttachment = new Attachment(attachment.officeId, attachment.missionId, attachment.bienId, attachment.collectionType, newLabel, attachment.displayColumn, attachment.displayRow, attachment.fileExtension);
		
		moveFile(path(attachment), path(newAttachment));
		return newAttachment;
	}

	public File getContent(Attachment attachment) {
		File file = new File(path(attachment).toString());
		if(!file.isFile()) {
			throw new AttachmentPersistenceException('An error occured while accessing ' + file.absolutePath);
		}
		return file
	}
	
	public Path path(Attachment attachment) throws AttachmentPersistenceException {
		String repoFileName = attachment.displayColumn + '_' + attachment.displayRow + '_' + attachment.label + '.' + attachment.fileExtension;
		return FileSystems.getDefault().getPath(rootDirectory.absolutePath, Long.toString(attachment.officeId), Long.toString(attachment.missionId), Long.toString(attachment.bienId), attachment.collectionType.value, repoFileName);
	}
	
	private Map<Integer, Map<Integer, Attachment>> insert(File file, Attachment attachment) throws AttachmentPersistenceException {
		Map<Integer, Map<Integer, Attachment>> columns = findByOwnerAndCollectionTypeMapByColumnAndRow(attachment.officeId, attachment.missionId, attachment.bienId, attachment.collectionType);
		for(int i = columns[attachment.displayColumn].size(); i > attachment.displayRow; i--) {
			Attachment shifted = columns[attachment.displayColumn][i-1];
			columns[attachment.displayColumn][i] = new Attachment(shifted.officeId, shifted.missionId, shifted.bienId, shifted.collectionType, shifted.label, shifted.displayColumn, i, shifted.fileExtension);
			moveFile(path(columns[attachment.displayColumn][i-1]),path(columns[attachment.displayColumn][i]));
		}
		
		copyFile(file.toPath(), path(attachment));
		columns[attachment.displayColumn][attachment.displayRow] = attachment;
		
		return columns;
	}

	private Map<Integer, Map<Integer, Attachment>> cleanCollection(Map<Integer, Map<Integer, List<Attachment>>> dirty) throws AttachmentPersistenceException {
		Map<Integer, Map<Integer, Attachment>> clean = [:];
		dirty.each { columnNbr, lines ->
			lines.eachWithIndex { lineNbr, attachments, lineIndex ->
				attachments.eachWithIndex  { attachment, i ->
					if(!clean[columnNbr]) {
						clean[columnNbr] = [:];
					}
					if(columnNbr != attachment.displayColumn || lineIndex + i != attachment.displayRow) {
						Attachment moved = new Attachment(attachment.officeId, attachment.missionId, attachment.bienId, attachment.collectionType, attachment.label, columnNbr, lineIndex + i, attachment.fileExtension);
						moveFile(path(attachment), path(moved));
						clean[columnNbr][lineIndex + i] = moved;
					} else {
						clean[columnNbr][lineIndex + i] = attachment;
					}
				}
			}
		}
		return clean;
	}
	
	private void copyFile(fromPath, toPath) throws AttachmentPersistenceException {
		try {
			Files.copy(fromPath, toPath);
		} catch(Throwable t) {
			throw new AttachmentPersistenceException('An error occured while reading from ' + fromPath + ' or writing to ' + toPath, t);
		}
	}
	
	private void moveFile(fromPath, toPath) throws AttachmentPersistenceException {
		try {
			Files.move(fromPath, toPath);
		} catch(Throwable t) {
			throw new AttachmentPersistenceException('An error occured while reading from ' + fromPath + ' or writing to ' + toPath, t);
		}
	}
}
