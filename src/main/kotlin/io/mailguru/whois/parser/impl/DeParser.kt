package io.mailguru.whois.parser.impl

import io.mailguru.whois.enumeration.Status
import io.mailguru.whois.model.MutableWhoisResult
import io.mailguru.whois.model.WhoisResult
import io.mailguru.whois.parser.Parser
import org.parboiled.Action
import org.parboiled.Context
import org.parboiled.Rule
import org.parboiled.errors.ErrorUtils
import org.parboiled.parserunners.ReportingParseRunner
import org.parboiled.support.ParseTreeUtils

/**
 * See https://www.denic.de/fileadmin/public/documentation/DENIC-12p_EN.pdf
 */
open class DeParser : Parser(setOf("whois.denic.de")) {

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

    override fun parse(input: String): WhoisResult {
        return ReportingParseRunner<MutableWhoisResult>(start()).run(input).let { result ->

            check(result.parseErrors.isEmpty()) {
                ParseTreeUtils.printNodeTree(result)
                result.parseErrors.forEach {
                    ErrorUtils.printParseError(it)
                }

                PARSE_ERROR_MSG
            }

            result.resultValue.let { mutableResult ->
                WhoisResult(
                    domain = mutableResult.domain ?: error(PARSE_ERROR_MSG),
                    status = mutableResult.status ?: error(PARSE_ERROR_MSG),
                    headerComment = mutableResult.header.joinToString("\n") { it.trim() }.trim('\n'),
                    changedAt = mutableResult.changed
                )
            }
        }
    }
}
