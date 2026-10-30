package com.shinyhut.vernacular.protocol.messages

import spock.lang.Specification

import static com.shinyhut.vernacular.protocol.messages.MessageHeaderFlags.*

class ClientCutTextCapsTest extends Specification {

    def "should encode a single format other than text"() {
        given:
        def output = new ByteArrayOutputStream()

        when:
        new ClientCutTextCaps([(HTML): 1024]).encode(output)

        then:
        output.toByteArray() == [
                0x06, // message type
                0x00, 0x00, 0x00, // padding
                0x00, 0x00, 0x00, 0x08, // length = flags + 1 size
                0x11, 0x00, 0x00, 0x04, // flags = caps, provide, html
                0x00, 0x00, 0x04, 0x00 // html size = 1024
        ] as byte[]
    }

    def "should order sizes by format bit and ignore action flags"() {
        given:
        def output = new ByteArrayOutputStream()
        def sizes = new LinkedHashMap<MessageHeaderFlags, Integer>()
        sizes[HTML] = 2
        sizes[CAPS] = 99
        sizes[TEXT] = 1

        when:
        new ClientCutTextCaps(sizes).encode(output)

        then:
        output.toByteArray() == [
                0x06, // message type
                0x00, 0x00, 0x00, // padding
                0x00, 0x00, 0x00, 0x0c, // length = flags + 2 sizes
                0x11, 0x00, 0x00, 0x05, // flags = caps, provide, html, text
                0x00, 0x00, 0x00, 0x01, // text size
                0x00, 0x00, 0x00, 0x02 // html size
        ] as byte[]
    }
}
