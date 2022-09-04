package io.mailguru.whois.parser.impl

import io.mailguru.whois.TestUtil
import io.mailguru.whois.model.WhoisResult
import io.mailguru.whois.parser.Parser
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.fail

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal abstract class AbstractParserImplTest {

    protected abstract fun getSut(): Parser

    protected abstract fun provideData(): Stream<Arguments>

    @ParameterizedTest
    @MethodSource("provideData")
    fun `parse result of connected domains`(
        inputFilename: String,
        expectation: WhoisResult?
    ) {
        TestUtil.getResourceAsText(inputFilename)?.let { input ->
            getSut().parse(input).also { result ->
                expectation?.apply {
                    assertEquals(changedAt, result.changedAt)
                    assertEquals(domain, result.domain)
                    assertEquals(status, result.status)
                    assertEquals(headerComment, result.headerComment)
                } ?: assertNull(result)
            }
        } ?: fail("Input may not be null.")
    }
}
