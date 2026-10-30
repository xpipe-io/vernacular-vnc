package com.shinyhut.vernacular.client

import com.shinyhut.vernacular.protocol.messages.Decoder
import spock.lang.Specification

class ServerEventHandlerTest extends Specification {

    def "should stop immediately without reporting an error while waiting for server messages"() {
        given: "a connection on which the server never sends anything"
        def serverSocket = new ServerSocket(0, 1, InetAddress.loopbackAddress)
        def clientSocket = new Socket(InetAddress.loopbackAddress, serverSocket.localPort)
        def acceptedSocket = serverSocket.accept()

        def session = new VncSession(new VernacularConfig(), clientSocket.inputStream, clientSocket.outputStream)
        session.messageDecoder = Decoder.raw(session.inputStream)
        def errors = []
        def handler = new ServerEventHandler(session, { errors << it })
        handler.start()
        Thread.sleep(100)

        when:
        def start = System.nanoTime()
        handler.stop()
        def elapsedMillis = (System.nanoTime() - start) / 1_000_000

        then:
        elapsedMillis < 500
        errors.isEmpty()

        cleanup:
        acceptedSocket?.close()
        clientSocket?.close()
        serverSocket?.close()
    }
}
