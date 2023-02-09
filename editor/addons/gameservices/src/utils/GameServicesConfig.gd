#
# GameServicesConfig.gd
# sjc - 16/1/23
# Data class wrapping the config which we store in the main ProjectSettings
#

extends Resource

const SETTINGS_PATH = "gameservices/config"

const LEADERBOARD_IDS_KEY = "leaderboard_ids"
const LEADERBOARD_NAMES_KEY = "names"
const LEADERBOARD_ANDROID_KEY = "android"
const LEADERBOARD_IOS_KEY = "ios"

var config = {
	LEADERBOARD_IDS_KEY: {
		LEADERBOARD_NAMES_KEY: [],
		LEADERBOARD_ANDROID_KEY: [],
		LEADERBOARD_IOS_KEY: []
	},
}


func _init():
	var loaded_config = _load()
	_merge_dict(self.config, loaded_config)
	if Engine.is_editor_hint():
		save()

#
# Accessing config values
#

func leaderboard_ids_for_platform(platform: String) -> Dictionary:

	print("looking up defaults for: '", platform.to_lower(), "'")

	# get the data, based on the name returned by OS.get_name()
	var leaderboard_ids = config.get(LEADERBOARD_IDS_KEY, {})
	var values = leaderboard_ids.get(platform.to_lower())
	
	# check whether we support this platform
	if values == null:
		return {}

	var names = leaderboard_ids.get(LEADERBOARD_NAMES_KEY, [])
	var ids = {}

	for i in names.size():
		# don't add an entry for empty string, so the lookup returns the key instead
		var value = values[i] if values.size() > i else ""
		if not value.empty():
			ids[names[i]] = value
	
	print("returning: ", ids)

	return ids

#
# Loading and saving
#

func _load() -> Dictionary:
	if ProjectSettings.has_setting(SETTINGS_PATH):
		return ProjectSettings.get_setting(SETTINGS_PATH)
	return {}


func save():
	ProjectSettings.set_setting(SETTINGS_PATH, self.config)
	ProjectSettings.save()

#
# Utils
#

# lifted directly from the AdMob AdMobSettings.gd file
func _merge_dict(target : Dictionary, patch : Dictionary):
	for key in patch:
		if target.has(key):
			var tv = target[key]
			if typeof(tv) == TYPE_DICTIONARY:
				_merge_dict(tv, patch[key])
			else:
				target[key] = patch[key]
		else:
			target[key] = patch[key]
