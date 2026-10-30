package com.shinyhut.vernacular.client

import com.shinyhut.vernacular.protocol.messages.Encoder
import spock.lang.Specification

class ClientEventHandlerTest extends Specification {

    def output = new ByteArrayOutputStream()
    def session = new VncSession(new VernacularConfig(), new ByteArrayInputStream(new byte[0]), output)
    def handler = new ClientEventHandler(session, {})

    def setup() {
        session.messageEncoder = Encoder.raw(output)
    }

    def "should send caps and the text in the extended clipboard format if enabled"() {
        when:
        handler.copyText('test')

        then:
        def bytes = output.toByteArray()
        bytes[0..11] == [
                0x06, // message type
                0x00, 0x00, 0x00, // padding
                0x00, 0x00, 0x00, 0x04, // length = flags only, as no sizes are configured
                0x1f, 0x00, 0x00, 0x00 // flags = caps, request, peek, notify, provide
        ] as byte[]
        bytes[12] == 0x06
        bytes[16] < 0 // negative length indicates the extended format
        bytes[20..23] == [0x10, 0x00, 0x00, 0x01] as byte[] // flags = provide, text
    }

    def "should use the original clipboard format if the extended clipboard is disabled"() {
        given:
        session.config.enableExtendedClipboard = false

        when:
        handler.copyText('test')

        then:
        output.toByteArray() == [
                0x06, // message type
                0x00, 0x00, 0x00, // padding
                0x00, 0x00, 0x00, 0x04, // text length = 4
                0x74, 0x65, 0x73, 0x74  // text = 'test'
        ] as byte[]
    }
}
