package uk.co.jaml.gameservices;

import org.godotengine.godot.Godot;
import org.godotengine.godot.Dictionary;
import org.godotengine.godot.plugin.SignalInfo;

import android.app.Activity;
import android.app.Application.ActivityLifecycleCallbacks;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.DisplayCutout;
import android.view.Gravity;
import android.view.WindowInsets;
import android.widget.FrameLayout; //get Godot Layout
import android.view.View;
import android.provider.Settings;
import androidx.annotation.NonNull;
import androidx.collection.ArraySet;

import java.lang.Integer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import com.google.android.gms.games.AnnotatedData;
import com.google.android.gms.games.AuthenticationResult;
import com.google.android.gms.games.GamesSignInClient;
import com.google.android.gms.games.LeaderboardsClient;
import com.google.android.gms.games.leaderboard.Leaderboard;
import com.google.android.gms.games.leaderboard.LeaderboardScore;
import com.google.android.gms.games.leaderboard.LeaderboardScoreBuffer;
import com.google.android.gms.games.leaderboard.ScoreSubmissionData;
import com.google.android.gms.games.PageDirection;
import com.google.android.gms.games.Player;
import com.google.android.gms.games.PlayGames;
import com.google.android.gms.games.PlayGamesSdk;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

public class GameServices extends org.godotengine.godot.plugin.GodotPlugin implements ActivityLifecycleCallbacks {

    private boolean aIsInitialized = false;
    private Activity aActivity;

    private Player aLocalPlayer;
    // keep player scores in a leaderboard:score map, to deal with parallel calls to fetch multiple leaderboards
    private HashMap<String,LeaderboardScore> aLocalPlayerLeaderboardScores = new HashMap<String,LeaderboardScore>();
    // TODO: should we do the same with buffers and page sizes, making fetch_next(leaderboardId) ???
    private LeaderboardScoreBuffer aLeaderboardScoreBuffer;

    private int aScoresPageSize = 10;

    // Other screens this class is responsible for showing

    private static final int GAME_SERVICE_SCREEN_NONE = -1;
    private static final int GAME_SERVICE_SCREEN_LEADERBOARD = 21000;

    private int aGameServiceScreen = GAME_SERVICE_SCREEN_NONE;
    private String aLeaderboardId;

    // TODO do we actually use these?
    private FrameLayout aGodotLayout; // store the godot layout
    private FrameLayout.LayoutParams aGodotLayoutParams; // Store the godot layout params

    public GameServices(Godot godot) {
        super(godot);
    }

    @Override
    public View onMainCreate(Activity pActivity) {
        // passed an Activity, which is usefull
        aActivity = pActivity;
        // register for that activity's lifecycle notifications, so we can signal when it becomes
        //  active again after showing other activities
        aActivity.getApplication().registerActivityLifecycleCallbacks(this);
        // do Android setup stuff
        aGodotLayout = new FrameLayout(pActivity);
        return aGodotLayout;
    }

    @NonNull
    @Override
    public String getPluginName() {
        return getClass().getSimpleName();
    }

    @NonNull
    @Override
    public List<String> getPluginMethods() {
        return Arrays.asList(
                
                "get_service_name",

                "initialize",

                "can_sign_in",
                "sign_in",

                "show_leaderboard",
                "show_all_leaderboards",

                "fetch_top_scores",
                "fetch_next_scores",

                "submit_score"

        );
    }

    @NonNull
    @Override
    public Set<SignalInfo> getPluginSignals() {
        Set<SignalInfo> signals = new ArraySet<>();

        signals.add(new SignalInfo("debug_message", String.class));

        signals.add(new SignalInfo("authorization_complete", Boolean.class, Dictionary.class));
        signals.add(new SignalInfo("authorization_failed", String.class));

        signals.add(new SignalInfo("show_leaderboard_complete", String.class));
        signals.add(new SignalInfo("show_leaderboard_failed", String.class, String.class));
        signals.add(new SignalInfo("show_leaderboard_dismissed", String.class));

        signals.add(new SignalInfo("fetch_scores_complete", Dictionary.class, Dictionary.class, Dictionary.class, Boolean.class));
        signals.add(new SignalInfo("fetch_scores_failed", String.class, String.class));

        signals.add(new SignalInfo("submit_score_complete", String.class));
        signals.add(new SignalInfo("submit_score_failed", String.class, String.class));

        return signals;
    }

