#
# GSScore.gd
# sjc - 14/1/32
# Data class representing a score entry in a leaderboard
#

class_name GSScore
extends Reference

var rank := -1
var score := -1
var formatted_score := ""
var player: GSPlayer = null

func _init(info: Dictionary):
    self.rank = info.get("rank", -1)
    self.score = info.get("score", -1)
    self.formatted_score = info.get("formatted_score", "")

    var player_info = info.get("player", null)
    if player_info != null and not player_info.empty():
        self.player = GSPlayer.new(player_info)

func _to_string() -> String:
    return "GSScore rank: %d score: %d formatted: %s\n\tplayer: %s" % [self.rank, self.score, self.formatted_score, (self.player if self.player != null else "Unknown")]
