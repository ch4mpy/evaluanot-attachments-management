package com.c4soft.evaluanot.attachment;

import static org.hamcrest.Matchers.*
import static org.junit.Assert.*;

import java.nio.file.CopyMoveHelper.CopyOptions;
import java.nio.file.Files;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

class AttachmentRepositoryImplTest {
	
	private AttachmentRepositoryImpl repo;
	
	private final File testDataDir = new File('target/test-classes/test-repo');
	
	@Before
	public void before() {
		repo = new AttachmentRepositoryImpl(new File('target/attachmentRepo'));
		repo.rootDirectory.deleteDir();
		repo.rootDirectory.mkdirs();
		testDataDir.eachDir { fromOwnerDir ->
			File toOwnerDir = new File(repo.rootDirectory, fromOwnerDir.name);
			fromOwnerDir.eachDir { fromMissionDir ->
				File toMissionDir = new File(toOwnerDir, fromMissionDir.name);
				fromMissionDir.eachDir { fromBienDir ->
					File toBienDir = new File(toMissionDir, fromBienDir.name);
					fromBienDir.eachDir { fromCollectionDir ->
						File toCollectionDir = new File(toBienDir, fromCollectionDir.name);
						toCollectionDir.mkdirs();
						fromCollectionDir.eachFile {
							Files.copy(it.toPath(), new File(toCollectionDir, it.name).toPath());
						}
					}
				}
			}
		}
	}

	@Test
	public void testThatValidCollectionsAreLoadedWithoutModifications() {
		Map<Integer, Map<Integer, Attachment>> actual = repo.findByOfficeIdAndMissionIdAndBienIdAndCollectionTypeMapByColumnAndRow(4001L, 51L, 69L, CollectionType.PHOTO);
		assertThat(actual.size(), is (2));
		assertThat(actual[0].size(), is (1));
		assertThat(actual[0][0], is(new Attachment(4001L, 51L, 69L, CollectionType.PHOTO, 'virage à droite', 0, 0, 'JPG')));
		assertThat(actual[1].size(), is (2));
		assertThat(actual[1][0], is(new Attachment(4001L, 51L, 69L, CollectionType.PHOTO, 'Belle montagne', 1, 0, 'JPG')));
		assertThat(actual[1][1], is(new Attachment(4001L, 51L, 69L, CollectionType.PHOTO, 'eyes-wide-open', 1, 1, 'JPG')));
	}

	@Test
	public void testThatInvalidCollectionsAreCorrectedAtLoadTime() {
		Map<Integer, Map<Integer, Attachment>> actual = repo.findByOfficeIdAndMissionIdAndBienIdAndCollectionTypeMapByColumnAndRow(4001L, 51L, 42L, CollectionType.PHOTO);
		assertThat(actual.size(), is (1));
		assertThat(actual[0].size(), is (3));
		assertThat(actual[0][0], is(new Attachment(4001L, 51L, 42L, CollectionType.PHOTO, 'missing 0_0', 0, 0, 'JPG')));
		assertThat(actual[0][1], is(new Attachment(4001L, 51L, 42L, CollectionType.PHOTO, 'order collision', 0, 1, 'JPG')));
		assertThat(actual[0][2], is(new Attachment(4001L, 51L, 42L, CollectionType.PHOTO, '0_invalid name', 0, 2, 'JPG')));
	}
	
	@Test
	public void testThatRetrievingNotExistingCollectionCreatesAnEmptyOne() {
		Map<Integer, Map<Integer, Attachment>> actual = repo.findByOfficeIdAndMissionIdAndBienIdAndCollectionTypeMapByColumnAndRow(0L, 1L, 2L, CollectionType.PHOTO);
		assertThat(actual.size(), is (0));
		assertTrue(new File(repo.rootDirectory, '0/1/2/photo').isDirectory());
	}
	
	@Test
	public void testThatCreateInExistingColumnMovesExistingAttachments() {
		Map<Integer, Map<Integer, Attachment>> actual = repo.create(new File('target/test-classes/moto.jpg'), 4001L, 51L, 69L, CollectionType.PHOTO, 'monster', 1, 1);
		assertThat(actual[1].size(), is (3));
		assertThat(actual[1][0], is(new Attachment(4001L, 51L, 69L, CollectionType.PHOTO, 'Belle montagne', 1, 0, 'JPG')));
		assertThat(actual[1][1], is(new Attachment(4001L, 51L, 69L, CollectionType.PHOTO, 'monster', 1, 1, 'jpg')));
		assertThat(actual[1][2], is(new Attachment(4001L, 51L, 69L, CollectionType.PHOTO, 'eyes-wide-open', 1, 2, 'JPG')));
	}
	
	@Test
	public void testThatDeleteActuallyRemovesFileFromFileSystemAndShiftsOtherColumnAttachments() {
		Map<Integer, Map<Integer, Attachment>> actual = repo.delete(new Attachment(4001L, 51L, 69L, CollectionType.PHOTO, 'Belle montagne', 1, 0, 'JPG'));
		assertThat(actual[1].size(), is (1));
		assertThat(actual[1][0], is(new Attachment(4001L, 51L, 69L, CollectionType.PHOTO, 'eyes-wide-open', 1, 0, 'JPG')));
		assertThat(new File(repo.rootDirectory, '4001/51/69/photo').list().length, is(2));
	}
	
	@Test
	public void testThatMoveRenamesFilesAndReturnsNewAttachments() {
		Map<Integer, Map<Integer, Attachment>> actual = repo.move(new Attachment(4001L, 51L, 69L, CollectionType.PHOTO, 'Belle montagne', 1, 0, 'JPG'), 0, 0);
		assertThat(actual.size(), is (2));
		assertThat(actual[0].size(), is (2));
		assertThat(actual[0][0], is(new Attachment(4001L, 51L, 69L, CollectionType.PHOTO, 'Belle montagne', 0, 0, 'JPG')));
		assertThat(actual[0][1], is(new Attachment(4001L, 51L, 69L, CollectionType.PHOTO, 'virage à droite', 0, 1, 'JPG')));
		assertThat(actual[1].size(), is (1));
		assertThat(actual[1][0], is(new Attachment(4001L, 51L, 69L, CollectionType.PHOTO, 'eyes-wide-open', 1, 0, 'JPG')));
	}
	
	@Test
	public void testThatRenameModifiesFileNameAndReturnsAttachmentWithNewLabel() {
		Attachment actual = repo.rename(new Attachment(4001L, 51L, 69L, CollectionType.PHOTO, 'Belle montagne', 1, 0, 'JPG'), 'toto');
		assertThat(actual, is(new Attachment(4001L, 51L, 69L, CollectionType.PHOTO, 'toto', 1, 0, 'JPG')));
		assertThat(new File(repo.rootDirectory, '4001/51/69/photo').list().length, is(3));
		assertTrue(new File(repo.rootDirectory, '4001/51/69/photo/1_0_toto.JPG').isFile());
	}
	
	@Test
	public void testThatGetContentReturnsActualFile() {
		assertTrue(repo.getContent(new Attachment(4001L, 51L, 69L, CollectionType.PHOTO, 'Belle montagne', 1, 0, 'JPG')).isFile());
	}

}
