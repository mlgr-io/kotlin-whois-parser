package io.mailguru.whois.parser.impl

import io.mailguru.whois.enumeration.Status
import io.mailguru.whois.model.MutableWhoisResult
import io.mailguru.whois.parser.Parser
import org.parboiled.Action
import org.parboiled.Context
import org.parboiled.Rule

/**
 * https://github.com/DK-Hostmaster/whois-service-specification
 */
open class DkParser : Parser(setOf("whois.dk-hostmaster.dk")) {

    override fun start(): Rule = MutableWhoisResult().let { result ->
        Sequence(
            ZeroOrMore(
                singleRowWithAction("#") { result.addHeader(pop() as String) },
            ),
            FirstOf(
                Sequence(
                    IgnoreCase("no entries found for the selected source."),
                    object : Action<String> {
                        override fun run(context: Context<String>): Boolean = result.setStatus(Status.FREE)
                    },
                ),
                Sequence(
                    singleRowWithAction("domain:") { result.setDomain(pop() as String) },
                    singleRowWithActionIgnorePrevious("status:") { _ ->
                        when ((pop() as String).lowercase()) {
                            "active" -> Status.CONNECT
                            "deactivated" -> Status.DEACTIVATED
                            "reserved" -> Status.RESERVED
                            "offered to waiting list" -> Status.WAITING_LIST
                            else -> null
                        }?.let {
                            result.setStatus(it)
                        } ?: false
                    },
                ),
            ),
            object : Action<MutableWhoisResult> {
                override fun run(context: Context<MutableWhoisResult>): Boolean = push(result)
            },
        )
    }
}
