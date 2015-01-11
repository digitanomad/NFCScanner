package com.digitanomad.nfcscanner;

import java.nio.charset.Charset;
import java.util.Arrays;

import android.net.Uri;
import android.nfc.NdefRecord;

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.primitives.Bytes;

public class UriRecord implements ParsedRecord {

	public static final String RECORD_TYPE = "UriRecord";

	/**
	 * NFC Forum "URI Record Type Definition"
	 * section 3.2.2 of the NFC Forum URI Record Type Definition document.
	 * URI Identifier Codes(URI 식별자 코드)를 URI String 접두사로 맵핑시켜준다.
	 */
	private static final BiMap<Byte, String> URI_PREFIX_MAP = ImmutableBiMap
			.<Byte, String> builder().put((byte) 0x00, "")
			.put((byte) 0x01, "http://www.").put((byte) 0x02, "https://www.")
			.put((byte) 0x03, "http://").put((byte) 0x04, "https://")
			.put((byte) 0x05, "tel:").put((byte) 0x06, "mailto:")
			.put((byte) 0x07, "ftp://anonymous:anonymous@")
			.put((byte) 0x08, "ftp://ftp.").put((byte) 0x09, "ftps://")
			.put((byte) 0x0A, "sftp://").put((byte) 0x0B, "smb://")
			.put((byte) 0x0C, "nfs://").put((byte) 0x0D, "ftp://")
			.put((byte) 0x0E, "dav://").put((byte) 0x0F, "news:")
			.put((byte) 0x10, "telnet://").put((byte) 0x11, "imap:")
			.put((byte) 0x12, "rtsp://").put((byte) 0x13, "urn:")
			.put((byte) 0x14, "pop:").put((byte) 0x15, "sip:")
			.put((byte) 0x16, "sips:").put((byte) 0x17, "tftp:")
			.put((byte) 0x18, "btspp://").put((byte) 0x19, "btl2cap://")
			.put((byte) 0x1A, "btgoep://").put((byte) 0x1B, "tcpobex://")
			.put((byte) 0x1C, "irdaobex://").put((byte) 0x1D, "file://")
			.put((byte) 0x1E, "urn:epc:id:").put((byte) 0x1F, "urn:epc:tag:")
			.put((byte) 0x20, "urn:epc:pat:").put((byte) 0x21, "urn:epc:raw:")
			.put((byte) 0x22, "urn:epc:").put((byte) 0x23, "urn:nfc:").build();

	private final Uri mUri;

	private UriRecord(Uri uri) {
		this.mUri = Preconditions.checkNotNull(uri);
	}

	@Override
	public int getType() {
		return ParsedRecord.TYPE_URI;
	}

	public Uri getUri() {
		return mUri;
	}

	/**
	 * {@link android.nfc.NdefRecord}를 {@link android.net.Uri}로 변환해준다.
	 * TNF_WELL_KNOW / RTD_URI와 TNF_ABSOLUTE_URI 타입을 둘다 처리한다.
	 * 
	 * @throws IllegalArgumentException NdefRecord가 URI를 담고 있지 않을 때 발생
	 * @param record
	 * @return UriRecord
	 */
	public static UriRecord parse(NdefRecord record) {
		short tnf = record.getTnf();
		if (tnf == NdefRecord.TNF_WELL_KNOWN) {
			return parseWellKnown(record);
		} else if (tnf == NdefRecord.TNF_ABSOLUTE_URI) {
			return parseAbsolute(record);
		}
		throw new IllegalArgumentException("Unknown TNF " + tnf);
	}
	
	private static UriRecord parseAbsolute(NdefRecord record) {
		byte[] payload = record.getPayload();
		Uri uri = Uri.parse(new String(payload, Charset.forName("UTF-8")));
		return new UriRecord(uri);
	}
	
	private static UriRecord parseWellKnown(NdefRecord record) {
		Preconditions.checkArgument(Arrays.equals(record.getType(), NdefRecord.RTD_URI));
		byte[] payload = record.getPayload();
		/*
		 * NFC Forum "URI Record Type Definition" section 3.2.2.
		 * payload[0]에는 URI 식별자를 담고 있다.
         * payload[1]부터 payload[payload.length - 1] 까지는 나머지 URI를 담고 있다.
		 */
		String prefix = URI_PREFIX_MAP.get(payload[0]);
		byte[] fullUri = Bytes.concat(prefix.getBytes(Charset.forName("UTF-8")), 
				Arrays.copyOfRange(payload, 1, payload.length));
		Uri uri = Uri.parse(new String(fullUri, Charset.forName("UTF-8")));
		return new UriRecord(uri);
	}
	
	public static boolean isUri(NdefRecord record) {
		try {
			parse(record);
			return true;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

}
