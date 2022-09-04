package io.mailguru.whois.model

import io.mailguru.whois.enumeration.Status

data class WhoisResult(
    val domain: String,
    val status: Status,
    val headerComment: String? = null,
    val changedAt: String? = null,
)
