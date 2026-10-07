package com.gpgex;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.app.Activity;

import org.haxe.extension.Extension;
import org.haxe.lime.HaxeObject;

import android.os.AsyncTask;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.File;
import java.security.MessageDigest;

import com.google.android.gms.tasks.*;
import android.content.Intent;

import com.google.android.gms.games.PlayGamesSdk;
import com.google.android.gms.games.PlayGames;
import com.google.android.gms.games.GamesSignInClient;

//import com.google.android.gms.games.Games;
//import com.google.android.gms.games.Players;
//import com.google.android.gms.games.Player;
//import com.google.android.gms.games.PlayerBuffer;
//import com.google.android.gms.common.api.GoogleApiClient;
//import com.google.android.gms.common.ConnectionResult;
//import com.google.android.gms.common.api.ResultCallback;
//import com.google.android.gms.common.images.ImageManager;
//import com.google.android.gms.games.leaderboard.Leaderboards;
//import com.google.android.gms.games.leaderboard.LeaderboardVariant;
//import com.google.android.gms.games.leaderboard.LeaderboardScore;
//import com.google.android.gms.games.GamesStatusCodes;
//import com.google.android.gms.games.achievement.Achievements;
//import com.google.android.gms.games.achievement.Achievement;

import com.google.android.gms.games.snapshot.Snapshot;
import com.google.android.gms.games.SnapshotsClient;
import com.google.android.gms.games.snapshot.SnapshotMetadata;

//import com.google.android.gms.games.snapshot.SnapshotMetadataChange.Builder;

public class GooglePlayGames extends Extension /*implements GameHelper.GameHelperListener*/ {

  private static GooglePlayGames instance = null;
  //private static GameHelper mHelper = null;
  public static final String TAG = "EXTENSION-GOOGLEPLAYGAMES";
  private static boolean userRequiresLogin = false;
  private static SecureHaxeObject callbackObject = null;
  private static boolean enableCloudStorage = false;

  private static GamesSignInClient gamesSignInClient = null;

  ////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////

 /**
  * Called when the activity is starting.
  */
 public void onCreate() {
    Log.i(TAG, "Created");
    instance = this;
  }

  @Override
  public boolean onActivityResult(int requestCode, int resultCode, Intent intent) {
    if (intent != null) {
      Log.i(TAG, "Activity Result triggered for intent\n" + intent.toString());
      if (intent.hasExtra(SnapshotsClient.EXTRA_SNAPSHOT_METADATA)) {
        // Load a snapshot.
        SnapshotMetadata snapshotMetadata
            = intent.getParcelableExtra(SnapshotsClient.EXTRA_SNAPSHOT_METADATA);
        String saveName = snapshotMetadata.getUniqueName();
        Log.i(TAG, "Found save with name " + saveName);
        // Load the game data from the Snapshot
        // ...
      } else if (intent.hasExtra(SnapshotsClient.EXTRA_SNAPSHOT_NEW)) {
        // Create a new snapshot named with a unique string
        //String unique = new BigInteger(281, new Random()).toString(13);
        //mCurrentSaveName = "snapshotTemp-" + unique;
        Log.i(TAG, "New save");

        // Create the new snapshot
        // ...
      }
    } else {
      Log.i(TAG, "Activity Result triggered for null");
    }

    return true;
  }

