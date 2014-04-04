package edu.ucla.cens.FocusGame;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Displays characters for a short period of time, then displays a new
 * character a different, short period of time later. Also, there is a "bad
 * character". Keeps track of the number of times and how quickly each
 * character was tapped, if the character was a bad character or not, and how
 * many times characters were missed.
 * 
 * The game has multiple rounds which change the frequency and length of time
 * displayed for each letter.
 * 
 * Nothing is passed into or returned from this Activity. It calls the Results
 * Activity after the game is over.
 * 
 * @author John Jenkins
 * @version 1.0
 */
public class Game extends Activity implements OnClickListener
{
	private static final String TAG = "FocusGame.Game";
	
	// Key to use to retrieve the input from the user.
	private static final String KEY_INPUT = "input";
	
	// Key to assign a value when the game type is unknown.
	private static final String KEY_UNKNOWN = "unknown";
	
	// Possible game types.
	private static enum GameType { LETTER, IMAGE };
	
	// Handler message types.
	private static final int NEW_CHAR_MESSAGE = 1;
	private static final int CLEAR_CHAR_MESSAGE = 2;
	
	private static final int NEW_IMAGE_MESSAGE = 3;
	private static final int CLEAR_IMAGE_MESSAGE = 4;
	
	private static final int END_GAME_MESSAGE = 5;
	
	// Visibility constants.
	private static final long FIRST_DELAY = 2000;
	private static final long VISIBLE_MILLIS = 500;
	private static final long[] DELAYS_MILLIS = { 1000, 2000 };
	public static final int NUM_ITEMS_PER_ROUND = 30;
	
	// The frequency at which a bad image/character will be shown.
	private static final float BAD_FREQUENCY = 0.1f;
	
	// Possible good images/characters.
	private static final char[] AVAILABLE_CHARS = { 'B', 'C', 'D', 'F', 'G', 'H', 'J', 'K', 'L', 'M', 'N', 'P', 'R', 'S', 'T' };
	private Bitmap[] imageReferences;
	
	// Result keys.
	public static final String REPORT_GOOD_CHAR_HITS = "good_char_hits";
	public static final String REPORT_BAD_CHAR_HITS = "bad_char_hits";
	public static final String REPORT_GOOD_CHAR_MISSES = "good_char_misses";
	public static final String REPORT_BAD_CHAR_SKIPS = "bad_char_skips";
	public static final String REPORT_REPEAT_TAPS = "repeat_taps";
	public static final String REPORT_RESPONSE_TIMES = "response_times";
	public static final String REPORT_SINGLE_VALUE_RESULT = "score";
	public static final String REPORT_FEEDBACK = "feedback";
	
	// A dummy, empty image.
	private Bitmap emptyImage;
	
	// The bad character/image.
	private static final char BAD_CHAR = 'X';
	private Bitmap badImage;
	
	/**
	 * This should be computed rather than being a constant.
	 */
	private static final float TEXT_SIZE = 250.0f;
	
	private GameType gameType;
	
	private TextView charText;
	private ImageView imageView;
	
	private Random randomGenerator;
	
	private char currChar;
	private Bitmap currImage;
	private Map<Bitmap, String> imageLookup;
	
	private int numBadItemHits;
	private int numGoodItemHits;
	private int numGoodItemMisses;
	private int numBadItemSkips;
	private int numRepeatTaps;
	private int numItems;
	
	private boolean currItemMissed;
	
	private long timeCurrItemDisplayed;
	private JSONArray responseTimes;
	
	private int round;
	private long delay;
	
	/**
	 * Used to catch "tick" events when what is being displayed changes.
	 */
	Handler switcher = new Handler()
	{
		/**
		 * Handles the message by calling its respective local function.
		 */
		@Override
		public void handleMessage(Message msg)
		{
			switch(msg.what)
			{
			case NEW_CHAR_MESSAGE:
				printNewChar();
				break;
				
			case CLEAR_CHAR_MESSAGE:
				clearChar();
				break;
				
			case NEW_IMAGE_MESSAGE:
				showNewImage();
				break;
				
			case CLEAR_IMAGE_MESSAGE:
				clearImage();
				break;
				
			case END_GAME_MESSAGE:
				endGame();
				break;
				
			default:
				break;
			}
		}
	};
	
