---
layout: default
title: godot-game-services - GSPlayer
---

## GSPlayer

Information about a player.

### `id: String`

The ID assigned to the player by the platform game services.

**Note:** On Android, an ID may not be assigned to the local player until the first time they interact with the service (eg. not until the first time they post a score).

**Note:** On iOS, this value will be the `.playerID` on devices running < iOS 13.0, and `.gamePlayerID` on >= 13.0 devices.

### `display_name: String`

The display name chosen by the player.

### `is_local_player: bool`

Whether this `GSPlayer` object represents the local authorized player.
