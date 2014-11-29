evaluanot-attachments-management
================================
A small library made to store Evalu@not attached documents in a file system.
Those documents are grouped by officeId, missionId, bienId and Gallery (maps, pictures, etc.).
Attachments within a group can be ordered in columns and rows.
Columns behave as stacks, so empty cells are removed and following cells are shifted.
An effort was made for robustness against file system external modification and failure : provided repository implementation methods return what is actually available from the file system (and not what it expect to be there).
To conclude, a file can be stored in different Format (fullsize, thubnail, etc.). As so we manipulate pairs of Attachment and set of available Formats

1.0.2 released
--------------
I just released 1.0.2 wich might be production ready.<br>
Latest improvements include making added & renamed attachments labels unique within a gallery and allow many more exotic characters within labels.

Sample usage
------------
    @Controller
    class SampleController implements CommandLineRunner {
    	
    	@Autowired
    	private final AttachmentRepository attachmentRepo;
    	
    	@Value('${img.thumbnail}')
    	private final File fullsizeFile;
    	
    	@Value('${img.fullsize}')
    	private final File thumbnailFile;
    	
    	@Autowired
    	@Qualifier('photo')
    	private final Gallery photo;
    	
    	@Autowired
    	@Qualifier('fullsize')
    	private final Format fullsize;
    	
    	@Autowired
    	@Qualifier('thumbnail')
    	private final Format thumbnail;
    
    	@Override
    	public void run(String... arg0) throws Exception {
    		Map<Integer, Map<Integer,  Entry<Attachment, Set<Format>>>> columns;
    		Attachment opera;
    		Set<Format> availableOperaFormats;
    		
    		//List photo gallery for office "1", Mission "2" and bien "3"
    		columns = attachmentRepo.findByOfficeIdAndMissionIdAndBienIdAndGalleryMapByColumnAndRow(1L, 2L, 3L, photo);
    		assert(columns.size() == 0);
    		
    		//Map input files by format
    		Map<Format, File> input = [(fullsize) : fullsizeFile, (thumbnail) : thumbnailFile];
    		//And add it to the repo
    		columns = attachmentRepo.create(input, 1L, 2L, 3L, photo, 'opera', 0, 0);
    		opera = columns[0][0].key;
    		availableOperaFormats = columns[0][0].value;
    		assert(columns.size() == 1);
    		assert(columns[0].size() == 1);
    		assert(columns[0][0].key == new Attachment(1L, 2L, 3L, photo, 'opera', 0, 0, 'jpg'));
    		
    		//Elect an attachment to be the report cover for a "Bien"
    		attachmentRepo.setCover(opera);
    		assert(attachmentRepo.getCover(1L, 2L, 3L) == opera);
    		
    		//Retrieve actual files from repo (i.e. to display it)
    		Map<Format, File> operaFilesByFormat = attachmentRepo.getContentByFormat(opera);
    		assert(operaFilesByFormat.size() == 2);
    		assert(operaFilesByFormat[fullsize].name == '0_0_opera.jpg');
    		assert(operaFilesByFormat[fullsize].path.contains('fullsize'));
    		assert(operaFilesByFormat[thumbnail].name == '0_0_opera.jpg');
    		assert(operaFilesByFormat[thumbnail].path.contains('thumbnail'));
    		
    		Logger.getLogger(CommandLineRunner.class.canonicalName).info('All done, bye');
    	}
    
    }
