package io.mailguru.whois.model

import io.mailguru.whois.enumeration.Status

internal data class MutableWhoisResult(
    var changed: String? = null,
    var dnsKey: String? = null,
    var domain: String? = null,
    var domainAce: String? = null,
    var status: Status? = null,

    val header: MutableList<String> = mutableListOf(),
    val nserver: MutableSet<String> = mutableSetOf(),
    val nsentry: MutableSet<String> = mutableSetOf(),
) {

    fun addHeader(value: String): Boolean = header.add(value)
    fun addNserver(value: String): Boolean = nserver.add(value)
    fun addNsentry(value: String): Boolean = nsentry.add(value)

    fun setChanged(value: String): Boolean {
        check(null == changed) // setter is available only once
        changed = value
        return true
    }

    fun setDnsKey(value: String): Boolean {
        check(null == dnsKey) // setter is available only once
        dnsKey = value
        return true
    }

    fun setDomain(value: String): Boolean {
        check(null == domain) // setter is available only once
        domain = value
        return true
    }

    fun setDomainAce(value: String): Boolean {
        check(null == domainAce) // setter is available only once
        domainAce = value
        return true
    }

    fun setStatus(value: Status): Boolean {
        check(null == status) // setter is available only once
        status = value
        return true
    }
}
