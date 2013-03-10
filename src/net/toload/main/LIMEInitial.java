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

import net.toload.main.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnCancelListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.database.Cursor;
import android.database.sqlite.SQLiteCursor;


/**
 * 
 * @author Art Hung
 * 
 */
public class LIMEInitial extends Activity {
	

	private IDBService DBSrv = null;
	Button btnInitPreloadDB = null;
	Button btnInitEmptyDB = null;
	Button btnResetDB = null;
	Button btnBackupDB = null;
	Button btnRestoreDB = null;
	Button btnStoreDevice = null;
	Button btnStoreSdcard = null;
	
	boolean hasReset = false;
	LIMEPreferenceManager mLIMEPref;
	ConnectivityManager connManager = null;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle icicle) {
		
		super.onCreate(icicle);
		this.setContentView(R.layout.initial);

		// Startup Service
		getApplicationContext().bindService(new Intent(IDBService.class.getName()), serConn, Context.BIND_AUTO_CREATE);
		mLIMEPref = new LIMEPreferenceManager(this.getApplicationContext());
		
        connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE); 
        
        
		// Initial Buttons
		initialButton();

		btnResetDB.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				
				// Reset Table Information
				resetLabelInfo("custom");
				resetLabelInfo("cj");
				resetLabelInfo("scj");
				resetLabelInfo("array");
				resetLabelInfo("array10");
				resetLabelInfo("dayi");
				resetLabelInfo("ez");
				resetLabelInfo("phonetic");
				
				btnResetDB.setEnabled(false);
				
				AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
		    	     				builder.setMessage(getText(R.string.l3_message_database_reset_confirm));
		    	     				builder.setCancelable(false);
		    	     				builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
		    	     					public void onClick(DialogInterface dialog, int id) {
						    					initialButton();
							    				try {
							    					DBSrv.closeDatabse();
							    					DBSrv.resetDownloadDatabase();
							    					btnInitPreloadDB.setEnabled(true);
							    					btnInitEmptyDB.setEnabled(true);
							    					btnResetDB.setEnabled(true);
						    						mLIMEPref.setParameter("im_loading", false);
						    						mLIMEPref.setParameter("im_loading_table", "");
						    						mLIMEPref.setParameter(LIME.DOWNLOAD_START, false);
						    						mLIMEPref.setParameter("dbtarget","device");
						    						btnStoreDevice.setEnabled(false);
						    						btnStoreSdcard.setEnabled(true);

						    						btnStoreDevice.setText(getText(R.string.l3_initial_btn_store_device));
						    						btnStoreSdcard.setText(getText(R.string.l3_initial_btn_store_sdcard));
							    				} catch (RemoteException e) {
							    					e.printStackTrace();
							    				}
							    				hasReset = true;
						    	        	}
						    	     });
		    	        
						    	    builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
						    	    	public void onClick(DialogInterface dialog, int id) {
					    						btnResetDB.setEnabled(true);
						    	        		hasReset = false;
						    	        	}
						    	     });   
		    	        
				AlertDialog alert = builder.create();
		    				alert.show();
		    	    
				
				// Reset for SearchSrv
				mLIMEPref.setParameter(LIME.SEARCHSRV_RESET_CACHE,false);
			}
		});
		

		btnInitPreloadDB.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {

		        if(connManager.getActiveNetworkInfo() != null && connManager.getActiveNetworkInfo().isConnected()){
		        	try {
    					DBSrv.closeDatabse();
		        		btnInitEmptyDB.setEnabled(false);
						btnInitPreloadDB.setEnabled(false);
						btnStoreDevice.setEnabled(false);
						btnStoreSdcard.setEnabled(false);

						String dbtarget = mLIMEPref.getParameterString("dbtarget");
						if(dbtarget.equals("device")){
							btnStoreSdcard.setText("");
						}else if(dbtarget.equals("sdcard")){
							btnStoreDevice.setText("");
						}
						
						mLIMEPref.setParameter(LIME.DOWNLOAD_START, true);
						Toast.makeText(v.getContext(), getText(R.string.l3_initial_download_database), Toast.LENGTH_SHORT).show();
						DBSrv.downloadPreloadedDatabase();

						// Reset for SearchSrv
						mLIMEPref.setParameter(LIME.SEARCHSRV_RESET_CACHE,false);
						
					} catch (RemoteException e) {
						mLIMEPref.setParameter(LIME.DOWNLOAD_START, false);
						e.printStackTrace();
					}
		        }else{
		        	Toast.makeText(v.getContext(), getText(R.string.l3_tab_initial_error), Toast.LENGTH_SHORT).show();
				}
			}
		});
		
		btnInitEmptyDB.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {

		        if(connManager.getActiveNetworkInfo() != null && connManager.getActiveNetworkInfo().isConnected()){
		        	try {
    					DBSrv.closeDatabse();
		        		btnInitEmptyDB.setEnabled(false);
						btnInitPreloadDB.setEnabled(false);
						btnStoreDevice.setEnabled(false);
						btnStoreSdcard.setEnabled(false);

						String dbtarget = mLIMEPref.getParameterString("dbtarget");
						if(dbtarget.equals("device")){
							btnStoreSdcard.setText("");
						}else if(dbtarget.equals("sdcard")){
							btnStoreDevice.setText("");
						}
						mLIMEPref.setParameter(LIME.DOWNLOAD_START, true);
						Toast.makeText(v.getContext(), getText(R.string.l3_initial_download_database), Toast.LENGTH_SHORT).show();
						DBSrv.downloadEmptyDatabase();

						// Reset for SearchSrv
						mLIMEPref.setParameter(LIME.SEARCHSRV_RESET_CACHE,false);
						
					} catch (RemoteException e) {
						mLIMEPref.setParameter(LIME.DOWNLOAD_START, false);
						e.printStackTrace();
					}
		        }else{
		        	Toast.makeText(v.getContext(), getText(R.string.l3_tab_initial_error), Toast.LENGTH_SHORT).show();
				}
			}
		});
		
		btnBackupDB.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
		    			
					File srcFile = new File(LIME.DATABASE_DECOMPRESS_FOLDER + File.separator + LIME.DATABASE_NAME);
					File srcFile2 = new File(LIME.DATABASE_DECOMPRESS_FOLDER_SDCARD + File.separator + LIME.DATABASE_NAME);
					if((srcFile2.exists() && srcFile2.length() > 1024) || (srcFile.exists() && srcFile.length() > 1024)){
						AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
	     				builder.setMessage(getText(R.string.l3_initial_backup_confirm));
	     				builder.setCancelable(false);
	     				builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
	     					public void onClick(DialogInterface dialog, int id) {
			    					try {
				    					DBSrv.closeDatabse();
										DBSrv.backupDatabase();
									} catch (RemoteException e) {
										e.printStackTrace();
									}
			    	        	}
			    	     });
			    	    builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
			    	    	public void onClick(DialogInterface dialog, int id) {
			    	        	}
			    	     }); 
	        
						AlertDialog alert = builder.create();
									alert.show();
					}else{
						Toast.makeText(v.getContext(), getText(R.string.l3_initial_backup_error), Toast.LENGTH_SHORT).show();
					}
			}
		});
		
		btnRestoreDB.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
					File srcFile = new File(LIME.IM_LOAD_LIME_ROOT_DIRECTORY + File.separator + LIME.DATABASE_BACKUP_NAME);
					File srcFile2 = new File(LIME.DATABASE_DECOMPRESS_FOLDER_SDCARD + File.separator + LIME.DATABASE_NAME);
					if((srcFile2.exists() && srcFile2.length() > 1024) || (srcFile.exists() && srcFile.length() > 1024)){
						
						AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
	     				builder.setMessage(getText(R.string.l3_initial_restore_confirm));
	     				builder.setCancelable(false);
	     				builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
	     					public void onClick(DialogInterface dialog, int id) {
			    					try {

			    						String dbtarget = mLIMEPref.getParameterString("dbtarget");
			    						if(dbtarget.equals("device")){
			    							btnStoreSdcard.setText("");
			    						}else if(dbtarget.equals("sdcard")){
			    							btnStoreDevice.setText("");
			    						}
		    							btnStoreSdcard.setEnabled(false);
		    							btnStoreDevice.setEnabled(false);
				    					DBSrv.closeDatabse();
			    						DBSrv.restoreDatabase();
									} catch (RemoteException e) {
										e.printStackTrace();
									}
			    	        	}
			    	     });
			    	    builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
			    	    	public void onClick(DialogInterface dialog, int id) {
			    	        	}
			    	     }); 
	        
						AlertDialog alert = builder.create();
									alert.show();
							
						// Reset for SearchSrv
						mLIMEPref.setParameter(LIME.SEARCHSRV_RESET_CACHE,false);
					}else{
						Toast.makeText(v.getContext(), getText(R.string.l3_initial_restore_error), Toast.LENGTH_SHORT).show();
					}
			}
		});
		
		btnStoreDevice.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
 				builder.setMessage(getText(R.string.l3_initial_btn_store_to_device));
 				builder.setCancelable(false);
 				builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
 					public void onClick(DialogInterface dialog, int id) {
	 						mLIMEPref.setParameter("dbtarget","device");
	 						btnStoreDevice.setEnabled(false);
	 						btnStoreSdcard.setEnabled(true);
	    	        	}
	    	     });
	    	    builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
	    	    	public void onClick(DialogInterface dialog, int id) {
	    	        	}
	    	     }); 
    
				AlertDialog alert = builder.create();
							alert.show();
				
			} 
		});
		
		btnStoreSdcard.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				
				AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
 				builder.setMessage(getText(R.string.l3_initial_btn_store_to_sdcard));
 				builder.setCancelable(false);
 				builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
 					public void onClick(DialogInterface dialog, int id) {
	 						mLIMEPref.setParameter("dbtarget","sdcard");
	 						btnStoreDevice.setEnabled(true);
	 						btnStoreSdcard.setEnabled(false);
	    	        	}
	    	     });
	    	    builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
	    	    	public void onClick(DialogInterface dialog, int id) {
	    	        	}
	    	     }); 
    
				AlertDialog alert = builder.create();
							alert.show();
							
			}
		});

	}
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onStart()
	 */
	@Override
	protected void onStart() {
		super.onStart();
		initialButton();
	}
	

	/* (non-Javadoc)
	 * @see android.app.Activity#onStart()
	 */
	@Override
	protected void onResume() {
		super.onStart();
		initialButton();
	}

	public void resetLabelInfo(String imtype){
		mLIMEPref.setParameter(imtype+LIME.IM_MAPPING_FILENAME,"");
		mLIMEPref.setParameter(imtype+LIME.IM_MAPPING_VERSION,"");
		mLIMEPref.setParameter(imtype+LIME.IM_MAPPING_TOTAL,0);
		mLIMEPref.setParameter(imtype+LIME.IM_MAPPING_DATE,"");
	}
	
	
	private void initialButton(){

		// Check if button 
		if(btnResetDB == null){
			btnResetDB = (Button) findViewById(R.id.btnResetDB);
			btnInitPreloadDB = (Button) findViewById(R.id.btnInitPreloadDB);	
			btnInitEmptyDB = (Button) findViewById(R.id.btnInitEmptyDB);	
			btnBackupDB = (Button) findViewById(R.id.btnBackupDB);
			btnRestoreDB = (Button) findViewById(R.id.btnRestoreDB);
			btnStoreDevice = (Button) findViewById(R.id.btnStoreDevice);
			btnStoreSdcard = (Button) findViewById(R.id.btnStoreSdcard);
		}
		
		String dbtarget = mLIMEPref.getParameterString("dbtarget");
		if(dbtarget.equals("")){
			dbtarget = "device";
			mLIMEPref.setParameter("dbtarget","device");
		}

		File checkSdFile = new File(LIME.DATABASE_DECOMPRESS_FOLDER_SDCARD + File.separator + LIME.DATABASE_NAME);
		File checkDbFile = new File(LIME.DATABASE_DECOMPRESS_FOLDER + File.separator + LIME.DATABASE_NAME);
		if(!checkSdFile.exists() && !checkDbFile.exists() && !mLIMEPref.getParameterBoolean(LIME.DOWNLOAD_START)){
			
			btnInitPreloadDB.setEnabled(true);
			btnInitEmptyDB.setEnabled(true);
			Toast.makeText(this, getText(R.string.l3_tab_initial_message), Toast.LENGTH_SHORT).show();
			
			if(dbtarget.equals("device")){
				btnStoreDevice.setEnabled(false);
				btnStoreSdcard.setEnabled(true);
			}else if(dbtarget.equals("sdcard")){
				btnStoreDevice.setEnabled(true);
				btnStoreSdcard.setEnabled(false);
			}
				
		}else{
			if(dbtarget.equals("device")){
				btnStoreSdcard.setText("");
			}else if(dbtarget.equals("sdcard")){
				btnStoreDevice.setText("");
			}
			btnStoreDevice.setEnabled(false);
			btnStoreSdcard.setEnabled(false);
			btnInitPreloadDB.setEnabled(false);
			btnInitEmptyDB.setEnabled(false);
		}
		
		
	}

	private ServiceConnection serConn = new ServiceConnection() {
		public void onServiceConnected(ComponentName name, IBinder service) {
			if(DBSrv == null){
				//Log.i("ART","Start up db service");
				DBSrv = IDBService.Stub.asInterface(service);
			}
		}
		public void onServiceDisconnected(ComponentName name) {}

	};
	

	@Override
    public boolean onCreateOptionsMenu(Menu menu){
    	int idGroup = 0;
    	int orderMenuItem1 = Menu.NONE;
    	
    	try {
			PackageInfo pinfo = this.getPackageManager().getPackageInfo(this.getPackageName(), 0);
	    	menu.add(idGroup, Menu.FIRST, orderMenuItem1, "LIME v" + pinfo.versionName + " - " + pinfo.versionCode);
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
    	return super.onCreateOptionsMenu(menu);
    }
	
}