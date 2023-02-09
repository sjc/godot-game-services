---
layout: default
title: godot-game-services - GSScore
---

## GSScore

A score registered with a leaderboard.

### `rank: int`

The position in the leaderboard, or -1 if no score has been submitted (eg. for the local authorized player before they submit their first score).

### `score: int`

The recorded score, or -1 if no score has been submitted (eg. for the local authorized player before they submit their first score).

### `formatted_score: String`

The `score` formatted according to the method selected in the platform game services console for the parent leaderboard. Will be an empty string if `score` is -1.

### `player: GSPlayer`

A [`GSPlayer`](gsplayer.html) object representing the player who recorded this score.
