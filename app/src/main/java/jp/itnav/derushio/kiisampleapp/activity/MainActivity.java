package jp.itnav.derushio.kiisampleapp.activity;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.kii.cloud.storage.Kii;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import jp.itnav.derushio.kiimanager.KiiManager;
import jp.itnav.derushio.kiisampleapp.R;
import jp.itnav.derushio.kiisampleapp.adapter.MemoRecyclerAdapter;

public class MainActivity extends AppCompatActivity {

	// ActivityForResult REQUEST_CODE
	public static final int REQUEST_CODE_LOGIN = 0;

	// アクセス先バケット名
	public static final String BUCKET_NAME = "bucket";

	// オブジェクトのキー
	public static final String OBJECT_KEY_MEMO = "objectKeyMemo";

	private KiiManager kiiManager;

	private RecyclerView memoRecycler;
	private MemoRecyclerAdapter memoRecyclerAdapter;
	private ArrayList<String> memoDataSet;

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

		setupViews();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		switch (requestCode) {
			case (REQUEST_CODE_LOGIN): {
				if (resultCode != RESULT_OK) {
					return;
				}

				getBucketData();
				break;
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case (R.id.loginMenu): {
				if (kiiManager.isLoggedIn()) {
					AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
					builder.setCancelable(false);
					builder.setMessage("既にログインしています");
					builder.setPositiveButton("OK", null);
					builder.show();
					break;
				}
				Intent intent = new Intent(this, LoginActivity.class);
				startActivityForResult(intent, REQUEST_CODE_LOGIN);
				break;
			}
			case (R.id.logoutMenu): {
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle("確認");
				builder.setCancelable(false);
				builder.setMessage("ログアウトしますか？");
				builder.setPositiveButton("はい", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						kiiManager.logout(new KiiManager.OnFinishActionListener() {
							@Override
							public void onSuccess(JSONObject data) {
								AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
								builder.setCancelable(false);
								builder.setMessage("ログアウトしました");
								builder.setPositiveButton("OK", null);
								builder.show();
							}

							@Override
							public void onFail(Exception e) {

							}
						});
					}
				});
				builder.setNegativeButton("いいえ", null);
				builder.show();
				break;
			}
			case (R.id.allDeleteMenu): {
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle("確認");
				builder.setCancelable(false);
				builder.setMessage("全件削除しますか？");
				builder.setPositiveButton("はい", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						allObjectDelete();
					}
				});
				builder.setNegativeButton("いいえ", null);
				builder.show();
				break;
			}
		}

		return super.

				onOptionsItemSelected(item);

	}

	public void setupViews() {
		getSupportActionBar().setTitle("メモ一覧");

		memoRecycler = (RecyclerView) findViewById(R.id.memoRecycler);

		LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
		linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);

		memoRecycler.setLayoutManager(linearLayoutManager);
		memoDataSet = new ArrayList<>();
		memoRecyclerAdapter = new MemoRecyclerAdapter(memoDataSet);
		memoRecycler.setAdapter(memoRecyclerAdapter);
	}

	// ******************** Android M later permission request Start ********************
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
	// ******************** Android M later permission request End **********************

	// ******************** Kii Cloud Control Start ********************
	private void loginWithStoredCredentials() {
		kiiManager = KiiManager.getInstance();
		kiiManager.kiiInit(this, getString(R.string.kii_app_id), getString(R.string.kii_app_key), Kii.Site.JP);
		kiiManager.loginWithStoredCredentials(new KiiManager.OnFinishActionListener() {
			@Override
			public void onSuccess(JSONObject data) {
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
		kiiManager.allQueryObjectData(BUCKET_NAME, new KiiManager.OnFinishActionListener() {
					@Override
					public void onSuccess(JSONObject data) {
						try {
							JSONArray memoDataArray = data.getJSONArray(KiiManager.DATA_QUERY_DATA);
							memoDataSet.clear();
							for (int i = 0; i < memoDataArray.length(); i++) {
								JSONObject object = memoDataArray.getJSONObject(i).getJSONObject(KiiManager.DATA_OBJECT_DATA);
								if (!object.has(OBJECT_KEY_MEMO)) {
									continue;
								}

								memoDataSet.add(object.getString(OBJECT_KEY_MEMO));
							}

							memoRecyclerAdapter.notifyDataSetChanged();
						} catch (JSONException e) {
						}
					}

					@Override
					public void onFail(Exception e) {
					}
				}
		);
	}

	// オブジェクト追加
	public void addObject(String key, String value) {

		try {
			JSONObject object = new JSONObject();
			object.put(key, value);

			kiiManager.putObjectData(BUCKET_NAME, object, new KiiManager.OnFinishActionListener() {
				@Override
				public void onSuccess(JSONObject data) {
					getBucketData();
				}

				@Override
				public void onFail(Exception e) {
				}
			});
		} catch (JSONException e) {
		}
	}

	// バケット内全てのデータ削除
	public void allObjectDelete() {
		kiiManager.allQueryObjectData(BUCKET_NAME, new KiiManager.OnFinishActionListener() {
			@Override
			public void onSuccess(JSONObject data) {
				try {
					JSONArray memoDataArray = null;
					memoDataArray = data.getJSONArray(KiiManager.DATA_QUERY_DATA);
					memoDataSet.clear();
					for (int i = 0; i < memoDataArray.length(); i++) {
						JSONObject object = memoDataArray.getJSONObject(i);

						if (i != (memoDataArray.length() - 1)) {
							kiiManager.deleteObjectData(Uri.parse(object.getString(KiiManager.DATA_OBJECT_URI)), null);
						} else {
							kiiManager.deleteObjectData(Uri.parse(object.getString(KiiManager.DATA_OBJECT_URI)), new KiiManager.OnFinishActionListener() {
								@Override
								public void onSuccess(JSONObject data) {
									getBucketData();
								}

								@Override
								public void onFail(Exception e) {
									getBucketData();
								}
							});
						}
					}
				} catch (JSONException e) {
				}
			}

			@Override
			public void onFail(Exception e) {

			}
		});
	}
	// ******************** Kii Cloud Control End **********************

	public void onAddFABClick(View v) {
		addObject(OBJECT_KEY_MEMO, "memo");
	}
}
