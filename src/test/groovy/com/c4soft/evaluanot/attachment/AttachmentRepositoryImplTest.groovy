package com.c4soft.evaluanot.attachment;

import static org.hamcrest.Matchers.*
import static org.junit.Assert.*

import java.nio.file.Files
import java.util.Map.Entry

import org.junit.Before
import org.junit.Test

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
		repo.setCover(4001L, 51L, 69L, new Attachment(4001L, 51L, 69L, PHOTO, 'Belle montagne', 1, 0, 'JPG'));
		assertThat(actual.size(), is (2));
		assertThat(actual[0].size(), is (1));

		assertThat(actual[0][0].key, is(new Attachment(4001L, 51L, 69L, PHOTO, 'virage à droite', 0, 0, 'JPG')));
		assertThat(actual[0][0].value.size(), is (1));
		assertThat(actual[0][0].value, containsInAnyOrder([FULLSIZE].toArray()));

		assertThat(actual[1].size(), is (2));

		assertThat(actual[1][0].key, is(new Attachment(4001L, 51L, 69L, PHOTO, 'Belle montagne', 1, 0, 'JPG')));
		assertThat(actual[1][0].value.size(), is (1));
		assertThat(actual[1][0].value, containsInAnyOrder([FULLSIZE].toArray()));

		assertThat(actual[1][1].key, is(new Attachment(4001L, 51L, 69L, PHOTO, 'eyes-wide-open', 1, 1, 'JPG')));
		assertThat(actual[1][1].value.size(), is (2));
		assertThat(actual[1][1].value, containsInAnyOrder([FULLSIZE, THUMBNAIL].toArray()));
	}

	@Test
	public void testThatInvalidCollectionsAreCorrectedAtLoadTime() {
		Map<Integer, Map<Integer,  Entry<Attachment, Set<Format>>>> actual = repo.findByOfficeIdAndMissionIdAndBienIdAndGalleryMapByColumnAndRow(4001L, 51L, 42L, PHOTO);
		assertThat(actual.size(), is (1));
		assertThat(actual[0].size(), is (12));
		assertThat(actual[0][0].key, is(new Attachment(4001L, 51L, 42L, PHOTO, 'missing 0_0', 0, 0, 'JPG')));
		assertThat(actual[0][1].key, is(new Attachment(4001L, 51L, 42L, PHOTO, 'order collision', 0, 1, 'JPG')));
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
		assertThat(actual[1][0].key, is(new Attachment(4001L, 51L, 69L, PHOTO, 'Belle montagne', 1, 0, 'JPG')));
		assertEquals(new Attachment(4001L, 51L, 69L, PHOTO, "Ch4mp's monster (2)", 1, 1, 'jpg'), actual[1][1].key);
		assertThat(actual[1][2].key, is(new Attachment(4001L, 51L, 69L, PHOTO, 'eyes-wide-open', 1, 2, 'JPG')));
	}

	@Test
	public void testThatDeleteActuallyRemovesFileFromFileSystemAndShiftsOtherColumnAttachments() {
		Map<Integer, Map<Integer,  Entry<Attachment, Set<Format>>>> actual = repo.delete(new Attachment(4001L, 51L, 69L, PHOTO, 'Belle montagne', 1, 0, 'JPG'));
		assertThat(actual[1].size(), is (1));
		assertThat(actual[1][0].key, is(new Attachment(4001L, 51L, 69L, PHOTO, 'eyes-wide-open', 1, 0, 'JPG')));
		assertThat(new File(repo.rootDirectory, '4001/51/69/photo/fullsize').list().length, is(2));
	}

	@Test
	public void testThatMoveRenamesFilesAndReturnsNewAttachments() {
		Map<Integer, Map<Integer,  Entry<Attachment, Set<Format>>>> actual = repo.move(new Attachment(4001L, 51L, 69L, PHOTO, 'Belle montagne', 1, 0, 'JPG'), 0, 0);
		assertThat(actual.size(), is (2));
		assertThat(actual[0].size(), is (2));
		assertThat(actual[0][0].key, is(new Attachment(4001L, 51L, 69L, PHOTO, 'Belle montagne', 0, 0, 'JPG')));
		assertThat(actual[0][1].key, is(new Attachment(4001L, 51L, 69L, PHOTO, 'virage à droite', 0, 1, 'JPG')));
		assertThat(actual[1].size(), is (1));
		assertThat(actual[1][0].key, is(new Attachment(4001L, 51L, 69L, PHOTO, 'eyes-wide-open', 1, 0, 'JPG')));
	}

	@Test
	public void testThatRenameModifiesFileNameAndReturnsAttachmentWithNewLabel() {
		Attachment actual = repo.rename(new Attachment(4001L, 51L, 69L, PHOTO, 'eyes-wide-open', 1, 1, 'JPG'), 'toto');
		assertThat(actual, is(new Attachment(4001L, 51L, 69L, PHOTO, 'toto', 1, 1, 'JPG')));
		assertThat(new File(repo.rootDirectory, '4001/51/69/photo/fullsize').list().length, is(3));
		assertTrue(new File(repo.rootDirectory, '4001/51/69/photo/fullsize/1_1_toto.JPG').isFile());
		assertTrue(new File(repo.rootDirectory, '4001/51/69/photo/thumbnail/1_1_toto.JPG').isFile());
	}

	@Test
	public void testThatGetContentReturnsActualFileFormats() {
		Map<Format, File> actual = repo.getContentByFormat(new Attachment(4001L, 51L, 69L, PHOTO, 'eyes-wide-open', 1, 1, 'JPG'));
		assertThat(actual.size(), is(2));
		assertThat(actual[FULLSIZE].path, containsString('1_1_eyes-wide-open.JPG'));
		assertThat(actual[FULLSIZE].path, containsString(FULLSIZE.name));
		assertThat(actual[THUMBNAIL].path, containsString('1_1_eyes-wide-open.JPG'));
		assertThat(actual[THUMBNAIL].path, containsString(THUMBNAIL.name));
	}

	@Test
	public void testListFormats() {
		File photoDir = new File(repo.rootDirectory, '4001/51/69/photo');
		Map<Format, File> actual = repo.getFilesByFormats(photoDir, new Attachment(4001L, 51L, 69L, PHOTO, 'Belle montagne', 1, 0, 'JPG'));
		assertThat(actual.size(), is(1));
		assertThat(actual[FULLSIZE].path, containsString('1_0_Belle montagne.JPG'));
		assertThat(actual[FULLSIZE].path, containsString(FULLSIZE.name));
	}
	
	@Test
	public void testGetServletPathsByFormat() {
		Map<Format, String> actual = repo.getServletPathByFormat(new Attachment(4001L, 51L, 69L, PHOTO, 'virage à droite', 0, 0, 'JPG'));
		assertThat(actual.size(), is(1));
		assertEquals('/documents/4001/51/69/photo/fullsize/0_0_virage+%C3%A0+droite.JPG', actual[FULLSIZE]);
	}
}
