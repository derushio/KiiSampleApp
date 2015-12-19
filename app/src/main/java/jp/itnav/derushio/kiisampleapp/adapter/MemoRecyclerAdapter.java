package jp.itnav.derushio.kiisampleapp.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

import jp.itnav.derushio.kiisampleapp.R;

/**
 * Created by derushio on 2015/12/19.
 */
public class MemoRecyclerAdapter extends RecyclerView.Adapter<MemoRecyclerAdapter.MemoItemViewHolder> {

	// ******************** MemoItemViewHolder Start ********************
	public static class MemoItemViewHolder extends RecyclerView.ViewHolder {
		public TextView memoTitleText;

		public MemoItemViewHolder(View itemView) {
			super(itemView);
			memoTitleText = (TextView) itemView.findViewById(R.id.memoTitleText);
		}
	}
	// ******************** MemoItemViewHolder End **********************

	// ******************** MemoRecyclerAdapter Start ********************
	private ArrayList<String> memoDataSet;

	public MemoRecyclerAdapter(ArrayList<String> memoDataSet) {
		this.memoDataSet = memoDataSet;
	}

	@Override
	public MemoItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		View memoItemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_memo_item_view, parent, false);
		return new MemoItemViewHolder(memoItemView);
	}

	@Override
	public void onBindViewHolder(MemoItemViewHolder holder, int position) {
		holder.memoTitleText.setText(memoDataSet.get(position));
	}

	@Override
	public int getItemCount() {
		return memoDataSet.size();
	}
	// ******************** MemoRecyclerAdapter End **********************
}
