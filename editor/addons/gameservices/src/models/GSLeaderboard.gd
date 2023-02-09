#
# GSLeaderboard.gd
# sjc - 14/1/32
# Data class representing the meta data of a leaderboard
#

class_name GSLeaderboard
extends Reference

var id := ""
var display_name := ""

func _init(info: Dictionary):
    self.id = info.get("id", "")
    self.display_name = info.get("display_name", "")

func _to_string() -> String:
    return "GSLeaderboard id: %s name: %s" % [self.id, self.display_name]
