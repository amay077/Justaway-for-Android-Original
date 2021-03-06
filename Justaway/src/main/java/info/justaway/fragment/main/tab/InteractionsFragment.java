package info.justaway.fragment.main.tab;

import android.os.AsyncTask;
import android.view.View;

import java.util.ArrayList;

import info.justaway.event.model.StreamingCreateFavoriteEvent;
import info.justaway.event.model.StreamingUnFavoriteEvent;
import info.justaway.model.AccessTokenManager;
import info.justaway.model.Row;
import info.justaway.model.TabManager;
import info.justaway.model.TwitterManager;
import info.justaway.settings.BasicSettings;
import info.justaway.util.StatusUtil;
import twitter4j.Paging;
import twitter4j.ResponseList;
import twitter4j.Status;

/**
 * 将来「つながり」タブ予定のタブ、現在はリプしか表示されない
 */
public class InteractionsFragment extends BaseFragment {

    /**
     * このタブを表す固有のID、ユーザーリストで正数を使うため負数を使う
     */
    public long getTabId() {
        return TabManager.INTERACTIONS_TAB_ID;
    }

    /**
     * このタブに表示するツイートの定義
     * @param row ストリーミングAPIから受け取った情報（ツイート＋ふぁぼ）
     *            CreateFavoriteEventをキャッチしている為、ふぁぼイベントを受け取ることが出来る
     * @return trueは表示しない、falseは表示する
     */
    @Override
    protected boolean isSkip(Row row) {
        if (row.isFavorite()) {
            return row.getSource().getId() == AccessTokenManager.getUserId();
        }
        if (row.isStatus()) {

            Status status = row.getStatus();
            Status retweet = status.getRetweetedStatus();

            /**
             * 自分のツイートがRTされた時
             */
            if (retweet != null && retweet.getUser().getId() == AccessTokenManager.getUserId()) {
                return false;
            }

            /**
             * 自分宛のメンション（但し「自分をメンションに含むツイートがRTされた時」はうざいので除く）
             */
            if (retweet == null && StatusUtil.isMentionForMe(status)) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void taskExecute() {
        new MentionsTimelineTask().execute();
    }

    private class MentionsTimelineTask extends AsyncTask<Void, Void, ResponseList<Status>> {
        @Override
        protected ResponseList<twitter4j.Status> doInBackground(Void... params) {
            try {
                Paging paging = new Paging();
                if (mMaxId > 0 && !mReloading) {
                    paging.setMaxId(mMaxId - 1);
                    paging.setCount(BasicSettings.getPageCount());
                }
                return TwitterManager.getTwitter().getMentionsTimeline(paging);
            } catch (OutOfMemoryError e) {
                return null;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(ResponseList<twitter4j.Status> statuses) {
            mFooter.setVisibility(View.GONE);
            if (statuses == null || statuses.size() == 0) {
                mReloading = false;
                mPullToRefreshLayout.setRefreshComplete();
                mListView.setVisibility(View.VISIBLE);
                return;
            }
            if (mReloading) {
                clear();
                for (twitter4j.Status status : statuses) {
                    if (mMaxId <= 0L || mMaxId > status.getId()) {
                        mMaxId = status.getId();
                    }
                    mAdapter.add(Row.newStatus(status));
                }
                mReloading = false;
            } else {
                for (twitter4j.Status status : statuses) {
                    if (mMaxId <= 0L || mMaxId > status.getId()) {
                        mMaxId = status.getId();
                    }
                    mAdapter.extensionAdd(Row.newStatus(status));
                }
                mAutoLoader = true;
                mListView.setVisibility(View.VISIBLE);
            }
            mPullToRefreshLayout.setRefreshComplete();
        }
    }

    /**
     * ストリーミングAPIからふぁぼを受け取った時のイベント
     * @param event ふぁぼイベント
     */
    public void onEventMainThread(StreamingCreateFavoriteEvent event) {
        addStack(event.getRow());
    }

    /**
     * ストリーミングAPIからあんふぁぼイベントを受信
     * @param event ツイート
     */
    public void onEventMainThread(StreamingUnFavoriteEvent event) {
        ArrayList<Integer> removePositions = mAdapter.removeStatus(event.getStatus().getId());
        for (Integer removePosition : removePositions) {
            if (removePosition >= 0) {
                int visiblePosition = mListView.getFirstVisiblePosition();
                if (visiblePosition > removePosition) {
                    View view = mListView.getChildAt(0);
                    int y = view != null ? view.getTop() : 0;
                    mListView.setSelectionFromTop(visiblePosition - 1, y);
                    break;
                }
            }
        }
    }
}
