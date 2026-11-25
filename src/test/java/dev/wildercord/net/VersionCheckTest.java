package dev.wildercord.net;

import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class VersionCheckTest {
	@Test void matchingVersionsProduceNoWarning() {
		assertNull(VersionCheck.mismatch("0.16.1-alpha+mc26.3", "0.16.1-alpha+mc26.3"));
	}

	@Test void mismatchedVersionsWarnAboutUiAndAttachments() {
		var warning = VersionCheck.mismatch("0.16.1-alpha+mc26.3", "0.15.0-alpha+mc26.3");
		assertNotNull(warning);
		String text = warning.getString();
		assertTrue(text.contains("0.16.1-alpha+mc26.3"));
		assertTrue(text.contains("0.15.0-alpha+mc26.3"));
		assertTrue(text.contains("attachments"));
	}

	@Test void olderClientWarningNamesServerVersion() {
		var warning = VersionCheck.olderClientWarning("0.16.1-alpha+mc26.3");
		String text = warning.getString();
		assertTrue(text.contains("0.16.1-alpha+mc26.3"));
		assertTrue(text.contains("older"));
	}

	@Test void clientAndServerPayloadsRoundTrip() {
		var sBuf = new RegistryFriendlyByteBuf(Unpooled.buffer(), null);
		var serverMsg = new VersionCheck.ServerVersion("0.16.1-alpha+mc26.3");
		VersionCheck.ServerVersion.CODEC.encode(sBuf, serverMsg);
		assertEquals(serverMsg, VersionCheck.ServerVersion.CODEC.decode(sBuf));
		assertEquals(0, sBuf.readableBytes());

		var cBuf = new RegistryFriendlyByteBuf(Unpooled.buffer(), null);
		var clientMsg = new VersionCheck.ClientVersion("0.15.9-alpha+mc26.3");
		VersionCheck.ClientVersion.CODEC.encode(cBuf, clientMsg);
		assertEquals(clientMsg, VersionCheck.ClientVersion.CODEC.decode(cBuf));
		assertEquals(0, cBuf.readableBytes());
	}
}