    @NonNull
    public String get_service_name() {
        return "Google Play Services";
    }

    public void initialize() {
        PlayGamesSdk.initialize(aActivity.getApplicationContext());
        aIsInitialized = true;
        checkAuthenticated(PlayGames.getGamesSignInClient(aActivity).isAuthenticated());
    }

    @NonNull
    public boolean can_sign_in() { return true; }

    public void sign_in() {
        checkAuthenticated(PlayGames.getGamesSignInClient(aActivity).signIn());
    }

    private void checkAuthenticated(Task<AuthenticationResult> pTask) {
        pTask
            .addOnCompleteListener(isAuthenticatedTask -> {

                boolean isAuthenticated = (isAuthenticatedTask.isSuccessful() && isAuthenticatedTask.getResult().isAuthenticated());

                if (!isAuthenticated) {
                    // Disable your integration with Play Games Services or show a
                    // login button to ask  players to sign-in. Clicking it should
                    // call GamesSignInClient.signIn().
                    emitSignal("authorization_complete", isAuthenticated, new Dictionary());
                    return;
                }

                // Continue with Play Games Services
                PlayGames.getPlayersClient(aActivity).getCurrentPlayer().addOnCompleteListener(mTask -> {
                    if (!mTask.isSuccessful()) {
                        emitSignal("authorization_complete", isAuthenticated, new Dictionary());
                        return;
                    }
                    aLocalPlayer = mTask.getResult();
                    emitSignal("authorization_complete", isAuthenticated, dictionaryFromPlayer(aLocalPlayer));
                });
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(Exception e) {
                    emitSignal("authorization_failed", e.getLocalizedMessage());
                }
            });
    }

    //
    // Leaderboards
    //

    public void show_leaderboard(String pLeaderboardId, int pPlayers, int pTime) {
        startLeaderboardIntent(
            PlayGames
                .getLeaderboardsClient(aActivity)
                .getLeaderboardIntent(pLeaderboardId, pTime, pPlayers),
            pLeaderboardId
        );
    }
    
    public void show_all_leaderboards() {
        startLeaderboardIntent(
            PlayGames
                .getLeaderboardsClient(aActivity)
                .getAllLeaderboardsIntent(),
            ""
        );
    }

