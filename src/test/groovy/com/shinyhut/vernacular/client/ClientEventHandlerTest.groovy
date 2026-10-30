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

    def "should use the original clipboard format if the server did not indicate extended clipboard support"() {
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

    def "should use the extended clipboard format once the server indicated support"() {
        when:
        handler.handleExtendedClipboardCaps()
        handler.copyText('test')

        then: "no caps are sent back by default, only the extended format text"
        def bytes = output.toByteArray()
        bytes[0] == 0x06
        bytes[4] < 0 // negative length indicates the extended format
        bytes[8..11] == [0x10, 0x00, 0x00, 0x01] as byte[] // flags = provide, text
    }
}
