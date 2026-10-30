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

    def "should always use the original clipboard format to copy text"() {
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
