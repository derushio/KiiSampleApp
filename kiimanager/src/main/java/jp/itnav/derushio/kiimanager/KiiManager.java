package jp.itnav.derushio.kiimanager;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import com.kii.cloud.storage.Kii;
import com.kii.cloud.storage.KiiObject;
import com.kii.cloud.storage.KiiUser;
import com.kii.cloud.storage.query.KiiQuery;
import com.kii.cloud.storage.query.KiiQueryResult;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * Created by derushio on 2015/12/08.
 * Kii管理用 シングルトンオブジェクト
 * 通信はサブスレッドで実行され、ハンドラはUIスレッドで帰ってきます。
 */
public class KiiManager {
	// シングルトンインスタンス
	private static KiiManager instance;

	// uiThreadで実行されるハンドラ
	private Handler uiThreadHandler;

	// コンストラクタを閉じる
	private KiiManager() {
		this.uiThreadHandler = new Handler(Looper.getMainLooper());
	}

	// シングルトンインスタンスを取得
	public static KiiManager getInstance() {
		if (instance == null) {
			instance = new KiiManager();
		}

		return instance;
	}

	// ログイン済みか
	public boolean isLoggedIn() {
		return KiiUser.isLoggedIn();
	}

	// Kii初期化アプリケーション開始時に必ず呼び出してください
	public static void kiiInit(Context context, String appId, String appKey, Kii.Site serverSite) {
		Kii.initialize(context, appId, appKey, serverSite);
	}

	public static void kiiInit(Context context, String appId, String appKey, String serverUrl) {
		Kii.initialize(context, appId, appKey, serverUrl);
	}

	// サインアップ
	public void signup(final String username, final String password, final OnFinishActionListener onFinishActionListener) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					KiiUser.Builder builder = KiiUser.builderWithName(username);
					KiiUser user = builder.build();

					user.register(password);
					returnOnUIThread(onFinishActionListener, true, null, null);
				} catch (Exception e) {
					returnOnUIThread(onFinishActionListener, false, null, e);
				}
			}
		}).start();
	}

	// 保存情報からログインを実行戻り値で成功失敗を返す
	public void loginWithStoredCredentials(final OnFinishActionListener onFinishActionListener) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					KiiUser user = KiiUser.loginWithStoredCredentials();
					user.refresh();
					returnOnUIThread(onFinishActionListener, true, null, null);
				} catch (Exception e) {
					// ログイン失敗
					returnOnUIThread(onFinishActionListener, false, null, e);
				}
			}
		}).start();
	}

	// ユーザーネームとパスワードからログイン
	public void login(final String username, final String password, final OnFinishActionListener onFinishActionListener) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					KiiUser.logIn(username, password);
					returnOnUIThread(onFinishActionListener, true, null, null);
				} catch (Exception e) {
					// ログイン失敗
				}
			}
		}).start();
	}

	// バケットにデータをプットする
	// data{"objectUri" : objectUri}
	public void putObjectData(final String bucketName, final JSONObject objectData, final OnFinishActionListener onFinishActionListener) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					KiiObject object = Kii.user().bucket(bucketName).object();
					object.set("objectData", objectData);
					object.save();

					JSONObject data = new JSONObject();
					data.put("objectUri", object.toUri().toString());
					returnOnUIThread(onFinishActionListener, true, data, null);
				} catch (Exception e) {
					returnOnUIThread(onFinishActionListener, false, null, e);
				}
			}
		}).start();
	}

	// オブジェクトデータをゲットする
	// data{"objectData" : objectData}
	public void getObjectData(final Uri objectUri, final OnFinishActionListener onFinishActionListener) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					KiiObject object = KiiObject.createByUri(objectUri);
					JSONObject data = object.toJSON();

					returnOnUIThread(onFinishActionListener, true, data, null);
				} catch (Exception e) {
					returnOnUIThread(onFinishActionListener, false, null, e);
				}
			}
		}).start();
	}

	// バケット内のすべてのデータをゲットする
	// data{objectUri :{ "queryData" : [objectData1, objectData2, ...]}}
	public void allQueryObjectData(final String bucketName, final OnFinishActionListener onFinishActionListener) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					KiiQuery allQuery = new KiiQuery();
					KiiQueryResult<KiiObject> result = Kii.user().bucket(bucketName).query(allQuery);

					JSONArray queryData = new JSONArray();
					List<KiiObject> objectList = result.getResult();
					for (KiiObject object : objectList) {
						queryData.put(object.toJSON());
					}

					JSONObject data = new JSONObject();
					data.put("queryData", queryData);

					returnOnUIThread(onFinishActionListener, true, data, null);
				} catch (Exception e) {
					returnOnUIThread(onFinishActionListener, false, null, e);
				}
			}
		}).start();
	}

	// オブジェクトを削除する
	public void deleteObjectData(final Uri objectUri, final OnFinishActionListener onFinishActionListener) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					KiiObject object = KiiObject.createByUri(objectUri);
					object.delete();

					returnOnUIThread(onFinishActionListener, true, null, null);
				} catch (Exception e) {
					returnOnUIThread(onFinishActionListener, false, null, e);
				}
			}
		}).start();
	}

	// UIThreadでリスナを呼ぶ（終了後にUI部分の変更を加えることがあるため）
	private void returnOnUIThread(final OnFinishActionListener onFinishActionListener, final boolean success, final JSONObject data, final Exception e) {
		if (onFinishActionListener == null) {
			return;
		}

		uiThreadHandler.post(new Runnable() {
			@Override
			public void run() {
				if (success) {
					onFinishActionListener.onSuccess(data);
				} else {
					onFinishActionListener.onFail(e);
				}
			}
		});
	}

	// KiiManagerのアクションが終了したら呼ばれるリスナ
	public interface OnFinishActionListener {

		void onSuccess(JSONObject data);

		void onFail(Exception e);
	}
}
