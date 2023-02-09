#
# GSPlayer.gd
# sjc - 14/1/32
# Data class representing a player entity
#

class_name GSPlayer
extends Reference

var id := ""
var display_name := ""
var is_local_player := false

func _init(info: Dictionary):
    self.id = info.get("id", "")
    self.display_name = info.get("display_name", "")
    self.is_local_player = info.get("is_local_player", false)

func _to_string() -> String:
    return "GSPlayer id: %s name: %s local player? %s" % [self.id, self.display_name, ("yes" if self.is_local_player else "no")]
