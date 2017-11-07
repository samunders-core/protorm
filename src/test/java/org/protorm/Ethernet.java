package org.protorm;

import org.protorm.codecs.NetworkOrder;

public @interface Ethernet {
	static interface FrameEncoder extends Ethernet, Encoder<Ethernet, FrameEncoder> {}
	static final Codec<Ethernet, FrameEncoder> CODEC = new Codec<Ethernet, FrameEncoder>() {{
		rawBytes(6).dstMac();
		rawBytes(6).srcMac();
		withCodec(NetworkOrder.TWO_BYTES).type();
		rawBytes(Integer.MIN_VALUE).payload();
	}};
	byte[] dstMac();
	byte[] srcMac();
	int type();
	// just an example, correct usage is: if (frame.type() == 0x0800) { IPv4 packet = IPv4.CODEC.payloadOf(ethernetFrame); } 
	byte[] payload();
}
