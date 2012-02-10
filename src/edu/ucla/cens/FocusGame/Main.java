package edu.ucla.cens.FocusGame;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

/**
 * The main Activity of the program, it instructs the user to begin the game
 * or to view the instructions.
 * 
 * @author John Jenkins
 * @version 1.0
 */
public class Main extends Activity implements OnClickListener
{
	private static final String TAG = "FocusGame.Main";
	
	private static final int GAME_REQUEST_CODE = 1;
	
    /** 
     * Called when the activity is first created. 
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
    	Log.i(TAG, "onCreate()");
    	
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        ((Button) findViewById(R.id.begin_button)).setOnClickListener(this);
        ((Button) findViewById(R.id.instructions_button)).setOnClickListener(this);
    }
    
    /**
     * Handles the button clicks.
     */
    public void onClick(View v)
    {
    	if(v.getId() == R.id.begin_button)
    	{
    		final Context self = this;
    		
    		AlertDialog.Builder builder = new AlertDialog.Builder(this);
    		builder.setTitle("Letters or Images?")
    			   .setPositiveButton("Letters", new DialogInterface.OnClickListener() {
    				   @Override
    				   public void onClick(DialogInterface dialog, int which) {
    					   Intent newGame = new Intent(self, Game.class);
    					   newGame.putExtra("input", "letter");
    					   startActivityForResult(newGame, GAME_REQUEST_CODE);
    				   }
    			   })
    			   .setNegativeButton("Images", new DialogInterface.OnClickListener() {
    				   @Override
    				   public void onClick(DialogInterface dialog, int which) {
    					   Intent newGame = new Intent(self, Game.class);
    					   newGame.putExtra("input", "image");
    					   startActivityForResult(newGame, GAME_REQUEST_CODE);
    				   }
    			   });
    		builder.create().show();
    	}
    	else if(v.getId() == R.id.instructions_button)
    	{
    		Intent instructions = new Intent(this, Instructions.class);
    		startActivity(instructions);
    	}
    }
    
    /**
     * Catches the return of the game.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
    	switch(requestCode)
    	{
    	case GAME_REQUEST_CODE:
    		if(resultCode == Activity.RESULT_OK)
    		{
    			Intent results = new Intent(this, Results.class);
    			results.putExtras(data.getExtras());
    			startActivity(results);
    		}
    		else if(requestCode == Activity.RESULT_CANCELED)
    		{
    			Log.i(TAG, "Game canceled, so the scores won't be displayed.");
    		}
    		else
    		{
    			Log.i(TAG, "Unknown result code.");
    		}
    		break;
    		
    	default:
    		Log.e(TAG, "Unknown request code: " + requestCode);
    	}
    }
}