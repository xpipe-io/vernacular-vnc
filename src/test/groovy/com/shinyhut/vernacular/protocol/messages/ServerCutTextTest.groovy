package com.shinyhut.vernacular.protocol.messages

import com.shinyhut.vernacular.client.exceptions.InvalidMessageException
import spock.lang.Specification

class ServerCutTextTest extends Specification {

    def "should decode a valid ServerCutText message"() {
        given:
        def input = new ByteArrayInputStream([
                0x03, // message type
                0x00, 0x00, 0x00, // padding
                0x00, 0x00, 0x00, 0x04, // text length = 4
                0x74, 0x65, 0x73, 0x74  // text = 'test'

        ] as byte[])

        when:
        def result = ServerCutText.decode(input)

        then:
        result.text == 'test'
    }

    def "should decode a valid ServerCutText extended format message"() {
        given:
        def input = new ByteArrayInputStream([
                0x03, // message type
                0x00, 0x00, 0x00, // padding
                -0x01, -0x01, -0x01, -0x15, // data length = -21
                0x10, 0x00, 0x00, 0x01,  // flags
                0x78, -0x64, 0x63, 0x60, 0x60, 0x60, 0x2d, 0x49, 0x2d, 0x2e, 0x61, 0x00, 0x00, 0x06, 0x40, 0x01, -0x3a // compressed text

        ] as byte[])

        when:
        def result = ServerCutText.decode(input)

        then:
        result.text == 'test'
    }

    def "should decode an extended format caps message"() {
        given:
        def input = new ByteArrayInputStream([
                0x03, // message type
                0x00, 0x00, 0x00, // padding
                -0x01, -0x01, -0x01, -0x08, // data length = -8
                0x01, 0x00, 0x00, 0x01,  // flags = caps, text
                0x01, 0x40, 0x00, 0x00 // text size = 20 MiB
        ] as byte[])

        when:
        def result = ServerCutText.decode(input)

        then:
        result.caps
        result.text == ''
    }

    def "should reject an extended format message too short to contain flags"() {
        given:
        def input = new ByteArrayInputStream([
                0x03, // message type
                0x00, 0x00, 0x00, // padding
                -0x01, -0x01, -0x01, -0x02, // data length = -2
                0x00, 0x00
        ] as byte[])

        when:
        ServerCutText.decode(input)

        then:
        thrown(InvalidMessageException)
    }

    def "should ignore an extended format message which does not provide text"() {
        given:
        def input = new ByteArrayInputStream([
                0x03, // message type
                0x00, 0x00, 0x00, // padding
                -0x01, -0x01, -0x01, -0x05, // data length = -5
                0x10, 0x00, 0x00, 0x04,  // flags = provide html
                0x00
        ] as byte[])

        when:
        def result = ServerCutText.decode(input)

        then:
        result.text == ''
    }

    def "should decode large extended format text encoded by the client"() {
        given:
        def text = 'äöü test ' * 500_000
        def output = new ByteArrayOutputStream()
        new ClientCutTextExtendedClipboard(text).encode(output)

        when:
        def result = ServerCutText.decode(new ByteArrayInputStream(output.toByteArray()))

        then:
        result.text == text
    }
}
