/*    
**    Copyright 2010, The LimeIME Open Source Project
** 
**    Project Url: http://code.google.com/p/limeime/
**                 http://android.toload.net/
**
**    This program is free software: you can redistribute it and/or modify
**    it under the terms of the GNU General Public License as published by
**    the Free Software Foundation, either version 3 of the License, or
**    (at your option) any later version.

**    This program is distributed in the hope that it will be useful,
**    but WITHOUT ANY WARRANTY; without even the implied warranty of
**    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
**    GNU General Public License for more details.

**    You should have received a copy of the GNU General Public License
**    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package net.toload.main;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import net.toload.main.SearchService.SearchServiceImpl;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;

public class SearchService extends Service {

	private LimeDB db = null;
	private LimeHanConverter hanConverter = null;
	private static LinkedList diclist = null;
	private static List scorelist = null;

	private static StringBuffer selectedText = new StringBuffer();
	private static String tablename = "";

	private NotificationManager notificationMgr;
	
	private LIMEPreferenceManager mLIMEPref;

	// Temp Mapping Object For updateMapping method.
	Mapping updateMappingTemp = null;
	
	private static SearchServiceImpl obj = null;

	private static int recAmount = 0;
	private static boolean softkeypressed;
	
	private static List preresultlist = null;
	private static String precode = null;
	
	private static ConcurrentHashMap<String, List> cache = null;
	private static ConcurrentHashMap<String, List> engcache = null;

	public class SearchServiceImpl extends ISearchService.Stub {

		Context ctx = null;

		SearchServiceImpl(Context ctx) {
			this.ctx = ctx;	
			mLIMEPref = new LIMEPreferenceManager(ctx);
			
		}
		
		public void setSelectedText(String text){
			selectedText = new StringBuffer();
			selectedText.append(text);
		}
		
		public String getSelectedText(){
			if(selectedText != null){
				return selectedText.toString().trim();
			}else{
				return "";
			}
		}
		
		public String hanConvert(String input){
			if(hanConverter == null){
				FileUtilities fu = new FileUtilities();
				File hanDBFile = fu.isFileNotExist("/data/data/net.toload.main/databases/hanconvert.db");
				if(hanDBFile!=null){
					fu.copyRAWFile(ctx.getResources().openRawResource(R.raw.hanconvert), hanDBFile);
					
				}
				hanConverter = new LimeHanConverter(ctx);
				}
			//SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
			Integer hanConvertOption = mLIMEPref.getHanCovertOption(); //Integer.parseInt(sp.getString(HanConvertOption, "0"));
			
			return hanConverter.convert(input, hanConvertOption);
			
		}
		
		public String getTablename(){
			return tablename;
		}
		
		public void setTablename(String table){
			if(db == null){loadLimeDB();}
			db.setTablename(table);
			tablename = table;
		}
		
		private void loadLimeDB()
		{	
/*			FileUtilities fu = new FileUtilities();
			fu.copyPreLoadLimeDB(ctx);	*/		
			db = new LimeDB(ctx);
		}
		
		//Modified by Jeremy '10,3 ,12 for more specific related word
		//-----------------------------------------------------------
		public List queryUserDic(String word) throws RemoteException {
			if(db == null){loadLimeDB();}
			List result = db.queryUserDict(word);
			return result;
		}
		//-----------------------------------------------------------
		
		public Cursor getDictionaryAll(){
			if(db == null){loadLimeDB();}
			return db.getDictionaryAll();
			
		}
		
		//Add by jeremy '10, 4,1
		public void rQuery(String word) throws RemoteException {
			if(db == null){loadLimeDB();}
			String result = db.getRMapping(word);
			if(result!=null && !result.equals("")){
				displayNotificationMessage(result);
			}
		
		}
		
		public List query(String code, boolean softkeyboard) throws RemoteException {
			
			if(db == null){loadLimeDB();}
			
			//Log.i("ART","Run SearchSrv query:"+ code);
			// Check if system need to reset cache
			
			if(mLIMEPref.getParameterBoolean(LIME.SEARCHSRV_RESET_CACHE)){
				cache = new ConcurrentHashMap(LIME.SEARCHSRV_RESET_CACHE_SIZE);
				engcache = new ConcurrentHashMap(LIME.SEARCHSRV_RESET_CACHE_SIZE);
				mLIMEPref.setParameter(LIME.SEARCHSRV_RESET_CACHE,false);
			}
			
			if(preresultlist == null){preresultlist = new LinkedList();}
			List<Mapping> result = new LinkedList();
			// Modified by Jeremy '10, 3, 28.  yes-> .. The database is loading (yes) and finished (no).
			//if(code != null && loadingstatus != null && loadingstatus.equalsIgnoreCase("no")){
			if(code!=null) {
				// clear mappingidx when user switching between softkeyboard and hard keyboard.
				if(softkeypressed != softkeyboard){
					softkeypressed = softkeyboard;
				}
				//recAmount = mLIMEPref.getSimilarCodeCandidates();
			
				
				if (code != null) {
					Mapping temp = new Mapping();
						    temp.setCode(code);
						    temp.setWord(code);
				    result.add(temp);
				    // Do this in updatecandidates already
					code = code.toLowerCase();
					if(code.length() == 1){
						preresultlist = new LinkedList();
					}
				}
				
				
			    List cacheTemp = cache.get(db.getTablename()+code);
			    
				if(cacheTemp != null){
					result.addAll(cacheTemp);
					preresultlist = cacheTemp;
				}else{

					List templist = db.getMapping(code, softkeyboard);
					if(templist.size() > 0){
						result.addAll(templist);
						preresultlist = templist;
						cache.put(db.getTablename()+code, templist);
					}else{
						if(code.length() > 3 &&  
								cache.get(db.getTablename()+code.subSequence(0, code.length()-1)) != null && 
								cache.get(db.getTablename()+code.subSequence(0, code.length()-2)) != null 
						){ 
							boolean similiarCheck = true;
							result.addAll(preresultlist);
						}
					}
					
				}
			}
			return result;
		}

		public void initial() throws RemoteException {
			cache = new ConcurrentHashMap(LIME.SEARCHSRV_RESET_CACHE_SIZE);
			engcache = new ConcurrentHashMap(LIME.SEARCHSRV_RESET_CACHE_SIZE);
		}

		/*public List<Mapping> sortArray(String precode, List<Mapping> src) {
			
			// Modified by jeremy '10, 4, 5. Buf fix for 3row remap. code may not equal to precode.
				if(src != null && src.size() > 1){
					for (int i = 1; i < (src.size() - 1); i++) {
						for (int j = i + 1; j < src.size(); j++) {
							if (src.get(j).getScore() > src.get(i).getScore()) {
								Mapping dummy = src.get(i);
								if(!dummy.getCode().equals(precode) && !src.get(j).getCode().equals(precode)){
									src.set(i, src.get(j));
									src.set(j, dummy);
								}
							}
						}
					}
				}
			return src;
		}*/

		public void addUserDict(String id, String code, String word,
				String pword, int score, boolean isDictionary)
				throws RemoteException {

				Log.i("ART","addUserDict:"+diclist);
			
				if(diclist == null){diclist = new LinkedList();}
				
				Mapping temp = new Mapping();
			      temp.setId(id);
			      temp.setCode(code);
			      temp.setWord(word);
			      temp.setPword(pword);
			      temp.setScore(score);
			      temp.setDictionary(isDictionary);
			    diclist.addLast(temp);
		}

		public void updateUserDict() throws RemoteException {
			//Log.i("ART","updateUserDict:"+diclist);
			
			if(db == null){db = new LimeDB(ctx);}
			if(diclist != null && diclist.size() > 1){
				SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
				boolean item = sp.getBoolean(LIME.CANDIDATE_SUGGESTION, false);
				if(item && diclist != null){
					//Log.i("ART","updateUserDict:"+item);
					db.addDictionary(diclist);
					diclist.clear();
				}

				boolean item2 = sp.getBoolean(LIME.LEARNING_SWITCH, false);

				if(item2 && scorelist != null){
					for(int i=0 ; i < scorelist.size(); i++){
						//Log.i("ART","updateUserDict addScore:"+((Mapping)scorelist.get(i)).getCode() + " " + ((Mapping)scorelist.get(i)).getId());
						db.addScore((Mapping)scorelist.get(i));
					}
					scorelist.clear();
				}	
			}
		}
		
		public String keyToChar(String code){
			if(db == null){loadLimeDB();}
			return db.keyToChar(code, tablename);
		}

		@Override
		public void updateMapping(String id, String code, String word,
				String pword, int score, boolean isDictionary)
				throws RemoteException {
			//Log.i("ART","updateMapping:"+scorelist);
			SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
			boolean item = sp.getBoolean(LIME.LEARNING_SWITCH, false);

			if(scorelist == null){scorelist = new ArrayList();}
			if(db == null){db = new LimeDB(ctx);}
			
			updateMappingTemp = new Mapping();
			updateMappingTemp.setId(id);
			updateMappingTemp.setCode(code);
			updateMappingTemp.setWord(word);
			updateMappingTemp.setPword(pword);
			updateMappingTemp.setScore(score);
			updateMappingTemp.setDictionary(isDictionary);
		      
			if(item){
				//Log.i("ART","updateMapping:"+updateMappingTemp);
				scorelist.add(updateMappingTemp);
			}		
			
		}

		@Override
		public List getKeyboardList() throws RemoteException {
			if(db == null){db = new LimeDB(ctx);}
			List<KeyboardObj> result = db.getKeyboardList();
			return result;
		}

		@Override
		public List getImList() throws RemoteException {
			if(db == null){db = new LimeDB(ctx);}
			List<ImObj> result = db.getImList();
			return result;
		}

		@Override
		public void clear() throws RemoteException {
			if(diclist != null){
				diclist.clear();
			}
			if(scorelist != null){
				scorelist.clear();
			}
			if(cache != null){
				cache.clear();
				engcache.clear();
			}
		}

		@Override
		public List queryDictionary(String word) throws RemoteException {

			List<Mapping> result = new LinkedList<Mapping>();
		    List cacheTemp = engcache.get(word);
		    
			if(cacheTemp != null){
				result.addAll(cacheTemp);
			}else{
				if(db == null){loadLimeDB();}
				List<String> tempResult = db.queryDictionary(word);
				for(String u: tempResult){
					Mapping temp = new Mapping();
				      temp.setWord(u);
				      temp.setDictionary(true);
				      result.add(temp);
				}
				if(result.size() > 0){
					engcache.put(word, result);
				}
			}
			return result;
			
		}
	}

	@Override
	public IBinder onBind(Intent arg0) {
		if(obj == null){
			obj = new SearchServiceImpl(this);
		}
		return obj;
	}

	
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Service#onCreate()
	 */
	@Override
	public void onCreate() {
		notificationMgr =(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		super.onCreate();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Service#onDestroy()
	 */
	@Override
	public void onDestroy() {
		if(db != null){
			db.close();
		}
		super.onDestroy();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Service#onStart(android.content.Intent, int)
	 */
	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
	}
	
	private void displayNotificationMessage(String message){
		Notification notification = new Notification(R.drawable.icon, message, System.currentTimeMillis());
		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,new Intent(this, LIMEMenu.class), 0);
			      	 notification.setLatestEventInfo(this, this.getText(R.string.ime_setting), message, contentIntent);
			         notificationMgr.notify(0, notification);
	}
	
	
}
