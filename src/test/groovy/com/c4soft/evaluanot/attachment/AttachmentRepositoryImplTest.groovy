package com.c4soft.evaluanot.attachment;

import static org.hamcrest.Matchers.*
import static org.junit.Assert.*
import groovy.io.FileType;

import java.nio.file.Files
import java.util.Map.Entry
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Before
import org.junit.Test
import org.junit.rules.ExpectedException;

class AttachmentRepositoryImplTest {

	private AttachmentRepositoryImpl repo;

	private final File testDataDir = new File('target/test-classes/test-repo');

	private static final Format FULLSIZE = new Format('fullsize');

	private static final Format THUMBNAIL = new Format('thumbnail');

	private static final Gallery PHOTO = new Gallery('photo', (Set<Format>)[FULLSIZE, THUMBNAIL]);

	@Before
	public void before() {
		repo = new AttachmentRepositoryImpl(new File('target/attachmentRepo'), '/documents/');
		repo.rootDirectory.deleteDir();
		repo.rootDirectory.mkdirs();
		testDataDir.eachFileRecurse {
			if(it.isFile()) {
				String subPath = it.absolutePath - testDataDir.absolutePath;
				File copy = new File(repo.rootDirectory, subPath);
				copy.parentFile.mkdirs();
				Files.copy(it.toPath(), copy.toPath());
			}
		}
	}

	@Test
	public void testThatValidCollectionsAreLoadedWithoutModifications() {
		Map<Integer, Map<Integer,  Entry<Attachment, Set<Format>>>> actual = repo.findByOfficeIdAndMissionIdAndBienIdAndGalleryMapByColumnAndRow(4001L, 51L, 69L, PHOTO);
		repo.setCover(4001L, 51L, 69L, new Attachment(4001L, 51L, 69L, PHOTO, '13', 'Belle montagne', 1, 0, 'JPG'));
		assertThat(actual.size(), is (2));
		assertThat(actual[0].size(), is (1));

		assertThat(actual[0][0].key, is(new Attachment(4001L, 51L, 69L, PHOTO, '12', 'virage à droite', 0, 0, 'JPG')));
		assertThat(actual[0][0].value.size(), is (1));
		assertThat(actual[0][0].value, containsInAnyOrder([FULLSIZE].toArray()));

		assertThat(actual[1].size(), is (2));

		assertThat(actual[1][0].key, is(new Attachment(4001L, 51L, 69L, PHOTO, '13', 'Belle montagne', 1, 0, 'JPG')));
		assertThat(actual[1][0].value.size(), is (1));
		assertThat(actual[1][0].value, containsInAnyOrder([FULLSIZE].toArray()));

		assertThat(actual[1][1].key, is(new Attachment(4001L, 51L, 69L, PHOTO, '14', 'Jérôme', 1, 1, 'JPG')));
		assertThat(actual[1][1].value.size(), is (2));
		assertThat(actual[1][1].value, containsInAnyOrder([FULLSIZE, THUMBNAIL].toArray()));
	}

	@Test
	public void testThatInvalidCollectionsAreCorrectedAtLoadTime() {
		Map<Integer, Map<Integer,  Entry<Attachment, Set<Format>>>> actual = repo.findByOfficeIdAndMissionIdAndBienIdAndGalleryMapByColumnAndRow(4001L, 51L, 42L, PHOTO);
		assertThat(actual.size(), is (1));
		assertThat(actual[0].size(), is (10));
		assertThat(actual[0][0].key, is(new Attachment(4001L, 51L, 42L, PHOTO, '10', '0_1', 0, 0, 'JPG')));
		assertThat(actual[0][1].key, is(new Attachment(4001L, 51L, 42L, PHOTO, '8', '0_2', 0, 1, 'JPG')));
	}

	@Test
	public void testThatRetrievingNotExistingCollectionCreatesAnEmptyOne() {
		Map<Integer, Map<Integer,  Entry<Attachment, Set<Format>>>> actual = repo.findByOfficeIdAndMissionIdAndBienIdAndGalleryMapByColumnAndRow(0L, 1L, 2L, PHOTO);
		assertThat(actual.size(), is (0));
		assertTrue(new File(repo.rootDirectory, '0/1/2/photo').isDirectory());
	}

