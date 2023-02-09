---
layout: default
title: godot-game-services - Authorization
---

## Authorization

Allow the player to sign-in to the platform game service.

**Note:** During development, before your services are made generally available, you will need to register test users.

### Functions

#### `initialize()`

Should be called as soon as possible in your app's lifecycle. If a player has previously authenticated, they will be signed-in automatically. If no player has authenticated, the platform sign-in screen will be shown.

Causes the `authorization_complete` or `authorization_failed` signal to be sent once complete.

#### `is_authorized: bool`

Whether there is an authorized local player.

#### `can_sign_in() -> bool`

Returns whether the platform game service allows a player to sign-in after the initial call to `initialize()`, and thus whether calling `sign_in()` does anything.

This will be `true` for Android, and `false` for iOS.

#### `sign_in()`

Triggers the platform game service sign-in screen to be shown, if available.

Causes the `authorization_complete` or `authorization_failed` signal to be sent once complete.

### Signals

#### `authorization_complete(authenticated: bool, player: GSPlayer)`

Sent after calls to `initialize()` or `sign_in()`.

If `authenticated` is `false` then `player` will be `null`. Otherwise, `player` will be a [`GSPlayer`](gsplayer.html) object representing the signed-in local player.

#### `authorization_failed(error_message: String)`

Sent after calls to `initialize()` or `sign_in()`, if they failed with an error. Not having an authorized local player is not considered an error.
