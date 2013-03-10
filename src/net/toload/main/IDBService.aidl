package net.toload.main;

interface IDBService
{
	void loadMapping(String filename, String tablename);
	void resetMapping(String tablename);
	void resetDownloadDatabase();
	void downloadDayiOvCin();
	void downloadPreloadedDatabase();
	void downloadEmptyDatabase();
	void backupDatabase();
	void restoreDatabase();
	void resetImInfo(String im);
	void removeImInfo(String im, String field);
	void setImInfo(String im, String field, String value);
	void setKeyboardInfo(String im, String value,String keyboard);
	void closeDatabse();
	String getImInfo(String im, String field);
	String getKeyboardCode(String im);
	List getKeyboardList();
	
}