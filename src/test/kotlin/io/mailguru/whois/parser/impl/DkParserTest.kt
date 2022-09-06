package io.mailguru.whois.parser.impl

import io.mailguru.whois.enumeration.Status
import io.mailguru.whois.model.WhoisResult
import io.mailguru.whois.parser.Parser
import org.junit.jupiter.params.provider.Arguments
import java.util.stream.Stream

internal class DkParserTest : AbstractParserImplTest() {

    private val sut = DkParser()

    override fun getSut(): Parser = sut

    override fun provideData(): Stream<Arguments> = Stream.of(
        Arguments.of(
            "/whois/whois.dk-hostmaster.dk/connect-dk.txt",
            WhoisResult(
                changedAt = null, // data not provided in response
                domain = "connect.dk",
                status = Status.CONNECT,
                headerComment = "Copyright (c) 2002 - 2022 by DK Hostmaster A/S\n\nVersion: 5.0.2",
            )
        ),
        Arguments.of(
            "/whois/whois.dk-hostmaster.dk/free-dk.txt",
            WhoisResult(
                changedAt = null, // data not provided in response
                domain = "example.com", // data not provided in response
                status = Status.FREE,
                headerComment = "Copyright (c) 2002 - 2022 by DK Hostmaster A/S\n\nVersion: 5.0.2"
            )
        ),
    )
}
