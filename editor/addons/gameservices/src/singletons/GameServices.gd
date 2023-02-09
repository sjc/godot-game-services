#
# GameServices.gd
# sjc - 16/1/23
# Public interface to the GameServices plugins
#

extends "res://addons/gameservices/src/singletons/GameServicesSingleton.gd" 

func get_service_name() -> String:
	if _plugin:
		return _plugin.get_service_name()
	return ""


func initialize() -> void:
	if _plugin:
		_plugin.initialize()


func can_sign_in() -> bool:
	if _plugin:
		return _plugin.can_sign_in()
	return false


func sign_in() -> void:
	if _plugin:
		_plugin.sign_in()

#
# Leaderboards
#

enum LeaderboardPlayers {
	ALL, 		# 
	FRIENDS = 3 # 
}

enum LeaderboardTime {
	DAILY,		# TIME_SPAN_DAILY or GKLeaderboardTimeScopeToday
	WEEKLY,		# TIME_SPAN_WEEKLY or GKLeaderboardTimeScopeWeek
	ALL_TIME,	# TIME_SPAN_ALL_TIME or GKLeaderboardTimeScopeAllTime
}

func show_leaderboard(leaderboard_id: String, players: int = LeaderboardPlayers.ALL, time: int = LeaderboardTime.ALL_TIME) -> void:
	if _plugin:
		_plugin.show_leaderboard(_platform_leaderboard_id(leaderboard_id), players, time)


func show_all_leaderboards() -> void:
	if _plugin:
		_plugin.show_all_leaderboards()


#
# Leaderboard data
#

func fetch_top_scores(leaderboard_id: String, page_size: int = 10, players: int = LeaderboardPlayers.ALL, time: int = LeaderboardTime.ALL_TIME) -> void:
	if _plugin:
		_plugin.fetch_top_scores(_platform_leaderboard_id(leaderboard_id), page_size, players, time)


func fetch_next_scores() -> void:
	if _plugin:
		_plugin.fetch_next_scores()


#
# Scores
#

func submit_score(leaderboard_id: String, score: int) -> void:
	if _plugin:
		_plugin.submit_score(_platform_leaderboard_id(leaderboard_id), score)