  public static void init(boolean cloudStorage, HaxeObject callbackObj) {
    Log.i(TAG, "Init");

    if (callbackObj != null) {
      GooglePlayGames.callbackObject = new SecureHaxeObject(callbackObj, mainActivity, TAG);
    }
    //Log.i(TAG, "Init 2");

    /*
		if(mHelper!=null){		
			if(mHelper.isConnecting() || mHelper.isSignedIn()) return;
			mHelper=null;
		}
		enableCloudStorage=cloudStorage;
		final int maxAutoSignInAttempts = userRequiresLogin?1:0;
		userRequiresLogin=false;
     */
    PlayGamesSdk.initialize(Extension.mainContext);

    gamesSignInClient = PlayGames.getGamesSignInClient(Extension.mainActivity);
    signInSilently();
    //Log.i(TAG, "Init 3");


    /*
		mainActivity.runOnUiThread(new Runnable() {
			public void run() {
				try{
					mHelper = new GameHelper(mainActivity, GameHelper.CLIENT_GAMES | (enableCloudStorage?GameHelper.CLIENT_CLOUD_STORAGE:0));
					mHelper.enableDebugLog(true);
					mHelper.setup(GooglePlayGames.getInstance());
					mHelper.setMaxAutoSignInAttempts(maxAutoSignInAttempts);
					mHelper.onStart(mainActivity);
					Log.i(TAG, "PlayGames: INIT COMPLETE");      		
				}catch(Exception e) {
					Log.i(TAG, "PlayGames: INIT Exception");
					Log.i(TAG, e.toString());
				}
			}
		});
     */
  }

  static boolean loggingIn = false;

  public static void login(/*Runnable onSuccess = null*/) {
    Log.i(TAG, "Logn, has client: " + (gamesSignInClient != null));
    if (!loggingIn && gamesSignInClient != null) {
      loggingIn = true;
      gamesSignInClient
          .signIn()
          .addOnCompleteListener(task -> {
            loggingIn = false;
            isAuthenticated = task.getResult().isAuthenticated();
            if (task.isSuccessful() && isAuthenticated) {
              Log.i(TAG, "Sign in successful");
              //if (onSuccess != null) onSuccess.run();
            } else {
              Log.i(TAG, "Sign in failed");
            }
          });
    } else {
      Log.i(TAG, "Sign in client is null");
    }
  }

  public static boolean isLoggedIn() {
    return isAuthenticated;
  }

  static boolean isAuthenticated = false;

  static boolean signingIn = false;

  private static void signInSilently() {
    Log.i(TAG, "Silent signIn");
    if (!signingIn && gamesSignInClient != null) {
      signingIn = true;
      gamesSignInClient.isAuthenticated().addOnCompleteListener(isAuthenticatedTask -> {
        signingIn = false;
        isAuthenticated
            = (isAuthenticatedTask.isSuccessful()
            && isAuthenticatedTask.getResult().isAuthenticated());
        if (isAuthenticated) {
          Log.i(TAG, "Authenticated");
        } else {
          Log.i(TAG, "Not authenticated");
        }
      });
    }
  }

