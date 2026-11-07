package com.shinyhut.vernacular.client.rendering

import com.shinyhut.vernacular.client.VernacularConfig
import com.shinyhut.vernacular.client.VncSession
import com.shinyhut.vernacular.protocol.messages.Decoder
import com.shinyhut.vernacular.protocol.messages.FramebufferUpdate
import com.shinyhut.vernacular.protocol.messages.PixelFormat
import spock.lang.Specification

class FramebufferTest extends Specification {

    private static byte[] extendedDesktopSizeRect(int initiator, int status, int width, int height) {
        def bytes = new ByteArrayOutputStream()
        def out = new DataOutputStream(bytes)
        out.writeShort(initiator)
        out.writeShort(status)
        out.writeShort(width)
        out.writeShort(height)
        out.writeInt(-308)
        // One screen
        out.writeByte(1)
        out.write(new byte[3])
        out.writeInt(0)
        out.writeShort(0)
        out.writeShort(0)
        out.writeShort(width)
        out.writeShort(height)
        out.writeInt(0)
        // Marker after the update, to check that the update was read completely
        out.writeByte(0x42)
        bytes.toByteArray()
    }

    private static VncSession session(byte[] data) {
        def session = new VncSession(new VernacularConfig(), new ByteArrayInputStream(data), new ByteArrayOutputStream())
        session.messageDecoder = Decoder.raw(session.inputStream)
        session.pixelFormat = new PixelFormat(32, 24, true, true, 255, 255, 255, 8, 16, 24)
        session.framebufferWidth = 800
        session.framebufferHeight = 600
        session
    }

    def "should resize the framebuffer on a successful extended desktop size update"() {
        given:
        def session = session(extendedDesktopSizeRect(1, 0, 1024, 768))
        def framebuffer = new Framebuffer(session)

        when:
        framebuffer.processUpdate(new FramebufferUpdate(1))

        then:
        session.framebufferWidth == 1024
        session.framebufferHeight == 768
        session.inputStream.read() == 0x42
    }

    def "should not resize the framebuffer if the resize request was rejected"() {
        given: "a reply to a client resize request with status 1 (prohibited), which contains the requested size"
        def session = session(extendedDesktopSizeRect(1, 1, 1024, 768))
        def framebuffer = new Framebuffer(session)

        when:
        framebuffer.processUpdate(new FramebufferUpdate(1))

        then:
        session.framebufferWidth == 800
        session.framebufferHeight == 600
        session.inputStream.read() == 0x42
    }
}
