evaluanot-attachments-management
================================
A small library made to store Evalu@not attached documents in a file system.
Those documents are grouped by officeId, missionId, bienId and CollectionType (maps, pictures, etc.).
Attachments within a group can be ordered in columns and rows.
Columns behave as stacks, so empty cells are removed and following cells are shifted.
An effort was made for robustness against file system external modification and failure : provided repository implementation methods return what is actually available from the file system (and not what it expect to be there).