package jp.itnav.derushio.kiisampleapp.activity;

import android.Manifest;
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

import java.util.ArrayList;
import java.util.HashMap;

import jp.itnav.derushio.kiimanager.KiiManager;
import jp.itnav.derushio.kiisampleapp.R;

public class MainActivity extends AppCompatActivity {

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

		kiiManager.getObjectData(bucketName, "test", new KiiManager.OnFinishActionListener() {
					@Override
					public void onSuccess(HashMap<String, String> data) {
						Log.d("test data", data.get("value"));
					}

					@Override
					public void onFail(Exception e) {

					}
				}

		);
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
			public void onSuccess(HashMap<String, String> data) {
				Log.d("loginWithSC", "success");
				// TODO データのやりとりのサンプル
			}

			@Override
			public void onFail(Exception e) {
				Intent intent = new Intent(MainActivity.this, LoginActivity.class);
				startActivity(intent);
			}
		});
	}

	public void onAddFABClick(View v) {
		kiiManager.putObjectData(bucketName, "test", "test", new KiiManager.OnFinishActionListener() {
			@Override
			public void onSuccess(HashMap<String, String> data) {
				Log.d("add object", "put success");
			}

			@Override
			public void onFail(Exception e) {
				Log.d("add object", "put fail");
			}
		});
	}

}