  /**
   * Called after {@link #onRestart}, or {@link #onPause}, for your activity
   * to start interacting with the user.
   */
  public void onResume() {
    signInSilently();
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////


	/*
	public static void login() {
		Log.i(TAG, "PlayGames: Forcing Login");
		userRequiresLogin = true;
		// mHelper=null;
		// init(enableCloudStorage,null);
	}*/

	////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public static boolean setScore(String id, int score) {
    Log.i(TAG, "setScore " + id + " -> " + score + " (authenticated: " + isAuthenticated);
    try {
      PlayGames.getLeaderboardsClient(mainActivity)
          .submitScore(id, score);
    } catch (Exception e) {
      Log.i(TAG, "setScore Exception");
      Log.i(TAG, e.toString());
      signInSilently();
      return false;
    }
    /*
		try {
			long score = (((long)high_score << 32) | ((long)low_score & 0xFFFFFFFF));
			Games.Leaderboards.submitScore(mHelper.mGoogleApiClient, id, score);
    } catch (Exception e) {
			Log.i(TAG, "PlayGames: setScore Exception");
			Log.i(TAG, e.toString());
			return false;
		}
		Log.i(TAG, "PlayGames: setScore complete");
     */
    return true;

  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////
	/*
	public static boolean displayScoreboard(String id){
		try {
			mainActivity.startActivityForResult(Games.Leaderboards.getLeaderboardIntent(mHelper.mGoogleApiClient, id), 0);
		} catch (Exception e) {
			// Try connecting again
			Log.i(TAG, "PlayGames: displayScoreboard Exception");
			Log.i(TAG, e.toString());
			login();
			return false;
		}
		return true;
	}*/

	////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final int RC_LEADERBOARD_UI = 9004;

  public static boolean displayScoreboard(String leaderboardId) {
    Log.i(TAG, "displayScoreboard " + leaderboardId + " (authenticated: " + isAuthenticated);
    if (!isAuthenticated) {
      login();
      return false;
    } else {
      try {
        PlayGames.getLeaderboardsClient(mainActivity)
            .getLeaderboardIntent(leaderboardId)
            .addOnSuccessListener(new OnSuccessListener<Intent>() {
              @Override
              public void onSuccess(Intent intent) {
                mainActivity.startActivityForResult(intent, RC_LEADERBOARD_UI);
              }
            })
            .addOnFailureListener(new OnFailureListener() {
              @Override
              public void onFailure(Exception e) {
                Log.i(TAG, "displayScoreboard failed:");
                Log.i(TAG, e.toString());
                signInSilently();
                //mainActivity.startActivityForResult(intent, RC_SAVED_GAMES);
              }
            });
        // mainActivity.startActivityForResult(Games.Leaderboards.getAllLeaderboardsIntent(mHelper.mGoogleApiClient), 0);
      } catch (Exception e) {
        // Try connecting again
        Log.i(TAG, "displayScoreboard Exception");
        Log.i(TAG, e.toString());
        signInSilently();
        //login();
        return false;
      }
    }
    return true;
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////

	public static GooglePlayGames getInstance() {
    if (instance == null) {
      instance = new GooglePlayGames();
    }
    return instance;
  }

  /*

	@Override
  public void onSignInFailed() {
    if (callbackObject != null)
	 callbackObject.call1("loginResultCallback",-1);
    Log.i(TAG, "PlayGames: onSignInFailed");
  }

  @Override
  public void onSignInSucceeded() {
		if (callbackObject != null) callbackObject.call1("loginResultCallback",1);
    Log.i(TAG, "PlayGames: onSignInSucceeded");
  }
	
	@Override
  public void onSignInStart() {
		if (callbackObject != null) callbackObject.call1("loginResultCallback",0);
    Log.i(TAG, "PlayGames: onSignInStart");
  }

   */
  ////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public static boolean unlock(String id) {
    Log.i(TAG, "Unlock achievement " + id);
    try {
      PlayGames.getAchievementsClient(mainActivity).unlock(id);
    } catch (Exception e) {
      Log.i(TAG, "unlock Exception");
      Log.i(TAG, e.toString());
      signInSilently();
      return false;
    }
    return true;
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////
	/*
	public static boolean reveal(String id){
		try{
			Games.Achievements.reveal(mHelper.mGoogleApiClient, id);
		}catch (Exception e) {
			Log.i(TAG, "PlayGames: reveal Exception");
			Log.i(TAG, e.toString());
			return false;
		}
		return true;
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////

	public static boolean increment(String id, int step){
		try{
			Games.Achievements.increment(mHelper.mGoogleApiClient, id, step);
		}catch (Exception e) {
			Log.i(TAG, "PlayGames: increment Exception");
			Log.i(TAG, e.toString());
			return false;
		}
		return true;
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////

	public static boolean setSteps(String id, int steps){
		try{
			Games.Achievements.setSteps(mHelper.mGoogleApiClient, id, steps);
		}catch (Exception e) {
			Log.i(TAG, "PlayGames: setSteps Exception");
			Log.i(TAG, e.toString());
			return false;
		}
		return true;
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////

	*/
	////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////
	private static final int RC_ACHIEVEMENT_UI = 9003;

  public static boolean displayAchievements() {
    Log.i(TAG, "displayAchievements (authenticated: " + isAuthenticated);
    if (!isAuthenticated) {
      login();
      return false;
    } else {
      try {
        PlayGames.getAchievementsClient(mainActivity)
            .getAchievementsIntent()
            .addOnSuccessListener(new OnSuccessListener<Intent>() {
              @Override
              public void onSuccess(Intent intent) {
                mainActivity.startActivityForResult(intent, RC_ACHIEVEMENT_UI);
              }
            })
            .addOnFailureListener(new OnFailureListener() {
              @Override
              public void onFailure(Exception e) {
                Log.i(TAG, "displayScoreboard failed:");
                Log.i(TAG, e.toString());
                signInSilently();
                //mainActivity.startActivityForResult(intent, RC_SAVED_GAMES);
              }
            });				//mainActivity.startActivityForResult(Games.Achievements.getAchievementsIntent(mHelper.mGoogleApiClient), 0);
      } catch (Exception e) {
        // Try connecting again
        signInSilently();
        Log.i(TAG, "displayAchievements Exception");
        Log.i(TAG, e.toString());
        //login();
        return false;
      }
    }
    return true;
  }

  /*
	public static String getPlayerId() {
		try {
			Log.i(TAG, "PlayGames: getPlayerId BEGIN");
			return Games.Players.getCurrentPlayerId(mHelper.mGoogleApiClient);
		} catch (Exception e) {
			// Try connecting again
			Log.i(TAG, "PlayGames: getPlayerId Exception");
			Log.i(TAG, e.toString());
		}
		return null;
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////

	public static String getPlayerDisplayName() {
		try {
			Log.i(TAG, "PlayGames: getPlayerDisplayName BEGIN");
			Player p = Games.Players.getCurrentPlayer(mHelper.mGoogleApiClient);
			return p.getDisplayName();
		} catch (Exception e) {
			// Try connecting again
			Log.i(TAG, "PlayGames: getPlayerDisplayName Exception");
			Log.i(TAG, e.toString());
		}
		return null;
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////

	public static void getPlayerImage(final String playerID) {
		mainActivity.runOnUiThread(new Runnable() {
			public void run() {
				try{
					Log.i(TAG, "PlayGames: getPlayerImage BEGIN ");
					if(playerID==null || playerID=="null" || playerID.isEmpty()){
						Player p = Games.Players.getCurrentPlayer(mHelper.mGoogleApiClient);
						final String url = p.getIconImageUri().toString();
						loadImage(getPlayerId(), p.getIconImageUri(), url);	
					} else {
						Games.Players.loadPlayer(mHelper.mGoogleApiClient, playerID).setResultCallback(new ResultCallback<Players.LoadPlayersResult>() {
							@Override
							public void onResult(Players.LoadPlayersResult loadPlayersResult) {
								Log.i(TAG, "PlayGames: getPlayerImage load players on result ");
								if(loadPlayersResult.getStatus().getStatusCode() == GamesStatusCodes.STATUS_OK){
									PlayerBuffer buffer = loadPlayersResult.getPlayers();
									if(buffer.getCount()>0){
										Player p = buffer.get(0);
										if(p.getIconImageUri()!=null){
											final String url = p.getIconImageUri().toString();
											loadImage(playerID, p.getIconImageUri(), url);
										}
									}
									buffer.release();
								}
							}
						});
					}
					
					   		
				}catch(Exception e) {
					Log.i(TAG, "PlayGames: getPlayerImage Exception");
					Log.i(TAG, e.toString());
				}
			}
		});
	}

	private static void loadImage(final String playerID, Uri uri, final String url){
		ImageManager im = ImageManager.create(mHelper.mAppContext);
		im.loadImage(new ImageManager.OnImageLoadedListener() {
			@Override
			public void onImageLoaded(Uri uri, Drawable drawable, boolean isRequestedDrawable) {
				
				Bitmap bitmap = ((BitmapDrawable)drawable).getBitmap();
				try {
					String cache = Extension.mainContext.getCacheDir().getAbsolutePath();
					MessageDigest md = MessageDigest.getInstance("MD5");
					byte[] array = md.digest(url.getBytes());
					StringBuffer sb = new StringBuffer();
    			for (int i = 0; i < array.length; ++i) {
      			sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).substring(1,3));
   				}
					String md5 = sb.toString();
					String path = cache+"/cache/"+md5+".png";
					File file = new File(path);
					file.getParentFile().mkdirs(); // Will create parent directories if not exists
					file.createNewFile();
					FileOutputStream fos = new FileOutputStream(file,false);
						bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
						Log.i(TAG, "PlayGames: "+playerID+"'s saved in "+path);
						if (callbackObject != null) callbackObject.call2("onGetPlayerImage", playerID, path);
				} catch (Exception e) {
					Log.i(TAG, "PlayGames: getPlayerImage exception trying to save: ");
					Log.i(TAG, e.toString());
						//TODO: Handle exception
				}
			}
		}, uri);
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////

	public static boolean getPlayerScore(final String idScoreboard) {
		try {
			Games.Leaderboards.loadCurrentPlayerLeaderboardScore(mHelper.mGoogleApiClient, idScoreboard, LeaderboardVariant.TIME_SPAN_ALL_TIME, LeaderboardVariant.COLLECTION_PUBLIC).setResultCallback(new ResultCallback<Leaderboards.LoadPlayerScoreResult>() {
				@Override
				public void onResult(final Leaderboards.LoadPlayerScoreResult playerScore) {
					if ((playerScore != null) && (playerScore.getStatus().getStatusCode() == GamesStatusCodes.STATUS_OK) && (playerScore.getScore() != null)) {
						long score = playerScore.getScore().getRawScore();
						int high_score = (int) (score >>> 32);
						int low_score = (int) (score & 0xFFFFFFFF);
						if (callbackObject != null) callbackObject.call3("onGetScoreboard", idScoreboard, high_score, low_score);
					}
				}
			});
		} catch (Exception e) {
			// Try connecting again
			Log.i(TAG, "PlayGames: getPlayerScore Exception");
			Log.i(TAG, e.toString());
			return false;
		}
		return true;
	}
   */
  ////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////

 /*
	public static boolean loadInvitablePlayers(boolean clearCache) {
		return loadAllPlayers(false, clearCache, 0);
	}

	public static boolean loadConnectedPlayers(boolean clearCache) {
		return loadAllPlayers(true, clearCache, 0);
	}

	private static boolean loadAllPlayers(final boolean getConnectedPlayers, boolean clearCache, final int resultsCount) {
		try{
			final int FRIENDS_PER_PAGE = 25;
			final ResultCallback<Players.LoadPlayersResult> resultCallback = new ResultCallback<Players.LoadPlayersResult>(){
				@Override
				public void onResult(Players.LoadPlayersResult result) {
					
					PlayerBuffer playerBuffer = result.getPlayers();
					Log.w(TAG, "loadAllFriends: onResult... got " + playerBuffer.getCount());

					if (!getConnectedPlayers && playerBuffer.getCount() >= resultsCount+FRIENDS_PER_PAGE) {
						Log.w(TAG, "loadAllFriends: Maybe there're more players... calling loadMoreInvitablePlayers.");
						loadAllPlayers(false, false, playerBuffer.getCount());
						return;
					}

					String friends = "";
					for (Player player : playerBuffer) {
						friends+=player.getPlayerId()+"\1"+player.getDisplayName()+"\2";
						//Log.i(TAG, String.format("Found player with id [%s] and display name [%s]", player.getPlayerId(), player.getDisplayName()));
					}
					Log.w(TAG, "loadAllFriends: Done! Now sending serialized friends to HAXE");
					if (callbackObject != null) callbackObject.call2("onLoadPlayers",friends,getConnectedPlayers);					
				}
			};

			if(getConnectedPlayers) {
				Games.Players.loadConnectedPlayers(mHelper.mGoogleApiClient, clearCache).setResultCallback(resultCallback);
			}else{
				if (resultsCount == 0){
					Games.Players.loadInvitablePlayers(mHelper.mGoogleApiClient, FRIENDS_PER_PAGE, clearCache).setResultCallback(resultCallback);
				}else{
					Games.Players.loadMoreInvitablePlayers(mHelper.mGoogleApiClient, FRIENDS_PER_PAGE).setResultCallback(resultCallback);					
				}
			}
  	} catch (Exception e) {
			// Try connecting again
			Log.i(TAG, "PlayGames: loadAllFriends Exception");
			Log.i(TAG, e.toString());
			return false;
		}
		return true;
  }

	////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////

	public static boolean getAchievementStatus(final String idAchievement) {
		try {
			Games.Achievements.load(mHelper.mGoogleApiClient, false).setResultCallback(new ResultCallback<Achievements.LoadAchievementsResult>() {
				@Override
				public void onResult(Achievements.LoadAchievementsResult loadAchievementsResult) {
					for (Achievement ach: loadAchievementsResult.getAchievements()) {
						if (ach.getAchievementId().equals(idAchievement)) {
							if (ach.getState() == Achievement.STATE_UNLOCKED) if (callbackObject != null) callbackObject.call2("onGetAchievementStatus", idAchievement, 1);
							else if (callbackObject != null) callbackObject.call2("onGetAchievementStatus", idAchievement, 0);
						}
					}
				}
			});
		} catch (Exception e) {
			// Try connecting again
			Log.i(TAG, "PlayGames: getAchievementStatus Exception");
			Log.i(TAG, e.toString());
			return false;
		}
		return true;
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////

	public static boolean getCurrentAchievementSteps(final String idAchievement) {
		try {
			Games.Achievements.load(mHelper.mGoogleApiClient, false).setResultCallback(new ResultCallback<Achievements.LoadAchievementsResult>() {
				@Override
				public void onResult(Achievements.LoadAchievementsResult loadAchievementsResult) {
					for (Achievement ach: loadAchievementsResult.getAchievements()) {
						if (ach.getAchievementId().equals(idAchievement)) {
							if (ach.getType() == Achievement.TYPE_INCREMENTAL) if (callbackObject != null) callbackObject.call2("onGetAchievementSteps", idAchievement, ach.getCurrentSteps());
						}
					}
				}
			});
		} catch (Exception e) {
			// Try connecting again
			Log.i(TAG, "PlayGames: getCurrentAchievementSteps Exception");
			Log.i(TAG, e.toString());
			return false;
		}
		return true;
	}
	*/

	/*
	public static void openGame(String name){

	}*/

	private static final int RC_SAVED_GAMES = 9009;

  public static void displaySavedGames(String title, boolean allowAddButton, boolean allowDelete, int maxNumberOfSavedGamesToShow) {
    Log.i(TAG, "displaySavedGames " + title + " (authenticated): " + isAuthenticated);
    if (!isAuthenticated) {
      login();
      // return false;
    } else {
      try {
        SnapshotsClient snapshotsClient = PlayGames.getSnapshotsClient(Extension.mainActivity);
        if (title == null || title.compareTo("") == 0) {
          title = " ";
        }
        snapshotsClient.getSelectSnapshotIntent(
            title,
            allowAddButton,
            allowDelete,
            maxNumberOfSavedGamesToShow
        ).addOnSuccessListener(new OnSuccessListener<Intent>() {
          @Override
          public void onSuccess(Intent intent) {
            Log.i(TAG, "displaySavedGames Sucess / snaphot intent");
            mainActivity.startActivityForResult(intent, RC_SAVED_GAMES);
          }
        }).addOnFailureListener(new OnFailureListener() {
          @Override
          public void onFailure(Exception e) {
            Log.i(TAG, "displaySavedGames failed:");
            Log.i(TAG, e.toString());
            signInSilently();
            //mainActivity.startActivityForResult(intent, RC_SAVED_GAMES);
          }
        });

      } catch (Exception e) {
        // Try connecting again
        Log.i(TAG, "displaySavedGames Exception");
        Log.i(TAG, e.toString());
        signInSilently();
        //login();
      }
    }
  }

  private static Snapshot snapshot = null;
  private static boolean savedGamesWorking = false;

  /*
  public static void loadSavedGame(final String savedName) {
    if (!isAuthenticated) {
      login();
      Log.i(TAG, "loadSavedGame - nog logged in to Google Play Services.");
      return;
    }

    if (savedGamesWorking) {
      Log.i(TAG, "PlayGames: loadSavedGame (still opening game... won't do anything).");
      return;
    }

    if (snapshot != null) {
      discardAndCloseGame();
    }

    AsyncTask<Void, Void, Integer> task = new AsyncTask<Void, Void, Integer>() {
      @Override
      protected Integer doInBackground(Void... params) {
        int statusCode = 0;
        try {
          String name = new String(savedName);
          byte[] mSaveGameData = null;
          byte[] mConfictSaveGameData = null;
          Snapshot conflictSnapshot = null;
          // Open the saved game using its name.
          Snapshots.OpenSnapshotResult result = PlayGames.Snapshots.open(mHelper.mGoogleApiClient, name, true).await();

          statusCode = result.getStatus().getStatusCode();
          boolean hadConflict = false;

          if (statusCode == GamesStatusCodes.STATUS_SNAPSHOT_CONFLICT) {
            hadConflict = true;
            while (statusCode == GamesStatusCodes.STATUS_SNAPSHOT_CONFLICT) {
              conflictSnapshot = result.getConflictingSnapshot();
              PlayGames.Snapshots.resolveConflict(mHelper.mGoogleApiClient, result.getConflictId(), result.getSnapshot()).await();
              result = PlayGames.Snapshots.open(mHelper.mGoogleApiClient, name, true).await();
              statusCode = result.getStatus().getStatusCode();
            }
          }

          // Check the result of the open operation
          if (result.getStatus().isSuccess()) {
            snapshot = result.getSnapshot();
            // Read the byte content of the saved game.
            try {
              mSaveGameData = snapshot.getSnapshotContents().readFully();
            } catch (IOException e) {
              Log.e(TAG, "Error while reading Snapshot.", e);
            }
          } else {
            Log.e(TAG, "Error while loading: " + result.getStatus().getStatusCode());
          }

          savedGamesWorking = false;
          if (hadConflict) {
            try {
              mConfictSaveGameData = conflictSnapshot.getSnapshotContents().readFully();
            } catch (IOException e) {
              Log.e(TAG, "Error while reading Snapshot.", e);
            }
            if (callbackObject != null) {
              callbackObject.call3("onLoadSavedGameConflict", name, mSaveGameData == null ? null : new String(mSaveGameData), mConfictSaveGameData == null ? null : new String(mConfictSaveGameData));
            }
          } else {
            if (callbackObject != null) {
              callbackObject.call3("onLoadSavedGameComplete", name, result.getStatus().getStatusCode(), mSaveGameData == null ? null : new String(mSaveGameData));
            }
          }

        } catch (Exception e) {
          // Try connecting again
          Log.i(TAG, "PlayGames: loadSavedGame / doInBackground Exception");
          Log.i(TAG, e.toString());
          savedGamesWorking = false;
        }
        return statusCode;
      }
    };
    savedGamesWorking = true;
    task.execute();
  }

  public static boolean discardAndCloseGame() {
    if (savedGamesWorking) {
      Log.i(TAG, "PlayGames: discardAndCloseGame (still opening game... won't do anything).");
      return false;
    }
    try {
      if (snapshot == null) {
        return true;
      }
      PlayGames.Snapshots.discardAndClose(mHelper.mGoogleApiClient, snapshot);
      snapshot = null;
    } catch (Exception e) {
      // Try connecting again
      Log.i(TAG, "PlayGames: discardAndCloseGame Exception");
      Log.i(TAG, e.toString());
      return false;
    }
    return true;
  }

  public static boolean commitAndCloseGame(String data, String description) {
    if (savedGamesWorking) {
      Log.i(TAG, "PlayGames: commitAndCloseGame (still opening game... won't do anything).");
      return false;
    }
    try {
      if (snapshot == null) {
        Log.i(TAG, "PlayGames: commitAndCloseGame (trying to save unopened game!)");
        return true;
      }
      snapshot.getSnapshotContents().writeBytes(data.getBytes());
      // Create the change operation
      SnapshotMetadataChange metadataChange = new SnapshotMetadataChange.Builder()
          //.setCoverImage(coverImage)
          .setDescription(description)
          .build();

      PlayGames.Snapshots.commitAndClose(mHelper.mGoogleApiClient, snapshot, metadataChange);
      snapshot = null;
    } catch (Exception e) {
      // Try connecting again
      Log.i(TAG, "PlayGames: commitAndCloseGame Exception");
      Log.i(TAG, e.toString());
      return false;
    }
    return true;
  }


*/

}
