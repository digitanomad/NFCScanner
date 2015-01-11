package com.digitanomad.nfcscanner;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Locale;

import com.google.common.base.Charsets;
import com.google.common.primitives.Bytes;

import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.tech.NfcF;
import android.os.Bundle;
import android.os.Parcelable;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class NFCScanner extends Activity {

	public static final int REQ_CODE_PUSH = 1001;

	private NfcAdapter mAdapter;
	/**
	 * NFC 태그가 인식되어 브로드캐스팅으로 인텐트를 받았을 때 실행할 액티비티를 지정하는 역할
	 */
	private PendingIntent mPendingIntent;
	private IntentFilter[] mFilters;
	private String[][] mTechLists;

	private TextView mText;
	private Button broadcastBtn;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_nfcscanner);

		// NFCAdapter 객체가 활성화되어 있지 않다면(null 리턴) NFC를 사용할 수 없는 상태가 된다.
		mAdapter = NfcAdapter.getDefaultAdapter(this);

		mText = (TextView) findViewById(R.id.text);
		if (mAdapter == null) {
			mText.setText("사용하기 전에 NFC를 활성화하세요.");
		} else {
			mText.setText("NFC 태그를 스캔하세요.");
		}

		broadcastBtn = (Button) findViewById(R.id.broadcastBtn);
		broadcastBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				int type = ParsedRecord.TYPE_TEXT;
				String msg = "Hello Android!";

				NdefMessage mMessage = createTagMessage(msg, type);
				NdefMessage[] msgs = new NdefMessage[1];
				msgs[0] = mMessage;

				Intent intent = new Intent(NfcAdapter.ACTION_NDEF_DISCOVERED);
				intent.putExtra(NfcAdapter.EXTRA_NDEF_MESSAGES, msgs);
				intent.setType("text/plain");
				startActivity(intent);
			}
		});

		/*
		 * 이 메소드에서 사용되는 PendingIntent 객체는 NFC 태그 데이터가 포함된 인텐트를 전달받았을 때 실행될
		 * 인텐트이므로 태그를 스캔했을 때 이 액티비티에서 데이터를 그대로 전달받기 위해 자기 자신인 NFCScanner.class를
		 * 지정 메인 액티비티가 또 다시 메모리 상에 만들어지는 것을 방지하기 위해 인텐트의 플래그를
		 * FLAG_ACTIVITY_SINGLE_TOP으로 설정
		 */
		Intent targetIntent = new Intent(this, NFCScanner.class);
		targetIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		mPendingIntent = PendingIntent.getActivity(this, 0, targetIntent, 0);

		IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
		try {
			ndef.addDataType("*/*");
		} catch (MalformedMimeTypeException e) {
			throw new RuntimeException("fail", e);
		}

		mFilters = new IntentFilter[] { ndef, };
		mTechLists = new String[][] { new String[] { NfcF.class.getName() } };

		// 앱이 실행중이 아닌 홈화면에서 태그가 스캔된 경우
		Intent passedIntent = getIntent();
		if (passedIntent != null) {
			String action = passedIntent.getAction();
			if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
				processTag(passedIntent);
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.nfcscanner, menu);
		return true;
	}

	@Override
	protected void onResume() {
		super.onResume();

		if (mAdapter != null) {
			// 화면이 띄워져 있을 때 스캔된 태그 정보를 인텐트로 전달받기 위한 코드
			mAdapter.enableForegroundDispatch(this, mPendingIntent, mFilters,
					mTechLists);
		}
	}

	@Override
	protected void onPause() {
		if (mAdapter != null) {
			mAdapter.disableForegroundDispatch(this);
		}

		super.onPause();
	}

	private NdefMessage createTagMessage(String msg, int type) {
		NdefRecord[] records = new NdefRecord[1];

		if (type == ParsedRecord.TYPE_TEXT) {
			records[0] = createTextRecord(msg, Locale.KOREAN, true);
		} else if (type == ParsedRecord.TYPE_URI) {
			records[0] = createUriRecord(msg.getBytes());
		}

		// NdefMessage 생성자는 NdefRecord 배열 객체를 파라미터로 전달받는다.
		NdefMessage mMessage = new NdefMessage(records);
		return mMessage;
	}

	private NdefRecord createTextRecord(String text, Locale locale,
			boolean encodeInUtf8) {
		final byte[] langBytes = locale.getLanguage().getBytes(
				Charsets.US_ASCII);
		final Charset utfEncoding = encodeInUtf8 ? Charsets.UTF_8 : Charset
				.forName("UTF-16");
		final byte[] textBytes = text.getBytes(utfEncoding);
		final int utfBit = encodeInUtf8 ? 0 : (1 << 7);
		final char status = (char) (utfBit + langBytes.length);
		final byte[] data = Bytes.concat(new byte[] { (byte) status },
				langBytes, textBytes);

		// NdefRecord의 생성자의 첫 번째 파라미터는 3bit로 표현되는 TNF 값. 잘 알려진 MIME 타입으로 지정
		// 두 번째 파라미터는 구체적인 데이터 타입으로 NdefRecord.RTD_TEXT의 경우 MIME 타입 중에
		// 'text/plain'과 같다.
		return new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT,
				new byte[0], data);
	}

	private NdefRecord createUriRecord(byte[] data) {
		return new NdefRecord(NdefRecord.TNF_ABSOLUTE_URI, NdefRecord.RTD_URI,
				new byte[0], data);
	}

	private void processTag(Intent passedIntent) {
		// 인텐트 객체안에 들어있는 태그 데이터를 확인. 리턴된 배열 객체에는 NdefMessage 타입의 객체들이 들어있다.
		Parcelable[] rawMsgs = passedIntent
				.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
		if (rawMsgs == null) {
			return;
		}

		mText.setText(rawMsgs.length + "개 태그 스캔됨");

		NdefMessage[] msgs;
		if (rawMsgs != null) {
			msgs = new NdefMessage[rawMsgs.length];
			for (int i = 0; i < rawMsgs.length; i++) {
				msgs[i] = (NdefMessage) rawMsgs[i];
				showTag(msgs[i]);
			}
		}
	}

	/**
	 * 태그 메시지 객체를 파싱하는 코드
	 * 
	 * @param mMessage
	 */
	private int showTag(NdefMessage mMessage) {
		List<ParsedRecord> records = NdefMessageParser.parse(mMessage);
		final int size = records.size();
		mText.append("\n");

		for (int i = 0; i < size; i++) {
			ParsedRecord record = records.get(i);

			int recordType = record.getType();
			String recordStr = "";
			if (recordType == ParsedRecord.TYPE_TEXT) {
				recordStr = "TEXT: " + ((TextRecord) record).getText() + "\n";
			} else if (recordType == ParsedRecord.TYPE_URI) {
				recordStr = "URI: " + ((UriRecord) record).getUri().toString()
						+ "\n";
			}

			mText.append(recordStr);
			mText.invalidate();
		}
		
		return size;
	}
	
	@Override
	protected void onNewIntent(Intent passedIntent) {
		super.onNewIntent(passedIntent);
		
		if (passedIntent != null) {
			processTag(passedIntent);
		}
	}
	
}
