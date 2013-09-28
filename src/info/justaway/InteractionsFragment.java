package info.justaway;

import twitter4j.ResponseList;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

/**
 * 将来「つながり」タブ予定のタブ、現在はリプしか表示されない
 */
public class InteractionsFragment extends BaseFragment {

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        /**
         * Streamingだけだと淋しいので、初期化時にMeationsTimelineを読み込む
         */
        new LoadMeationsTimeline().execute();
    }

    /**
     * ページ最上部だと自動的に読み込まれ、スクロールしていると動かないという美しい挙動
     */
    public void add(final Row row) {
        final ListView listView = getListView();
        if (listView == null) {
            return;
        }

        listView.post(new Runnable() {
            @Override
            public void run() {

                // 表示している要素の位置
                int position = listView.getFirstVisiblePosition();

                // 縦スクロール位置
                View view = listView.getChildAt(0);
                int y = view != null ? view.getTop() : 0;

                // 要素を上に追加（ addだと下に追加されてしまう ）
                TwitterAdapter adapter = (TwitterAdapter) listView.getAdapter();
                adapter.insert(row, 0);

                // 少しでもスクロールさせている時は画面を動かさない様にスクロー位置を復元する
                MainActivity activity = (MainActivity) getActivity();
                if (position != 0 || y != 0) {
                    listView.setSelectionFromTop(position + 1, y);
                    activity.onNewInteractions(false);
                } else {
                    activity.onNewInteractions(true);
                }
            }
        });
    }

    private class LoadMeationsTimeline extends
            AsyncTask<String, Void, ResponseList<twitter4j.Status>> {

        @Override
        protected ResponseList<twitter4j.Status> doInBackground(
                String... params) {
            try {
                MainActivity activity = (MainActivity) getActivity();
                ResponseList<twitter4j.Status> statuses = activity.getTwitter()
                        .getMentionsTimeline();
                return statuses;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(ResponseList<twitter4j.Status> statuses) {
            if (statuses != null) {
                ListView listView = getListView();
                TwitterAdapter adapter = (TwitterAdapter) listView.getAdapter();
                adapter.clear();
                for (twitter4j.Status status : statuses) {
                    adapter.add(Row.newStatus(status));
                }
            } else {
                MainActivity activity = (MainActivity) getActivity();
                activity.showToast("Meationsの取得に失敗しました＞＜");
            }
        }
    }
}