    private void startLeaderboardIntent(Task<Intent> pLeaderboardIntent, String pLeaderboardId) {

        // Store leaderboard ID for future signals
        aLeaderboardId = pLeaderboardId;

        // Add listeners to the pass-in Task
        pLeaderboardIntent
            .addOnSuccessListener(new OnSuccessListener<Intent>() {
                @Override
                public void onSuccess(Intent intent) {
                    aGameServiceScreen = GAME_SERVICE_SCREEN_LEADERBOARD;
                    aActivity.startActivityForResult(intent, GAME_SERVICE_SCREEN_LEADERBOARD);
                    emitSignal("show_leaderboard_complete", pLeaderboardId);
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(Exception e) {
                    emitSignal("show_leaderboard_failed", pLeaderboardId, e.getLocalizedMessage());
                }
            });
    }

    //
    // Leaderboard data
    //

    public void fetch_top_scores(String pLeaderboardId, int pPageSize, int pPlayers, int pTime) {

        // Clear out state from previous calls
        aLeaderboardScoreBuffer = null;

        // Record the page size for use later
        aScoresPageSize = pPageSize;

        // Record the leaderboard ID for future signals
        aLeaderboardId = pLeaderboardId;

        // start by fetching the local player's score for this leaderboard, since unlike iOS
        //  Google Play Services doesn't return this with every call
        PlayGames
            .getLeaderboardsClient(aActivity)
                .loadCurrentPlayerLeaderboardScore(pLeaderboardId, pTime, pPlayers)
                .addOnSuccessListener(new OnSuccessListener<AnnotatedData<LeaderboardScore>>() {
                    @Override
                    public void onSuccess(AnnotatedData<LeaderboardScore> data) {

                        // Store the value we got back, which may be null if the player hasn't
                        //  logged a score for this leaderboard yet
                        LeaderboardScore score = data.get();
                        if (score != null) {
                            aLocalPlayerLeaderboardScores.put(pLeaderboardId, score);
                        }

                        // Continue to make the call to fetch the first page of leaderboard data
                        fetchScores(
                            PlayGames
                                .getLeaderboardsClient(aActivity)
                                .loadTopScores(pLeaderboardId, pTime, pPlayers, pPageSize)
                        );
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        emitSignal("fetch_scores_failed", pLeaderboardId, e.getLocalizedMessage());
                    }
                });
    }

    public void fetch_next_scores() {

        if (aLeaderboardScoreBuffer == null) {
            emitSignal("fetch_scores_failed", "", "fetch_next before fetch_top");
            return;
        }

        fetchScores(
            PlayGames
                .getLeaderboardsClient(aActivity)
                .loadMoreScores(aLeaderboardScoreBuffer, aScoresPageSize, PageDirection.NEXT)
        );
    }

    private void fetchScores(Task<AnnotatedData<LeaderboardsClient.LeaderboardScores>> pScoresTask) {
        pScoresTask
            .addOnSuccessListener(new OnSuccessListener<AnnotatedData<LeaderboardsClient.LeaderboardScores>>() {
                @Override
                public void onSuccess(AnnotatedData<LeaderboardsClient.LeaderboardScores> data) {
                    //emitSignal("debug_message", "got something back from fetch_top_scores");
                    parseAndReportScores(data.get());
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(Exception e) {
                    emitSignal("fetch_scores_failed", aLeaderboardId, e.getLocalizedMessage());
                }
            });
    }

    private void parseAndReportScores(LeaderboardsClient.LeaderboardScores data) {

        Leaderboard leaderboard = data.getLeaderboard();
        Dictionary leaderboard_info = dictionaryFromLeaderboard(leaderboard);
        Dictionary player_score = dictionaryFromScore(aLocalPlayerLeaderboardScores.get(leaderboard.getLeaderboardId()));

        LeaderboardScoreBuffer buffer = data.getScores();
        Dictionary scores = new Dictionary();
        int count = buffer.getCount();
        int i = 0;

        while (i < count) {
            LeaderboardScore score = buffer.get(i);
            scores.put(String.valueOf(i++), dictionaryFromScore(score));
        }

        // There doesn't seem to be a limit to the range of scores which can be returned, and there
        //  also doesn't seem to be a way that Play Services flags the final batch of scores has
        //  been returned, so we'll use this janky check for whether we got a full page of results
        //  to inform the caller whether we think there are more to come.
        boolean more_available = count == aScoresPageSize;

        emitSignal("fetch_scores_complete", leaderboard_info, player_score, scores, more_available);

        if (more_available) {
            aLeaderboardScoreBuffer = buffer;
        } else {
            data.release();
            aLeaderboardScoreBuffer = null;
            aLeaderboardId = null;
        }
    }

    private Dictionary dictionaryFromPlayer(Player player) {
        Dictionary dict = new Dictionary();
        if (player != null) {
            dict.put("id", player.getPlayerId());
            dict.put("display_name", player.getDisplayName());
            dict.put("is_local_player", player.getPlayerId().equals(aLocalPlayer.getPlayerId()));
        }
        return dict;
    }

    private Dictionary dictionaryFromLeaderboard(Leaderboard leaderboard) {
        Dictionary dict = new Dictionary();
        dict.put("id", leaderboard.getLeaderboardId());
        dict.put("display_name", leaderboard.getDisplayName());
        return dict;
    }

    private Dictionary dictionaryFromScore(LeaderboardScore score) {
        Dictionary dict = new Dictionary();
        if (score != null) {
            dict.put("rank", score.getRank());
            dict.put("score", score.getRawScore());
            dict.put("formatted_score", score.getDisplayScore());
            dict.put("player", dictionaryFromPlayer(score.getScoreHolder()));
        }
        return dict;
    }

    //
    // Scores
    //

    public void submit_score(String pLeaderboardId, int pScore) {
        PlayGames
            .getLeaderboardsClient(aActivity)
            .submitScoreImmediate(pLeaderboardId, pScore)
                .addOnSuccessListener(new OnSuccessListener<ScoreSubmissionData>() {
                    @Override
                    public void onSuccess(ScoreSubmissionData data) {
                        emitSignal("submit_score_complete", pLeaderboardId);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        emitSignal("submit_score_failed", pLeaderboardId, e.getLocalizedMessage());
                    }
                });
    }

    //
    // Helpers
    //





//    private void loadConsentForm() {
//        UserMessagingPlatform.loadConsentForm(
//                aActivity,
//                consentForm -> {
//                    String consentStatusMsg = "";
//                    if (aConsentInformation.getConsentStatus() == ConsentInformation.ConsentStatus.REQUIRED) {
//                        consentForm.show(
//                                aActivity,
//                                formError -> {
//                                    loadConsentForm();
//                                    emitSignal("consent_form_dismissed");
//                                }
//                        );
//                        consentStatusMsg = "User consent required but not yet obtained.";
//                    }
//                    switch (aConsentInformation.getConsentStatus()) {
//                        case ConsentInformation.ConsentStatus.UNKNOWN:
//                            consentStatusMsg = "Unknown consent status.";
//                            break;
//                        case ConsentInformation.ConsentStatus.NOT_REQUIRED:
//                            consentStatusMsg = "User consent not required. For example, the user is not in the EEA or the UK.";
//                            break;
//                        case ConsentInformation.ConsentStatus.OBTAINED:
//                            consentStatusMsg = "User consent obtained. Personalization not defined.";
//                            break;
//                    }
//                    emitSignal("consent_status_changed", consentStatusMsg);
//                },
//                formError -> emitSignal("consent_form_load_failure", formError.getErrorCode(), formError.getMessage())
//        );
//    }

//    public void request_user_consent() {
//        aConsentInformation = UserMessagingPlatform.getConsentInformation(aActivity);
//
//        ConsentRequestParameters.Builder paramsBuilder = new ConsentRequestParameters.Builder().setTagForUnderAgeOfConsent(aIsForChildDirectedTreatment);
//
//        ConsentRequestParameters params;
//        if (aIsTestEuropeUserConsent) //https://developers.google.com/admob/ump/android/quick-start#testing
//        {
//            ConsentDebugSettings debugSettings = new ConsentDebugSettings.Builder(aActivity)
//                    .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
//                    .addTestDeviceHashedId(getDeviceId())
//                    .build();
//            params = paramsBuilder.setConsentDebugSettings(debugSettings).build();
//        } else {
//            params = paramsBuilder.build();
//        }
//
//        aConsentInformation.requestConsentInfoUpdate(aActivity, params,
//                () -> {
//                    if (aConsentInformation.isConsentFormAvailable()) {
//                        emitSignal("consent_info_update_success", "Consent Form Available");
//                        loadConsentForm();
//                    } else {
//                        emitSignal("consent_info_update_success", "Consent Form not Available");
//                    }
//                },
//                formError -> emitSignal("consent_info_update_failure", formError.getErrorCode(), formError.getMessage())
//        );
//    }

//    public void reset_consent_state() {
//        aConsentInformation.reset(); //https://developers.google.com/admob/ump/android/quick-start#reset_consent_state
//    }

//    private void setMobileAdsRequestConfiguration(boolean pIsForChildDirectedTreatment, String pMaxAdContentRating, boolean pIsReal) {
//        RequestConfiguration requestConfiguration;
//        RequestConfiguration.Builder requestConfigurationBuilder = new RequestConfiguration.Builder();
//
//        if (!pIsReal) {
//            requestConfigurationBuilder.setTestDeviceIds(Collections.singletonList(getDeviceId()));
//        }
//
//        requestConfigurationBuilder.setTagForChildDirectedTreatment(pIsForChildDirectedTreatment ? 1 : 0);
//
//        if (pIsForChildDirectedTreatment) {
//            requestConfigurationBuilder.setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_G);
//        } else {
//            switch (pMaxAdContentRating) {
//                case RequestConfiguration.MAX_AD_CONTENT_RATING_G:
//                case RequestConfiguration.MAX_AD_CONTENT_RATING_MA:
//                case RequestConfiguration.MAX_AD_CONTENT_RATING_PG:
//                case RequestConfiguration.MAX_AD_CONTENT_RATING_T:
//                case RequestConfiguration.MAX_AD_CONTENT_RATING_UNSPECIFIED:
//                    requestConfigurationBuilder.setMaxAdContentRating(pMaxAdContentRating);
//                    break;
//            }
//        }
//
//        requestConfiguration = requestConfigurationBuilder.build();
//
//        MobileAds.setRequestConfiguration(requestConfiguration);
//    }

//    private AdRequest getAdRequest() {
//        AdRequest.Builder adRequestBuilder = new AdRequest.Builder();
//
//        return adRequestBuilder.build();
//    }

//    private Rect getSafeArea() {
//        final Rect safeInsetRect = new Rect();
//        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
//            return safeInsetRect;
//        }
//
//        final WindowInsets windowInsets = aActivity.getWindow().getDecorView().getRootWindowInsets();
//        if (windowInsets == null) {
//            return safeInsetRect;
//        }
//
//        final DisplayCutout displayCutout = windowInsets.getDisplayCutout();
//        if (displayCutout != null) {
//            safeInsetRect.set(displayCutout.getSafeInsetLeft(), displayCutout.getSafeInsetTop(), displayCutout.getSafeInsetRight(), displayCutout.getSafeInsetBottom());
//        }
//
//        return safeInsetRect;
//    }

    //BANNER only one is allowed, please do not try to place more than one, as your ads on the app may have the chance to be banned!
//    public void load_banner(final String pAdUnitId, final int pPosition, final String pSize, final boolean pShowInstantly, final boolean pRespectSafeArea) {
//        aActivity.runOnUiThread(() -> {
//            if (aIsInitialized) {
//                if (aAdView != null) destroy_banner();
//                aAdView = new AdView(aActivity);
//
//                aAdView.setAdUnitId(pAdUnitId);
//                switch (pSize) {
//                    case "BANNER":
//                        aAdView.setAdSize(AdSize.BANNER);
//                        break;
//                    case "LARGE_BANNER":
//                        aAdView.setAdSize(AdSize.LARGE_BANNER);
//                        break;
//                    case "MEDIUM_RECTANGLE":
//                        aAdView.setAdSize(AdSize.MEDIUM_RECTANGLE);
//                        break;
//                    case "FULL_BANNER":
//                        aAdView.setAdSize(AdSize.FULL_BANNER);
//                        break;
//                    case "LEADERBOARD":
//                        aAdView.setAdSize(AdSize.LEADERBOARD);
//                        break;
//                    case "ADAPTIVE":
//                        aAdView.setAdSize(getAdSizeAdaptive());
//                        break;
//                    default:
//                        aAdView.setAdSize(AdSize.SMART_BANNER);
//                        break;
//                }
//                aAdSize = aAdView.getAdSize(); //store AdSize of banner due a bug (throws error when do aAdView.getAdSize(); called by Godot)
//                aAdView.setAdListener(new AdListener() {
//                    @Override
//                    public void onAdLoaded() {
//                        // Code to be executed when an ad finishes loading.
//                        emitSignal("banner_loaded");
//
//                        if (pShowInstantly){
//                            show_banner();
//                        }
//                        else{
//                            hide_banner();
//                        }
//                        aIsBannerLoaded = true;
//                    }
//
//                    @Override
//                    public void onAdFailedToLoad(@NonNull LoadAdError adError) {
//                        // Code to be executed when an ad request fails.
//                        emitSignal("banner_failed_to_load", adError.getCode());
//                    }
//
//                    @Override
//                    public void onAdOpened() {
//                        // Code to be executed when an ad opens an overlay that
//                        // covers the screen.
//                        emitSignal("banner_opened");
//                    }
//
//                    @Override
//                    public void onAdClicked() {
//                        // Code to be executed when the native ad is closed.
//                        emitSignal("banner_clicked");
//                    }
//
//                    @Override
//                    public void onAdClosed() {
//                        // Code to be executed when the user is about to return
//                        // to the app after tapping on an ad.
//                        emitSignal("banner_closed");
//                    }
//
//                    @Override
//                    public void onAdImpression() {
//                        // Code to be executed when the user is about to return
//                        // to the app after tapping on an ad.
//                        emitSignal("banner_recorded_impression");
//                    }
//                });
//
//                aGodotLayoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
//                if (pPosition == 0)//BOTTOM
//                {
//                    aGodotLayoutParams.gravity = Gravity.BOTTOM;
//                    if (pRespectSafeArea)
//                        aAdView.setY(-getSafeArea().bottom); //Need to validate if this value will be positive or negative
//                } else if (pPosition == 1)//TOP
//                {
//                    aGodotLayoutParams.gravity = Gravity.TOP;
//                    if (pRespectSafeArea)
//                        aAdView.setY(getSafeArea().top);
//                }
//                aGodotLayout.addView(aAdView, aGodotLayoutParams);
//
//                aAdView.loadAd(getAdRequest());
//
//            }
//        });
//    }
//    private AdSize getAdSizeAdaptive() {
//        // Determine the screen width (less decorations) to use for the ad width.
//        Display display = aActivity.getWindowManager().getDefaultDisplay();
//        DisplayMetrics outMetrics = new DisplayMetrics();
//        display.getMetrics(outMetrics);
//
//        float density = outMetrics.density;
//
//        float adWidthPixels = aGodotLayout.getWidth();
//
//        // If the ad hasn't been laid out, default to the full screen width.
//        if (adWidthPixels == 0) {
//            adWidthPixels = outMetrics.widthPixels;
//        }
//
//        int adWidth = (int) (adWidthPixels / density);
//        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(aActivity, adWidth);
//    }

//    public void destroy_banner()//IF THIS METHOD IS CALLED ON GODOT, THE BANNER WILL ONLY APPEAR AGAIN IF THE BANNER IS LOADED AGAIN
//    {
//        aActivity.runOnUiThread(() -> {
//            if (aIsInitialized && aAdView != null) {
//                aGodotLayout.removeView(aAdView);
//                aAdView.destroy();
//                aAdView = null;
//
//                emitSignal("banner_destroyed");
//                aIsBannerLoaded = false;
//            }
//        });
//    }
//    public void show_banner()
//    {
//        aActivity.runOnUiThread(() -> {
//            if (aIsInitialized && aAdView != null) {
//                if (aAdView.getVisibility() != View.VISIBLE){
//                    aAdView.setVisibility(View.VISIBLE);
//                    aAdView.resume();
//                }
//            }
//        });
//    }
//    public void hide_banner()
//    {
//        aActivity.runOnUiThread(() -> {
//            if (aIsInitialized && aAdView != null) {
//                if (aAdView.getVisibility() != View.GONE){
//                    aAdView.setVisibility(View.GONE);
//                    aAdView.pause();
//                }
//            }
//        });
//    }

//    public int get_banner_width() {
//        if (aIsInitialized && aAdSize != null) {
//            return aAdSize.getWidth();
//        }
//        return 0;
//    }

//    public int get_banner_height() {
//        if (aIsInitialized && aAdSize != null) {
//            return aAdSize.getHeight();
//        }
//        return 0;
//    }

//    public int get_banner_width_in_pixels() {
//        if (aIsInitialized && aAdSize != null) {
//            return aAdSize.getWidthInPixels(aActivity);
//        }
//        return 0;
//    }

//    public int get_banner_height_in_pixels() {
//        if (aIsInitialized && aAdSize != null) {
//            return aAdSize.getHeightInPixels(aActivity);
//        }
//        return 0;
//    }


    //BANNER
    //INTERSTITIAL
//    public void load_interstitial(final String pAdUnitId)
//    {
//        aActivity.runOnUiThread(() -> {
//            if (aIsInitialized) {
//                InterstitialAd.load(aActivity, pAdUnitId, getAdRequest(), new InterstitialAdLoadCallback() {
//                    @Override
//                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
//                        // Code to be executed when an ad finishes loading.
//                        aInterstitialAd = interstitialAd;
//
//                        emitSignal("interstitial_loaded");
//                        aIsInterstitialLoaded = true;
//                    }
//
//                    @Override
//                    public void onAdFailedToLoad(@NonNull LoadAdError adError) {
//                        // Code to be executed when an ad request fails.
//                        aInterstitialAd = null;
//                        emitSignal("interstitial_failed_to_load", adError.getCode());
//                    }
//                });
//            }
//        });
//    }
//    public void show_interstitial()
//    {
//        aActivity.runOnUiThread(() -> {
//            if (aIsInitialized) {
//                if (aInterstitialAd != null) {
//                    aInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
//                        @Override
//                        public void onAdClicked() {
//                            // Called when a click is recorded for an ad.
//                            emitSignal("interstitial_clicked");
//                        }
//
//                        @Override
//                        public void onAdDismissedFullScreenContent() {
//                            // Called when fullscreen content is dismissed.
//                            aInterstitialAd = null;
//                            emitSignal("interstitial_closed");
//                            aIsInterstitialLoaded = false;
//                        }
//
//                        @Override
//                        public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
//                            // Called when fullscreen content failed to show.
//                            aInterstitialAd = null;
//                            emitSignal("interstitial_failed_to_show", adError.getCode());
//                            aIsInterstitialLoaded = false;
//                        }
//
//                        @Override
//                        public void onAdImpression() {
//                            // Called when an impression is recorded for an ad.
//                            emitSignal("interstitial_recorded_impression");
//                        }
//
//                        @Override
//                        public void onAdShowedFullScreenContent() {
//                            // Called when fullscreen content is shown.
//                            emitSignal("interstitial_opened");
//                        }
//                    });
//
//                    aInterstitialAd.show(aActivity);
//                }
//            }
//        });
//    }
//    //INTERSTITIAL
//    //REWARDED
//    public void load_rewarded(final String pAdUnitId)
//    {
//        aActivity.runOnUiThread(() -> {
//            if (aIsInitialized) {
//                RewardedAd.load(aActivity, pAdUnitId, getAdRequest(), new RewardedAdLoadCallback(){
//                    @Override
//                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
//                        // Handle the error.
//                        aRewardedAd = null;
//                        emitSignal("rewarded_ad_failed_to_load", loadAdError.getCode());
//
//                    }
//
//                    @Override
//                    public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
//                        aRewardedAd = rewardedAd;
//                        emitSignal("rewarded_ad_loaded");
//
//                        aIsRewardedLoaded = true;
//                    }
//                });
//            }
//        });
//    }

//    public void show_rewarded()
//    {
//        aActivity.runOnUiThread(() -> {
//            if (aIsInitialized) {
//                if (aRewardedAd != null) {
//                    aRewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
//                        @Override
//                        public void onAdClicked() {
//                            // Called when a click is recorded for an ad.
//                            emitSignal("rewarded_ad_clicked");
//                        }
//
//                        @Override
//                        public void onAdDismissedFullScreenContent() {
//                            // Called when ad is dismissed.
//                            aRewardedAd = null;
//                            emitSignal("rewarded_ad_closed");
//                            aIsRewardedLoaded = false;
//                        }
//
//                        @Override
//                        public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
//                            // Called when ad fails to show.
//                            aRewardedAd = null;
//                            emitSignal("rewarded_ad_failed_to_show", adError.getCode());
//                        }
//
//                        @Override
//                        public void onAdImpression() {
//                            // Called when an impression is recorded for an ad.
//                            emitSignal("rewarded_ad_recorded_impression");
//                        }
//
//                        @Override
//                        public void onAdShowedFullScreenContent() {
//                            // Called when ad is shown.
//                            emitSignal("rewarded_ad_opened");
//                        }
//                    });
//
//                    aRewardedAd.show(aActivity, rewardItem -> {
//                        // Handle the reward.
//                        emitSignal("user_earned_rewarded", rewardItem.getType(), rewardItem.getAmount());
//                    });
//                }
//            }
//        });
//    }
    //

//    public void load_rewarded_interstitial(final String pAdUnitId)
//    {
//        aActivity.runOnUiThread(() -> {
//            if (aIsInitialized) {
//                RewardedInterstitialAd.load(aActivity, pAdUnitId, getAdRequest(), new RewardedInterstitialAdLoadCallback(){
//                    @Override
//                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
//                        // Handle the error.
//                        aRewardedInterstitialAd = null;
//                        emitSignal("rewarded_interstitial_ad_failed_to_load", loadAdError.getCode());
//                    }
//
//                    @Override
//                    public void onAdLoaded(@NonNull RewardedInterstitialAd rewardedInterstitialAd) {
//                        aRewardedInterstitialAd = rewardedInterstitialAd;
//                        emitSignal("rewarded_interstitial_ad_loaded");
//                        aIsRewardedInterstitialLoaded = true;
//                    }
//                });
//            }
//        });
//    }


//    public void show_rewarded_interstitial()
//    {
//        aActivity.runOnUiThread(() -> {
//            if (aIsInitialized) {
//                if (aRewardedInterstitialAd != null) {
//                    aRewardedInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
//                        @Override
//                        public void onAdClicked() {
//                            // Called when a click is recorded for an ad.
//                            emitSignal("rewarded_interstitial_ad_clicked");
//                        }
//
//                        @Override
//                        public void onAdDismissedFullScreenContent() {
//                            // Called when ad is dismissed.
//                            aRewardedInterstitialAd = null;
//                            emitSignal("rewarded_interstitial_ad_closed");
//                            aIsRewardedInterstitialLoaded = false;
//                        }
//
//                        @Override
//                        public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
//                            // Called when ad fails to show.
//                            aRewardedInterstitialAd = null;
//                            emitSignal("rewarded_interstitial_ad_failed_to_show", adError.getCode());
//                            aIsRewardedInterstitialLoaded = false;
//                        }
//
//                        @Override
//                        public void onAdImpression() {
//                            // Called when an impression is recorded for an ad.
//                            emitSignal("rewarded_interstitial_ad_recorded_impression");
//                        }
//
//                        @Override
//                        public void onAdShowedFullScreenContent() {
//                            // Called when ad is shown.
//                            emitSignal("rewarded_interstitial_ad_opened");
//                        }
//                    });
//
//                    aRewardedInterstitialAd.show(aActivity, rewardItem -> {
//                        // Handle the reward.
//                        emitSignal("user_earned_rewarded", rewardItem.getType(), rewardItem.getAmount());
//                    });
//                }
//            }
//        });
//    }

    /**
     * Generate MD5 for the deviceID
     * @param  s The string to generate de MD5
     * @return String The MD5 generated
     */
//    private String md5(final String s)
//    {
//        try
//        {
//            // Create MD5 Hash
//            MessageDigest digest = MessageDigest.getInstance("MD5");
//            digest.update(s.getBytes());
//            byte[] messageDigest = digest.digest();
//
//            // Create Hex String
//            StringBuilder hexString = new StringBuilder();
//            for (byte b : messageDigest) {
//                StringBuilder h = new StringBuilder(Integer.toHexString(0xFF & b));
//                while (h.length() < 2) h.insert(0, "0");
//                hexString.append(h);
//            }
//            return hexString.toString();
//        }
//        catch(NoSuchAlgorithmException e)
//        {
//            //Logger.logStackTrace(TAG,e);
//        }
//        return "";
//    }

    /**
     * Get the Device ID for AdMob
     * @return String Device ID
     */
//    private String getDeviceId()
//    {
//        String android_id = Settings.Secure.getString(aActivity.getContentResolver(), Settings.Secure.ANDROID_ID);
//        return md5(android_id).toUpperCase(Locale.US);
//    }




    // @Override
    // public void onCreate() {
    //     super.onCreate();           
    // }

    @Override
    public void onActivityStopped(Activity activity) {}

    @Override
    public void onActivityStarted(Activity activity) {}

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}

    @Override
    public void onActivityResumed(Activity activity) {
        if (aIsInitialized && activity == aActivity) {

            emitSignal("debug_message", "Activity resumed!");

            switch(aGameServiceScreen) {
            case GAME_SERVICE_SCREEN_NONE:
                break;
            case GAME_SERVICE_SCREEN_LEADERBOARD:
                emitSignal("show_leaderboard_dismissed", (aLeaderboardId == null ? "" : aLeaderboardId));
            }

            aGameServiceScreen = GAME_SERVICE_SCREEN_NONE;
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {}

    @Override
    public void onActivityDestroyed(Activity activity) {}

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}


}