#
# GameServicesPluging.gd
# sjc - 16/1/23
#

tool
extends EditorPlugin

const GameServicesTab = preload("res://addons/gameservices/src/ui/GameServicesTab.tscn")

var tab_instance = null

func _enter_tree():
	# Setup the singleton, accessible from Godot code
	add_autoload_singleton("GameServices", "res://addons/gameservices/src/singletons/GameServices.gd")

	# Setup the configuration tab shown in the editor
	tab_instance = GameServicesTab.instance()
	get_editor_interface().get_editor_viewport().add_child(tab_instance)
	make_visible(false)


func _exit_tree():
	# Remove the singleton
	remove_autoload_singleton("GameServices")
	
	# Remove the tab from the editor
	if tab_instance != null:
		tab_instance.queue_free()
		tab_instance = null
	

func has_main_screen():
	return true


func make_visible(visible):
	if tab_instance != null:
		tab_instance.visible = visible


func get_plugin_name():
	return "GameServices"
