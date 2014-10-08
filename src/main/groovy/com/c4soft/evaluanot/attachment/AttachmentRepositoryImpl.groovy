package com.c4soft.evaluanot.attachment

import groovy.io.FileType
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper

import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.Map;
import java.util.Map.Entry
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

	private static final Pattern INPUT_FILE_NAME_PATTERN = ~/^(.+)\.(\w+)$/

	private static final String META_DATA_FILE = 'meta-data.json';

	final File rootDirectory;
	
	final String servletDocumentsPath;

	public AttachmentRepositoryImpl(File rootDirectory, String servletDocumentsPath) {
		rootDirectory.mkdirs();
		this.rootDirectory = rootDirectory;
		this.servletDocumentsPath = servletDocumentsPath;
	}

	public AttachmentRepositoryImpl(String rootDirectoryPath, String servletDocumentsPath) {
		this(new File(rootDirectoryPath), servletDocumentsPath);
	}

	@Override
	public Map<Integer, Map<Integer,  Entry<Attachment, Set<Format>>>> findByOfficeIdAndMissionIdAndBienIdAndGalleryMapByColumnAndRow(long officeId, long missionId, long bienId, Gallery gallery) throws IllegalArgumentException {
		File ownerDir = new File(rootDirectory, Long.toString(officeId));
		File missionDir = new File(ownerDir, Long.toString(missionId));
		File propertyDir = new File(missionDir, Long.toString(bienId));
		File collectionDir = new File(propertyDir, gallery.name);
		Map<String, Map<Attachment, Set<Format>>> attachmentsByLabel = new TreeMap();

		collectionDir.mkdirs();
		collectionDir.eachDir { formatDir ->
			formatDir.eachFile(FileType.FILES) {
				Matcher matcher = REPO_FILE_NAME_PATTERN.matcher(it.name);
				if(matcher.matches()) {
					Integer column = Integer.valueOf(matcher[0][1]);
					Integer row = Integer.valueOf(matcher[0][2]);
					String label = matcher[0][3];
					String extension = matcher[0][4];
					Attachment attachment = new Attachment(officeId, missionId, bienId, gallery, label, column, row, extension);

					if(!attachmentsByLabel[label]) {
						attachmentsByLabel[label] = [:];
					}
					if(!attachmentsByLabel[label][attachment]) {
						attachmentsByLabel[label][attachment] = [];
					}
					attachmentsByLabel[label][attachment] << new Format(formatDir.name);
				}
			}
		}

		return cleanCollection(attachmentsByLabel);
	}

	@Override
	public Map<Integer, Map<Integer,  Entry<Attachment, Set<Format>>>> create(Map<Format, File> fileByFormat, long officeId, long missionId, long bienId, Gallery gallery, String label, int column, int row) throws IllegalArgumentException, AttachmentPersistenceException {
		if(! fileByFormat) {
			return findByOfficeIdAndMissionIdAndBienIdAndGalleryMapByColumnAndRow(officeId, missionId, bienId, gallery);
		}

		Map<Integer, Map<Integer,  Entry<Attachment, Set<Format>>>> columns;
		fileByFormat.each { format, file ->
			columns = insert(format, file,  officeId, missionId, bienId, gallery, label, column, row);
		}

		return columns;
	}

	private Map<Integer, Map<Integer,  Entry<Attachment, Set<Format>>>> insert(Format format, File file, long officeId, long missionId, long bienId, Gallery gallery, String label, int column, int row) throws IllegalArgumentException, AttachmentPersistenceException {
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

		Attachment attachment = new Attachment(officeId, missionId, bienId, gallery, label, column, row, inputMatcher[0][2]);
		return insert(format, file, attachment);
	}

	@Override
	public Map<Integer, Map<Integer,  Entry<Attachment, Set<Format>>>> delete(Attachment attachment) {
		Map<Integer, Map<Integer,  Entry<Attachment, Set<Format>>>> columns = findByOfficeIdAndMissionIdAndBienIdAndGalleryMapByColumnAndRow(attachment.officeId, attachment.missionId, attachment.bienId, attachment.gallery);
		Set<Format> formats = columns[attachment.displayColumn][attachment.displayRow].value;

		if(attachment != columns[attachment.displayColumn][attachment.displayRow].key) {
			return columns;
		}

		Attachment cover = getCover(attachment.officeId, attachment.missionId, attachment.bienId);
		if(cover == attachment) {
			setCover(attachment.officeId, attachment.missionId, attachment.bienId, null);
		}

		formats.each { format ->
			Files.delete(fsPath(attachment, format));
		}

		for(int i = attachment.displayRow; i < columns[attachment.displayColumn].size() - 1; i++) {
			Entry<Attachment, Set<Format>> shifted = columns[attachment.displayColumn][i+1];
			Attachment newAttachment = new Attachment(attachment.officeId, attachment.missionId, attachment.bienId, shifted.key.gallery, shifted.key.label, shifted.key.displayColumn, i, shifted.key.fileExtension);
			if(cover == shifted) {
				setCover(attachment.officeId, attachment.missionId, attachment.bienId, newAttachment);
			}
			columns[attachment.displayColumn][i] = [(newAttachment) : shifted.value].entrySet().first();
			shifted.value.each { format ->
				Files.move(fsPath(shifted.key, format), fsPath(newAttachment, format));
			}
		}
		columns[attachment.displayColumn].remove(columns[attachment.displayColumn].size() - 1);

		return columns;
	}

	@Override
	public Map<Integer, Map<Integer,  Entry<Attachment, Set<Format>>>> move(Attachment attachment, int newColumn, int newRow) throws AttachmentPersistenceException {
		Map<Format, File> contentByFormat = getContentByFormat(attachment);
		contentByFormat.each { format, file ->
			insert(format, file, new Attachment(attachment.officeId, attachment.missionId, attachment.bienId, attachment.gallery, attachment.label, newColumn, newRow, attachment.fileExtension));
		}
		delete(attachment);

		return findByOfficeIdAndMissionIdAndBienIdAndGalleryMapByColumnAndRow(attachment.officeId, attachment.missionId, attachment.bienId, attachment.gallery);
	}

	@Override
	public Attachment rename(Attachment attachment, String newLabel) throws AttachmentPersistenceException {
		if(!newLabel) {
			throw new IllegalArgumentException("new name can't be empty");
		}

		Attachment newAttachment = new Attachment(attachment.officeId, attachment.missionId, attachment.bienId, attachment.gallery, newLabel, attachment.displayColumn, attachment.displayRow, attachment.fileExtension);

		Attachment cover = getCover(attachment.officeId, attachment.missionId, attachment.bienId);
		if(cover?.displayColumn == newAttachment.displayColumn && cover?.displayRow == newAttachment.displayRow && cover?.gallery == newAttachment.gallery) {
			setCover(attachment.officeId, attachment.missionId, attachment.bienId, newAttachment);
		}

		moveFiles(attachment, newAttachment, getContentByFormat(attachment).keySet());
		return newAttachment;
	}

	@Override
	public Map<Format, File> getContentByFormat(Attachment attachment) {
		Path collectionPath = collectionPath(rootDirectory.absolutePath, attachment);
		return getFilesByFormats(new File(collectionPath.toString()), attachment);
	}

	@Override
	public Map<Format, String> getServletPathByFormat(Attachment attachment) throws IllegalArgumentException, AttachmentPersistenceException {
		Path collectionPath = collectionPath(rootDirectory.absolutePath, attachment);
		File collectionDir = new File(collectionPath.toString());
		Map<Format, String> formats = [:];
		String repoFileName = repoFileName(attachment);
		String collectionServletPath = servletDocumentsPath;
		collectionServletPath += '/';
		collectionServletPath += (collectionDir.absolutePath - rootDirectory.absolutePath).replaceAll(/[\\]/, '/');
		collectionServletPath += '/';
		collectionServletPath = collectionServletPath.replaceAll('/+', '/');
		collectionDir.eachDir { formatDir ->
			formatDir.eachFileMatch(repoFileName) {
				formats[new Format(formatDir.name)] = collectionServletPath + formatDir.name + '/' + java.net.URLEncoder.encode(repoFileName, 'UTF-8');
			}
		}
		return formats;
	}

	@Override
	public void setCover(long officeId, long missionId, long bienId, Attachment attachment) {
		if(attachment && (attachment.officeId != officeId || attachment.bienId != bienId || attachment.bienId != bienId)) {
			return;
		}

		File metaDataFile = getMetaDataFile(officeId, missionId, bienId);
		BienMetaData metaData = parseMetaData(metaDataFile);
		metaData.cover = attachment;

		if(!metaDataFile.isFile()) {
			metaDataFile.createNewFile();
		}
		metaDataFile.text = new JsonBuilder(metaData).toPrettyString();
	}

	@Override
	public Attachment getCover(long officeId, long missionId, long bienId) {
		File metaDataFile = getMetaDataFile(officeId, missionId, bienId);
		BienMetaData metaData = parseMetaData(metaDataFile);
		return metaData.cover;
	}

	private File getMetaDataFile(long officeId, long missionId, long bienId) {
		File officeDir = new File(rootDirectory.absolutePath, Long.toString(officeId));
		File missionDir = new File(officeDir, Long.toString(missionId));
		File bienDir = new File(missionDir, Long.toString(bienId));
		bienDir.mkdirs();
		return new File(bienDir, META_DATA_FILE);
	}

	private BienMetaData parseMetaData(File metaDataFile) {
		BienMetaData metaData;

		if(metaDataFile.isFile()) {
			Object raughData = new JsonSlurper().parse(metaDataFile);
			Attachment cover;
			try {
				cover = new Attachment(raughData.cover.officeId, raughData.cover.missionId, raughData.cover.bienId, new Gallery(raughData.cover.gallery.name), raughData.cover.label, raughData.cover.displayColumn, raughData.cover.displayRow, raughData.cover.fileExtension);
			} catch(Throwable t) {
				cover = null;
			}
			metaData = new BienMetaData(cover: cover);
		} else {
			metaData = new BienMetaData();
		}

		return metaData;
	}

	private Path collectionPath(String root, Attachment attachment) {
		if(!attachment) {
			return null;
		}
		return FileSystems.getDefault().getPath(root, Long.toString(attachment.officeId), Long.toString(attachment.missionId), Long.toString(attachment.bienId), attachment.gallery.name);
	}

	private String repoFileName(Attachment attachment) {
		if(!attachment) {
			return null;
		}
		return attachment.displayColumn + '_' + attachment.displayRow + '_' + attachment.label + '.' + attachment.fileExtension;
	}

	private Map<Format, File> getFilesByFormats(File collectionDir, Attachment attachment) {
		Map<Format, File> formats = [:];
		String repoFileName = repoFileName(attachment);
		collectionDir.eachDir { formatDir ->
			formatDir.eachFileMatch(repoFileName) {
				formats[new Format(formatDir.name)] = it;
			}
		}
		return formats;
	}

	private Path fsPath(Attachment attachment, Format format) throws AttachmentPersistenceException {
		String collectionPath = collectionPath(rootDirectory.absolutePath, attachment)?.toString();
		String repoFileName =  repoFileName(attachment);
		return FileSystems.getDefault().getPath(collectionPath, format.toString(), repoFileName);
	}

	private Map<Integer, Map<Integer,  Entry<Attachment, Set<Format>>>> insert(Format format, File file, Attachment attachment) throws AttachmentPersistenceException {
		Map<Integer, Map<Integer,  Entry<Attachment, Set<Format>>>> columns = findByOfficeIdAndMissionIdAndBienIdAndGalleryMapByColumnAndRow(attachment.officeId, attachment.missionId, attachment.bienId, attachment.gallery);
		Attachment cover = getCover(attachment.officeId, attachment.missionId, attachment.bienId);

		if(!columns[attachment.displayColumn]) {
			columns[attachment.displayColumn] = [:];
		}
		if(columns[attachment.displayColumn][attachment.displayRow]?.key == attachment) {
			columns[attachment.displayColumn][attachment.displayRow].value << format;
		} else {
			for(int i = columns[attachment.displayColumn].size(); i > attachment.displayRow; i--) {
				Attachment before = columns[attachment.displayColumn][i-1].key;
				Attachment after = new Attachment(before.officeId, before.missionId, before.bienId, before.gallery, before.label, before.displayColumn, i, before.fileExtension);
				columns[attachment.displayColumn][i] = [(after) : columns[attachment.displayColumn][i-1].value].entrySet().first();
				moveFiles(before, after, columns[attachment.displayColumn][i-1].value);

				if(before == cover) {
					setCover(attachment.officeId, attachment.missionId, attachment.bienId, after);
				}
			}
			columns[attachment.displayColumn][attachment.displayRow] = [(attachment) : (Set<Format>)[format]].entrySet().first();
		}

		copyFile(file.toPath(), fsPath(attachment, format));

		return columns;
	}

	private Map<Integer, Map<Integer, Entry<Attachment, Set<Format>>>> cleanCollection(Map<String, Map<Attachment, Set<Format>>> dirty) throws AttachmentPersistenceException {
		SortedMap<Integer, SortedMap<Integer, SortedMap<Attachment, SortedSet<Format>>>> dirtyByPosition = new TreeMap();
		Map<Integer, Map<Integer, Entry<Attachment, Set<Format>>>> clean = [:];
		Attachment ref;

		dirty.each { label, formatsByPosition ->
			ref = formatsByPosition.keySet().first();
			if(!dirtyByPosition[ref.displayColumn]) {
				dirtyByPosition[ref.displayColumn] = new TreeMap();
			}
			if(!dirtyByPosition[ref.displayColumn][ref.displayRow]) {
				dirtyByPosition[ref.displayColumn][ref.displayRow] = new TreeMap();
			}
			if(!dirtyByPosition[ref.displayColumn][ref.displayRow][ref]) {
				dirtyByPosition[ref.displayColumn][ref.displayRow][ref] = new TreeSet();
			}
			formatsByPosition.each { attachment, formats ->
				dirtyByPosition[ref.displayColumn][ref.displayRow][ref].addAll(formats);
			}
		}

		dirtyByPosition.each { columnNbr, line ->
			if(!clean[columnNbr]) {
				clean[columnNbr] = [:];
			}

			line.eachWithIndex { rowNbr, attachments, lineIndex ->
				attachments.eachWithIndex { attachment, formats, i ->
					Map<Attachment, Set<Format>> tmp;
					if(columnNbr != attachment.displayColumn || lineIndex + i != attachment.displayRow) {
						Attachment moved = new Attachment(attachment.officeId, attachment.missionId, attachment.bienId, attachment.gallery, attachment.label, columnNbr, lineIndex + i, attachment.fileExtension);
						moveFiles(attachment, moved, formats);
						tmp = [(moved) : formats];
					} else {
						tmp = [(attachment) : formats];
					}
					clean[columnNbr][lineIndex + i] = tmp.entrySet().first();
				}
			}
		}

		if(ref) {
			Attachment cover = getCover(ref.officeId, ref.missionId, ref.bienId);
			if(cover && clean[cover.displayColumn] && clean[cover.displayColumn][cover.displayRow] && clean[cover.displayColumn][cover.displayRow].key != cover) {
				setCover(ref.officeId, ref.missionId, ref.bienId, null);
			}
		}

		return clean;
	}

	private void copyFile(Path fromPath, Path toPath) throws AttachmentPersistenceException {
		try {
			new File(toPath.parent.toString()).mkdirs();
			Files.copy(fromPath, toPath, StandardCopyOption.REPLACE_EXISTING);
		} catch(Throwable t) {
			throw new AttachmentPersistenceException('An error occured while reading from ' + fromPath + ' or writing to ' + toPath, t);
		}
	}

	private void moveFiles(Attachment from, Attachment to, Set<Format> formats) throws AttachmentPersistenceException {
		formats.each {	format ->
			Path fromPath = fsPath(from, format);
			Path toPath = fsPath(to, format);
			try {
				new File(toPath.parent.toString()).mkdirs();
				Files.move(fromPath, toPath, StandardCopyOption.REPLACE_EXISTING);
			} catch(Throwable t) {
				throw new AttachmentPersistenceException('An error occured while reading from ' + fromPath + ' or writing to ' + toPath, t);
			}
		}
	}
}
