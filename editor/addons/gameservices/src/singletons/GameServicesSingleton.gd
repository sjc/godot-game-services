#
# GameServices.gd
# sjc - 16/1/23
# Private interface to the GameServices plugins and helpers, including signal management
#

extends Node

#
# Model classes
#

const GSPlayer = preload("../models/GSPlayer.gd")
const GSLeaderboard = preload("../models/GSLeaderboard.gd")
const GSScore = preload("../models/GSScore.gd")

#
# Signals
#

signal authorization_complete(authenticated, player)
signal authorization_failed(error_message)

signal show_leaderboard_complete(leaderboard_id)
signal show_leaderboard_failed(leaderboard_id, error_message)
signal show_leaderboard_dismissed()

signal fetch_scores_complete(leaderboard_info, player_score, scores, more_available)
signal fetch_scores_failed(leaderboard_id, error_message)

signal submit_score_complete(leaderboard_id)
signal submit_score_failed(leaderboard_id, error_message)

#
# vars
#

var GameServicesConfig = preload("res://addons/gameservices/src/utils/GameServicesConfig.gd").new()
onready var leaderboard_ids = GameServicesConfig.leaderboard_ids_for_platform(OS.get_name())

var _plugin : Object

var is_authorized := false


func _ready() -> void:
	if (Engine.has_singleton("GameServices")):
		_plugin = Engine.get_singleton("GameServices")
		_connect_signals()
		# TODO: flag to automatic call authorise via the config?
		#_plugin.initialize()


func _connect_signals() -> void:

	_plugin.connect("debug_message", self, "_on_GameServices_debug_message")

	_plugin.connect("authorization_complete", self, "_on_GameServices_authorization_complete")
	_plugin.connect("authorization_failed", self, "_on_GameServices_authorization_failed")

	_plugin.connect("show_leaderboard_complete", self, "_on_GameServices_show_leaderboard_complete")
	_plugin.connect("show_leaderboard_failed", self, "_on_GameServices_show_leaderboard_failed")
	_plugin.connect("show_leaderboard_dismissed", self, "_on_GameServices_show_leaderboard_dismissed")

	_plugin.connect("fetch_scores_complete", self, "_on_GameServices_fetch_scores_complete")
	_plugin.connect("fetch_scores_failed", self, "_on_GameServices_fetch_scores_failed")

	_plugin.connect("submit_score_complete", self, "_on_GameServices_submit_score_complete")
	_plugin.connect("submit_score_failed", self, "_on_GameServices_submit_score_failed")

#
# Signal callbacks
#

#
# Debug
#

func _on_GameServices_debug_message(message: String) -> void:
	print("GameServices: ", message)

#
# Lifecycle
#

func _on_GameServices_authorization_complete(authorized: bool, player_dict: Dictionary) -> void:
	self.is_authorized = authorized
	var player = GSPlayer.new(player_dict) if not player_dict.empty() else null
	emit_signal("authorization_complete", authorized, player)

func _on_GameServices_authorization_failed(error_message: String) -> void:
	emit_signal("authorization_failed", error_message)

#
# Leaderboard helpers
#

func _platform_leaderboard_id(leaderboard_id: String) -> String:
	return self.leaderboard_ids.get(leaderboard_id, leaderboard_id)


func _shared_leaderboard_id(platform_leaderboard_id: String) -> String:
	var index = self.leaderboard_ids.values().find(platform_leaderboard_id)
	if index != -1:
		platform_leaderboard_id = self.leaderboard_ids.keys()[index]
	return platform_leaderboard_id

#
# Leaderboards
#

func _on_GameServices_show_leaderboard_complete(leaderboard_id: String) -> void:
	emit_signal("show_leaderboard_complete", _shared_leaderboard_id(leaderboard_id))

func _on_GameServices_show_leaderboard_failed(leaderboard_id: String, error_message: String) -> void:
	emit_signal("show_leaderboard_failed", _shared_leaderboard_id(leaderboard_id), error_message)

func _on_GameServices_show_leaderboard_dismissed(leaderboard_id: String) -> void:
	emit_signal("show_leaderboard_dismissed", _shared_leaderboard_id(leaderboard_id))

#
# Leaderboard data
#

func _on_GameServices_fetch_scores_complete(leaderboard_dict, player_score_dict, scores_dict, more_available) -> void:

	# parse the leaderboard info, fixing the ID to be the shared version
	var leaderboard = null
	if leaderboard_dict != null:
		leaderboard_dict["id"] = _shared_leaderboard_id(leaderboard_dict.get("id",""))
		leaderboard = GSLeaderboard.new(leaderboard_dict)

	# parse the player
	var player_score = GSScore.new(player_score_dict) if not player_score_dict.empty() else null

	# parse the scores
	# we can't return arrays from plugins, so the result is a dictionary with index as key
	var scores = []
	for i in range(scores_dict.size()):
		var key = String(i)
		var score_dict = scores_dict.get(key, {})
		if not score_dict.empty():
			scores.append(GSScore.new(score_dict))

	emit_signal("fetch_scores_complete", leaderboard, player_score, scores, more_available)

func _on_GameServices_fetch_scores_failed(leaderboard_id: String, error_message: String) -> void:
	emit_signal("fetch_scores_failed", _shared_leaderboard_id(leaderboard_id), error_message)

#
# Scores
#

func _on_GameServices_submit_score_complete(leaderboard_id: String) -> void:
	emit_signal("submit_score_complete", _shared_leaderboard_id(leaderboard_id))

func _on_GameServices_submit_score_failed(leaderboard_id: String, error_message: String) -> void:
	emit_signal("submit_score_failed", _shared_leaderboard_id(leaderboard_id), error_message)

