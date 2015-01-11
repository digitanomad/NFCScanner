package com.digitanomad.nfcscanner;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import android.nfc.NdefRecord;

import com.google.common.base.Preconditions;

public class TextRecord implements ParsedRecord {

	/** ISO/IANA language code */
	private final String mLanguageCode;
	
	private final String mText;
	
	private TextRecord(String languageCode, String text) {
		mLanguageCode = Preconditions.checkNotNull(languageCode);
		mText = Preconditions.checkNotNull(text);
	}
	
	@Override
	public int getType() {
		return ParsedRecord.TYPE_TEXT;
	}
	
	public String getText() {
		return mText;
	}
	
	/**
	 * 텍스트와 연관된 ISO/IANA language code를 반환
	 */
	public String getLanguageCode() {
		return mLanguageCode;
	}
	
	/**
	 * {@link android.nfc.NdefRecord}를 분석해서 언어코드와 텍스트가 담긴 {@link TextRecord}로 변환한다.
	 * @param record
	 * @return {@link TextRecord}
	 */
	public static TextRecord parse(NdefRecord record) {
		Preconditions.checkArgument(record.getTnf() == NdefRecord.TNF_WELL_KNOWN);
		Preconditions.checkArgument(Arrays.equals(record.getType(), NdefRecord.RTD_TEXT));
		try {
			byte[] payload = record.getPayload();
			/*
			 * NFC Forum "Text Record Type Definition" section 3.2.1.
			 * payload[0]에는 "Status Byte Encodings(인코딩 상태)" 필드가 들어있다.
			 * 
			 * bit7은 텍스트 인코딩 필드다.
			 *  if (Bit_7 == 0): UTF-8로 인코딩된 텍스트
			 *  if (Bit_7 == 1): UTF-16으로 인코딩된 텍스트
			 *  
			 *  Bit6은 미래에사용하기 위해 남겨든 비트이므로 0으로 지정.
			 *  Bit 0에서 5는 IANA language code의 길이
			 */
			String textEncoding = ((payload[0] & 0200) == 0) ? "UTF-8" : "UTF-16";
			int languageCodeLength = payload[0] & 0077;
			String languageCode = new String(payload, 1, languageCodeLength, "US-ASCII");
			String text = new String(payload, languageCodeLength + 1, 
					payload.length - languageCodeLength - 1, textEncoding);
			
			return new TextRecord(languageCode, text);
		} catch (UnsupportedEncodingException e) {
			throw new IllegalArgumentException(e);
		}
	}
	
	public static boolean isText(NdefRecord record) {
		try {
			parse(record);
			return true;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

}
