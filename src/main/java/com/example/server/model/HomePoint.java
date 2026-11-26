package com.example.server.model;

import java.util.UUID;

/**
 * A named teleport home owned by a player.
 */
public record HomePoint(UUID id, String name, int x, int y, int z, String iconItemId) {
}
