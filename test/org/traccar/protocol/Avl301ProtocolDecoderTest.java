package org.traccar.protocol;

import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.Test;
import org.traccar.ProtocolDecoderTest;
import org.traccar.helper.ChannelBufferTools;


public class Avl301ProtocolDecoderTest extends ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        Avl301ProtocolDecoder decoder = new Avl301ProtocolDecoder(new Avl301Protocol());

        verifyNothing(decoder, binary(
                "244c0f086058500087335500010d0a"));

        verifyNothing(decoder, binary(
                "24480d1001c3065c0d00010d0a"));

        verifyPosition(decoder, binary(
                "24242c0f041710001d0e060146944904ff4ac40000148f0651044b001a081001be06590daa00000108a30d0a"));

    }

}
