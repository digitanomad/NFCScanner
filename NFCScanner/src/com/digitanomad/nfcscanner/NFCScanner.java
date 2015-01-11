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
	 * NFC �±װ� �νĵǾ� ��ε�ĳ�������� ����Ʈ�� �޾��� �� ������ ��Ƽ��Ƽ�� �����ϴ� ����
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

		// NFCAdapter ��ü�� Ȱ��ȭ�Ǿ� ���� �ʴٸ�(null ����) NFC�� ����� �� ���� ���°� �ȴ�.
		mAdapter = NfcAdapter.getDefaultAdapter(this);

		mText = (TextView) findViewById(R.id.text);
		if (mAdapter == null) {
			mText.setText("����ϱ� ���� NFC�� Ȱ��ȭ�ϼ���.");
		} else {
			mText.setText("NFC �±׸� ��ĵ�ϼ���.");
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
		 * �� �޼ҵ忡�� ���Ǵ� PendingIntent ��ü�� NFC �±� �����Ͱ� ���Ե� ����Ʈ�� ���޹޾��� �� �����
		 * ����Ʈ�̹Ƿ� �±׸� ��ĵ���� �� �� ��Ƽ��Ƽ���� �����͸� �״�� ���޹ޱ� ���� �ڱ� �ڽ��� NFCScanner.class��
		 * ���� ���� ��Ƽ��Ƽ�� �� �ٽ� �޸� �� ��������� ���� �����ϱ� ���� ����Ʈ�� �÷��׸�
		 * FLAG_ACTIVITY_SINGLE_TOP���� ����
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

		// ���� �������� �ƴ� Ȩȭ�鿡�� �±װ� ��ĵ�� ���
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
			// ȭ���� ����� ���� �� ��ĵ�� �±� ������ ����Ʈ�� ���޹ޱ� ���� �ڵ�
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

		// NdefMessage �����ڴ� NdefRecord �迭 ��ü�� �Ķ���ͷ� ���޹޴´�.
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

		// NdefRecord�� �������� ù ��° �Ķ���ʹ� 3bit�� ǥ���Ǵ� TNF ��. �� �˷��� MIME Ÿ������ ����
		// �� ��° �Ķ���ʹ� ��ü���� ������ Ÿ������ NdefRecord.RTD_TEXT�� ��� MIME Ÿ�� �߿�
		// 'text/plain'�� ����.
		return new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT,
				new byte[0], data);
	}

	private NdefRecord createUriRecord(byte[] data) {
		return new NdefRecord(NdefRecord.TNF_ABSOLUTE_URI, NdefRecord.RTD_URI,
				new byte[0], data);
	}

	private void processTag(Intent passedIntent) {
		// ����Ʈ ��ü�ȿ� ����ִ� �±� �����͸� Ȯ��. ���ϵ� �迭 ��ü���� NdefMessage Ÿ���� ��ü���� ����ִ�.
		Parcelable[] rawMsgs = passedIntent
				.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
		if (rawMsgs == null) {
			return;
		}

		mText.setText(rawMsgs.length + "�� �±� ��ĵ��");

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
	 * �±� �޽��� ��ü�� �Ľ��ϴ� �ڵ�
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
