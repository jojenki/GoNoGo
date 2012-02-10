package edu.ucla.cens.FocusGame;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

/**
 * Displays the instructions. Press "Back" to return to wherever you were. 
 * 
 * @author John Jenkins
 * @version 1.0
 */
public class Instructions extends Activity
{
	private static final String TAG = "FocusGame.Instructions";
	
	/**
	 * Creates the content view and displays it.
	 */
	@Override
	public void onCreate(Bundle savedInstance)
	{
		Log.i(TAG, "onCreate()");
		
		super.onCreate(savedInstance);
		setContentView(R.layout.instructions);
	}
}
