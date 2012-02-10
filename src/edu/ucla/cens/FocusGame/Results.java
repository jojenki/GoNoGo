package edu.ucla.cens.FocusGame;

import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

/**
 * Displays the results of the game that called it or whatever is passed into
 * the Bundle. Whoever calls this should use the public constants as the
 * Strings in the Bundle they package with the Intent.
 * 
 * @author John Jenkins
 * @version 1.0
 */
public class Results extends Activity
{
	private static final String TAG = "FocusGame.Results";
	
	private int numBadCharHits;
	private int numGoodCharHits;
	private int numGoodCharMisses;
	private int numBadCharSkips;
	private int numRepeatTaps;
	private double score;
	
	private JSONArray responseTimes;
	
	/**
	 * Called when the Activity is first created. Shows all the stats as
	 * collected from the Bundle in the Intent that called this Activity.
	 */
	public void onCreate(Bundle savedInstance)
	{
		Log.i(TAG, "onCreate()");
		
		super.onCreate(savedInstance);
		setContentView(R.layout.results);
		
		Bundle extras = getIntent().getExtras();
		numBadCharHits = extras.getInt(Game.REPORT_BAD_CHAR_HITS);
		numGoodCharHits = extras.getInt(Game.REPORT_GOOD_CHAR_HITS);
		numGoodCharMisses = extras.getInt(Game.REPORT_GOOD_CHAR_MISSES);
		numBadCharSkips = extras.getInt(Game.REPORT_BAD_CHAR_SKIPS);
		numRepeatTaps = extras.getInt(Game.REPORT_REPEAT_TAPS);
		score = extras.getDouble(Game.REPORT_SINGLE_VALUE_RESULT);
		
		((TextView) findViewById(R.id.num_bad_chars)).setText((new StringBuilder()).append("Number of times a bad item was tapped: ").append(numBadCharHits).toString());
		((TextView) findViewById(R.id.num_good_chars)).setText((new StringBuilder()).append("Number of times a good item was tapped: ").append(numGoodCharHits).toString());
		((TextView) findViewById(R.id.num_good_char_misses)).setText((new StringBuilder()).append("Number of times a good item was missed: ").append(numGoodCharMisses).toString());
		((TextView) findViewById(R.id.num_bad_char_skips)).setText((new StringBuilder()).append("Number of times a bad item was missed: ").append(numBadCharSkips).toString());
		((TextView) findViewById(R.id.num_repeats)).setText((new StringBuilder()).append("Number of duplicate taps: ").append(numRepeatTaps).toString());
		((TextView) findViewById(R.id.score)).setText((new StringBuilder()).append("Score: ").append(score).toString());
		
		try
		{
			responseTimes = new JSONArray(extras.getString(Game.REPORT_RESPONSE_TIMES));
			
			StringBuilder responseTimesString = new StringBuilder();
			responseTimesString.append("Response times for each item:\n");
			for(int i = 0; i < responseTimes.length(); i++)
			{
				JSONObject currResponse = responseTimes.getJSONObject(i);
				Iterator<?> keys = currResponse.keys();
				while(keys.hasNext())
				{
					String key = (String) keys.next();
					
					responseTimesString.append("\t").append(key).append(": ")
					   				   .append(currResponse.getLong(key)).append(" milliseconds\n");
				}
			}
			((TextView) findViewById(R.id.response_times)).setText(responseTimesString.toString());
		}
		catch(JSONException e) 
		{
			Log.e(TAG, "Error while reading response times.", e);
		}
	}
}
