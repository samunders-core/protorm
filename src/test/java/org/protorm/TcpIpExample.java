package org.protorm;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class TcpIpExample {
	public static void main(String[] args) throws IOException {
		InputStream is = null;
		DataOutputStream os = null;
		Ethernet.FrameEncoder e = Ethernet.CODEC.newEncoder();
		for (Ethernet frame : Ethernet.CODEC.decode(is)) {
			if (frame.type() == 0x0800) {
//				IP packet = IP.CODEC.payloadOf(frame);
			}
			System.out.println(frame);
			e.initFrom(frame).set(e.type(), 0x0800).writeTo(os);
		}
	}
}
