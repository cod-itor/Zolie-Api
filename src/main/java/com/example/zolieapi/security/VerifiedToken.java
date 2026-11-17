package com.example.zolieapi.security;

public record VerifiedToken(String uid, String email, boolean admin) {
}
