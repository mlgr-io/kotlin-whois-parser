package io.mailguru.whois.parser.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mailguru.whois.callPrivateMethod
import io.mailguru.whois.enumeration.Status
import io.mailguru.whois.getPrivateProperty
import io.mailguru.whois.mockFieldByName
import io.mailguru.whois.mockPrivateFields
import io.mailguru.whois.model.WhoisResult
import io.mailguru.whois.model.exception.NotPermittedException
import io.mailguru.whois.parser.Parser
import io.mailguru.whois.service.WhoisService
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import org.apache.commons.net.whois.WhoisClient
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.fail
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import java.io.IOException
import java.net.SocketException
import java.net.UnknownHostException
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class WhoisServiceTest {

    private val sut = WhoisService.INSTANCE

    private val availableParsers: Map<String, Parser>? = sut.getPrivateProperty("availableParsers")

    @BeforeEach
    fun `clear the cache`() {
        sut.clearCache()
    }

    @ParameterizedTest
    @ValueSource(strings = ["whois.denic.de"])
    fun `the service should find all available parser implementations`(whoisServer: String) {
        availableParsers?.also { map ->
            withClue("Retrieve parser for $whoisServer:") {
                map[whoisServer] shouldNotBe null
            }
        } ?: fail("The map of available parsers should not be null.")
    }

    @ParameterizedTest
    @CsvSource(
        "ch,whois.nic.ch",
        "es,whois.nic.es",
        "ch,whois.nic.li",
    )
    fun `some whois servers does not output any information at all`(tld: String, whoisServer: String) {

        // Mocking stuff

        val whoisClientMock = mockk<WhoisClient>()
        every { whoisClientMock.connect(any<String>()) } just runs
        every { whoisClientMock.query("foobar.$tld") } returns "some data that doesn't matter"
        every { whoisClientMock.query(tld) } returns "whois:$whoisServer"
        every { whoisClientMock.disconnect() } just runs

        val sut = WhoisService.INSTANCE
        val preservedWhoisClient: WhoisClient = sut.getPrivateProperty("whois")
            ?: fail("There is no WhoisClient to mock.")
        sut.mockPrivateFields(whoisClientMock)

        // Do the tests
        shouldThrow<NotPermittedException> {
            sut.lookup("foobar.$tld")
        }

        // Unmocking

        sut.mockPrivateFields(preservedWhoisClient)
    }

    @ParameterizedTest
    @CsvSource(
        "google.co.uk,uk",
        "google.com,com",
        "google.de,de",
    )
    fun `retrieval of the appropriate responsible whois server`(input: String, expected: String) {

        // Mocking stuff

        val whoisClientMock = mockk<WhoisClient>()
        every { whoisClientMock.connect("whois.iana.org") } just runs
        slot<String>().also { request ->
            every { whoisClientMock.query(capture(request)) } answers {
                request.captured.let { capturedValue ->
                    capturedValue shouldBe expected
                    "whois:nic.$capturedValue"
                }
            }
        }
        every { whoisClientMock.disconnect() } just runs

        val sut = WhoisService.INSTANCE
        val preservedWhoisClient: WhoisClient = sut.getPrivateProperty("whois")
            ?: fail("There is no WhoisClient to mock.")
        sut.mockPrivateFields(whoisClientMock)

        // Do the tests

        val result: String? = sut.callPrivateMethod("getWhoisServer", input)
        result shouldBe "nic.$expected"

        // Unmocking

        sut.mockPrivateFields(preservedWhoisClient)
    }

    @Test
    fun `lookup() should lookup the whois server, request it and then call the parser`() {

        // Mocking stuff

        val whoisClientMock = mockk<WhoisClient>()
        every { whoisClientMock.connect(any<String>()) } just runs
        every { whoisClientMock.query("de") } returns "whois:whois.denic.de"
        every { whoisClientMock.query("google.de") } returns """
            Domain: google.de
            Nserver: ns1.google.com
            Nserver: ns2.google.com
            Nserver: ns3.google.com
            Nserver: ns4.google.com
            Status: connect
            Changed: 2018-03-12T21:44:25+01:00
        """.trimIndent()
        every { whoisClientMock.disconnect() } just runs

        val sut = WhoisService.INSTANCE
        val preservedWhoisClient: WhoisClient = sut.getPrivateProperty("whois")
            ?: fail("There is no WhoisClient to mock.")
        sut.mockPrivateFields(whoisClientMock)

        sut.mockFieldByName(
            "availableParsers\$delegate", // sic!
            object : Lazy<Map<String, Parser>> {
                override fun isInitialized(): Boolean = true
                override val value: Map<String, Parser>
                    get() = mapOf(
                        "whois.denic.de" to mockk<Parser>().also { parserMock ->
                            every { parserMock.parse(any(), any()) } returns WhoisResult(
                                domain = "google.de",
                                status = Status.CONNECT
                            )
                        }
                    )
            }
        )

        // Do the tests

        sut.lookup("google.de")?.let { whoisResult ->
            whoisResult.domain shouldBe "google.de"
            whoisResult.status shouldBe Status.CONNECT
        } ?: fail("Calling lookup() with valid input should not return null.")

        verify(exactly = 2) {
            whoisClientMock.connect(any<String>())
            whoisClientMock.query(any<String>())
            whoisClientMock.disconnect()
        }

        verify(exactly = 1) {
            // connect() should have been called exactly once with "whois.iana.org" as argument
            whoisClientMock.connect("whois.iana.org")
            whoisClientMock.connect("whois.denic.de")

            // query() should have been called once with input as argument
            whoisClientMock.query("de")
            whoisClientMock.query("google.de")
        }

        verifyOrder {
            whoisClientMock.connect("whois.iana.org")
            whoisClientMock.query("de")
            whoisClientMock.disconnect()

            whoisClientMock.connect("whois.denic.de")
            whoisClientMock.query("google.de")
            whoisClientMock.disconnect()
        }

        // Unmocking

        sut.mockPrivateFields(preservedWhoisClient)
    }

    @ParameterizedTest
    @ValueSource(strings = ["example", "test"])
    fun `unreasonable hostnames should lead to null result`(input: String) {

        // Mocking stuff

        val whoisClientMock = mockk<WhoisClient>()
        every { whoisClientMock.connect("whois.iana.org") } just runs
        every { whoisClientMock.query(any<String>()) } returns ""
        every { whoisClientMock.disconnect() } just runs

        val sut = WhoisService.INSTANCE
        val preservedWhoisClient: WhoisClient = sut.getPrivateProperty("whois")
            ?: fail("There is no WhoisClient to mock.")
        sut.mockPrivateFields(whoisClientMock)

        // Do the tests

        val result: String? = sut.callPrivateMethod("getWhoisServer", input)
        result shouldBe null

        verify(exactly = 1) {
            // connect() should have been called exactly once with "whois.iana.org" as argument
            whoisClientMock.connect("whois.iana.org")
            whoisClientMock.connect(any<String>())

            // query() should have been called once with input as argument
            whoisClientMock.query(input)
            whoisClientMock.query(any<String>())

            whoisClientMock.disconnect()
        }

        verifyOrder {
            whoisClientMock.connect("whois.iana.org")
            whoisClientMock.query(input)
            whoisClientMock.disconnect()
        }

        // Unmocking

        sut.mockPrivateFields(preservedWhoisClient)
    }

    @Test
    fun `consecutive calls to getWhoisServer() with related args should result in a single whois request`() {

        // Mocking stuff

        val whoisClientMock = mockk<WhoisClient>()
        every { whoisClientMock.connect("whois.iana.org") } just runs
        every { whoisClientMock.query(any<String>()) } returns "whois:nic.test"
        every { whoisClientMock.disconnect() } just runs

        val sut = WhoisService.INSTANCE
        val preservedWhoisClient: WhoisClient = sut.getPrivateProperty("whois")
            ?: fail("There is no WhoisClient to mock.")
        sut.mockPrivateFields(whoisClientMock)

        // Doing the first row of requests with all the same TLD

        setOf("example.com", "foo.com", "google.com").forEach { hostname ->
            val result: String? = sut.callPrivateMethod("getWhoisServer", hostname)
            result shouldBe "nic.test"
        }

        verify(exactly = 1) {
            // connect() should have been called exactly once with "whois.iana.org" as argument
            whoisClientMock.connect("whois.iana.org")
            whoisClientMock.connect(any<String>())

            // query() should have been called exactly once with input as argument
            whoisClientMock.query("com")
            whoisClientMock.query(any<String>())

            whoisClientMock.disconnect()
        }

        // Request another TLD should result in a 2nd call to whois.iana.org

        val result: String? = sut.callPrivateMethod("getWhoisServer", "anotherTLD.test")
        result shouldBe "nic.test"

        verify(exactly = 2) {
            // connect() should have been called with "whois.iana.org" as the only argument
            whoisClientMock.connect("whois.iana.org")
            whoisClientMock.connect(any<String>())

            whoisClientMock.query(any<String>())

            whoisClientMock.disconnect()
        }

        verify(exactly = 1) {
            // query() should have been called once with each TLD as argument
            whoisClientMock.query("com")
            whoisClientMock.query("test")
        }

        verifyOrder {
            whoisClientMock.connect("whois.iana.org")
            whoisClientMock.query("com")
            whoisClientMock.disconnect()

            whoisClientMock.connect("whois.iana.org")
            whoisClientMock.query("test")
            whoisClientMock.disconnect()
        }

        // Unmocking

        sut.mockPrivateFields(preservedWhoisClient)
    }

    @Suppress("UnusedPrivateMember")
    private fun provideWhoisClientExceptions() = Stream.of(
        Arguments.of(IOException("")),
        Arguments.of(SocketException("")),
        Arguments.of(UnknownHostException("")),
    )

    @ParameterizedTest
    @MethodSource("provideWhoisClientExceptions")
    fun `exceptions should be handled properly`(exception: Exception) {

        // Mocking stuff

        val whoisClientMock = mockk<WhoisClient>()
        every { whoisClientMock.connect("whois.iana.org") } throws exception

        val sut = WhoisService.INSTANCE
        val preservedWhoisClient: WhoisClient = sut.getPrivateProperty("whois")
            ?: fail("There is no WhoisClient to mock.")
        sut.mockPrivateFields(whoisClientMock)

        // Doing the first row of requests with all the same TLD

        val result: String? = sut.callPrivateMethod("getWhoisServer", "arbitrary.host.name")
        result shouldBe null

        verify(exactly = 1) {
            // connect() should have been called exactly once with "whois.iana.org" as argument
            whoisClientMock.connect("whois.iana.org")
            whoisClientMock.connect(any<String>())
        }

        verify(inverse = true) { whoisClientMock.query(any<String>()) }
        verify(inverse = true) { whoisClientMock.disconnect() }

        // Unmocking

        sut.mockPrivateFields(preservedWhoisClient)
    }
}