	@Test
	public void testThatCreateInExistingColumnMovesExistingAttachments() {
		Map<Integer, Map<Integer,  Entry<Attachment, Set<Format>>>> actual = repo.create([(FULLSIZE) : new File("target/test-classes/moto'_(1).jpg")], 4001L, 51L, 69L, PHOTO, "Ch4mp's monster (2)", 1, 1);
		assertThat(actual[1].size(), is (3));
		assertThat(actual[1][0].key, is(new Attachment(4001L, 51L, 69L, PHOTO, '13', 'Belle montagne', 1, 0, 'JPG')));
		assertThat(actual[1][1].key.toString(), containsString("Ch4mp's monster (2)"));
		assertThat(actual[1][2].key, is(new Attachment(4001L, 51L, 69L, PHOTO, '14', 'Jérôme', 1, 2, 'JPG')));
	}

	@Test
	public void testThatCreateAttachmentCreatesOneFilePerFormat() {
		Map<Integer, Map<Integer,  Entry<Attachment, Set<Format>>>> actual = repo.create([(FULLSIZE) : new File("target/test-classes/moto'_(1).jpg"), (THUMBNAIL) : new File("target/test-classes/moto'_(1).jpg")], 4001L, 51L, 69L, PHOTO, "Ch4mp's monster", 1, 1);
		File fullsizeDir = new File(repo.rootDirectory, '4001/51/69/' + PHOTO.name + '/' + FULLSIZE.name);
		File thumbnailDir = new File(repo.rootDirectory, '4001/51/69/' + PHOTO.name + '/' + THUMBNAIL.name);
		String fullsizeId, thumbnailId;
		Pattern expectedPattern = ~/1_1_([\w\-]+)\.\w+/;
		fullsizeDir.eachFileMatch(FileType.FILES, expectedPattern) {
			Matcher m = expectedPattern.matcher(it.name);
			fullsizeId = m[0]?.get(1);
		}
		thumbnailDir.eachFileMatch(FileType.FILES, expectedPattern) {
			Matcher m = expectedPattern.matcher(it.name);
			thumbnailId = m[0]?.get(1);
		}
		assert(fullsizeId);
		assertEquals(fullsizeId, thumbnailId);
	}
	
	
	@Test
	public void testThatCreateAttachmentWithSameLabelAtSamePositionActuallyCreatesNewAttachment() {
		Map<Integer, Map<Integer,  Entry<Attachment, Set<Format>>>> actual = repo.create([(FULLSIZE) : new File("target/test-classes/moto'_(1).jpg")], 4001L, 51L, 69L, PHOTO, "Ch4mp's monster (2)", 1, 1);
		assertThat(actual[1].size(), is (3));
		actual = repo.create([(FULLSIZE) : new File("target/test-classes/moto'_(1).jpg")], 4001L, 51L, 69L, PHOTO, "Ch4mp's monster (2)", 1, 1);
		assertThat(actual[1].size(), is (4));
		assertThat(actual[1][0].key, is(new Attachment(4001L, 51L, 69L, PHOTO, '13', 'Belle montagne', 1, 0, 'JPG')));
		assertThat(actual[1][1].key.toString(), containsString("Ch4mp's monster (3)"));
		assertThat(actual[1][2].key.toString(), containsString("Ch4mp's monster (2)"));
		assertThat(actual[1][3].key, is(new Attachment(4001L, 51L, 69L, PHOTO, '14', 'Jérôme', 1, 3, 'JPG')));
	}

	@Test
	public void testThatDeleteActuallyRemovesFileFromFileSystemAndShiftsOtherColumnAttachments() {
		Map<Integer, Map<Integer,  Entry<Attachment, Set<Format>>>> actual = repo.delete(new Attachment(4001L, 51L, 69L, PHOTO, '13', 'Belle montagne', 1, 0, 'JPG'));
		assertThat(actual[1].size(), is (1));
		assertThat(actual[1][0].key, is(new Attachment(4001L, 51L, 69L, PHOTO, '14', 'Jérôme', 1, 0, 'JPG')));
		assertThat(new File(repo.rootDirectory, '4001/51/69/photo/fullsize').list().length, is(2));
	}

