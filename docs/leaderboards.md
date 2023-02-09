---
layout: default
title: godot-game-services - Leaderboards
---

## Leaderboards

**Note:** You must create the leaderboards in the platform service console before they can be accessed.

* [Android Play Game Services](https://developers.google.com/games/services/android/leaderboards)

* [iOS Game Center](https://developer.apple.com/documentation/gamekit/gkleaderboard)


### Constants

#### `LeaderboardPlayers`

Enum passed as a parameter to select which friend group to display leaderboard for.

|Value|Meaning|
|-|-|
|`ALL`|Show results for all players|
|`FRIENDS`|Show results for friends of the current player only|

#### `LeaderboardTime`

Passed as a parameter to select which time group to display leaderboard for.

|Value|Meaning|
|-|-|
|`DAILY`|Show today's scores|
|`WEEKLY`|Show this week's scores|
|`ALL_TIME`|Show all scores|


### Functions

#### `show_leaderboard(leaderboard_id: String, players: LeaderboardPlayers, time: LeaderboardTime)`

Trigger the display of a particular leaderboard using the platform game service UI.

`leaderboard_id` will be looked-up in the table from the Game Services editor tab. If no match is found, the raw value is passed to the native library.

On iOS, if the `leaderboard_id` is not valid, the UI for "all leaderboards" will be shown instead.

Causes the `show_leaderboard_complete` or `show_leaderboard_failed` signals to be sent. If the leaderboard was shown, the `show_leaderboard_dismissed` signal will be sent when the user returns to the app.

#### `show_all_leaderboards()`

Trigger the display of the platform game service's main screen listing all leaderboard for the app.

Causes the `show_leaderboard_complete` or `show_leaderboard_failed` signals to be sent. If the leaderboard was shown, the `show_leaderboard_dismissed` signal will be sent when the user returns to the app.

#### `submit_score(leaderboard_name: String, score: int)`

Submits a score to the given leaderboard.

`leaderboard_id` will be looked-up in the table from the Game Services editor tab. If no match is found, the raw value is passed to the native library.

Causes the `submit_score_complete` or `submit_score_failed` signals to be sent.

#### `fetch_top_scores(leaderboard_id: String, page_size: int, players: LeaderboardPlayers, time: LeaderboardTime)`

Begin paging scores from the given leaderboard.

`leaderboard_id` will be looked-up in the table from the Game Services editor tab. If no match is found, the raw value is passed to the native library.

`page_size` is the number of results returned per call.

Causes the `fetch_scores_complete` or `fetch_scores_failed` signals to be sent.

#### `fetch_next_scores()`

Continues paging scores started with the `fetch_top_scores()` function.

If this function is called before `fecth_top_scores()` an error is returned via the `fetch_scores_failed` signal.

The `more_available` parameter of the `fetch_scores_complete` signal should be consulted before calling this function. If the value of `more_available` is `false` then there are no more scores to return.


### Signals

#### `show_leaderboard_complete(leaderboard_id: String)`

Sent in response to `show_leaderboard()` or `show_all_leaderboards()`.

In the case of `show_all_leaderboards()`, the `leaderboard_id` will be an empty string.

#### `show_leaderboard_failed(leaderboard_id: String, error_message: String)`

Sent in response to `show_leaderboard()` or `show_all_leaderboards()`.

In the case of `show_all_leaderboards()`, the `leaderboard_id` will be an empty string.

#### `show_leaderboard_dismissed()`

Sent when the user navigates back to the app from a leaderboard shown by either `show_leaderboard()` or `show_all_leaderboards()`.

#### `submit_score_completed(leaderboard_id: String)`

Sent after `submit_score()` succeeds.

#### `submit_score_failed(leaderboard_id: String, error_message: String)`

Sent if `submit_score()` fails.

#### `fetch_scores_complete(leaderboard_info: GSLeaderboard, player_score: GSScore, scores: Array<GSScore>, more_available: bool)`

Sent in response to `fetch_top_scores()` and `fetch_next_scores()` calls.

`leaderboard_info` is a [`GSLeaderboard`](gsleaderboard.html) object.

`player_score` is a [`GSScore`](gsscore.html) object holding the current score for the local authorized player. This score may or may not appear in the `scores` array.

`scores` is an array of [`GSScore`](gsscore.html) objects.

`more_available` is a flag indicating whether more scores can be fetched with a call to `fetch_next_scores()`.

#### `fetch_scores_failed(leaderboard_id: String, error_message: String)`

Sent if `fetch_top_scores()` or `fetch_more_scores()` fails.
