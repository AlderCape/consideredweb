package com.consideredweb.auth

import com.consideredweb.core.Request

/**
 * Common authentication extensions for ConsideredWeb requests
 */

/**
 * Common header names for authentication context
 */
object AuthHeaders {
    const val USER_ID = "X-Auth-User-Id"
    const val USER_EMAIL = "X-Auth-User-Email"
    const val USER_NAME = "X-Auth-User-Name"
    const val ORG_ID = "X-Auth-Org-Id"
    const val MEMBER_ID = "X-Auth-Member-Id"
}

/**
 * Extension functions to extract authenticated user information from enriched requests
 */
fun Request.currentUserId(): String? = header(AuthHeaders.USER_ID)?.takeIf { it.isNotBlank() }
fun Request.currentUserEmail(): String? = header(AuthHeaders.USER_EMAIL)?.takeIf { it.isNotBlank() }
fun Request.currentUserName(): String? = header(AuthHeaders.USER_NAME)?.takeIf { it.isNotBlank() }
fun Request.currentOrgId(): String? = header(AuthHeaders.ORG_ID)?.takeIf { it.isNotBlank() }
fun Request.currentMemberId(): String? = header(AuthHeaders.MEMBER_ID)?.takeIf { it.isNotBlank() }

/**
 * Helper to add authentication context to request headers
 */
fun Request.withAuthHeaders(
    userId: String? = null,
    userEmail: String? = null,
    userName: String? = null,
    orgId: String? = null,
    memberId: String? = null
): Request {
    val authHeaders = mutableMapOf<String, String>()

    userId?.let { authHeaders[AuthHeaders.USER_ID] = it }
    userEmail?.let { authHeaders[AuthHeaders.USER_EMAIL] = it }
    userName?.let { authHeaders[AuthHeaders.USER_NAME] = it }
    orgId?.let { authHeaders[AuthHeaders.ORG_ID] = it }
    memberId?.let { authHeaders[AuthHeaders.MEMBER_ID] = it }

    return this.copy(headers = this.headers + authHeaders)
}

/**
 * Check if request has valid authentication context
 */
fun Request.isAuthenticated(): Boolean = currentUserId() != null || currentUserEmail() != null

/**
 * Require authentication - throws exception if not authenticated
 */
fun Request.requireAuth(): Request {
    if (!isAuthenticated()) {
        throw IllegalStateException("Request requires authentication")
    }
    return this
}