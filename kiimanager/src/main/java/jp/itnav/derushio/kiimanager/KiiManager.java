package jp.itnav.derushio.kiimanager;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.kii.cloud.storage.Kii;
import com.kii.cloud.storage.KiiObject;
import com.kii.cloud.storage.KiiUser;

import java.util.HashMap;

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
				KiiUser.Builder builder = KiiUser.builderWithName(username);
				KiiUser user = builder.build();

				try {
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

	// バケットからデータをゲットする
	public void getObjectData(final String bucketName, final String key, final OnFinishActionListener onFinishActionListener) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				KiiObject object = Kii.user().bucket(bucketName).object();
				String value = object.getString(key);
				HashMap<String, String> data = new HashMap<>();
				data.put("value", value);

				returnOnUIThread(onFinishActionListener, true, data, null);
			}
		}).start();
	}

	// バケットにデータをプットする
	public void putObjectData(final String bucketName, final String key, final String value, final OnFinishActionListener onFinishActionListener) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				KiiObject object = Kii.user().bucket(bucketName).object();
				object.set(key, value);

				try {
					object.save();
					returnOnUIThread(onFinishActionListener, true, null, null);
				} catch (Exception e) {
					returnOnUIThread(onFinishActionListener, false, null, e);
				}
			}
		}).start();
	}

	// UIThreadでリスナを呼ぶ（終了後にUI部分の変更を加えることがあるため）
	private void returnOnUIThread(final OnFinishActionListener onFinishActionListener, final boolean success, final HashMap<String, String> data, final Exception e) {
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

		void onSuccess(HashMap<String, String> data);

		void onFail(Exception e);
	}
}
