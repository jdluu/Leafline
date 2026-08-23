package com.jdluu.leafline.opds

/**
 * Local configuration for a Grimmory OPDS server.
 *
 * Credentials are kept only here and never written to git, logs, or remote
 * analytics. This is an in-memory model for the first OPDS slice.
 */
data class OpdsServerConfig(
    val id: String = "default",
    val catalogUrl: String,
    val username: String,
    val password: String
)
