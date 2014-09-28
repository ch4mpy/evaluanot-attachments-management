package com.c4soft.evaluanot.attachment

import groovy.io.FileType

import java.io.File;
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher
import java.util.regex.Pattern

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

	public Map<Integer, Map<Integer,  Entry<Attachment, Set<String>>>> findByOfficeIdAndMissionIdAndBienIdAndCollectionTypeMapByColumnAndRow(long officeId, long missionId, long bienId, String collectionType) throws IllegalArgumentException {
		File ownerDir = new File(rootDirectory, Long.toString(officeId));
		File missionDir = new File(ownerDir, Long.toString(missionId));
		File propertyDir = new File(missionDir, Long.toString(bienId));
		File collectionDir = new File(propertyDir, collectionType);
		Map<String, Map<Attachment, Set<String>>> attachmentsByLabel = [:];

		collectionDir.mkdirs();
		collectionDir.eachDir { formatDir ->
			formatDir.eachFile(FileType.FILES) {
				Matcher matcher = REPO_FILE_NAME_PATTERN.matcher(it.name);
				if(matcher.matches()) {
					Integer column = Integer.valueOf(matcher[0][1]);
					Integer row = Integer.valueOf(matcher[0][2]);
					String label = matcher[0][3];
					String extension = matcher[0][4];
					Attachment attachment = new Attachment(officeId, missionId, bienId, collectionType, label, column, row, extension);

					if(!attachmentsByLabel[label]) {
						attachmentsByLabel[label] = [:];
					}
					if(!attachmentsByLabel[label][attachment]) {
						attachmentsByLabel[label][attachment] = [];
					}
					attachmentsByLabel[label][attachment] << formatDir.name;
				}
			}
		}

		return cleanCollection(attachmentsByLabel);
	}

	public Map<Integer, Map<Integer,  Entry<Attachment, Set<String>>>> create(Map<String, File> fileByFormat, long officeId, long missionId, long bienId, String collectionType, String label, int column, int row) throws IllegalArgumentException, AttachmentPersistenceException {
		if(! fileByFormat) {
			return findByOfficeIdAndMissionIdAndBienIdAndCollectionTypeMapByColumnAndRow(officeId, missionId, bienId, collectionType);
		}
		
		Map<Integer, Map<Integer,  Entry<Attachment, Set<String>>>> columns;
		fileByFormat.each { format, file ->
			columns = insert(format, file,  officeId, missionId, bienId, collectionType, label, column, row);
		}
		
		return columns;
	}

	private Map<Integer, Map<Integer,  Entry<Attachment, Set<String>>>> insert(String format, File file, long officeId, long missionId, long bienId, String collectionType, String label, int column, int row) throws IllegalArgumentException, AttachmentPersistenceException {
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
		return insert(format, file, attachment);
	}

	public Map<Integer, Map<Integer,  Entry<Attachment, Set<String>>>> delete(Attachment attachment) {
		Map<Integer, Map<Integer,  Entry<Attachment, Set<String>>>> columns = findByOfficeIdAndMissionIdAndBienIdAndCollectionTypeMapByColumnAndRow(attachment.officeId, attachment.missionId, attachment.bienId, attachment.collectionType);
		Set<String> formats = columns[attachment.displayColumn][attachment.displayRow].value;
		
		if(attachment != columns[attachment.displayColumn][attachment.displayRow].key) {
			return columns;
		}
		
		formats.each { format ->
			Files.delete(path(attachment, format));
		}
		
		for(int i = attachment.displayRow; i < columns[attachment.displayColumn].size() - 1; i++) {
			Entry<Attachment, Set<String>> shifted = columns[attachment.displayColumn][i+1];
			Attachment newAttachment = new Attachment(attachment.officeId, attachment.missionId, attachment.bienId, shifted.key.collectionType, shifted.key.label, shifted.key.displayColumn, i, shifted.key.fileExtension);
			columns[attachment.displayColumn][i] = [(newAttachment) : shifted.value].entrySet().first();
			shifted.value.each { format ->
			Files.move(path(shifted.key, format), path(newAttachment, format));
			}
		}
		columns[attachment.displayColumn].remove(columns[attachment.displayColumn].size() - 1);
		
		return columns;
	}

	public Map<Integer, Map<Integer,  Entry<Attachment, Set<String>>>> move(Attachment attachment, int newColumn, int newRow) throws AttachmentPersistenceException {
		Map<String, File> contentByFormat = getContentByFormat(attachment);
		contentByFormat.each { format, file ->
			insert(format, file, new Attachment(attachment.officeId, attachment.missionId, attachment.bienId, attachment.collectionType, attachment.label, newColumn, newRow, attachment.fileExtension));
		}
		delete(attachment);

		return findByOfficeIdAndMissionIdAndBienIdAndCollectionTypeMapByColumnAndRow(attachment.officeId, attachment.missionId, attachment.bienId, attachment.collectionType);
	}

	public Attachment rename(Attachment attachment, String newLabel) throws AttachmentPersistenceException {
		if(!newLabel) {
			throw new IllegalArgumentException("new name can't be empty");
		}

		Attachment newAttachment = new Attachment(attachment.officeId, attachment.missionId, attachment.bienId, attachment.collectionType, newLabel, attachment.displayColumn, attachment.displayRow, attachment.fileExtension);

		moveFiles(attachment, newAttachment, getContentByFormat(attachment).keySet());
		return newAttachment;
	}

	public Map<String, File> getContentByFormat(Attachment attachment) {
		Path collectionPath = collectionPath(attachment);
		return listFormats(new File(collectionPath.toString()), attachment);
	}

	private Path collectionPath(Attachment attachment) {
		if(!attachment) {
			return null;
		}
		FileSystems.getDefault().getPath(rootDirectory.absolutePath, Long.toString(attachment.officeId), Long.toString(attachment.missionId), Long.toString(attachment.bienId), attachment.collectionType);
	}

	private String repoFileName(Attachment attachment) {
		if(!attachment) {
			return null;
		}
		return attachment.displayColumn + '_' + attachment.displayRow + '_' + attachment.label + '.' + attachment.fileExtension;
	}

	private Map<String, File> listFormats(File collectionDir, Attachment attachment) {
		Map<String, File> formats = [:];
		String repoFileName = repoFileName(attachment);
		collectionDir.eachDir { formatDir ->
			formatDir.eachFileMatch(repoFileName) {
				formats[formatDir.name] = it;
			}
		}
		return formats;
	}

	private Path path(Attachment attachment, String format) throws AttachmentPersistenceException {
		String collectionPath = collectionPath(attachment)?.toString();
		String repoFileName =  repoFileName(attachment);
		return FileSystems.getDefault().getPath(collectionPath, format, repoFileName);
	}

	private Map<Integer, Map<Integer,  Entry<Attachment, Set<String>>>> insert(String format, File file, Attachment attachment) throws AttachmentPersistenceException {
		Map<Integer, Map<Integer,  Entry<Attachment, Set<String>>>> columns = findByOfficeIdAndMissionIdAndBienIdAndCollectionTypeMapByColumnAndRow(attachment.officeId, attachment.missionId, attachment.bienId, attachment.collectionType);
		if(!columns[attachment.displayColumn]) {
			columns[attachment.displayColumn] = [:];
		}
		if(columns[attachment.displayColumn][attachment.displayRow]?.key == attachment) {
			columns[attachment.displayColumn][attachment.displayRow].value << format;
		} else {
			for(int i = columns[attachment.displayColumn].size(); i > attachment.displayRow; i--) {
				Attachment before = columns[attachment.displayColumn][i-1].key;
				Attachment after = new Attachment(before.officeId, before.missionId, before.bienId, before.collectionType, before.label, before.displayColumn, i, before.fileExtension);
				columns[attachment.displayColumn][i] = [(after) : columns[attachment.displayColumn][i-1].value].entrySet().first();
				moveFiles(before, after, columns[attachment.displayColumn][i-1].value);
			}
			columns[attachment.displayColumn][attachment.displayRow] = [(attachment) : (Set<String>)[format]].entrySet().first();
		}
		
		copyFile(file.toPath(), path(attachment, format));

		return columns;
	}

	private Map<Integer, Map<Integer, Entry<Attachment, Set<String>>>> cleanCollection(Map<String, Map<Attachment, Set<String>>> dirty) throws AttachmentPersistenceException {
		Map<Integer, Map<Integer, Map<Attachment, Set<String>>>> dirtyByPosition = [:];
		Map<Integer, Map<Integer, Entry<Attachment, Set<String>>>> clean = [:];

		dirty.each { label, formatsByPosition ->
			Attachment ref = formatsByPosition.keySet().first();
			if(!dirtyByPosition[ref.displayColumn]) {
				dirtyByPosition[ref.displayColumn] = [:];
			}
			if(!dirtyByPosition[ref.displayColumn][ref.displayRow]) {
				dirtyByPosition[ref.displayColumn][ref.displayRow] = [:];
			}
			if(!dirtyByPosition[ref.displayColumn][ref.displayRow][ref]) {
				dirtyByPosition[ref.displayColumn][ref.displayRow][ref] = (Set<String>)[];
			}
			formatsByPosition.each { attachment, formats ->
				dirtyByPosition[ref.displayColumn][ref.displayRow][ref].addAll(formats);
			}
		}
		dirtyByPosition.sort();
		dirtyByPosition.each { columnNbr, line ->
			line.sort();
		}
		
		dirtyByPosition.each { columnNbr, line ->
			if(!clean[columnNbr]) {
				clean[columnNbr] = [:];
			}
			
			line.eachWithIndex { rowNbr, attachments, lineIndex ->
				attachments.eachWithIndex { attachment, formats, i ->
					Map<Attachment, Set<String>> tmp;
					if(columnNbr != attachment.displayColumn || lineIndex + i != attachment.displayRow) {
						Attachment moved = new Attachment(attachment.officeId, attachment.missionId, attachment.bienId, attachment.collectionType, attachment.label, columnNbr, lineIndex + i, attachment.fileExtension);
						moveFiles(attachment, moved, formats);
						tmp = [(moved) : formats];
					} else {
						tmp = [(attachment) : formats];
					}
					clean[columnNbr][lineIndex + i] = tmp.entrySet().first();
				}
			}
		}
		
		return clean;
	}

	private void copyFile(Path fromPath, Path toPath) throws AttachmentPersistenceException {
		try {
			Files.copy(fromPath, toPath);
		} catch(Throwable t) {
			throw new AttachmentPersistenceException('An error occured while reading from ' + fromPath + ' or writing to ' + toPath, t);
		}
	}

	private void moveFiles(Attachment from, Attachment to, Set<String> formats) throws AttachmentPersistenceException {
		formats.each {	format ->
			Path fromPath = path(from, format);
			Path toPath = path(to, format);
			try {
				Files.move(fromPath, toPath);
			} catch(Throwable t) {
				throw new AttachmentPersistenceException('An error occured while reading from ' + fromPath + ' or writing to ' + toPath, t);
			}
		}
	}
}