	@Test
	public void testThatDeleteAttachmentAboveCoverKeepsCover() {
		repo.setCover(4001L, 51L, 69L, new Attachment(4001L, 51L, 69L, PHOTO, '14', 'Jérôme', 1, 1, 'JPG'));
		Map<Integer, Map<Integer,  Entry<Attachment, Set<Format>>>> actual = repo.delete(new Attachment(4001L, 51L, 69L, PHOTO, '13', 'Belle montagne', 1, 0, 'JPG'));
		assertThat(repo.getCover(4001L, 51L, 69L), is(new Attachment(4001L, 51L, 69L, PHOTO, '14', 'Jérôme', 1, 0, 'JPG')));
	}

	@Test
	public void testThatMoveRenamesFilesAndReturnsNewAttachments() {
		Map<Integer, Map<Integer,  Entry<Attachment, Set<Format>>>> actual = repo.move(new Attachment(4001L, 51L, 69L, PHOTO, '13', 'Belle montagne', 1, 0, 'JPG'), 0, 0);
		assertThat(actual.size(), is (2));
		assertThat(actual[0].size(), is (2));
		assertThat(actual[0][0].key, is(new Attachment(4001L, 51L, 69L, PHOTO, '13', 'Belle montagne', 0, 0, 'JPG')));
		assertThat(actual[0][1].key, is(new Attachment(4001L, 51L, 69L, PHOTO, '12', 'virage à droite', 0, 1, 'JPG')));
		assertThat(actual[1].size(), is (1));
		assertThat(actual[1][0].key, is(new Attachment(4001L, 51L, 69L, PHOTO, '14', 'Jérôme', 1, 0, 'JPG')));
	}

	@Test
	public void testThatMoveKeepsCover() {
		Attachment beforeMove = new Attachment(4001L, 51L, 69L, PHOTO, '13', 'Belle montagne', 1, 0, 'JPG');
		Attachment afterMove = new Attachment(beforeMove.officeId, beforeMove.missionId, beforeMove.bienId, beforeMove.gallery, beforeMove.id, beforeMove.label, 0, 0, beforeMove.fileExtension);
		
		repo.setCover(beforeMove.officeId, beforeMove.missionId, beforeMove.bienId, beforeMove);
		
		Map<Integer, Map<Integer,  Entry<Attachment, Set<Format>>>> actual = repo.move(beforeMove, afterMove.displayColumn, afterMove.displayRow);
		
		assertThat(repo.getCover(beforeMove.officeId, beforeMove.missionId, beforeMove.bienId), is(afterMove));
	}

	@Test
	public void testThatMoveInsameColumnWorks() {
		Map<Integer, Map<Integer,  Entry<Attachment, Set<Format>>>> actual = repo.move(new Attachment(4001L, 51L, 69L, PHOTO, '13', 'Belle montagne', 1, 0, 'JPG'), 1, 1);
		assertThat(actual.size(), is (2));
		assertThat(actual[0].size(), is (1));
		assertThat(actual[0][0].key, is(new Attachment(4001L, 51L, 69L, PHOTO, '12', 'virage à droite', 0, 0, 'JPG')));
		assertThat(actual[1].size(), is (2));
		assertThat(actual[1][0].key, is(new Attachment(4001L, 51L, 69L, PHOTO, '14', 'Jérôme', 1, 0, 'JPG')));
		assertThat(actual[1][1].key, is(new Attachment(4001L, 51L, 69L, PHOTO, '13', 'Belle montagne', 1, 1, 'JPG')));
	}

	@Test
	public void testThatRenameReturnsAttachmentWithNewLabel() {
		Attachment actual = repo.rename(new Attachment(4001L, 51L, 69L, PHOTO, '14', 'Jérôme', 1, 1, 'JPG'), '/\\ en forêt');
		assertThat(actual, is(new Attachment(4001L, 51L, 69L, PHOTO, '14', '/\\ en forêt', 1, 1, 'JPG')));
		assertThat(new File(repo.rootDirectory, '4001/51/69/photo/fullsize').list().length, is(3));
		assertTrue(new File(repo.rootDirectory, '4001/51/69/photo/fullsize/1_1_14.JPG').isFile());
		assertTrue(new File(repo.rootDirectory, '4001/51/69/photo/thumbnail/1_1_14.JPG').isFile());
	}

