package org.protorm;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Assert;
import org.junit.Test;
import org.protorm.codecs.Text;

public class TextTest {
	@Test
	public void readsLines() throws IOException {
		boolean first = true;
		try (InputStream is = new FileInputStream("/etc/passwd")) {
			for (Passwd line : Passwd.CODEC.decode(is)) {
				if (first) {
					first = false;
					Assert.assertEquals("root", line.login());
					Assert.assertEquals(0, line.uid());
					Assert.assertEquals(0, line.gid());
					Assert.assertEquals("root", line.description());
					Assert.assertEquals("/root", line.homeDir());
					Assert.assertTrue(line.shell().startsWith("/bin/"));
					Assert.assertTrue(line.shell().endsWith("sh"));
				}
				System.out.println(line);
			}
		}
	}
	
	// root:x:0:0:root:/root:/bin/bash
	@Text(terminatedBy=':')
	public static @interface Passwd {
		static interface PasswdEncoder extends Passwd, Encoder<Passwd, PasswdEncoder> {}
		static final Codec<Passwd, PasswdEncoder> CODEC = new Codec<Passwd, PasswdEncoder>() {{
			withCodec(Text.Line.INSTANCE).login();
			skip(Validate.YES, "x:".getBytes());
			withCodec(Text.Line.INSTANCE).uid();
			withCodec(Text.Line.INSTANCE).gid();
			withCodec(Text.Line.INSTANCE).description();
			withCodec(Text.Line.INSTANCE).homeDir();
			withCodec(Text.Line.INSTANCE).shell();
		}};
		
		String login();
//		String password();
		int uid();
		int gid();
		String description();
		String homeDir();
		@Text(terminatedBy='\n')
		String shell();
	}
}