	/**
	 * Sets up the TextView on the screen and local variables. It then begins
	 * the flow of the program by starting the first round, round 0.
	 */
	@Override
	public void onCreate(Bundle savedInstance)
	{
		Log.i(TAG, "onCreate()");
		
		super.onCreate(savedInstance);
		setContentView(R.layout.game);
		LinearLayout layout = (LinearLayout) findViewById(R.id.main_layout);
		
		String tGameType = getIntent().getStringExtra(KEY_INPUT);
		
		if((tGameType == null) || (tGameType.toLowerCase().equals(GameType.LETTER.name().toLowerCase()))) {
			charText = new TextView(this);
			charText.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
			charText.setTextSize(TEXT_SIZE);
			charText.setOnClickListener(this);
			
			layout.setBackgroundColor(0x000000);
			layout.addView(charText, new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
			
			gameType = GameType.LETTER;
			
			switcher.sendMessageAtTime(switcher.obtainMessage(NEW_CHAR_MESSAGE), SystemClock.uptimeMillis() + FIRST_DELAY);
		}
		else if(tGameType.toLowerCase().equals(GameType.IMAGE.name().toLowerCase())) {
			layout.setBackgroundColor(android.graphics.Color.WHITE);
			emptyImage = BitmapFactory.decodeResource(getResources(), R.drawable.hole);

			imageView = new ImageView(this);
			imageView.setScaleType(ScaleType.FIT_CENTER);
			imageView.setBackgroundColor(android.graphics.Color.TRANSPARENT);
			imageView.setOnClickListener(this);
			imageView.setImageBitmap(emptyImage);
			layout.addView(imageView, new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
			
			imageReferences = new Bitmap[11];
			imageReferences[0] = BitmapFactory.decodeResource(getResources(), R.drawable.bighair);
			imageReferences[1] = BitmapFactory.decodeResource(getResources(), R.drawable.blonde);
			imageReferences[2] = BitmapFactory.decodeResource(getResources(), R.drawable.brunette);
			imageReferences[3] = BitmapFactory.decodeResource(getResources(), R.drawable.cowboy);
			imageReferences[4] = BitmapFactory.decodeResource(getResources(), R.drawable.eyeball);
			imageReferences[5] = BitmapFactory.decodeResource(getResources(), R.drawable.fez);
			imageReferences[6] = BitmapFactory.decodeResource(getResources(), R.drawable.french);
			imageReferences[7] = BitmapFactory.decodeResource(getResources(), R.drawable.hair);
			imageReferences[8] = BitmapFactory.decodeResource(getResources(), R.drawable.hawaiin);
			imageReferences[9] = BitmapFactory.decodeResource(getResources(), R.drawable.sombraro);
			imageReferences[10] = BitmapFactory.decodeResource(getResources(), R.drawable.space);
			
			badImage = BitmapFactory.decodeResource(getResources(), R.drawable.aubergine);
			
			imageLookup = new HashMap<Bitmap, String>();
			imageLookup.put(imageReferences[0], "bighair");
			imageLookup.put(imageReferences[1], "blonde");
			imageLookup.put(imageReferences[2], "brunette");
			imageLookup.put(imageReferences[3], "cowboy");
			imageLookup.put(imageReferences[4], "eyeball");
			imageLookup.put(imageReferences[5], "fez");
			imageLookup.put(imageReferences[6], "french");
			imageLookup.put(imageReferences[7], "hair");
			imageLookup.put(imageReferences[8], "hawaiin");
			imageLookup.put(imageReferences[9], "sombraro");
			imageLookup.put(imageReferences[10], "space");
			imageLookup.put(badImage, "aubergine");
			
			gameType = GameType.IMAGE;
			
			switcher.sendMessageAtTime(switcher.obtainMessage(NEW_IMAGE_MESSAGE), SystemClock.uptimeMillis() + FIRST_DELAY);
		}
		else {
			Toast.makeText(this, "Unknown game type: " + tGameType, Toast.LENGTH_LONG).show();
			finish();
		}
		
		randomGenerator = new Random();
		
		numBadItemHits = 0;
		numGoodItemHits = 0;
		numGoodItemMisses = 0;
		numBadItemSkips = 0;
		numRepeatTaps = 0;
		
		responseTimes = new JSONArray();
		
		round = 0;
		delay = DELAYS_MILLIS[round];
	}
	
	/**
	 * Called when a character is clicked but keeps track of state such that
	 * the exact same character is never clicked twice.
	 */
	@Override
	public void onClick(View v)
	{
		if(currItemMissed)
		{
			try
			{
				String key = KEY_UNKNOWN;
				if((gameType == null) || GameType.LETTER.equals(gameType))
				{
					key = Character.toString(currChar);
				}
				else if(GameType.IMAGE.equals(gameType))
				{
					key = imageLookup.get(currImage);
				}
				
				JSONObject response = new JSONObject();
				response.put(key, SystemClock.uptimeMillis() - timeCurrItemDisplayed);
				responseTimes.put(response);
			}
			catch(JSONException e)
			{
				Log.e(TAG, "Error while adding response to the list of responses.", e);
			}
			
			if((gameType == null) || GameType.LETTER.equals(gameType))
			{
				if(currChar == BAD_CHAR)
				{
					numBadItemHits++;
				}
				else
				{
					numGoodItemHits++;
				}
			}
			else if(GameType.IMAGE.equals(gameType))
			{
				if(currImage.equals(badImage))
				{
					numBadItemHits++;
				}
				else
				{
					numGoodItemHits++;
				}
			}
	
			currItemMissed = false;
		}
		else
		{
			numRepeatTaps++;
		}
	}
	
	/**
	 * Force-'finish()'s this without calling the Results Activity and cancels
	 * all related Handlers.
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		if(keyCode == KeyEvent.KEYCODE_BACK)
		{
			switcher.removeMessages(END_GAME_MESSAGE);
			
			switcher.removeMessages(CLEAR_IMAGE_MESSAGE);
			switcher.removeMessages(NEW_IMAGE_MESSAGE);
			
			switcher.removeMessages(CLEAR_CHAR_MESSAGE);
			switcher.removeMessages(NEW_CHAR_MESSAGE);
			
			endGame();
			
			return true;
		}
		
		return super.onKeyDown(keyCode, event);
	}
	
	/**
	 * Checks if the letter was missed then generates a new letter for the 
	 * user. It also calculates and sets the timer for when the letter should
	 * be hidden and when to switch to the next letter or end the game.
	 */
	private void printNewChar()
	{
		checkMiss();
		
		if(numItems >= NUM_ITEMS_PER_ROUND)
		{
			round++;
			delay = DELAYS_MILLIS[round];
			numItems = 1;
		}
		else
		{
			numItems++;
		}

		currItemMissed = true;
		
		currChar = newChar();
		charText.setText((new StringBuilder()).append(currChar).toString());
		
		timeCurrItemDisplayed = SystemClock.uptimeMillis();
		switcher.sendMessageAtTime(switcher.obtainMessage(CLEAR_CHAR_MESSAGE), timeCurrItemDisplayed + VISIBLE_MILLIS);
		if((numItems < NUM_ITEMS_PER_ROUND) || (round < (DELAYS_MILLIS.length - 1)))
		{
			switcher.sendMessageAtTime(switcher.obtainMessage(NEW_CHAR_MESSAGE), timeCurrItemDisplayed + delay);
		}
		else
		{
			switcher.sendMessageAtTime(switcher.obtainMessage(END_GAME_MESSAGE), timeCurrItemDisplayed + delay);
		}
	}
	
	/**
	 * Checks if the image was missed then generates a new image for the user.
	 * It also calculates and sets the timer for when the image should be
	 * hidden and when to switch to the next image or end the game.
	 */
	private void showNewImage()
	{
		checkMiss();
		
		if(numItems >= NUM_ITEMS_PER_ROUND)
		{
			round++;
			delay = DELAYS_MILLIS[round];
			numItems = 1;
		}
		else 
		{
			numItems++;
		}

		currItemMissed = true;
		
		currImage = newImage();
		imageView.setImageBitmap(currImage);
		
		timeCurrItemDisplayed = SystemClock.uptimeMillis();
		switcher.sendMessageAtTime(switcher.obtainMessage(CLEAR_IMAGE_MESSAGE), timeCurrItemDisplayed + VISIBLE_MILLIS);
		if((numItems < NUM_ITEMS_PER_ROUND) || (round < (DELAYS_MILLIS.length - 1)))
		{
			switcher.sendMessageAtTime(switcher.obtainMessage(NEW_IMAGE_MESSAGE), timeCurrItemDisplayed + delay);
		}
		else
		{
			switcher.sendMessageAtTime(switcher.obtainMessage(END_GAME_MESSAGE), timeCurrItemDisplayed + delay);
		}
	}

	/**
	 * Hides the current letter.
	 */
	private void clearChar()
	{
		charText.setText("");
	}
	
	/**
	 * Hides the current image.
	 */
	private void clearImage()
	{
		imageView.setImageBitmap(emptyImage);
	}
	
	/**
	 * Packs the results into a Bundle, sets them as the result Intent, and
	 * calls finish().
	 */
	private void endGame()
	{		
		checkMiss();
		
		Bundle extras = new Bundle();
		extras.putInt(REPORT_GOOD_CHAR_HITS, numGoodItemHits);
		extras.putInt(REPORT_BAD_CHAR_HITS, numBadItemHits);
		extras.putInt(REPORT_GOOD_CHAR_MISSES, numGoodItemMisses);
		extras.putInt(REPORT_BAD_CHAR_SKIPS, numBadItemSkips);
		extras.putInt(REPORT_REPEAT_TAPS, numRepeatTaps);
		extras.putString(REPORT_RESPONSE_TIMES, responseTimes.toString());
		
		double score = calculateScore();
		extras.putDouble(REPORT_SINGLE_VALUE_RESULT, score);
		extras.putString(REPORT_FEEDBACK, "Your score for this game was: " + score);

		Intent results = new Intent();
		results.putExtras(extras);
		setResult(Activity.RESULT_OK, results);
		
		finish();
	}
	
	/**
	 * Generates a new character or a BAD_CHAR based on a random choice from
	 * the pseudo-random number generator.
	 * 
	 * @return A pseudo-random character to be displayed to the user.
	 */
	private char newChar()
	{
		char returnChar;
		if(randomGenerator.nextDouble() <= BAD_FREQUENCY)
		{
			returnChar = BAD_CHAR;
		}
		else
		{
			returnChar = AVAILABLE_CHARS[randomGenerator.nextInt(AVAILABLE_CHARS.length)];
		}
		return returnChar;
	}
	
	/**
	 * Generates a new image reference or a BAD_IMAGE based on a random choice
	 * from the pseudo-random number generator.
	 * 
	 * @return A pseudo-random image reference to be displayed to the user.
	 */
	private Bitmap newImage()
	{
		Bitmap image;
		if(randomGenerator.nextDouble() <= BAD_FREQUENCY)
		{
			image = badImage;
		}
		else
		{
			image = imageReferences[randomGenerator.nextInt(imageReferences.length)];
		}
		return image;
	}
	
	/**
	 * If the character was a good character then it increases the number of
	 * good character misses; if it was a bad character, then it increases the
	 * number of bad character skips.
	 */
	private void checkMiss()
	{
		if(currItemMissed)
		{
			if((gameType == null) || GameType.LETTER.equals(gameType))
			{
				if(currChar == BAD_CHAR)
				{
					numBadItemSkips++;
				}
				else
				{
					numGoodItemMisses++;
				}
			}
			else if(GameType.IMAGE.equals(gameType))
			{
				if(currImage.equals(badImage))
				{
					numBadItemSkips++;
				}
				else
				{
					numGoodItemMisses++;
				}
			}
			
			try
			{
				String key = KEY_UNKNOWN;
				if((gameType == null) || GameType.LETTER.equals(gameType))
				{
					key = Character.toString(currChar);
				}
				else if(GameType.IMAGE.equals(gameType))
				{
					key = imageLookup.get(currImage);
				}
				
				JSONObject response = new JSONObject();
				response.put(key, SystemClock.uptimeMillis() - timeCurrItemDisplayed);
				responseTimes.put(response);
			}
			catch(JSONException e)
			{
				Log.e(TAG, "Error while adding response to the list of respones.", e);
			}
			
			currItemMissed = false;
		}
	}
	
	/**
	 * Calculates a score to be returned as a single value.
	 * 
	 * There are two "best scores", and this returns the "GO RT" version.
	 * 
	 * GO RT - Mean response for every non-"bad character" response.
	 * 
	 * % Inhibition - Number of BAD_CHAR_HITS divided by the total number of
	 * 				  BAD_CHARs shown.
	 * 
	 * @return A single score for the game.
	 */
	private double calculateScore()
	{
		if(responseTimes.length() == 0) {
			return 0.0;
		}
		
		double totalResponseTime = 0.0;
		for(int i = 0; i < responseTimes.length(); i++)
		{
			try
			{
				JSONObject currResponse = responseTimes.getJSONObject(i);
				Iterator<?> keys = currResponse.keys();
				while(keys.hasNext())
				{
					String key = (String) keys.next();
					if(! Character.toString(BAD_CHAR).equals(key))
					{
						totalResponseTime += currResponse.getDouble(key);
					}
				}
			}
			catch(JSONException e)
			{
				Log.e(TAG, "Error while reading response times object.", e);
			}
		}
		
		return totalResponseTime / responseTimes.length();
	}
}