	@Test
	public void testThatRenameMakesNewLabelUnique() {
		Attachment actual = repo.rename(new Attachment(4001L, 51L, 69L, PHOTO, '14', 'Jérôme', 1, 1, 'JPG'), 'Belle montagne');
		assertThat(actual, is(new Attachment(4001L, 51L, 69L, PHOTO, '14', 'Belle montagne (1)', 1, 1, 'JPG')));
		assertThat(new File(repo.rootDirectory, '4001/51/69/photo/fullsize').list().length, is(3));
		assertTrue(new File(repo.rootDirectory, '4001/51/69/photo/fullsize/1_1_14.JPG').isFile());
		assertTrue(new File(repo.rootDirectory, '4001/51/69/photo/thumbnail/1_1_14.JPG').isFile());
	}

	@Test
	public void testThatRenameWithSameLabelActuallyKeepsIt() {
		Attachment actual = repo.rename(new Attachment(4001L, 51L, 69L, PHOTO, '14', 'Jérôme', 1, 1, 'JPG'), 'Jérôme');
		assertThat(actual, is(new Attachment(4001L, 51L, 69L, PHOTO, '14', 'Jérôme', 1, 1, 'JPG')));
		assertThat(new File(repo.rootDirectory, '4001/51/69/photo/fullsize').list().length, is(3));
		assertTrue(new File(repo.rootDirectory, '4001/51/69/photo/fullsize/1_1_14.JPG').isFile());
		assertTrue(new File(repo.rootDirectory, '4001/51/69/photo/thumbnail/1_1_14.JPG').isFile());
	}

	@Test
	public void testThatRenameKeepsCover() {
		Attachment before = new Attachment(4001L, 51L, 69L, PHOTO, '14', 'Jérôme', 1, 1, 'JPG');
		repo.setCover(before.officeId, before.missionId, before.bienId, before);
		Attachment after = repo.rename(before, 'hop');
		assertThat(repo.getCover(before.officeId, before.missionId, before.bienId), is(after));
	}

	@Test
	public void testThatGetContentReturnsActualFileFormats() {
		Map<Format, File> actual = repo.getContentByFormat(new Attachment(4001L, 51L, 69L, PHOTO, '14', 'Jérôme', 1, 1, 'JPG'));
		assertThat(actual.size(), is(2));
		assertThat(actual[FULLSIZE].path, containsString('1_1_14.JPG'));
		assertThat(actual[FULLSIZE].path, containsString(FULLSIZE.name));
		assertThat(actual[THUMBNAIL].path, containsString('1_1_14.JPG'));
		assertThat(actual[THUMBNAIL].path, containsString(THUMBNAIL.name));
	}

	@Test
	public void testListFormats() {
		File photoDir = new File(repo.rootDirectory, '4001/51/69/photo');
		Map<Format, File> actual = repo.getFilesByFormats(photoDir, new Attachment(4001L, 51L, 69L, PHOTO, '13', 'Belle montagne', 1, 0, 'JPG'));
		assertThat(actual.size(), is(1));
		assertThat(actual[FULLSIZE].path, containsString('1_0_13.JPG'));
		assertThat(actual[FULLSIZE].path, containsString(FULLSIZE.name));
	}

	@Test
	public void testGetServletPathsByFormat() {
		Map<Format, String> actual = repo.getServletPathByFormat(new Attachment(4001L, 51L, 69L, PHOTO, '12', 'virage à droite', 0, 0, 'JPG'));
		assertThat(actual.size(), is(1));
		assertEquals('/documents/4001/51/69/photo/fullsize/0_0_12.JPG', actual[FULLSIZE]);
	}

	@Test
	public void testThatParseMetaDataFileRetunsValidMetaData() {
		File metaDataFile = new File(repo.rootDirectory, '4001/51/69/meta-data.json');
		BienMetaData metaData = BienMetaData.parseMetaData(metaDataFile);
		assertNotNull(metaData);
		assertEquals(69L, metaData.cover.bienId);
		assertEquals(4001L, metaData.cover.officeId);
		assertEquals(0, metaData.cover.displayRow);
		assertEquals(2, metaData.cover.gallery.formats.size());
		assertEquals('photo', metaData.cover.gallery.name);
	}
}
