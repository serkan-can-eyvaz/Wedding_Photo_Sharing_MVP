package com.weddingshare.auth;

import java.time.Instant;

public record LoginResponse(String token, Instant expiresAt) {
}
