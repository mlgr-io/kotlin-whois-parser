package io.mailguru.whois.parser.impl

import io.mailguru.whois.enumeration.Status
import io.mailguru.whois.model.WhoisResult
import io.mailguru.whois.parser.Parser
import org.junit.jupiter.params.provider.Arguments
import java.util.stream.Stream

internal class DeParserTest : AbstractParserImplTest() {

    private val sut = DeParser()

    override fun getSut(): Parser = sut

    override fun provideData(): Stream<Arguments> = Stream.of(
        Arguments.of(
            "/whois/whois.denic.de/connect-de.txt",
            WhoisResult(
                changedAt = "2018-03-12T21:44:25+01:00",
                domain = "connect.de",
                status = Status.CONNECT,
                headerComment = "Restricted rights.\n\nThis is a condensed test header."
            )
        ),
        Arguments.of(
            "/whois/whois.denic.de/free-de.txt",
            WhoisResult(
                changedAt = null,
                domain = "free.de",
                status = Status.FREE,
                headerComment = ""
            )
        ),
        Arguments.of(
            "/whois/whois.denic.de/xn--flge-1ra-de.txt",
            WhoisResult(
                changedAt = "2017-07-22T12:57:35+02:00",
                domain = "flüge.de",
                status = Status.CONNECT,
                headerComment = "Restricted rights.\n\nThis is a condensed test header."
            )
        ),
    )
}
