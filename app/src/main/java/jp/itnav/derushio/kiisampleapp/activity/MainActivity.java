package jp.itnav.derushio.kiisampleapp.activity;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.kii.cloud.storage.Kii;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;

import jp.itnav.derushio.kiimanager.KiiManager;
import jp.itnav.derushio.kiisampleapp.R;

public class MainActivity extends AppCompatActivity {

	public static final int REQUEST_CODE_LOGIN = 0;

	public static final String bucketName = "bucket";

	private KiiManager kiiManager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Android 6.0+はパーミッションの要求が必要
		boolean execReqPermissions = requestAppPermissions();
		if (!execReqPermissions) {
			// パーミッションが認証済みなのでログインを開始
			loginWithStoredCredentials();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		switch (requestCode) {
			case (REQUEST_CODE_LOGIN): {
				if (resultCode != Activity.RESULT_OK) {
					return;
				}

				getBucketData();
				break;
			}
			default: {
				break;
			}
		}
	}

	// 戻り値:リクエストを実行したか
	private boolean requestAppPermissions() {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
			return false;
		}

		ArrayList<String> reqList = new ArrayList<>();

		PackageManager pm = getPackageManager();
		int checkPermission = pm.checkPermission(Manifest.permission.INTERNET, getPackageName());
		if (checkPermission == PackageManager.PERMISSION_DENIED) {
			reqList.add(Manifest.permission.INTERNET);
		}

		checkPermission = pm.checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, getPackageName());
		if (checkPermission == PackageManager.PERMISSION_DENIED) {
			reqList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
		}

		if (reqList.size() <= 0) {
			return false;
		}

		requestPermissions(reqList.toArray(new String[reqList.size()]), 0);
		return true;
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		for (int grantResult : grantResults) {
			if (grantResult == PackageManager.PERMISSION_DENIED) {
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle("エラー");
				builder.setMessage("許可しなければアプリは動きません");
				builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						requestAppPermissions();
					}
				});
				builder.setCancelable(false);
				builder.show();
				return;
			}
		}

		// 承認が得られたらログインを開始
		loginWithStoredCredentials();
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
	}

	private void loginWithStoredCredentials() {
		// Kii初期化
		KiiManager.kiiInit(this, getString(R.string.kii_app_id), getString(R.string.kii_app_key), Kii.Site.JP);

		kiiManager = KiiManager.getInstance();
		kiiManager.loginWithStoredCredentials(new KiiManager.OnFinishActionListener() {
			@Override
			public void onSuccess(JSONObject data) {
				Log.d("loginWithSC", "success");
				// TODO データのやりとりのサンプル
				getBucketData();
			}

			@Override
			public void onFail(Exception e) {
				Intent intent = new Intent(MainActivity.this, LoginActivity.class);
				startActivityForResult(intent, REQUEST_CODE_LOGIN);
			}
		});
	}

	// Kiiからバケットデータを取ってくる
	public void getBucketData() {
		kiiManager.allQueryObjectData(bucketName, new KiiManager.OnFinishActionListener() {
					@Override
					public void onSuccess(JSONObject data) {
						Log.d("allQuery", "success -> " + data.toString());
						Iterator<String> keys = data.keys();
						while (keys.hasNext()) {
							String key = keys.next();
							try {
								Log.d("object", key + ": " + data.getJSONObject(key).toString());
							} catch (JSONException e) {
								continue;
							}
						}
					}

					@Override
					public void onFail(Exception e) {
						e.printStackTrace();
					}
				}
		);
	}

	public void onAddFABClick(View v) {
		try {
			kiiManager.putObjectData(bucketName, new JSONObject("{\"test\":\"narumi\"}"), new KiiManager.OnFinishActionListener() {
				@Override
				public void onSuccess(JSONObject data) {
					Log.d("add object", "put success");
				}

				@Override
				public void onFail(Exception e) {
					Log.d("add object", "put fail -> " + e.getMessage());
				}
			});
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

}
