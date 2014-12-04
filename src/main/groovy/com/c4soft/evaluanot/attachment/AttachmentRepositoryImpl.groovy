package com.c4soft.evaluanot.attachment

import groovy.io.FileType
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper

import java.nio.charset.Charset
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.DirectoryNotEmptyException
import java.nio.file.FileAlreadyExistsException
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.Map.Entry
import java.util.logging.Logger
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
	private static final Pattern REPO_FILE_NAME_PATTERN = ~/^(\d+)_(\d+)\.(\w+)$/;

	private static final Pattern INPUT_FILE_NAME_PATTERN = ~/^(.+)\.(\w+)$/
	
	private static final Pattern LABEL_UNICITY_PATTERN = ~/^(.+)\((\d+)\)$/

	private static final String META_DATA_FILE = 'meta-data.json';
	
	private static final Logger LOG = Logger.getLogger(AttachmentRepositoryImpl.class.name);

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
		BienMetaData metaData = readMetaData(officeId, missionId, bienId);
		Map<Integer, Map<Integer,  Entry<Attachment, Set<Format>>>>  attachments = findByOfficeIdAndMissionIdAndBienIdAndGalleryMapByColumnAndRow(metaData, officeId, missionId, bienId, gallery);
		writeMetaData(metaData, officeId, missionId, bienId);
		return attachments;
	}
	
	public Map<Integer, Map<Integer,  Entry<Attachment, Set<Format>>>> findByOfficeIdAndMissionIdAndBienIdAndGalleryMapByColumnAndRow(BienMetaData metaData, long officeId, long missionId, long bienId, Gallery gallery) throws IllegalArgumentException {
		File ownerDir = new File(rootDirectory, Long.toString(officeId));
		File missionDir = new File(ownerDir, Long.toString(missionId));
		File propertyDir = new File(missionDir, Long.toString(bienId));
		File collectionDir = new File(propertyDir, gallery.name);
		Map<Attachment, Set<Format>> attachments = new TreeMap();

		collectionDir.mkdirs();
		collectionDir.eachDir { formatDir ->
			formatDir.eachFile(FileType.FILES) {
				Matcher matcher = REPO_FILE_NAME_PATTERN.matcher(it.name);
				if(matcher.matches()) {
					Integer column = Integer.valueOf(matcher[0][1]);
					Integer row = Integer.valueOf(matcher[0][2]);
					String label = metaData.getLabel(gallery.name, column, row);
					String extension = matcher[0][3];
					Attachment attachment = new Attachment(officeId, missionId, bienId, gallery, label, column, row, extension);

					if(!attachments[attachment]) {
						attachments[attachment] = new TreeSet<Format>();
					}
					attachments[attachment] << new Format(formatDir.name);
				} else {
					LOG.warning(it.name + " does not match " + REPO_FILE_NAME_PATTERN.pattern);
				}
			}
		}

		return cleanCollection(metaData, attachments);
	}
	
	private String getUniqueLabel(Map<Integer, Map<Integer,  Entry<Attachment, Set<Format>>>> allAttachments, String label) {
		Set<String> labels = new HashSet<String>();
		String uniqueLabel = label;
		allAttachments.each { colNbr, col ->
			col.each { rowNbr, e ->
				labels << e.key.label;
			}
		}
		while(labels.contains(uniqueLabel)) {
			Matcher m = LABEL_UNICITY_PATTERN.matcher(uniqueLabel);
			if(m.matches()) {
				Integer i = Integer.valueOf(m[0][2]) + 1;
				uniqueLabel = m[0][1].toString().trim() + ' (' + i + ')';
			} else {
				uniqueLabel = uniqueLabel.trim() + ' (1)';
			}
		}
		return uniqueLabel;
	}

	@Override
	public Map<Integer, Map<Integer,  Entry<Attachment, Set<Format>>>> create(Map<Format, File> fileByFormat, long officeId, long missionId, long bienId, Gallery gallery, String label, int column, int row) throws IllegalArgumentException, AttachmentPersistenceException {
		BienMetaData metaData = readMetaData(officeId, missionId, bienId);
		Map<Integer, Map<Integer,  Entry<Attachment, Set<Format>>>> attachments = create(metaData, fileByFormat, officeId, missionId, bienId, gallery, label, column, row);
		writeMetaData(metaData, officeId, missionId, bienId);
		return attachments;
	}
	
	public Map<Integer, Map<Integer,  Entry<Attachment, Set<Format>>>> create(BienMetaData metaData, Map<Format, File> fileByFormat, long officeId, long missionId, long bienId, Gallery gallery, String label, int column, int row) throws IllegalArgumentException, AttachmentPersistenceException {
		if(! fileByFormat) {
			return findByOfficeIdAndMissionIdAndBienIdAndGalleryMapByColumnAndRow(metaData, officeId, missionId, bienId, gallery);
		}
		
		Map<Integer, Map<Integer,  Entry<Attachment, Set<Format>>>> allAttachments = findByOfficeIdAndMissionIdAndBienIdAndGalleryMapByColumnAndRow(metaData, officeId, missionId, bienId, gallery);
		String uniqueLabel = getUniqueLabel(allAttachments, label);

		Map<Integer, Map<Integer,  Entry<Attachment, Set<Format>>>> columns;
		fileByFormat.each { format, file ->
			columns = insert(metaData, allAttachments, format, file,  officeId, missionId, bienId, gallery, uniqueLabel, column, row);
		}

		return columns;
	}

	private Map<Integer, Map<Integer,  Entry<Attachment, Set<Format>>>> insert(BienMetaData metaData, Map<Integer, Map<Integer,  Entry<Attachment, Set<Format>>>> allAttachments, Format format, File file, long officeId, long missionId, long bienId, Gallery gallery, String label, int column, int row) throws IllegalArgumentException, AttachmentPersistenceException {
		if(!file?.isFile()) {
			throw new IllegalArgumentException('provided file ' + file?.name + ' is not valid');
		}
		Matcher inputMatcher = INPUT_FILE_NAME_PATTERN.matcher(file.name);
		if(!inputMatcher.matches()) {
			throw new IllegalArgumentException('provided file name must be composed of a name and an extension');
		}

		Attachment attachment = new Attachment(officeId, missionId, bienId, gallery, label, column, row, inputMatcher[0][2]);
		return insert(metaData, allAttachments, format, file, attachment);
	}

	@Override
	public Map<Integer, Map<Integer,  Entry<Attachment, Set<Format>>>> delete(Attachment attachment) {
		BienMetaData metaData = readMetaData(attachment.officeId, attachment.missionId, attachment.bienId);
		Map<Integer, Map<Integer,  Entry<Attachment, Set<Format>>>> attachments = delete(metaData, attachment);
		writeMetaData(metaData, attachment.officeId, attachment.missionId, attachment.bienId);
		return attachments;
	}
	
	public Map<Integer, Map<Integer,  Entry<Attachment, Set<Format>>>> delete(BienMetaData metaData, Attachment attachment) {
		Map<Integer, Map<Integer,  Entry<Attachment, Set<Format>>>> columns = findByOfficeIdAndMissionIdAndBienIdAndGalleryMapByColumnAndRow(metaData, attachment.officeId, attachment.missionId, attachment.bienId, attachment.gallery);
		Set<Format> formats = columns[attachment.displayColumn][attachment.displayRow].value;

		if(attachment != columns[attachment.displayColumn][attachment.displayRow].key) {
			return columns;
		}

		Attachment cover = getCover(attachment.officeId, attachment.missionId, attachment.bienId);
		if(cover == attachment) {
			metaData.cover =  null;
		}

		formats.each { format ->
			Files.delete(fsPath(attachment, format));
		}

		for(int i = attachment.displayRow; i < columns[attachment.displayColumn].size() - 1; i++) {
			Entry<Attachment, Set<Format>> shifted = columns[attachment.displayColumn][i+1];
			Attachment newAttachment = new Attachment(attachment.officeId, attachment.missionId, attachment.bienId, shifted.key.gallery, shifted.key.label, shifted.key.displayColumn, i, shifted.key.fileExtension);
			if(cover == shifted) {
				metaData.cover =  newAttachment;
			}
			columns[attachment.displayColumn][i] = [(newAttachment) : shifted.value].entrySet().first();
			shifted.value.each { format ->
				Files.move(fsPath(shifted.key, format), fsPath(newAttachment, format));
				metaData.setLabel(newAttachment.gallery.name, newAttachment.displayColumn, newAttachment.displayRow, newAttachment.label);
			}
		}
				metaData.setLabel(attachment.gallery.name, attachment.displayColumn, columns[attachment.displayColumn].size() - 1, null);
		columns[attachment.displayColumn].remove(columns[attachment.displayColumn].size() - 1);

		return columns;
	}

	@Override
	public Map<Integer, Map<Integer,  Entry<Attachment, Set<Format>>>> move(Attachment attachment, int newColumn, int newRow) throws AttachmentPersistenceException {
		BienMetaData metaData = readMetaData(attachment.officeId, attachment.missionId, attachment.bienId);
		Map<Integer, Map<Integer,  Entry<Attachment, Set<Format>>>> attachments = move(metaData, attachment, newColumn, newRow);
		writeMetaData(metaData, attachment.officeId, attachment.missionId, attachment.bienId);
		return attachments;
	}
	
	public Map<Integer, Map<Integer,  Entry<Attachment, Set<Format>>>> move(BienMetaData metaData, Attachment attachment, int newColumn, int newRow) throws AttachmentPersistenceException {
		Map<Format, File> contentByFormat = getContentByFormat(attachment);
		Map<Format, File> contentCopyByFormat = new TreeMap<Format, File>();
		double random = Math.random();
		
		contentByFormat.each { format, file ->
			contentCopyByFormat[format] = new File(rootDirectory, format.name + '_' + random);
			Files.copy(file.toPath(), contentCopyByFormat[format].toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
		Map<Integer, Map<Integer,  Entry<Attachment, Set<Format>>>> allAtachments = delete(metaData, attachment);
		contentByFormat.each { format, file ->
			insert(metaData, allAtachments, format, contentCopyByFormat[format], new Attachment(attachment.officeId, attachment.missionId, attachment.bienId, attachment.gallery, attachment.label, newColumn, newRow, attachment.fileExtension));
			contentCopyByFormat[format].delete();
		}

		return findByOfficeIdAndMissionIdAndBienIdAndGalleryMapByColumnAndRow(metaData, attachment.officeId, attachment.missionId, attachment.bienId, attachment.gallery);
	}

	@Override
	public Attachment rename(Attachment attachment, String newLabel) throws AttachmentPersistenceException {
		BienMetaData metaData = readMetaData(attachment.officeId, attachment.missionId, attachment.bienId);
		Attachment renamed = rename(metaData, attachment, newLabel);
		writeMetaData(metaData, attachment.officeId, attachment.missionId, attachment.bienId);
		return renamed;
	}
	
	public Attachment rename(BienMetaData metaData, Attachment attachment, String newLabel) throws AttachmentPersistenceException {
		if(attachment == null) {
			throw new IllegalArgumentException("can't rename null attachment");
		}
		if(!newLabel) {
			throw new IllegalArgumentException("new label can't be empty");
		} else if(newLabel == attachment.label) {
			return attachment;
		}
		
		Map<Integer, Map<Integer,  Entry<Attachment, Set<Format>>>> allAttachments = findByOfficeIdAndMissionIdAndBienIdAndGalleryMapByColumnAndRow(metaData, attachment.officeId, attachment.missionId, attachment.bienId, attachment.gallery);
		String uniqueLabel = getUniqueLabel(allAttachments, newLabel);

		Attachment newAttachment = new Attachment(attachment.officeId, attachment.missionId, attachment.bienId, attachment.gallery, uniqueLabel, attachment.displayColumn, attachment.displayRow, attachment.fileExtension);
		metaData.setLabel(newAttachment.gallery.name, newAttachment.displayColumn, newAttachment.displayRow, newAttachment.label);

		Attachment cover = getCover(attachment.officeId, attachment.missionId, attachment.bienId);
		if(cover?.displayColumn == newAttachment.displayColumn && cover?.displayRow == newAttachment.displayRow && cover?.gallery == newAttachment.gallery) {
			metaData.cover =  newAttachment;
		}

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
	
	public BienMetaData readMetaData(long officeId, long missionId, long bienId) {
		File metaDataFile = getMetaDataFile(officeId, missionId, bienId);
		BienMetaData metaData = BienMetaData.parseMetaData(metaDataFile);
		return metaData;
	}
	
	public void writeMetaData(BienMetaData metaData, long officeId, long missionId, long bienId) {
		File metaDataFile = getMetaDataFile(officeId, missionId, bienId);
		if(!metaDataFile.isFile()) {
			metaDataFile.createNewFile();
		}
		String jsonStr = new JsonBuilder(metaData).toPrettyString();
		metaDataFile.text = jsonStr;
	}

	@Override
	public void setCover(long officeId, long missionId, long bienId, Attachment attachment) {
		if(attachment && (attachment.officeId != officeId || attachment.bienId != bienId || attachment.bienId != bienId)) {
			return;
		}
		BienMetaData metaData = readMetaData(officeId, missionId, bienId);
		metaData.cover =  attachment;
		writeMetaData(metaData, officeId, missionId, bienId);
	}

	@Override
	public Attachment getCover(long officeId, long missionId, long bienId) {
		BienMetaData metaData = readMetaData(officeId, missionId, bienId);
		return metaData.cover;
	}

	private File getMetaDataFile(long officeId, long missionId, long bienId) {
		File officeDir = new File(rootDirectory.absolutePath, Long.toString(officeId));
		File missionDir = new File(officeDir, Long.toString(missionId));
		File bienDir = new File(missionDir, Long.toString(bienId));
		bienDir.mkdirs();
		return new File(bienDir, META_DATA_FILE);
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
		return attachment.displayColumn + '_' + attachment.displayRow + '.' + attachment.fileExtension;
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

	private Map<Integer, Map<Integer,  Entry<Attachment, Set<Format>>>> insert(BienMetaData metaData, Map<Integer, Map<Integer,  Entry<Attachment, Set<Format>>>> allAttachments, Format format, File file, Attachment attachment) throws AttachmentPersistenceException {
		Attachment cover = getCover(attachment.officeId, attachment.missionId, attachment.bienId);

		if(!allAttachments[attachment.displayColumn]) {
			allAttachments[attachment.displayColumn] = [:];
		}
		if(allAttachments[attachment.displayColumn][attachment.displayRow]?.key == attachment) {
			allAttachments[attachment.displayColumn][attachment.displayRow].value << format;
		} else {
			for(int i = allAttachments[attachment.displayColumn].size(); i > attachment.displayRow; i--) {
				Attachment before = allAttachments[attachment.displayColumn][i-1].key;
				Attachment after = new Attachment(before.officeId, before.missionId, before.bienId, before.gallery, before.label, before.displayColumn, i, before.fileExtension);
				allAttachments[attachment.displayColumn][i] = [(after) : allAttachments[attachment.displayColumn][i-1].value].entrySet().first();
				moveFiles(before, after, allAttachments[attachment.displayColumn][i-1].value);
				metaData.setLabel(after.gallery.name, after.displayColumn, after.displayRow, after.label);

				if(before == cover) {
					metaData.cover = after;
				}
			}
			allAttachments[attachment.displayColumn][attachment.displayRow] = [(attachment) : (Set<Format>)[format]].entrySet().first();
			metaData.setLabel(attachment.gallery.name, attachment.displayColumn, attachment.displayRow, attachment.label);
		}

		copyFile(file.toPath(), fsPath(attachment, format));

		return allAttachments;
	}

	private Map<Integer, Map<Integer, Entry<Attachment, Set<Format>>>> cleanCollection(BienMetaData metaData, Map<Attachment, Set<Format>> dirty) throws AttachmentPersistenceException {
		SortedMap<Integer, SortedMap<Integer, SortedMap<Attachment, SortedSet<Format>>>> dirtyByPosition = new TreeMap();
		Map<Integer, Map<Integer, Entry<Attachment, Set<Format>>>> clean = [:];

		dirty.each { attachment, formats ->
			if(!dirtyByPosition[attachment.displayColumn]) {
				dirtyByPosition[attachment.displayColumn] = new TreeMap();
			}
			if(!dirtyByPosition[attachment.displayColumn][attachment.displayRow]) {
				dirtyByPosition[attachment.displayColumn][attachment.displayRow] = new TreeMap();
			}
			if(!dirtyByPosition[attachment.displayColumn][attachment.displayRow][attachment]) {
				dirtyByPosition[attachment.displayColumn][attachment.displayRow][attachment] = new TreeSet();
			}
			dirtyByPosition[attachment.displayColumn][attachment.displayRow][attachment].addAll(formats);
		}

		dirtyByPosition.each { columnNbr, columnCells ->
			int i = 0;
			if(!clean[columnNbr]) {
				clean[columnNbr] = [:];
			}

			columnCells.each { rowNbr, cell ->
				cell.each { Attachment attachment, Set<Format> formats ->
					Map<Attachment, Set<Format>> tmp;
					if(columnNbr != attachment.displayColumn || i != attachment.displayRow) {
						Attachment moved = new Attachment(attachment.officeId, attachment.missionId, attachment.bienId, attachment.gallery, attachment.label, columnNbr, i, attachment.fileExtension);
						moveFiles(attachment, moved, formats);
						metaData.setLabel(moved.gallery.name, moved.displayColumn, moved.displayRow, moved.label);
						tmp = [(moved) : formats];
					} else {
						tmp = [(attachment) : formats];
					}
					clean[columnNbr][i++] = tmp.entrySet().first();
				}
			}
		}

		Set<Attachment> attachments = dirty.keySet();
		if(attachments) {
			Attachment ref = attachments.first();
			Attachment cover = getCover(ref.officeId, ref.missionId, ref.bienId);
			if(cover && clean[cover.displayColumn] && clean[cover.displayColumn][cover.displayRow] && clean[cover.displayColumn][cover.displayRow].key.gallery.name == cover.gallery.name && clean[cover.displayColumn][cover.displayRow].key != cover) {
				metaData.cover = null;
			}
		}

		return clean;
	}

	private void copyFile(Path fromPath, Path toPath) throws AttachmentPersistenceException {
		try {
			new File(toPath.parent.toString()).mkdirs();
			DataInputStream from = new File(fromPath.toString()).newDataInputStream();
			DataOutputStream to = new File(toPath.toString()).newDataOutputStream();
			to << from;
			from.close();
			to.close();
//			Files.copy(fromPath.toUri(), toPath.toUri(), StandardCopyOption.REPLACE_EXISTING);
		} catch(IOException | SecurityException t) {
			throw new AttachmentPersistenceException('An error occured while copying ' + fromPath + ' to ' + toPath, t);
		}
	}

	private void moveFiles(Attachment from, Attachment to, Set<Format> formats) throws AttachmentPersistenceException {
		formats.each {	format ->
			Path fromPath = fsPath(from, format);
			Path toPath = fsPath(to, format);
			try {
				new File(toPath.parent.toString()).mkdirs();
				Files.move(fromPath, toPath, StandardCopyOption.REPLACE_EXISTING);
			} catch(UnsupportedOperationException | FileAlreadyExistsException | DirectoryNotEmptyException | AtomicMoveNotSupportedException | IOException | SecurityException t) {
				LOG.warning("failed to create file with defaultCharset: " + Charset.defaultCharset());
				throw new AttachmentPersistenceException('An error occured while moving ' + fromPath + ' to ' + toPath, t);
			}
		}
	}
}
