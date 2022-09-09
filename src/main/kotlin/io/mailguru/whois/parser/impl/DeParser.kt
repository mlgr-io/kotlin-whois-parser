package io.mailguru.whois.parser.impl

import io.mailguru.whois.enumeration.Status
import io.mailguru.whois.model.MutableWhoisResult
import io.mailguru.whois.parser.Parser
import org.parboiled.Action
import org.parboiled.Context
import org.parboiled.Rule

/**
 * See https://www.denic.de/fileadmin/public/documentation/DENIC-12p_EN.pdf
 */
internal open class DeParser : Parser(setOf("whois.denic.de")) {

    override fun start(): Rule = MutableWhoisResult().let { result ->
        Sequence(
            ZeroOrMore(
                singleRowWithAction("%") { result.addHeader(pop() as String) },
            ),
            singleRowWithAction("domain:") { result.setDomain(pop() as String) },
            Optional(
                singleRowWithAction("domain-ace:") { result.setDomainAce(pop() as String) },
            ),
            Optional(
                FirstOf(
                    OneOrMore(
                        Sequence(
                            singleRowWithAction("nserver:") { result.addNserver(pop() as String) },
                            Optional(
                                singleRowWithAction("dnskey:") { result.setDnsKey(pop() as String) },
                            ),
                        ),
                    ),
                    OneOrMore(
                        singleRowWithAction("nsentry:") { result.addNsentry(pop() as String) },
                    )
                ),
            ),
            singleRowWithAction("status:") { _ ->
                when ((pop() as String).lowercase()) {
                    "connect" -> Status.CONNECT
                    "failed" -> Status.FAILED
                    "free" -> Status.FREE
                    "invalid" -> Status.INVALID
                    else -> null
                }?.let {
                    result.setStatus(it)
                } ?: false
            },
            Optional(
                singleRowWithAction("changed:") { result.setChanged(pop() as String) },
            ),
            object : Action<MutableWhoisResult> {
                override fun run(context: Context<MutableWhoisResult>): Boolean = push(result)
            },
//            Action { _: Context<MutableWhoisResult> -> push(result) },
        )
    }
}
